package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

final class VulkanTerrainPipeline implements AutoCloseable {
    private static final int FRAME_BYTES = 32 + 65 * 16;
    private static final int QUADS_PER_DRAW = 16_384;
    private static final String FRAME_LAYOUT = """
            layout(std140, set = 0, binding = 1) uniform Frame {
                vec4 fogColor;
                vec4 fog;
                vec4 mipWeights[65];
            } frame;
            layout(push_constant) uniform Draw {
                mat4 transform;
                vec4 parameters;
            } section;
            """;
    private static final String VERTEX_SHADER = FRAME_LAYOUT + """
            layout(location = 0) in vec3 position;
            layout(location = 1) in vec4 blockColor;
            layout(location = 2) in vec2 atlasUv;
            layout(location = 3) in ivec2 lightUv;
            layout(location = 0) out vec4 tint;
            layout(location = 1) out vec2 uv;
            layout(location = 2) out float distanceToCamera;
            layout(location = 3) out vec2 lightCoord;

            void main() {
                vec4 clip = section.transform * vec4(position, 1.0);
                #if OPENGL_CLIP_DEPTH
                gl_Position = clip;
                #else
                gl_Position = vec4(clip.xy, (clip.z + clip.w) * 0.5, clip.w);
                #endif
                distanceToCamera = abs(clip.w);
                tint = blockColor;
                uv = atlasUv;
                // Lightmap coordinates are in sixteenths of a texel, relative to its center.
                lightCoord = (vec2(lightUv) + 8.0) / 256.0;
            }
            """;
    private static final String FRAGMENT_SHADER = FRAME_LAYOUT + """
            layout(set = 0, binding = 0) uniform sampler2D atlas;
            layout(location = 0) in vec4 tint;
            layout(location = 1) in vec2 uv;
            layout(location = 2) in float distanceToCamera;
            layout(location = 3) in vec2 lightCoord;
            layout(location = 0) out vec4 color;

            layout(set = 0, binding = 2) uniform sampler2D lightmap;
            void main() {
                float layer = section.parameters.x;
                float lod = textureQueryLod(atlas, uv).x;
                float index = fract(lod) * 256.0;
                int lo = int(index);
                int hi = lo + 1;
                float weight = mix(frame.mipWeights[lo / 4][lo % 4], frame.mipWeights[hi / 4][hi % 4], fract(index));
                vec4 texel = textureLod(atlas, uv, section.parameters.y == 0.0 ? 0.0 : floor(lod) + weight);
                vec4 lit = texel * tint;
                if (layer != 0.0 && lit.a <= 0.1) discard;
                lit.rgb *= texture(lightmap, lightCoord).rgb;
                float visibility = 1.0;
                if (frame.fog.x == 1.0) {
                    visibility = (frame.fog.z - distanceToCamera) / max(frame.fog.z - frame.fog.y, 0.0001);
                } else if (frame.fog.x == 2.0) {
                    visibility = exp(-distanceToCamera * frame.fog.w);
                } else if (frame.fog.x == 3.0) {
                    float density = distanceToCamera * frame.fog.w;
                    visibility = exp(-density * density);
                }
                vec3 rgb = mix(frame.fogColor.rgb, lit.rgb, clamp(visibility, 0.0, 1.0));
                #if SRGB_TARGET
                rgb = mix(rgb / 12.92, pow((rgb + 0.055) / 1.055, vec3(2.4)), greaterThan(rgb, vec3(0.04045)));
                #endif
                color = vec4(rgb, layer == 3.0 ? lit.a : 1.0);
            }
            """;
    private static final int DRAW_STAGES = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;

    private final VkDevice device;
    private VulkanBuffer frameBuffer;
    private VulkanBuffer indices;
    private long descriptorLayout;
    private long descriptorPool;
    private long descriptorSet;
    private long layout;
    private long opaquePipeline;
    private long translucentPipeline;

    VulkanTerrainPipeline(VkDevice device) {
        this.device = device;
    }

    void initialize(VkPhysicalDevice physicalDevice, int colorFormat, int depthFormat) {
        frameBuffer = VulkanBuffer.mapped(physicalDevice, device, FRAME_BYTES, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
        indices = VulkanBuffer.mapped(physicalDevice, device, QUADS_PER_DRAW * 6 * Integer.BYTES, VK_BUFFER_USAGE_INDEX_BUFFER_BIT);
        var indexData = indices.data().asIntBuffer();
        for (int quad = 0; quad < QUADS_PER_DRAW; quad++) {
            int base = quad * 4;
            // Match the OpenGL reference diagonal so smooth lighting interpolates across the same triangles.
            indexData.put(base).put(base + 1).put(base + 3);
            indexData.put(base + 1).put(base + 2).put(base + 3);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            createDescriptors(stack);
            VkPushConstantRange.Buffer constants = VkPushConstantRange.calloc(1, stack);
            constants.get(0).stageFlags(DRAW_STAGES).size(80);
            VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                    .pSetLayouts(stack.longs(descriptorLayout)).pPushConstantRanges(constants);
            LongBuffer handle = stack.mallocLong(1);
            VulkanPresenter.check(vkCreatePipelineLayout(device, layoutInfo, null, handle), "create terrain pipeline layout");
            layout = handle.get(0);

            VkVertexInputBindingDescription.Buffer bindings = VkVertexInputBindingDescription.calloc(1, stack);
            bindings.get(0).stride(VulkanTerrainMeshes.STRIDE).inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
            VkVertexInputAttributeDescription.Buffer attributes = VkVertexInputAttributeDescription.calloc(4, stack);
            attributes.get(0).location(0).format(VK_FORMAT_R32G32B32_SFLOAT);
            attributes.get(1).location(1).format(VK_FORMAT_R8G8B8A8_UNORM).offset(12);
            attributes.get(2).location(2).format(VK_FORMAT_R32G32_SFLOAT).offset(16);
            attributes.get(3).location(3).format(VK_FORMAT_R16G16_SINT).offset(24);
            VkPipelineVertexInputStateCreateInfo vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default()
                    .pVertexBindingDescriptions(bindings).pVertexAttributeDescriptions(attributes);
            String vertex = "#version 450\n#define OPENGL_CLIP_DEPTH " +
                    (device.getCapabilities().VK_EXT_depth_clip_control ? "1\n" : "0\n") + VERTEX_SHADER;
            String fragment = "#version 450\n#define SRGB_TARGET " + ((colorFormat == VK_FORMAT_R8G8B8A8_SRGB || colorFormat == VK_FORMAT_B8G8R8A8_SRGB) ? "1\n" : "0\n") + FRAGMENT_SHADER;
            opaquePipeline = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, false, vertexInput, vertex, fragment);
            translucentPipeline = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, true, vertexInput, vertex, fragment);
        }
    }

    private void createDescriptors(MemoryStack stack) {
        LongBuffer handle = stack.mallocLong(1);
        VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(3, stack);
        bindings.get(0).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1)
                .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
        bindings.get(2).binding(2).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1)
                .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
        bindings.get(1).binding(1).descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER).descriptorCount(1).stageFlags(DRAW_STAGES);
        VkDescriptorSetLayoutCreateInfo descriptorInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default().pBindings(bindings);
        VulkanPresenter.check(vkCreateDescriptorSetLayout(device, descriptorInfo, null, handle), "create terrain descriptor layout");
        descriptorLayout = handle.get(0);
        VkDescriptorPoolSize.Buffer sizes = VkDescriptorPoolSize.calloc(2, stack);
        sizes.get(0).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(2);
        sizes.get(1).type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER).descriptorCount(1);
        VkDescriptorPoolCreateInfo pool = VkDescriptorPoolCreateInfo.calloc(stack).sType$Default().pPoolSizes(sizes).maxSets(1);
        VulkanPresenter.check(vkCreateDescriptorPool(device, pool, null, handle), "create terrain descriptor pool");
        descriptorPool = handle.get(0);
        VkDescriptorSetAllocateInfo allocate = VkDescriptorSetAllocateInfo.calloc(stack).sType$Default()
                .descriptorPool(descriptorPool).pSetLayouts(stack.longs(descriptorLayout));
        VulkanPresenter.check(vkAllocateDescriptorSets(device, allocate, handle), "allocate terrain descriptor set");
        descriptorSet = handle.get(0);
    }

    void setMipmapFilter(float[] weights) {
        ByteBuffer uniforms = frameBuffer.data();
        for (int i = 0; i < weights.length; i++) {
            uniforms.putFloat(32 + i * Float.BYTES, weights[i]);
        }
    }

    void bindTextures(VulkanTexture texture, VulkanTexture lightmap) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorImageInfo.Buffer image = VkDescriptorImageInfo.calloc(1, stack);
            image.get(0).imageView(texture.view()).sampler(texture.sampler()).imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            VkDescriptorBufferInfo.Buffer uniforms = VkDescriptorBufferInfo.calloc(1, stack);
            uniforms.get(0).buffer(frameBuffer.handle()).range(FRAME_BYTES);
            VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(3, stack);
            writes.get(0).sType$Default().dstSet(descriptorSet).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1).pImageInfo(image);
            writes.get(1).sType$Default().dstSet(descriptorSet).dstBinding(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER).descriptorCount(1).pBufferInfo(uniforms);
            VkDescriptorImageInfo.Buffer lighting = VkDescriptorImageInfo.calloc(1, stack);
            lighting.get(0).imageView(lightmap.view()).sampler(lightmap.sampler()).imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            writes.get(2).sType$Default().dstSet(descriptorSet).dstBinding(2).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1).pImageInfo(lighting);
            vkUpdateDescriptorSets(device, writes, null);
        }
    }

    void updateFrame(VulkanTerrainScene.Fog fog) {
        ByteBuffer data = frameBuffer.data();
        data.position(0);
        data.putFloat(fog.red()).putFloat(fog.green()).putFloat(fog.blue()).putFloat(1);
        data.putFloat(fog.mode()).putFloat(fog.start()).putFloat(fog.end()).putFloat(fog.density());
    }

    void begin(MemoryStack stack, VkCommandBuffer commands) {
        vkCmdBindDescriptorSets(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, layout, 0, stack.longs(descriptorSet), null);
        vkCmdBindIndexBuffer(commands, indices.handle(), 0, VK_INDEX_TYPE_UINT32);
    }

    int draw(MemoryStack stack, VkCommandBuffer commands, List<VulkanTerrainScene.Draw> draws, boolean translucent) {
        if (draws.isEmpty()) {
            return 0;
        }
        vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, translucent ? translucentPipeline : opaquePipeline);
        FloatBuffer constants = stack.callocFloat(20);
        LongBuffer buffer = stack.mallocLong(1);
        LongBuffer offset = stack.mallocLong(1);
        int calls = 0;
        for (VulkanTerrainScene.Draw draw : draws) {
            VulkanTerrainMeshes.Slice mesh = draw.mesh();
            draw.transform().get(0, constants);
            constants.put(16, draw.layer()).put(17, draw.mipmapped() ? 1 : 0);
            vkCmdPushConstants(commands, layout, DRAW_STAGES, 0, constants);
            buffer.put(0, mesh.buffer());
            for (int base = 0; base < mesh.quads(); base += QUADS_PER_DRAW) {
                offset.put(0, mesh.offset() + (long) base * 4 * VulkanTerrainMeshes.STRIDE);
                vkCmdBindVertexBuffers(commands, 0, buffer, offset);
                vkCmdDrawIndexed(commands, Math.min(QUADS_PER_DRAW, mesh.quads() - base) * 6, 1, 0, 0, 0);
                calls++;
            }
        }
        return calls;
    }

    @Override
    public void close() {
        if (translucentPipeline != 0) vkDestroyPipeline(device, translucentPipeline, null);
        if (opaquePipeline != 0) vkDestroyPipeline(device, opaquePipeline, null);
        if (layout != 0) vkDestroyPipelineLayout(device, layout, null);
        if (descriptorPool != 0) vkDestroyDescriptorPool(device, descriptorPool, null);
        if (descriptorLayout != 0) vkDestroyDescriptorSetLayout(device, descriptorLayout, null);
        if (indices != null) indices.close();
        if (frameBuffer != null) frameBuffer.close();
    }
}
