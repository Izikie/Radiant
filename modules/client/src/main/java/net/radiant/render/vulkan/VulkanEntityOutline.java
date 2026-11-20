package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.vkCmdBeginRendering;
import static org.lwjgl.vulkan.VK13.vkCmdEndRendering;

final class VulkanEntityOutline implements AutoCloseable {
    static final int MASK_FORMAT = VK_FORMAT_R8G8B8A8_UNORM;
    private static final String VERTEX = """
            #version 450
            void main() {
                vec2 p = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);
                gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
            }
            """;
    private static final String FRAGMENT = """
            layout(set = 0, binding = 0) uniform sampler2D mask;
            layout(location = 0) out vec4 color;
            void main() {
                ivec2 p = ivec2(gl_FragCoord.xy);
                ivec2 size = textureSize(mask, 0);
            #if EDGE_PASS
                vec4 center = texelFetch(mask, p, 0);
                vec3 rgb = center.rgb * center.a;
                float edge = 0.0;
                const ivec2 neighbors[4] = ivec2[4](ivec2(-1, 0), ivec2(1, 0), ivec2(0, -1), ivec2(0, 1));
                for (int i = 0; i < 4; i++) {
                    vec4 sampleColor = texelFetch(mask, clamp(p + neighbors[i], ivec2(0), size - 1), 0);
                    rgb += sampleColor.rgb * sampleColor.a;
                    edge += abs(center.a - sampleColor.a);
                }
                float alpha = min(edge, 1.0);
                // Retain interior color for the blur even where the edge alpha is zero.
                color = vec4(rgb * 0.2, alpha);
            #else
                vec4 sum = vec4(0.0);
                for (int i = -2; i <= 2; i++) {
                    ivec2 offset = HORIZONTAL_PASS == 1 ? ivec2(i, 0) : ivec2(0, i);
                    sum += texelFetch(mask, clamp(p + offset, ivec2(0), size - 1), 0);
                }
                color = vec4(sum.rgb * 0.2, min(sum.a, 1.0));
            #endif
            }
            """;

    private final VkPhysicalDevice physicalDevice;
    private final VkDevice device;
    private final VulkanTexture[] targets = new VulkanTexture[3];
    private int width;
    private int height;
    private long descriptorLayout;
    private long descriptorPool;
    private final long[] descriptorSets = new long[targets.length];
    private long layout;
    private long edgePipeline;
    private long blurPipeline;
    private long compositePipeline;

    VulkanEntityOutline(VkPhysicalDevice physicalDevice, VkDevice device) {
        this.physicalDevice = physicalDevice;
        this.device = device;
    }

    void configureTarget(int colorFormat, int depthFormat) {
        destroyPipelines();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (layout == 0) {
                createLayout(stack);
            }
            VkPipelineVertexInputStateCreateInfo input = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default();
            edgePipeline = createPipeline(stack, MASK_FORMAT, VK_FORMAT_UNDEFINED, VulkanGraphics.Pass.SKY, input, true, false);
            blurPipeline = createPipeline(stack, MASK_FORMAT, VK_FORMAT_UNDEFINED, VulkanGraphics.Pass.SKY, input, false, true);
            compositePipeline = createPipeline(stack, colorFormat, depthFormat, VulkanGraphics.Pass.OBJECT_OVERLAY, input, false, false);
        }
    }

    private long createPipeline(MemoryStack stack, int colorFormat, int depthFormat, VulkanGraphics.Pass pass,
                                VkPipelineVertexInputStateCreateInfo input, boolean edge, boolean horizontal) {
        String fragment = "#version 450\n#define EDGE_PASS " + (edge ? 1 : 0)
                + "\n#define HORIZONTAL_PASS " + (horizontal ? 1 : 0) + "\n" + FRAGMENT;
        return VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat,
                VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, pass, input, VERTEX, fragment);
    }

    private void createLayout(MemoryStack stack) {
        var handle = stack.mallocLong(1);
        var binding = VkDescriptorSetLayoutBinding.calloc(1, stack);
        binding.get(0).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1).stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
        var descriptorInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default().pBindings(binding);
        VulkanPresenter.check(vkCreateDescriptorSetLayout(device, descriptorInfo, null, handle), "create outline descriptor layout");
        descriptorLayout = handle.get(0);
        var size = VkDescriptorPoolSize.calloc(1, stack);
        size.get(0).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(targets.length);
        var poolInfo = VkDescriptorPoolCreateInfo.calloc(stack).sType$Default().maxSets(targets.length).pPoolSizes(size);
        VulkanPresenter.check(vkCreateDescriptorPool(device, poolInfo, null, handle), "create outline descriptor pool");
        descriptorPool = handle.get(0);
        var sets = stack.longs(descriptorLayout);
        var setLayouts = stack.mallocLong(targets.length);
        for (int i = 0; i < targets.length; i++) {
            setLayouts.put(i, descriptorLayout);
        }
        var allocate = VkDescriptorSetAllocateInfo.calloc(stack).sType$Default().descriptorPool(descriptorPool).pSetLayouts(setLayouts);
        var allocated = stack.mallocLong(targets.length);
        VulkanPresenter.check(vkAllocateDescriptorSets(device, allocate, allocated), "allocate outline descriptor sets");
        allocated.get(descriptorSets);
        var layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default().pSetLayouts(sets);
        VulkanPresenter.check(vkCreatePipelineLayout(device, layoutInfo, null, handle), "create outline pipeline layout");
        layout = handle.get(0);
    }

    private void ensureTargets(MemoryStack stack, int width, int height) {
        if (targets[0] != null && this.width == width && this.height == height) return;
        this.width = width;
        this.height = height;
        for (int i = 0; i < targets.length; i++) {
            if (targets[i] != null) targets[i].close();
            var target = new VulkanTexture(device);
            targets[i] = target;
            target.initializeAttachment(stack, physicalDevice, MASK_FORMAT, width, height);
            var image = VkDescriptorImageInfo.calloc(1, stack);
            image.get(0).imageView(target.view()).sampler(target.sampler()).imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            var write = VkWriteDescriptorSet.calloc(1, stack);
            write.get(0).sType$Default().dstSet(descriptorSets[i]).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1).pImageInfo(image);
            vkUpdateDescriptorSets(device, write, null);
        }
    }

    void recordMask(MemoryStack stack, VkCommandBuffer commands, int width, int height,
                    int viewWidth, int viewHeight, VulkanObjectRenderer objects, VulkanObjectCapture capture, VulkanTerrainScene.Fog fog) {
        ensureTargets(stack, width, height);
        var barrier = beginTarget(stack, commands, targets[0]);
        VulkanGraphics.viewport(stack, commands, (width - viewWidth) / 2, (height - viewHeight) / 2, viewWidth, viewHeight);
        objects.drawOutlineMask(stack, commands, capture, fog);
        endTarget(commands, barrier);

        for (int i = 1; i < targets.length; i++) {
            barrier = beginTarget(stack, commands, targets[i]);
            VulkanGraphics.viewport(stack, commands, width, height);
            drawPass(stack, commands, i == 1 ? edgePipeline : blurPipeline, descriptorSets[i - 1]);
            endTarget(commands, barrier);
        }
    }

    private VkImageMemoryBarrier.Buffer beginTarget(MemoryStack stack, VkCommandBuffer commands, VulkanTexture target) {
        var barrier = VkImageMemoryBarrier.calloc(1, stack);
        var transition = barrier.get(0).sType$Default().image(target.image())
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED).newLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
        transition.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).levelCount(1).layerCount(1);
        vkCmdPipelineBarrier(commands, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, 0, null, null, barrier);
        var attachments = VkRenderingAttachmentInfo.calloc(1, stack);
        attachments.get(0).sType$Default().imageView(target.view()).imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        var info = VkRenderingInfo.calloc(stack).sType$Default().layerCount(1).pColorAttachments(attachments);
        info.renderArea().extent().set(width, height);
        vkCmdBeginRendering(commands, info);
        return barrier;
    }

    private static void endTarget(VkCommandBuffer commands, VkImageMemoryBarrier.Buffer barrier) {
        vkCmdEndRendering(commands);
        barrier.get(0).oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL).newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
        vkCmdPipelineBarrier(commands, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, barrier);
    }

    void draw(MemoryStack stack, VkCommandBuffer commands) {
        drawPass(stack, commands, compositePipeline, descriptorSets[2]);
    }

    private void drawPass(MemoryStack stack, VkCommandBuffer commands, long pipeline, long descriptorSet) {
        vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        vkCmdBindDescriptorSets(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, layout, 0, stack.longs(descriptorSet), null);
        vkCmdDraw(commands, 3, 1, 0, 0);
    }

    private void destroyPipelines() {
        if (edgePipeline != 0) vkDestroyPipeline(device, edgePipeline, null);
        if (blurPipeline != 0) vkDestroyPipeline(device, blurPipeline, null);
        if (compositePipeline != 0) vkDestroyPipeline(device, compositePipeline, null);
        edgePipeline = blurPipeline = compositePipeline = 0;
    }

    @Override
    public void close() {
        destroyPipelines();
        if (layout != 0) vkDestroyPipelineLayout(device, layout, null);
        if (descriptorPool != 0) vkDestroyDescriptorPool(device, descriptorPool, null);
        if (descriptorLayout != 0) vkDestroyDescriptorSetLayout(device, descriptorLayout, null);
        for (VulkanTexture target : targets) {
            if (target != null) target.close();
        }
    }
}
