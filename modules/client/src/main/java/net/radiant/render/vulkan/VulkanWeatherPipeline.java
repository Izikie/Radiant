package net.radiant.render.vulkan;

import net.minecraft.client.Minecraft;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanWeatherPipeline implements AutoCloseable {
    private static final int STAGES = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;
    private static final int LIGHTMAP = 2;

    private record Batch(int texture, int[] vertices) {}

    static final class State {
        final List<Batch> batches = new ArrayList<>();
        private final int[] textureIds = {-1, -1};
        private final VulkanTextureBindings.Image[] images = new VulkanTextureBindings.Image[2];
        private final VulkanFogState draw = new VulkanFogState();
        private int bytes;
        int[] lightmap;

        void clear() {
            batches.clear();
            bytes = 0;
            lightmap = null;
        }

        void invalidateTextures() {
            Arrays.fill(textureIds, -1);
        }

        void add(int texture, int[] vertices) {
            if (batches.isEmpty()) {
                draw.capture();
                lightmap = Minecraft.get().entityRenderer.copyTerrainLightmap();
            }
            int id = glGetInteger(GL_TEXTURE_BINDING_2D);
            if (id != textureIds[texture]) {
                images[texture] = VulkanTextureBindings.Image.capture();
                textureIds[texture] = id;
            }
            batches.add(new Batch(texture, vertices));
            bytes += vertices.length * Integer.BYTES;
        }
    }

    private final VkDevice device;
    private final VulkanTextureBindings textures;
    private long layout, pipeline;
    private VulkanBuffer vertices;
    private int capacity;

    VulkanWeatherPipeline(VkDevice device) {
        this.device = device;
        textures = new VulkanTextureBindings(device, 3);
    }

    void initialize(int format, int depthFormat, VulkanTexture lightmap) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            textures.initialize(stack);
            textures.setTexture(LIGHTMAP, lightmap);
            var range = VkPushConstantRange.calloc(1, stack);
            range.get(0).size(VulkanFogState.BYTES).stageFlags(STAGES);
            var handle = stack.mallocLong(1);
            VulkanPresenter.check(vkCreatePipelineLayout(device, VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                    .pSetLayouts(stack.longs(textures.layout(), textures.layout())).pPushConstantRanges(range), null, handle), "create weather layout");
            layout = handle.get(0);
            var inputs = VulkanParticleVertices.input(stack);
            String header = VulkanFogState.shaderHeader(device);
            pipeline = VulkanGraphics.pipeline(stack, device, layout, format, depthFormat, VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
                    VulkanGraphics.Blend.TRANSLUCENT, false, VK_CULL_MODE_NONE, VulkanGraphics.RGBA, inputs,
                    header + VulkanParticleVertices.SHADER, header + VulkanFogState.FRAGMENT_FOG + """
                    layout(set=0,binding=0) uniform sampler2D weatherTexture;
                    layout(set=1,binding=0) uniform sampler2D lightmap;
                    layout(location=0) in vec2 uv;
                    layout(location=1) flat in vec4 tint;
                    layout(location=2) in float eyeZ;
                    layout(location=3) in vec2 lightCoord;
                    layout(location=0) out vec4 color;
                    void main() {
                        color = texture(weatherTexture, uv) * tint;
                        if (color.a <= draw.fogColorAndAlphaRef.a) discard;
                        color.rgb = applyFog(color.rgb * texture(lightmap, lightCoord).rgb, eyeZ);
                    }
                    """);
        }
    }

    void prepare(VkPhysicalDevice gpu, VkQueue queue, VkCommandBuffer commands, State weather) {
        for (int slot = 0; slot < weather.images.length; slot++) {
            if (weather.images[slot] != null) textures.prepare(gpu, queue, commands, slot, weather.images[slot]);
        }
        if (capacity < weather.bytes) {
            var replacement = VulkanBuffer.mapped(gpu, device, weather.bytes, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
            if (vertices != null) vertices.close();
            vertices = replacement;
            capacity = weather.bytes;
        }
        var target = vertices.data().asIntBuffer();
        for (Batch batch : weather.batches) target.put(batch.vertices);
    }

    void draw(MemoryStack stack, VkCommandBuffer commands, VulkanBuffer indices, State weather) {
        vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        vkCmdPushConstants(commands, layout, STAGES, 0, weather.draw.constants(stack));
        textures.bind(stack, commands, layout, LIGHTMAP, 1);
        vkCmdBindIndexBuffer(commands, indices.handle(), 0, VK_INDEX_TYPE_UINT32);
        vkCmdBindVertexBuffers(commands, 0, stack.longs(vertices.handle()), stack.longs(0));
        int firstVertex = 0;
        for (Batch batch : weather.batches) {
            int count = batch.vertices.length * Integer.BYTES / VulkanParticleVertices.STRIDE;
            textures.bind(stack, commands, layout, batch.texture);
            vkCmdDrawIndexed(commands, count / 4 * 6, 1, 0, firstVertex, 0);
            firstVertex += count;
        }
    }

    @Override
    public void close() {
        if (vertices != null) vertices.close();
        if (pipeline != 0) vkDestroyPipeline(device, pipeline, null);
        if (layout != 0) vkDestroyPipelineLayout(device, layout, null);
        textures.close();
    }
}
