package net.radiant.render.vulkan;

import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanWorldBorderPipeline implements AutoCloseable {
    private static final int STRIDE = 5 * Float.BYTES;
    private static final int STAGES = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;

    static final class State {
        int[] vertices;
        private final VulkanFogState draw = new VulkanFogState();
        private final Vector4f tint = new Vector4f();
        private int textureId = -1;
        private VulkanTextureBindings.Image image;

        void invalidateTexture() {
            textureId = -1;
        }

        void capture(int[] vertices) {
            this.vertices = vertices;
            draw.capture();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var color = stack.mallocFloat(4);
                glGetFloatv(GL_CURRENT_COLOR, color);
                tint.set(color);
            }
            int id = glGetInteger(GL_TEXTURE_BINDING_2D);
            if (textureId != id) {
                image = VulkanTextureBindings.Image.capture();
                textureId = id;
            }
        }
    }

    private final VkDevice device;
    private final VulkanTextureBindings texture;
    private VulkanBuffer vertices;
    private int capacity;
    private long layout, pipeline;

    VulkanWorldBorderPipeline(VkDevice device) {
        this.device = device;
        texture = new VulkanTextureBindings(device, 1);
    }

    void initialize(int format, int depthFormat) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            texture.initialize(stack);
            var range = VkPushConstantRange.calloc(1, stack);
            range.get(0).size(VulkanFogState.BYTES + 16).stageFlags(STAGES);
            var handle = stack.mallocLong(1);
            VulkanPresenter.check(vkCreatePipelineLayout(device, VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                    .pSetLayouts(stack.longs(texture.layout())).pPushConstantRanges(range), null, handle), "create world border layout");
            layout = handle.get(0);
            var stream = VkVertexInputBindingDescription.calloc(1, stack);
            stream.get(0).stride(STRIDE);
            var attrs = VkVertexInputAttributeDescription.calloc(2, stack);
            attrs.get(0).location(0).format(VK_FORMAT_R32G32B32_SFLOAT);
            attrs.get(1).location(1).offset(12).format(VK_FORMAT_R32G32_SFLOAT);
            var inputs = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default()
                    .pVertexBindingDescriptions(stream).pVertexAttributeDescriptions(attrs);
            String header = VulkanFogState.shaderHeader(device, "vec4 tint;");
            boolean emulateD24 = VulkanGraphics.emulateD24Bias(depthFormat);
            String depthOffset = emulateD24 ? "gl_FragDepth = clamp(gl_FragCoord.z - 12.0 / 16777215.0, 0.0, 1.0);" : "";
            pipeline = VulkanGraphics.pipeline(stack, device, layout, format, depthFormat, VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
                    VulkanGraphics.Blend.ADDITIVE, false, VK_CULL_MODE_NONE, VulkanGraphics.RGBA, -3, emulateD24 ? 0 : -3, inputs, header + """
                    layout(location=0) in vec3 position;
                    layout(location=1) in vec2 texCoord;
                    layout(location=0) out vec2 uv;
                    layout(location=1) out float eyeZ;
                    void main() {
                        uv = texCoord;
                        vec4 p = vec4(position, 1);
                        eyeZ = dot(draw.eyeZ, p);
                        vec4 clip = draw.transform * p;
                        #if !OPENGL_CLIP_DEPTH
                        clip.z = (clip.z + clip.w) * 0.5;
                        #endif
                        gl_Position = clip;
                    }
                    """, header + VulkanFogState.FRAGMENT_FOG + """
                    layout(binding=0) uniform sampler2D forcefield;
                    layout(location=0) in vec2 uv;
                    layout(location=1) in float eyeZ;
                    layout(location=0) out vec4 color;
                    void main() {
                        color = texture(forcefield, uv) * clamp(draw.tint, 0.0, 1.0);
                        if (color.a <= draw.fogColorAndAlphaRef.a) discard;
                        color.rgb = applyFog(color.rgb, eyeZ);
                        %s
                    }
                    """.formatted(depthOffset));
        }
    }

    void prepare(VkPhysicalDevice gpu, VkQueue queue, VkCommandBuffer commands, State border) {
        texture.prepare(gpu, queue, commands, 0, border.image);
        int bytes = border.vertices.length * Integer.BYTES;
        if (capacity < bytes) {
            var replacement = VulkanBuffer.mapped(gpu, device, bytes, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
            if (vertices != null) vertices.close();
            vertices = replacement;
            capacity = bytes;
        }
        vertices.data().asIntBuffer().put(border.vertices);
    }

    void draw(MemoryStack stack, VkCommandBuffer commands, VulkanBuffer indices, State border) {
        vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        vkCmdPushConstants(commands, layout, STAGES, 0, border.draw.constants(stack));
        vkCmdPushConstants(commands, layout, STAGES, VulkanFogState.BYTES, border.tint.get(stack.mallocFloat(4)));
        texture.bind(stack, commands, layout, 0);
        vkCmdBindVertexBuffers(commands, 0, stack.longs(vertices.handle()), stack.longs(0));
        vkCmdBindIndexBuffer(commands, indices.handle(), 0, VK_INDEX_TYPE_UINT32);
        int quads = border.vertices.length / 20;
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
        texture.close();
    }
}
