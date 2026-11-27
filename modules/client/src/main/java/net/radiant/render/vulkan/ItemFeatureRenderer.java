package net.radiant.render.vulkan;

import net.minecraft.client.renderer.OpenGlHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL;
import static org.lwjgl.vulkan.VK10.*;

/** Executes ordered baked item model submissions using Radiant's legacy item lighting. */
final class ItemFeatureRenderer implements AutoCloseable {
    private static final int WORDS_PER_VERTEX = 7;
    private static final int STAGES = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;
    private static final int CONSTANTS = VulkanFogState.BYTES + 2 * Float.BYTES;

    private record Submit(VulkanFogState pose, float blockLight, float skyLight, int[] quads, boolean glint) {}

    static final class Batch {
        private final List<Submit> models = new ArrayList<>();
        private int bytes;

        int vertexCount() {
            return bytes / (WORDS_PER_VERTEX * Integer.BYTES);
        }

        boolean isEmpty() {
            return models.isEmpty();
        }

        void clear() {
            models.clear();
            bytes = 0;
        }

    }

    static final class Submissions {
        final Batch drops = new Batch();
        final Batch pickups = new Batch();
        private VulkanTextureBindings.Image glintTexture;
        private int glintId = -1;

        boolean isEmpty() {
            return drops.isEmpty() && pickups.isEmpty();
        }

        void clear() {
            drops.clear();
            pickups.clear();
        }

        void invalidateGlint() {
            glintId = -1;
            glintTexture = null;
        }

        void submit(Batch batch, int[] quads, boolean glint) {
            if (glint) {
                int id = glGetInteger(GL_TEXTURE_BINDING_2D);
                if (id != glintId) {
                    glintTexture = VulkanTextureBindings.Image.capture();
                    glintId = id;
                }
                transformGlintUv(quads);
            }
            prepareLighting(quads);
            var pose = new VulkanFogState();
            pose.capture();
            batch.models.add(new Submit(pose, OpenGlHelper.lastBrightnessX, OpenGlHelper.lastBrightnessY, quads, glint));
            batch.bytes += quads.length * Integer.BYTES;
        }
    }

    private final VkDevice device;
    private final long layout, pipeline, glintPipeline;
    private final VulkanTextureBindings textures;
    private VulkanBuffer vertices;
    private int capacity;

    ItemFeatureRenderer(VkDevice device, int colorFormat, int depthFormat, long frameLayout) {
        this.device = device;
        textures = new VulkanTextureBindings(device, 1);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            textures.initialize(stack);
            var range = VkPushConstantRange.calloc(1, stack);
            range.get(0).size(CONSTANTS).stageFlags(STAGES);
            var handle = stack.mallocLong(1);
            VulkanPresenter.check(vkCreatePipelineLayout(device, VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                    .pSetLayouts(stack.longs(frameLayout, textures.layout())).pPushConstantRanges(range), null, handle), "create item layout");
            layout = handle.get(0);

            var stream = VkVertexInputBindingDescription.calloc(1, stack);
            stream.get(0).stride(WORDS_PER_VERTEX * Integer.BYTES);
            var attrs = VkVertexInputAttributeDescription.calloc(4, stack);
            attrs.get(0).location(0).format(VK_FORMAT_R32G32B32_SFLOAT);
            attrs.get(1).location(1).offset(12).format(VK_FORMAT_R8G8B8A8_UNORM);
            attrs.get(2).location(2).offset(16).format(VK_FORMAT_R32G32_SFLOAT);
            attrs.get(3).location(3).offset(24).format(VK_FORMAT_R32_SFLOAT);
            var inputs = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default()
                    .pVertexBindingDescriptions(stream).pVertexAttributeDescriptions(attrs);
            String header = VulkanFogState.shaderHeader(device, "vec2 lightUv;\n");
            String vertex = header + """
                    layout(location=0) in vec3 position;
                    layout(location=1) in vec4 vertexColor;
                    layout(location=2) in vec2 texCoord;
                    layout(location=3) in float diffuse;
                    layout(location=0) out vec2 uv;
                    layout(location=1) flat out vec4 tint;
                    layout(location=2) out float eyeZ;
                    void main() {
                        uv = texCoord;
                        tint = vec4(clamp(vertexColor.rgb * diffuse, 0.0, 1.0), vertexColor.a);
                        vec4 p = vec4(position, 1);
                        eyeZ = dot(draw.eyeZ, p);
                        vec4 clip = draw.transform * p;
                        #if !OPENGL_CLIP_DEPTH
                        clip.z = (clip.z + clip.w) * 0.5;
                        #endif
                        gl_Position = clip;
                    }
                    """;
            String fragment = VulkanFogState.FRAGMENT_FOG + """
                    layout(set=0,binding=2) uniform sampler2D lightmap;
                    layout(location=0) in vec2 uv;
                    layout(location=1) flat in vec4 tint;
                    layout(location=2) in float eyeZ;
                    layout(location=0) out vec4 color;
                    void main() {
                        color = textureLod(itemTexture, uv, 0.0) * tint;
                        if (color.a <= draw.fogColorAndAlphaRef.a) discard;
                        color.rgb *= texture(lightmap, (draw.lightUv + 8.0) / 256.0).rgb;
                        color.rgb = applyFog(color.rgb, eyeZ);
                    }
                    """;
            pipeline = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Blend.TRANSLUCENT, true, inputs, vertex,
                    header + "layout(set=0,binding=0) uniform sampler2D itemTexture;\n" + fragment);
            glintPipeline = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Blend.ITEM_GLINT, false, inputs, vertex,
                    header + "layout(set=1,binding=0) uniform sampler2D itemTexture;\n" + fragment);
        }
    }

    private static void transformGlintUv(int[] vertices) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer values = stack.mallocFloat(16);
            glGetFloatv(GL_TEXTURE_MATRIX, values);
            Matrix4f textureMatrix = new Matrix4f().set(values);
            Vector4f uv = new Vector4f();
            for (int i = 4; i < vertices.length; i += WORDS_PER_VERTEX) {
                uv.set(Float.intBitsToFloat(vertices[i]), Float.intBitsToFloat(vertices[i + 1]), 0, 1);
                textureMatrix.transform(uv);
                vertices[i] = Float.floatToRawIntBits(uv.x);
                vertices[i + 1] = Float.floatToRawIntBits(uv.y);
            }
        }
    }

    private static void prepareLighting(int[] vertices) {
        if (!glIsEnabled(GL_LIGHTING)) {
            for (int i = 6; i < vertices.length; i += WORDS_PER_VERTEX) vertices[i] = Float.floatToRawIntBits(1);
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer values = stack.mallocFloat(16);
            glGetFloatv(GL_MODELVIEW_MATRIX, values);
            Matrix4f inverse = new Matrix4f().set(values).invert();
            float rescale = 1;
            if (glIsEnabled(GL_RESCALE_NORMAL)) {
                rescale = 1 / new Vector3f(inverse.m02(), inverse.m12(), inverse.m22()).length();
            }
            boolean normalize = glIsEnabled(GL_NORMALIZE);
            inverse.transpose();
            glGetLightfv(GL_LIGHT0, GL_POSITION, values);
            Vector3f light0 = new Vector3f(values.get(0), values.get(1), values.get(2));
            glGetLightfv(GL_LIGHT1, GL_POSITION, values);
            Vector3f light1 = new Vector3f(values.get(0), values.get(1), values.get(2));
            Vector3f normal = new Vector3f();
            for (int i = 6; i < vertices.length; i += WORDS_PER_VERTEX) {
                int packed = vertices[i];
                normal.set((byte) packed / 127.0F, (byte) (packed >>> 8) / 127.0F, (byte) (packed >>> 16) / 127.0F);
                inverse.transformDirection(normal).mul(rescale);
                if (normalize) normal.normalize();
                float diffuse = 0.4F + 0.6F * Math.max(0, normal.dot(light0)) + 0.6F * Math.max(0, normal.dot(light1));
                vertices[i] = Float.floatToRawIntBits(diffuse);
            }
        }
    }

    void prepare(VkPhysicalDevice gpu, VkQueue queue, VkCommandBuffer commands, Submissions submissions) {
        if (submissions.glintTexture != null) textures.prepare(gpu, queue, commands, 0, submissions.glintTexture);
        int bytes = submissions.drops.bytes + submissions.pickups.bytes;
        if (capacity < bytes) {
            var replacement = VulkanBuffer.mapped(gpu, device, bytes, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
            if (vertices != null) vertices.close();
            vertices = replacement;
            capacity = bytes;
        }
        var target = vertices.data().asIntBuffer();
        for (Submit model : submissions.drops.models) target.put(model.quads);
        for (Submit model : submissions.pickups.models) target.put(model.quads);
    }

    void draw(MemoryStack stack, VkCommandBuffer commands, VulkanTerrainPipeline terrain, VulkanBuffer indices, Batch batch, int firstVertex) {
        terrain.bindFrame(stack, commands, layout);
        vkCmdBindIndexBuffer(commands, indices.handle(), 0, VK_INDEX_TYPE_UINT32);
        vkCmdBindVertexBuffers(commands, 0, stack.longs(vertices.handle()), stack.longs(0));
        ByteBuffer light = stack.malloc(2 * Float.BYTES);
        for (Submit model : batch.models) {
            vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, model.glint ? glintPipeline : pipeline);
            // Bound per-model stack use even when a frame contains many dropped stacks.
            try (MemoryStack modelStack = MemoryStack.stackPush()) {
                if (model.glint) textures.bind(modelStack, commands, layout, 0, 1);
                vkCmdPushConstants(commands, layout, STAGES, 0, model.pose.constants(modelStack));
            }
            light.putFloat(0, model.blockLight).putFloat(Float.BYTES, model.skyLight);
            vkCmdPushConstants(commands, layout, STAGES, VulkanFogState.BYTES, light);
            int quads = model.quads.length / (4 * WORDS_PER_VERTEX);
            for (int first = 0; first < quads; first += VulkanQuadIndices.CAPACITY) {
                int count = Math.min(quads - first, VulkanQuadIndices.CAPACITY);
                vkCmdDrawIndexed(commands, count * 6, 1, 0, firstVertex + first * 4, 0);
            }
            firstVertex += quads * 4;
        }
    }

    @Override
    public void close() {
        if (vertices != null) vertices.close();
        vkDestroyPipeline(device, glintPipeline, null);
        vkDestroyPipeline(device, pipeline, null);
        vkDestroyPipelineLayout(device, layout, null);
        textures.close();
    }
}
