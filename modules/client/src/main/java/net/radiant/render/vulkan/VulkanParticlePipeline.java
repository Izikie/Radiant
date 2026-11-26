package net.radiant.render.vulkan;

import net.minecraft.client.Minecraft;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanParticlePipeline implements AutoCloseable {
    private static final int STAGES = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;
    private static final int SHEET = 0, BLOCK_ATLAS = 1;

    private record Batch(int texture, boolean depthWrite, boolean mipmapped, int[] vertices) {}

    static final class State {
        private final List<Batch> batches = new ArrayList<>();
        private final VulkanFogState fog = new VulkanFogState();
        private int bytes;
        private int sheetId = -1;
        private VulkanTextureBindings.Image sheet;
        int[] lightmap;

        boolean isEmpty() {
            return batches.isEmpty();
        }

        void clear() {
            batches.clear();
            bytes = 0;
            lightmap = null;
        }

        void invalidateTexture() {
            sheetId = -1;
            sheet = null;
        }

        void add(boolean blockAtlas, boolean depthWrite, int[] vertices) {
            if (batches.isEmpty()) {
                fog.capture();
                lightmap = Minecraft.get().entityRenderer.copyTerrainLightmap();
            }
            boolean mipmapped = false;
            if (blockAtlas) {
                int filter = glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER);
                mipmapped = filter != GL_NEAREST && filter != GL_LINEAR;
            } else {
                int id = glGetInteger(GL_TEXTURE_BINDING_2D);
                if (id != sheetId) {
                    sheet = VulkanTextureBindings.Image.capture();
                    sheetId = id;
                }
            }
            batches.add(new Batch(blockAtlas ? BLOCK_ATLAS : SHEET, depthWrite, mipmapped, vertices));
            bytes += vertices.length * Integer.BYTES;
        }
    }

    private final VkDevice device;
    private final VulkanTextureBindings textures;
    private long layout, depthWritePipeline, translucentPipeline;
    private VulkanBuffer vertices;
    private int capacity;

    VulkanParticlePipeline(VkDevice device) {
        this.device = device;
        textures = new VulkanTextureBindings(device, 2);
    }

    void initialize(int format, int depthFormat, long frameLayout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            textures.initialize(stack);
            var range = VkPushConstantRange.calloc(1, stack);
            range.get(0).size(VulkanFogState.BYTES + Integer.BYTES).stageFlags(STAGES);
            var handle = stack.mallocLong(1);
            VulkanPresenter.check(vkCreatePipelineLayout(device, VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                    .pSetLayouts(stack.longs(frameLayout, textures.layout())).pPushConstantRanges(range), null, handle), "create particle layout");
            layout = handle.get(0);
            var inputs = VulkanParticleVertices.input(stack);
            String header = VulkanFogState.shaderHeader(device, "int mipmapped;\n");
            String vertex = header + VulkanParticleVertices.SHADER;
            String fragment = header + VulkanFogState.FRAGMENT_FOG + VulkanTerrainPipeline.FRAME_UNIFORMS
                    + VulkanTerrainPipeline.ATLAS_MIP_LOD + """
                    layout(set=1,binding=0) uniform sampler2D particleTexture;
                    layout(set=0,binding=2) uniform sampler2D lightmap;
                    layout(location=0) in vec2 uv;
                    layout(location=1) flat in vec4 tint;
                    layout(location=2) in float eyeZ;
                    layout(location=3) in vec2 lightCoord;
                    layout(location=0) out vec4 color;
                    void main() {
                        float lod = draw.mipmapped != 0 ? atlasMipLod(particleTexture, uv) : 0.0;
                        color = textureLod(particleTexture, uv, lod) * tint;
                        if (color.a <= draw.fogColorAndAlphaRef.a) discard;
                        color.rgb = applyFog(color.rgb * texture(lightmap, lightCoord).rgb, eyeZ);
                    }
                    """;
            depthWritePipeline = VulkanGraphics.pipeline(stack, device, layout, format, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Blend.PARTICLE, true, inputs, vertex, fragment);
            translucentPipeline = VulkanGraphics.pipeline(stack, device, layout, format, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Blend.PARTICLE, false, inputs, vertex, fragment);
        }
    }

    void setAtlas(VulkanTexture atlas) {
        textures.setTexture(BLOCK_ATLAS, atlas);
    }

    void prepare(VkPhysicalDevice gpu, VkQueue queue, VkCommandBuffer commands, State particles) {
        if (particles.sheet != null) textures.prepare(gpu, queue, commands, SHEET, particles.sheet);
        if (capacity < particles.bytes) {
            var replacement = VulkanBuffer.mapped(gpu, device, particles.bytes, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
            if (vertices != null) vertices.close();
            vertices = replacement;
            capacity = particles.bytes;
        }
        var target = vertices.data().asIntBuffer();
        for (Batch batch : particles.batches) target.put(batch.vertices);
    }

    void draw(MemoryStack stack, VkCommandBuffer commands, VulkanTerrainPipeline terrain, VulkanBuffer indices, State particles) {
        terrain.bindFrame(stack, commands, layout);
        vkCmdPushConstants(commands, layout, STAGES, 0, particles.fog.constants(stack));
        vkCmdBindIndexBuffer(commands, indices.handle(), 0, VK_INDEX_TYPE_UINT32);
        vkCmdBindVertexBuffers(commands, 0, stack.longs(vertices.handle()), stack.longs(0));
        int firstVertex = 0;
        for (Batch batch : particles.batches) {
            vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, batch.depthWrite ? depthWritePipeline : translucentPipeline);
            vkCmdPushConstants(commands, layout, STAGES, VulkanFogState.BYTES, stack.ints(batch.mipmapped ? 1 : 0));
            textures.bind(stack, commands, layout, batch.texture, 1);
            int quads = batch.vertices.length / (4 * VulkanParticleVertices.STRIDE / Integer.BYTES);
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
        if (depthWritePipeline != 0) vkDestroyPipeline(device, depthWritePipeline, null);
        if (translucentPipeline != 0) vkDestroyPipeline(device, translucentPipeline, null);
        if (layout != 0) vkDestroyPipelineLayout(device, layout, null);
        textures.close();
    }
}
