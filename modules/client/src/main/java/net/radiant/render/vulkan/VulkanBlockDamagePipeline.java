package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

final class VulkanBlockDamagePipeline implements AutoCloseable {
    private static final int STRIDE = VulkanTerrainMeshes.STRIDE;
    private static final int STAGES = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;

    static final class State {
        int[] vertices;
        private final VulkanFogState fog = new VulkanFogState();

        void capture(int[] vertices) {
            this.vertices = vertices;
            fog.capture();
        }
    }

    private final VkDevice device;
    private final VulkanTextureBindings atlas;
    private VulkanBuffer vertices;
    private int capacity;
    private long layout, pipeline;

    VulkanBlockDamagePipeline(VkDevice device) {
        this.device = device;
        atlas = new VulkanTextureBindings(device, 1);
    }

    void initialize(int format, int depthFormat) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            atlas.initialize(stack);
            var range = VkPushConstantRange.calloc(1, stack);
            range.get(0).size(VulkanFogState.BYTES).stageFlags(STAGES);
            var handle = stack.mallocLong(1);
            VulkanPresenter.check(vkCreatePipelineLayout(device, VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                    .pSetLayouts(stack.longs(atlas.layout())).pPushConstantRanges(range), null, handle), "create block damage layout");
            layout = handle.get(0);
            var stream = VkVertexInputBindingDescription.calloc(1, stack);
            stream.get(0).stride(STRIDE);
            var attrs = VkVertexInputAttributeDescription.calloc(3, stack);
            attrs.get(0).location(0).format(VK_FORMAT_R32G32B32_SFLOAT);
            attrs.get(1).location(1).offset(12).format(VK_FORMAT_R8G8B8A8_UNORM);
            attrs.get(2).location(2).offset(16).format(VK_FORMAT_R32G32_SFLOAT);
            var inputs = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default()
                    .pVertexBindingDescriptions(stream).pVertexAttributeDescriptions(attrs);
            String header = VulkanFogState.shaderHeader(device);
            boolean emulateD24 = VulkanGraphics.emulateD24Bias(depthFormat);
            String depthOffset = emulateD24 ? "gl_FragDepth = clamp(gl_FragCoord.z - 40.0 / 16777215.0, 0.0, 1.0);" : "";
            pipeline = VulkanGraphics.pipeline(stack, device, layout, format, depthFormat, VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
                    VulkanGraphics.Blend.BLOCK_DAMAGE, true, VK_CULL_MODE_BACK_BIT, VulkanGraphics.RGBA, -1, emulateD24 ? 0 : -10,
                    inputs, header + """
                    layout(location=0) in vec3 position;
                    layout(location=1) in vec4 blockColor;
                    layout(location=2) in vec2 texCoord;
                    layout(location=0) out vec2 uv;
                    layout(location=1) out float eyeZ;
                    layout(location=2) out vec4 tint;
                    void main() {
                        vec4 p = vec4(position, 1);
                        uv = texCoord;
                        eyeZ = dot(draw.eyeZ, p);
                        tint = blockColor;
                        vec4 clip = draw.transform * p;
                        #if !OPENGL_CLIP_DEPTH
                        clip.z = (clip.z + clip.w) * 0.5;
                        #endif
                        gl_Position = clip;
                    }
                    """, header + VulkanFogState.FRAGMENT_FOG + """
                    layout(binding=0) uniform sampler2D atlas;
                    layout(location=0) in vec2 uv;
                    layout(location=1) in float eyeZ;
                    layout(location=2) in vec4 tint;
                    layout(location=0) out vec4 color;
                    void main() {
                        // EntityRenderer disables the lightmap and atlas mipmapping for this pass.
                        color = textureLod(atlas, uv, 0.0) * tint;
                        if (color.a <= draw.fogColorAndAlphaRef.a) discard;
                        color.rgb = applyFog(color.rgb, eyeZ);
                        %s
                    }
                    """.formatted(depthOffset));
        }
    }

    void setAtlas(VulkanTexture texture) {
        atlas.setTexture(0, texture);
    }

    void prepare(VkPhysicalDevice gpu, State damage) {
        int bytes = Math.multiplyExact(damage.vertices.length, Integer.BYTES);
        if (capacity < bytes) {
            var replacement = VulkanBuffer.mapped(gpu, device, bytes, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
            if (vertices != null) vertices.close();
            vertices = replacement;
            capacity = bytes;
        }
        vertices.data().asIntBuffer().put(damage.vertices);
    }

    void draw(MemoryStack stack, VkCommandBuffer commands, VulkanBuffer indices, State damage) {
        vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        vkCmdPushConstants(commands, layout, STAGES, 0, damage.fog.constants(stack));
        atlas.bind(stack, commands, layout, 0);
        vkCmdBindVertexBuffers(commands, 0, stack.longs(vertices.handle()), stack.longs(0));
        vkCmdBindIndexBuffer(commands, indices.handle(), 0, VK_INDEX_TYPE_UINT32);
        int quads = damage.vertices.length / (4 * STRIDE / Integer.BYTES);
        for (int first = 0; first < quads; first += VulkanQuadIndices.CAPACITY) {
            int count = Math.min(quads - first, VulkanQuadIndices.CAPACITY);
            vkCmdDrawIndexed(commands, count * 6, 1, 0, first * 4, 0);
        }
    }

    @Override
    public void close() {
        if (vertices != null) vertices.close();
        if (pipeline != 0) vkDestroyPipeline(device, pipeline, null);
        if (layout != 0) vkDestroyPipelineLayout(device, layout, null);
        atlas.close();
    }
}
