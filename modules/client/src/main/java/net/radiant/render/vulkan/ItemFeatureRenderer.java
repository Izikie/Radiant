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
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL;
import static org.lwjgl.vulkan.VK10.*;

/** Executes ordered item model submissions using Radiant's legacy item lighting. */
final class ItemFeatureRenderer implements AutoCloseable {
    private static final int WORDS_PER_VERTEX = 7;
    private static final int STAGES = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;
    private static final int CONSTANTS = VulkanFogState.BYTES + 2 * Float.BYTES + Integer.BYTES;

    private enum Material { ATLAS, GLINT, SPECIAL, SPECIAL_UNCULLED, SHADOW, FIRE, TEXTURED_OPAQUE, FOOTPRINT }

    private record Submit(VulkanFogState pose, float blockLight, float skyLight, int[] quads,
                          Material material, VulkanTextureBindings.Image texture, boolean mipmapped) {}

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
        final Batch litParticles = new Batch();
        private VulkanTextureBindings.Image glintTexture;
        private int glintId = -1;
        private final Map<Integer, VulkanTextureBindings.Image> textureImages = new HashMap<>();

        boolean isEmpty() {
            return drops.isEmpty() && litParticles.isEmpty();
        }

        void clear() {
            drops.clear();
            litParticles.clear();
        }

        void invalidateTextures() {
            glintId = -1;
            glintTexture = null;
            textureImages.clear();
        }

        void textureDeleted(int id) {
            textureImages.remove(id);
            if (glintId == id) {
                glintId = -1;
                glintTexture = null;
            }
        }

        void submitSpecial(Batch batch, int[] quads) {
            Material material = glIsEnabled(GL_CULL_FACE) ? Material.SPECIAL : Material.SPECIAL_UNCULLED;
            submit(batch, quads, material, boundTexture(), false);
        }

        void submitOrb(Batch batch, int[] vertices) {
            // POSITION_TEX_COLOR_NORMAL places color after UV, while ITEM places it before UV.
            for (int i = 0; i < vertices.length; i += WORDS_PER_VERTEX) {
                int color = vertices[i + 5];
                vertices[i + 5] = vertices[i + 4];
                vertices[i + 4] = vertices[i + 3];
                vertices[i + 3] = color;
            }
            submit(batch, vertices, glIsEnabled(GL_BLEND) ? Material.SPECIAL : Material.TEXTURED_OPAQUE, boundTexture(), false);
        }

        void submitExplosion(Batch batch, int[] vertices) {
            // The explosion stream places UV, color, lightmap and normal after position.
            int[] quads = new int[vertices.length / 8 * WORDS_PER_VERTEX];
            for (int src = 0, dst = 0; src < vertices.length; src += 8, dst += WORDS_PER_VERTEX) {
                System.arraycopy(vertices, src, quads, dst, 3);
                quads[dst + 3] = vertices[src + 5];
                quads[dst + 4] = vertices[src + 3];
                quads[dst + 5] = vertices[src + 4];
                quads[dst + 6] = vertices[src + 7];
            }
            Material material = glIsEnabled(GL_BLEND) ? Material.SPECIAL : Material.TEXTURED_OPAQUE;
            int blockLight = vertices[6] & 0xffff;
            int skyLight = vertices[6] >>> 16;
            submit(batch, quads, material, boundTexture(), false, false, blockLight, skyLight);
        }

        void submitFootprint(Batch batch, int[] vertices) {
            submit(batch, expandPositionTexColor(vertices), Material.FOOTPRINT, boundTexture(), false);
        }

        void submitShadow(Batch batch, int[] vertices) {
            submit(batch, expandPositionTexColor(vertices), Material.SHADOW, boundTexture(), false);
        }

        private static int[] expandPositionTexColor(int[] vertices) {
            // POSITION_TEX_COLOR and ITEM have the same position, with color before UV in ITEM.
            int[] quads = new int[vertices.length / 6 * WORDS_PER_VERTEX];
            for (int src = 0, dst = 0; src < vertices.length; src += 6, dst += WORDS_PER_VERTEX) {
                System.arraycopy(vertices, src, quads, dst, 3);
                quads[dst + 3] = vertices[src + 5];
                quads[dst + 4] = vertices[src + 3];
                quads[dst + 5] = vertices[src + 4];
            }
            return quads;
        }

        void submitArrow(Batch batch, int[] vertices) {
            Material material = glIsEnabled(GL_BLEND) ? Material.SPECIAL : Material.TEXTURED_OPAQUE;
            submit(batch, expandPositionTex(vertices), material, boundTexture(), false, true);
        }

        void submitFire(Batch batch, int[] vertices) {
            int minFilter = glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER);
            submit(batch, expandPositionTex(vertices), Material.FIRE, null, minFilter != GL_NEAREST && minFilter != GL_LINEAR);
        }

        private static int[] expandPositionTex(int[] vertices) {
            int[] quads = new int[vertices.length / 5 * WORDS_PER_VERTEX];
            for (int src = 0, dst = 0; src < vertices.length; src += 5, dst += WORDS_PER_VERTEX) {
                System.arraycopy(vertices, src, quads, dst, 3);
                quads[dst + 3] = -1;
                quads[dst + 4] = vertices[src + 3];
                quads[dst + 5] = vertices[src + 4];
            }
            return quads;
        }

        private VulkanTextureBindings.Image boundTexture() {
            int id = glGetInteger(GL_TEXTURE_BINDING_2D);
            return textureImages.computeIfAbsent(id, ignored -> VulkanTextureBindings.Image.capture());
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
            submit(batch, quads, glint ? Material.GLINT : Material.ATLAS, null, false);
        }

        private void submit(Batch batch, int[] quads, Material material, VulkanTextureBindings.Image texture, boolean mipmapped) {
            submit(batch, quads, material, texture, mipmapped, material == Material.SHADOW);
        }

        private void submit(Batch batch, int[] quads, Material material, VulkanTextureBindings.Image texture,
                            boolean mipmapped, boolean useCurrentNormal) {
            submit(batch, quads, material, texture, mipmapped, useCurrentNormal,
                    OpenGlHelper.lastBrightnessX, OpenGlHelper.lastBrightnessY);
        }

        private void submit(Batch batch, int[] quads, Material material, VulkanTextureBindings.Image texture,
                            boolean mipmapped, boolean useCurrentNormal, float blockLight, float skyLight) {
            prepareLighting(quads, useCurrentNormal);
            var pose = new VulkanFogState();
            pose.capture();
            batch.models.add(new Submit(pose, blockLight, skyLight, quads, material, texture, mipmapped));
            batch.bytes += quads.length * Integer.BYTES;
        }
    }

    private final VkDevice device;
    private final long layout, pipeline, glintPipeline, specialPipeline, specialUnculledPipeline, shadowPipeline, firePipeline, texturedOpaquePipeline, footprintPipeline;
    private final VulkanTextureBindings textures;
    private final Map<VulkanTextureBindings.Image, VulkanTextureBindings> modelTextures = new IdentityHashMap<>();
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
            String header = VulkanFogState.shaderHeader(device, "vec2 lightUv; int mipmapped;\n");
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
            String fragment = VulkanFogState.FRAGMENT_FOG + VulkanTerrainPipeline.FRAME_UNIFORMS
                    + VulkanTerrainPipeline.ATLAS_MIP_LOD + """
                    layout(set=0,binding=2) uniform sampler2D lightmap;
                    layout(location=0) in vec2 uv;
                    layout(location=1) flat in vec4 tint;
                    layout(location=2) in float eyeZ;
                    layout(location=0) out vec4 color;
                    void main() {
                        float lod = draw.mipmapped != 0 ? atlasMipLod(itemTexture, uv) : 0.0;
                        color = textureLod(itemTexture, uv, lod) * tint;
                        if (color.a <= draw.fogColorAndAlphaRef.a) discard;
                        color.rgb *= texture(lightmap, (draw.lightUv + 8.0) / 256.0).rgb;
                        color.rgb = applyFog(color.rgb, eyeZ);
                    }
                    """;
            String atlasFragment = header + "layout(set=0,binding=0) uniform sampler2D itemTexture;\n" + fragment;
            pipeline = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Blend.TRANSLUCENT, true, inputs, vertex, atlasFragment);
            firePipeline = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Blend.OPAQUE, true, inputs, vertex, atlasFragment);
            String separateTextureFragment = header + "layout(set=1,binding=0) uniform sampler2D itemTexture;\n" + fragment;
            glintPipeline = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Blend.ITEM_GLINT, false, inputs, vertex, separateTextureFragment);
            specialPipeline = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Blend.TRANSLUCENT, true, inputs, vertex, separateTextureFragment);
            specialUnculledPipeline = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Blend.TRANSLUCENT, true,
                    VK_CULL_MODE_NONE, VulkanGraphics.RGBA, inputs, vertex, separateTextureFragment);
            texturedOpaquePipeline = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Blend.OPAQUE, true, inputs, vertex, separateTextureFragment);
            footprintPipeline = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Blend.PARTICLE, true, inputs, vertex, separateTextureFragment);
            shadowPipeline = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Blend.PARTICLE, false, inputs, vertex, separateTextureFragment);
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

    private static void prepareLighting(int[] vertices, boolean useCurrentNormal) {
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
            Vector3f currentNormal = new Vector3f();
            if (useCurrentNormal) {
                glGetFloatv(GL_CURRENT_NORMAL, values);
                currentNormal.set(values.get(0), values.get(1), values.get(2));
            }
            Vector3f normal = new Vector3f();
            for (int i = 6; i < vertices.length; i += WORDS_PER_VERTEX) {
                int packed = vertices[i];
                if (useCurrentNormal) {
                    normal.set(currentNormal);
                } else {
                    normal.set((byte) packed / 127.0F, (byte) (packed >>> 8) / 127.0F, (byte) (packed >>> 16) / 127.0F);
                }
                inverse.transformDirection(normal).mul(rescale);
                if (normalize) normal.normalize();
                float diffuse = 0.4F + 0.6F * Math.max(0, normal.dot(light0)) + 0.6F * Math.max(0, normal.dot(light1));
                vertices[i] = Float.floatToRawIntBits(diffuse);
            }
        }
    }

    void prepare(VkPhysicalDevice gpu, VkQueue queue, VkCommandBuffer commands, Submissions submissions) {
        if (submissions.isEmpty() && modelTextures.isEmpty()) return;
        if (submissions.glintTexture != null) textures.prepare(gpu, queue, commands, 0, submissions.glintTexture);
        // Keep textures referenced by an already submitted draw until that frame finishes.
        var images = Collections.newSetFromMap(new IdentityHashMap<VulkanTextureBindings.Image, Boolean>());
        images.addAll(submissions.textureImages.values());
        for (Submit model : submissions.drops.models) {
            if (model.texture != null) images.add(model.texture);
        }
        for (Submit model : submissions.litParticles.models) {
            if (model.texture != null) images.add(model.texture);
        }
        modelTextures.entrySet().removeIf(entry -> {
            if (images.contains(entry.getKey())) return false;
            entry.getValue().close();
            return true;
        });
        int bytes = submissions.drops.bytes + submissions.litParticles.bytes;
        if (bytes == 0) return;
        if (capacity < bytes) {
            var replacement = VulkanBuffer.mapped(gpu, device, bytes, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
            if (vertices != null) vertices.close();
            vertices = replacement;
            capacity = bytes;
        }
        var target = vertices.data().asIntBuffer();
        for (Batch batch : List.of(submissions.drops, submissions.litParticles)) {
            for (Submit model : batch.models) {
                target.put(model.quads);
                if (model.texture != null && !modelTextures.containsKey(model.texture)) {
                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        var texture = new VulkanTextureBindings(device, 1);
                        modelTextures.put(model.texture, texture);
                        texture.initialize(stack);
                        texture.prepare(gpu, queue, commands, 0, model.texture);
                    }
                }
            }
        }
    }

    void draw(MemoryStack stack, VkCommandBuffer commands, VulkanTerrainPipeline terrain, VulkanBuffer indices, Batch batch, int firstVertex) {
        terrain.bindFrame(stack, commands, layout);
        vkCmdBindIndexBuffer(commands, indices.handle(), 0, VK_INDEX_TYPE_UINT32);
        vkCmdBindVertexBuffers(commands, 0, stack.longs(vertices.handle()), stack.longs(0));
        ByteBuffer parameters = stack.malloc(CONSTANTS - VulkanFogState.BYTES);
        for (Submit model : batch.models) {
            long modelPipeline = switch (model.material) {
                case ATLAS -> pipeline;
                case GLINT -> glintPipeline;
                case SPECIAL -> specialPipeline;
                case SPECIAL_UNCULLED -> specialUnculledPipeline;
                case SHADOW -> shadowPipeline;
                case FIRE -> firePipeline;
                case FOOTPRINT -> footprintPipeline;
                case TEXTURED_OPAQUE -> texturedOpaquePipeline;
            };
            // Bound per-model stack use even when a frame contains many dropped stacks.
            try (MemoryStack modelStack = MemoryStack.stackPush()) {
                if (model.texture != null) {
                    modelTextures.get(model.texture).bind(modelStack, commands, layout, 0, 1);
                } else if (model.material == Material.GLINT) {
                    textures.bind(modelStack, commands, layout, 0, 1);
                }
                vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, modelPipeline);
                vkCmdPushConstants(commands, layout, STAGES, 0, model.pose.constants(modelStack));
            }
            parameters.putFloat(0, model.blockLight).putFloat(Float.BYTES, model.skyLight);
            parameters.putInt(2 * Float.BYTES, model.mipmapped ? 1 : 0);
            vkCmdPushConstants(commands, layout, STAGES, VulkanFogState.BYTES, parameters);
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
        vkDestroyPipeline(device, texturedOpaquePipeline, null);
        vkDestroyPipeline(device, firePipeline, null);
        vkDestroyPipeline(device, footprintPipeline, null);
        vkDestroyPipeline(device, shadowPipeline, null);
        vkDestroyPipeline(device, specialUnculledPipeline, null);
        vkDestroyPipeline(device, specialPipeline, null);
        vkDestroyPipeline(device, glintPipeline, null);
        vkDestroyPipeline(device, pipeline, null);
        vkDestroyPipelineLayout(device, layout, null);
        textures.close();
        modelTextures.values().forEach(VulkanTextureBindings::close);
    }
}
