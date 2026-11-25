package net.radiant.render.vulkan;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanCloudPipeline implements AutoCloseable {
    private static final int STRIDE = 24;
    private static final int STAGES = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;

    static final class State {
        boolean visible, afterTerrain, fancy;
        int colorPass;
        private byte[] mesh;
        private Matrix4f transform;
        private final Vector4f eyeZ = new Vector4f(), fogColor = new Vector4f(), fog = new Vector4f();
        private int textureId = -1;
        private VulkanTextureBindings.Image image;

        void invalidateTexture() {
            textureId = -1;
        }

        void capture(byte[] vertices, boolean afterTerrain, boolean fancy, int colorPass) {
            int id = glGetInteger(GL_TEXTURE_BINDING_2D);
            if (id != textureId) {
                image = VulkanTextureBindings.Image.capture();
                textureId = id;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer values = stack.mallocFloat(16);
                glGetFloatv(GL_MODELVIEW_MATRIX, values);
                eyeZ.set(values.get(2), values.get(6), values.get(10), values.get(14));
                transform = VulkanSkyBackground.projectionModelView(values);
                glGetFloatv(GL_FOG_COLOR, values);
                fogColor.set(values);
                fogColor.w = glIsEnabled(GL_ALPHA_TEST) ? glGetFloat(GL_ALPHA_TEST_REF) : -1;
                fog.set(glIsEnabled(GL_FOG) ? glGetInteger(GL_FOG_MODE) : 0,
                        glGetFloat(GL_FOG_START), glGetFloat(GL_FOG_END), glGetFloat(GL_FOG_DENSITY));
            }
            this.fancy = fancy;
            this.colorPass = colorPass;
            mesh = vertices;
            this.afterTerrain = afterTerrain;
            visible = true;
        }
    }

    private final VkDevice device;
    private final VulkanTextureBindings bindings;
    private VulkanBuffer vertices;
    private byte[] uploaded;
    private final long[] colorPipelines = new long[3];
    private long depthPipeline, layout;

    VulkanCloudPipeline(VkDevice device) {
        this.device = device;
        bindings = new VulkanTextureBindings(device, 1);
    }

    void initialize(int format, int depthFormat) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            bindings.initialize(stack);
            var handle = stack.mallocLong(1);
            var constants = VkPushConstantRange.calloc(1, stack);
            constants.get(0).size(112).stageFlags(STAGES);
            VulkanPresenter.check(vkCreatePipelineLayout(device, VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                    .pSetLayouts(stack.longs(bindings.layout())).pPushConstantRanges(constants), null, handle), "create cloud pipeline layout");
            layout = handle.get(0);
            var stream = VkVertexInputBindingDescription.calloc(1, stack);
            stream.get(0).stride(STRIDE);
            var attributes = VkVertexInputAttributeDescription.calloc(3, stack);
            attributes.get(0).location(0).format(VK_FORMAT_R32G32B32_SFLOAT);
            attributes.get(1).location(1).offset(12).format(VK_FORMAT_R32G32_SFLOAT);
            attributes.get(2).location(2).offset(20).format(VK_FORMAT_R8G8B8A8_UNORM);
            var inputs = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default()
                    .pVertexBindingDescriptions(stream).pVertexAttributeDescriptions(attributes);
            String header = "#version 450\n#define OPENGL_CLIP_DEPTH " + (device.getCapabilities().VK_EXT_depth_clip_control ? "1\n" : "0\n") + """
                    layout(push_constant) uniform Clouds {
                        mat4 transform;
                        vec4 eyeZ;
                        vec4 fogColorAndAlphaRef;
                        vec4 fog;
                    } clouds;
                    """;
            String vertex = header + """
                    layout(location=0) in vec3 position;
                    layout(location=1) in vec2 texCoord;
                    layout(location=2) in vec4 vertexColor;
                    layout(location=0) out vec2 uv;
                    layout(location=1) flat out vec4 tint;
                    layout(location=2) out float eyeZ;
                    void main() {
                        uv = texCoord;
                        tint = vertexColor;
                        vec4 p = vec4(position, 1);
                        eyeZ = dot(clouds.eyeZ, p);
                        vec4 clip = clouds.transform * p;
                        #if !OPENGL_CLIP_DEPTH
                        clip.z = (clip.z + clip.w) * 0.5;
                        #endif
                        gl_Position = clip;
                    }
                    """;
            String fragment = header + """
                    layout(binding=0) uniform sampler2D cloudTexture;
                    layout(location=0) in vec2 uv;
                    layout(location=1) flat in vec4 tint;
                    layout(location=2) in float eyeZ;
                    layout(location=0) out vec4 color;
                    void main() {
                        color = texture(cloudTexture, uv) * tint;
                        if (color.a <= clouds.fogColorAndAlphaRef.a) discard;
                        if (clouds.fog.x == 0) return;
                        float d = abs(eyeZ);
                        float f;
                        if (clouds.fog.x == 2048) f = exp(-clouds.fog.w * d);
                        else if (clouds.fog.x == 2049) f = exp(-pow(clouds.fog.w * d, 2.0));
                        else f = (clouds.fog.z - d) / max(clouds.fog.z - clouds.fog.y, 0.0001);
                        color.rgb = mix(clouds.fogColorAndAlphaRef.rgb, color.rgb, clamp(f, 0.0, 1.0));
                    }
                    """;
            int[] colorMasks = {VulkanGraphics.RGBA & ~VK_COLOR_COMPONENT_R_BIT, VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_A_BIT, VulkanGraphics.RGBA};
            for (int pass = 0; pass < colorPipelines.length; pass++) {
                colorPipelines[pass] = VulkanGraphics.pipeline(stack, device, layout, format, depthFormat, VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
                        VulkanGraphics.Blend.TRANSLUCENT, true, VK_CULL_MODE_NONE, colorMasks[pass], inputs, vertex, fragment);
            }
            depthPipeline = VulkanGraphics.pipeline(stack, device, layout, format, depthFormat, VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
                    VulkanGraphics.Blend.OPAQUE, true, VK_CULL_MODE_NONE, 0, inputs, vertex, fragment);
        }
    }

    void prepare(VkPhysicalDevice gpu, VkQueue queue, VkCommandBuffer commands, State state) {
        bindings.prepare(gpu, queue, commands, 0, state.image);
        if (uploaded != state.mesh) {
            if (vertices == null || uploaded.length != state.mesh.length) {
                var replacement = VulkanBuffer.mapped(gpu, device, state.mesh.length, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
                if (vertices != null) vertices.close();
                vertices = replacement;
            }
            vertices.data().put(state.mesh);
            uploaded = state.mesh;
        }
    }

    void draw(MemoryStack stack, VkCommandBuffer commands, VulkanBuffer indices, State state) {
        ByteBuffer constants = stack.malloc(112);
        state.transform.get(0, constants);
        state.eyeZ.get(64, constants);
        state.fogColor.get(80, constants);
        state.fog.get(96, constants);
        vkCmdPushConstants(commands, layout, STAGES, 0, constants);
        bindings.bind(stack, commands, layout, 0);
        vkCmdBindVertexBuffers(commands, 0, stack.longs(vertices.handle()), stack.longs(0));
        vkCmdBindIndexBuffer(commands, indices.handle(), 0, VK_INDEX_TYPE_UINT32);
        int indexCount = state.mesh.length / (STRIDE * 4) * 6;
        if (state.fancy) {
            vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, depthPipeline);
            vkCmdDrawIndexed(commands, indexCount, 1, 0, 0, 0);
        }
        vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, colorPipelines[state.colorPass]);
        vkCmdDrawIndexed(commands, indexCount, 1, 0, 0, 0);
    }

    @Override
    public void close() {
        for (long pipeline : colorPipelines) if (pipeline != 0) vkDestroyPipeline(device, pipeline, null);
        if (depthPipeline != 0) vkDestroyPipeline(device, depthPipeline, null);
        if (layout != 0) vkDestroyPipelineLayout(device, layout, null);
        bindings.close();
        if (vertices != null) vertices.close();
    }
}
