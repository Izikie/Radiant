package net.radiant.render.vulkan;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanCelestialPipeline implements AutoCloseable {
    private static final int STRIDE = 5 * Float.BYTES;
    private static final int QUAD_BYTES = 4 * STRIDE;
    private static final int STAGES = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;

    static final class Body {
        final int slot;
        boolean visible;
        private final byte[] vertices = new byte[QUAD_BYTES];
        private Matrix4f transform;
        private final Vector4f color = new Vector4f();
        private int textureId = -1;
        private VulkanTextureBindings.Image image;

        Body(boolean sun) {
            slot = sun ? 0 : 1;
        }

        void invalidateTexture() {
            textureId = -1;
        }

        void capture(ByteBuffer quad) {
            int id = glGetInteger(GL_TEXTURE_BINDING_2D);
            if (id != textureId) {
                image = VulkanTextureBindings.Image.capture();
                textureId = id;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer floats = stack.mallocFloat(16);
                glGetFloatv(GL_MODELVIEW_MATRIX, floats);
                transform = VulkanSkyBackground.projectionModelView(floats);
                glGetFloatv(GL_CURRENT_COLOR, floats);
                color.set(floats);
            }
            // The strip 0,1,3,2 shares the diagonal of the legacy OpenGL quad.
            quad.get(0, vertices, 0, 2 * STRIDE);
            quad.get(3 * STRIDE, vertices, 2 * STRIDE, STRIDE);
            quad.get(2 * STRIDE, vertices, 3 * STRIDE, STRIDE);
            visible = true;
        }
    }

    private final VkDevice device;
    private final VulkanTextureBindings bindings;
    private VulkanBuffer vertices;
    private long layout, pipeline;

    VulkanCelestialPipeline(VkDevice device) {
        this.device = device;
        bindings = new VulkanTextureBindings(device, 2);
    }

    void initialize(VkPhysicalDevice gpu, int format, int depthFormat) {
        vertices = VulkanBuffer.mapped(gpu, device, 2 * QUAD_BYTES, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            bindings.initialize(stack);
            LongBuffer handle = stack.mallocLong(1);
            var range = VkPushConstantRange.calloc(1, stack);
            range.get(0).size(80).stageFlags(STAGES);
            VulkanPresenter.check(vkCreatePipelineLayout(device, VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                    .pSetLayouts(stack.longs(bindings.layout())).pPushConstantRanges(range), null, handle), "create celestial pipeline layout");
            layout = handle.get(0);
            var stream = VkVertexInputBindingDescription.calloc(1, stack);
            stream.get(0).stride(STRIDE);
            var attributes = VkVertexInputAttributeDescription.calloc(2, stack);
            attributes.get(0).location(0).format(VK_FORMAT_R32G32B32_SFLOAT);
            attributes.get(1).location(1).format(VK_FORMAT_R32G32_SFLOAT).offset(12);
            var inputs = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default()
                    .pVertexBindingDescriptions(stream).pVertexAttributeDescriptions(attributes);
            String header = "#version 450\n#define OPENGL_CLIP_DEPTH " + (device.getCapabilities().VK_EXT_depth_clip_control ? "1\n" : "0\n")
                    + "layout(push_constant) uniform Body { mat4 transform; vec4 color; } body;\n";
            pipeline = VulkanGraphics.pipeline(stack, device, layout, format, depthFormat, VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP,
                    VulkanGraphics.Blend.ADDITIVE, false, inputs, header + """
                    layout(location=0) in vec3 position;
                    layout(location=1) in vec2 texCoord;
                    layout(location=0) out vec2 uv;
                    void main() {
                        uv = texCoord;
                        vec4 clip = body.transform * vec4(position, 1);
                        #if !OPENGL_CLIP_DEPTH
                        clip.z = (clip.z + clip.w) * 0.5;
                        #endif
                        gl_Position = clip;
                    }
                    """, header + """
                    layout(binding=0) uniform sampler2D celestial;
                    layout(location=0) in vec2 uv;
                    layout(location=0) out vec4 color;
                    void main() { color = texture(celestial, uv) * body.color; }
                    """);
        }
    }

    void prepare(VkPhysicalDevice gpu, VkQueue queue, VkCommandBuffer commands, Body body) {
        if (!body.visible) return;
        int slot = body.slot;
        vertices.data().put(slot * QUAD_BYTES, body.vertices);
        bindings.prepare(gpu, queue, commands, slot, body.image);
    }

    void draw(MemoryStack stack, VkCommandBuffer commands, Body body) {
        if (!body.visible) return;
        ByteBuffer constants = stack.malloc(80);
        body.transform.get(0, constants);
        body.color.get(64, constants);
        vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        vkCmdPushConstants(commands, layout, STAGES, 0, constants);
        bindings.bind(stack, commands, layout, body.slot);
        vkCmdBindVertexBuffers(commands, 0, stack.longs(vertices.handle()), stack.longs(body.slot * QUAD_BYTES));
        vkCmdDraw(commands, 4, 1, 0, 0);
    }

    @Override
    public void close() {
        if (pipeline != 0) vkDestroyPipeline(device, pipeline, null);
        if (layout != 0) vkDestroyPipelineLayout(device, layout, null);
        bindings.close();
        if (vertices != null) vertices.close();
    }
}
