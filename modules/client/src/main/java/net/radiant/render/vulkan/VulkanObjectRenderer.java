package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

final class VulkanObjectRenderer implements AutoCloseable {
    private static final int DESCRIPTORS_PER_POOL = 64;
    private static final String VERTEX = """
            #version 450
            layout(location = 0) in vec4 clip;
            layout(location = 1) in vec2 textureUv;
            layout(location = 2) in vec4 vertexColor;
            layout(location = 0) out vec2 uv;
            layout(location = 1) out vec4 tint;
            layout(location = 2) out float distanceToCamera;
            void main() {
                gl_Position = vec4(clip.x, -clip.y, (clip.z + clip.w) * 0.5, clip.w);
                uv = textureUv;
                tint = vertexColor;
                distanceToCamera = abs(clip.w);
            }
            """;
    private static final String FRAGMENT = """
            layout(set = 0, binding = 0) uniform sampler2D image;
            layout(push_constant) uniform Draw {
                vec4 fogColorAndAlphaCutoff;
                vec4 fog;
                float glintColorBlend;
            } draw;
            layout(location = 0) in vec2 uv;
            layout(location = 1) in vec4 tint;
            layout(location = 2) in float distanceToCamera;
            layout(location = 0) out vec4 color;
            void main() {
                vec4 lit = texture(image, uv) * tint;
                if (lit.a <= draw.fogColorAndAlphaCutoff.a) discard;
                float visibility = 1.0;
                if (draw.fog.x == 1.0) {
                    visibility = (draw.fog.z - distanceToCamera) / max(draw.fog.z - draw.fog.y, 0.0001);
                } else if (draw.fog.x == 2.0) {
                    visibility = exp(-distanceToCamera * draw.fog.w);
                } else if (draw.fog.x == 3.0) {
                    float density = distanceToCamera * draw.fog.w;
                    visibility = exp(-density * density);
                }
                vec3 rgb = mix(draw.fogColorAndAlphaCutoff.rgb, lit.rgb, clamp(visibility, 0.0, 1.0));
                #if SRGB_TARGET
                // The glint uses its source color as a blend coefficient. Linearizing it first squares a much smaller value.
                if (draw.glintColorBlend == 0.0) {
                    rgb = mix(rgb / 12.92, pow((rgb + 0.055) / 1.055, vec3(2.4)), greaterThan(rgb, vec3(0.04045)));
                }
                #endif
                color = vec4(rgb, lit.a);
            }
            """;

    private final VkPhysicalDevice physicalDevice;
    private final VkDevice device;
    private final Map<VulkanObjectCapture.PipelineKey, Long> pipelines = new HashMap<>();
    private final Map<VulkanObjectCapture.Image, Binding> textures = new IdentityHashMap<>();
    private final List<Pool> pools = new ArrayList<>();
    private long descriptorLayout;
    private long layout;
    private long vertexShader;
    private long fragmentShader;
    private int colorFormat;
    private int depthFormat;
    private Binding atlasBinding;
    private VulkanTexture boundAtlas;
    private VulkanBuffer vertices;
    private int vertexCapacity;

    VulkanObjectRenderer(VkPhysicalDevice physicalDevice, VkDevice device) {
        this.physicalDevice = physicalDevice;
        this.device = device;
    }

    void configureTarget(int colorFormat, int depthFormat) {
        destroyPipelines();
        this.colorFormat = colorFormat;
        this.depthFormat = depthFormat;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (layout == 0) {
                createLayout(stack);
            }
            String fragment = "#version 450\n#define SRGB_TARGET " + (VulkanGraphics.isSrgb(colorFormat) ? "1\n" : "0\n") + FRAGMENT;
            vertexShader = VulkanGraphics.vertexShader(stack, device, VERTEX);
            fragmentShader = VulkanGraphics.fragmentShader(stack, device, fragment);
        }
    }

    private long createPipeline(VulkanObjectCapture.PipelineKey pipeline) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkVertexInputBindingDescription.Buffer bindings = VkVertexInputBindingDescription.calloc(1, stack);
            bindings.get(0).stride(VulkanObjectCapture.STRIDE).inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
            VkVertexInputAttributeDescription.Buffer attributes = VkVertexInputAttributeDescription.calloc(3, stack);
            attributes.get(0).location(0).format(VK_FORMAT_R32G32B32A32_SFLOAT);
            attributes.get(1).location(1).format(VK_FORMAT_R32G32_SFLOAT).offset(16);
            attributes.get(2).location(2).format(VK_FORMAT_R8G8B8A8_UNORM).offset(24);
            VkPipelineVertexInputStateCreateInfo input = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default()
                    .pVertexBindingDescriptions(bindings).pVertexAttributeDescriptions(attributes);
            int targetColor = pipeline.outlineMask() ? VulkanEntityOutline.MASK_FORMAT : colorFormat;
            int targetDepth = pipeline.outlineMask() ? VK_FORMAT_UNDEFINED : depthFormat;
            return VulkanGraphics.pipeline(stack, device, layout, targetColor, targetDepth,
                    pipeline.topology(), pipeline.state(), input, vertexShader, fragmentShader);
        }
    }

    private void createLayout(MemoryStack stack) {
        LongBuffer handle = stack.mallocLong(1);
        VkDescriptorSetLayoutBinding.Buffer binding = VkDescriptorSetLayoutBinding.calloc(1, stack);
        binding.get(0).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1)
                .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
        VulkanPresenter.check(vkCreateDescriptorSetLayout(device,
                VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default().pBindings(binding), null, handle),
                "create object descriptor layout");
        descriptorLayout = handle.get(0);
        VkPushConstantRange.Buffer constants = VkPushConstantRange.calloc(1, stack);
        constants.get(0).stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT).size(36);
        VkPipelineLayoutCreateInfo info = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                .pSetLayouts(stack.longs(descriptorLayout)).pPushConstantRanges(constants);
        VulkanPresenter.check(vkCreatePipelineLayout(device, info, null, handle), "create object pipeline layout");
        layout = handle.get(0);
    }

    void prepare(VkQueue queue, VkCommandBuffer commands, VulkanObjectCapture capture, VulkanTexture atlas) {
        ByteBuffer data = capture.vertices();
        if (data.hasRemaining()) {
            if (data.remaining() > vertexCapacity) {
                if (vertices != null) {
                    vertices.close();
                }
                vertexCapacity = Math.max(vertexCapacity * 2, data.remaining());
                vertices = VulkanBuffer.mapped(physicalDevice, device, vertexCapacity, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
            }
            vertices.write(0, data);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (VulkanObjectCapture.Draw draw : capture.outlineDraws()) {
                prepareDraw(stack, queue, commands, capture, atlas, draw);
            }
            for (VulkanObjectCapture.Draw draw : capture.draws()) {
                prepareDraw(stack, queue, commands, capture, atlas, draw);
            }
            for (VulkanObjectCapture.Draw draw : capture.nameTagDraws()) {
                prepareDraw(stack, queue, commands, capture, atlas, draw);
            }
            // A texture deleted mid-capture can still be referenced by this frame.
            for (var iterator = textures.entrySet().iterator(); iterator.hasNext(); ) {
                var entry = iterator.next();
                if (entry.getKey().deleted
                        && capture.draws().stream().noneMatch(draw -> draw.material.image() == entry.getKey())
                        && capture.nameTagDraws().stream().noneMatch(draw -> draw.material.image() == entry.getKey())) {
                    Binding binding = entry.getValue();
                    VulkanPresenter.check(vkFreeDescriptorSets(device, binding.pool.handle, binding.set), "free object descriptor set");
                    binding.pool.available++;
                    binding.texture.close();
                    iterator.remove();
                }
            }
        }
    }

    private void prepareDraw(MemoryStack stack, VkQueue queue, VkCommandBuffer commands, VulkanObjectCapture capture,
                             VulkanTexture atlas, VulkanObjectCapture.Draw draw) {
        pipelines.computeIfAbsent(draw.material.pipeline(), this::createPipeline);
        VulkanObjectCapture.Image image = draw.material.image();
        if (image == capture.atlas()) {
            if (atlasBinding == null) {
                atlasBinding = allocateBinding(stack, null);
            }
            if (boundAtlas != atlas) {
                updateBinding(stack, atlasBinding.set, atlas);
                boundAtlas = atlas;
            }
        } else if (!textures.containsKey(image)) {
            upload(queue, commands, image);
        }
    }

    private void upload(VkQueue queue, VkCommandBuffer commands, VulkanObjectCapture.Image image) {
        VulkanTexture texture = new VulkanTexture(device);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            texture.initialize(physicalDevice, queue, commands, image.width, image.height, new ByteBuffer[]{image.pixels}, false,
                    image.addressModeU, image.addressModeV);
            Binding binding = allocateBinding(stack, texture);
            updateBinding(stack, binding.set, texture);
            textures.put(image, binding);
            image.releasePixels();
        } catch (RuntimeException | Error e) {
            texture.close();
            throw e;
        }
    }

    private Binding allocateBinding(MemoryStack stack, VulkanTexture texture) {
        for (Pool pool : pools) {
            if (pool.available > 0) {
                return allocateSet(stack, pool, texture);
            }
        }
        VkDescriptorPoolSize.Buffer sizes = VkDescriptorPoolSize.calloc(1, stack);
        sizes.get(0).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(DESCRIPTORS_PER_POOL);
        VkDescriptorPoolCreateInfo info = VkDescriptorPoolCreateInfo.calloc(stack).sType$Default()
                .flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT).maxSets(DESCRIPTORS_PER_POOL).pPoolSizes(sizes);
        LongBuffer handle = stack.mallocLong(1);
        VulkanPresenter.check(vkCreateDescriptorPool(device, info, null, handle), "create object descriptor pool");
        Pool pool = new Pool(handle.get(0));
        pools.add(pool);
        return allocateSet(stack, pool, texture);
    }

    private Binding allocateSet(MemoryStack stack, Pool pool, VulkanTexture texture) {
        VkDescriptorSetAllocateInfo info = VkDescriptorSetAllocateInfo.calloc(stack).sType$Default()
                .descriptorPool(pool.handle).pSetLayouts(stack.longs(descriptorLayout));
        LongBuffer handle = stack.mallocLong(1);
        VulkanPresenter.check(vkAllocateDescriptorSets(device, info, handle), "allocate object descriptor set");
        pool.available--;
        return new Binding(pool, handle.get(0), texture);
    }

    private void updateBinding(MemoryStack stack, long set, VulkanTexture texture) {
        VkDescriptorImageInfo.Buffer image = VkDescriptorImageInfo.calloc(1, stack);
        image.get(0).imageView(texture.view()).sampler(texture.sampler()).imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(1, stack);
        write.get(0).sType$Default().dstSet(set).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(1).pImageInfo(image);
        vkUpdateDescriptorSets(device, write, null);
    }

    void draw(MemoryStack stack, VkCommandBuffer commands, VulkanObjectCapture capture, VulkanTerrainScene.Fog fog,
              VulkanObjectCapture.Layer layer) {
        draw(stack, commands, capture.atlas(), capture.draws(), capture.drawStart(layer), capture.drawEnd(layer),
                fog, layer == VulkanObjectCapture.Layer.WORLD ? fog.mode() : 0);
    }

    void drawOutlineMask(MemoryStack stack, VkCommandBuffer commands, VulkanObjectCapture capture, VulkanTerrainScene.Fog fog) {
        List<VulkanObjectCapture.Draw> draws = capture.outlineDraws();
        draw(stack, commands, capture.atlas(), draws, 0, draws.size(), fog, 0);
    }

    void drawNameTags(MemoryStack stack, VkCommandBuffer commands, VulkanObjectCapture capture, VulkanTerrainScene.Fog fog) {
        List<VulkanObjectCapture.Draw> draws = capture.nameTagDraws();
        draw(stack, commands, capture.atlas(), draws, 0, draws.size(), fog, fog.mode());
    }

    private void draw(MemoryStack stack, VkCommandBuffer commands, VulkanObjectCapture.Image atlas, List<VulkanObjectCapture.Draw> draws,
                      int from, int to, VulkanTerrainScene.Fog fog, float fogMode) {
        if (from == to) {
            return;
        }
        vkCmdBindVertexBuffers(commands, 0, stack.longs(vertices.handle()), stack.longs(0));
        var constants = stack.floats(fog.red(), fog.green(), fog.blue(), 0, fogMode, fog.start(), fog.end(), fog.density(), 0);
        LongBuffer descriptor = stack.mallocLong(1);
        VulkanObjectCapture.PipelineKey previousPipeline = null;
        VulkanObjectCapture.Image previousImage = null;
        float previousCutoff = Float.NaN;
        boolean previousGlintBlend = false;
        for (int i = from; i < to; i++) {
            VulkanObjectCapture.Draw draw = draws.get(i);
            VulkanObjectCapture.Material material = draw.material;
            if (!material.pipeline().equals(previousPipeline)) {
                vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelines.get(material.pipeline()));
                previousPipeline = material.pipeline();
            }
            if (material.image() != previousImage) {
                Binding binding = material.image() == atlas ? atlasBinding : textures.get(material.image());
                vkCmdBindDescriptorSets(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, layout, 0, descriptor.put(0, binding.set), null);
                previousImage = material.image();
            }
            if (material.alphaCutoff() != previousCutoff || material.glintColorBlend() != previousGlintBlend) {
                constants.put(3, material.alphaCutoff());
                constants.put(8, material.glintColorBlend() ? 1 : 0);
                vkCmdPushConstants(commands, layout, VK_SHADER_STAGE_FRAGMENT_BIT, 0, constants);
                previousCutoff = material.alphaCutoff();
                previousGlintBlend = material.glintColorBlend();
            }
            vkCmdDraw(commands, draw.vertexCount, 1, draw.firstVertex, 0);
        }
    }

    private void destroyPipelines() {
        pipelines.values().forEach(pipeline -> vkDestroyPipeline(device, pipeline, null));
        pipelines.clear();
        if (vertexShader != 0) vkDestroyShaderModule(device, vertexShader, null);
        if (fragmentShader != 0) vkDestroyShaderModule(device, fragmentShader, null);
        vertexShader = 0;
        fragmentShader = 0;
    }

    @Override
    public void close() {
        destroyPipelines();
        if (layout != 0) vkDestroyPipelineLayout(device, layout, null);
        for (Pool pool : pools) {
            vkDestroyDescriptorPool(device, pool.handle, null);
        }
        if (descriptorLayout != 0) vkDestroyDescriptorSetLayout(device, descriptorLayout, null);
        textures.values().forEach(binding -> binding.texture.close());
        if (vertices != null) vertices.close();
    }

    private static final class Pool {
        final long handle;
        int available = DESCRIPTORS_PER_POOL;

        Pool(long handle) {
            this.handle = handle;
        }
    }

    private record Binding(Pool pool, long set, VulkanTexture texture) {
    }
}
