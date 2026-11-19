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
            #version 450
            layout(set = 0, binding = 0) uniform sampler2D mask;
            layout(location = 0) out vec4 color;
            void main() {
                ivec2 size = textureSize(mask, 0);
                ivec2 p = ivec2(gl_FragCoord.xy);
                vec4 center = texelFetch(mask, p, 0);
                float edge = 0.0;
                vec3 tint = center.rgb;
                for (int y = -2; y <= 2; y++) {
                    for (int x = -2; x <= 2; x++) {
                        ivec2 offset = ivec2(x, y);
                        float falloff = max(0.0, 1.35 - 0.42 * length(vec2(offset)));
                        vec4 neighbor = texelFetch(mask, clamp(p + offset, ivec2(0), size - 1), 0);
                        float strength = abs(center.a - neighbor.a) * falloff;
                        if (strength > edge) {
                            edge = strength;
                            tint = center.a > neighbor.a ? center.rgb : neighbor.rgb;
                        }
                    }
                }
                color = vec4(tint, edge);
            }
            """;

    private final VkPhysicalDevice physicalDevice;
    private final VkDevice device;
    private VulkanTexture mask;
    private int width;
    private int height;
    private long descriptorLayout;
    private long descriptorPool;
    private long descriptorSet;
    private long layout;
    private long pipeline;

    VulkanEntityOutline(VkPhysicalDevice physicalDevice, VkDevice device) {
        this.physicalDevice = physicalDevice;
        this.device = device;
    }

    void configureTarget(int colorFormat, int depthFormat) {
        if (pipeline != 0) {
            vkDestroyPipeline(device, pipeline, null);
            pipeline = 0;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (layout == 0) {
                createLayout(stack);
            }
            VkPipelineVertexInputStateCreateInfo input = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default();
            pipeline = VulkanGraphics.pipeline(stack, device, layout, colorFormat, depthFormat,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST, VulkanGraphics.Pass.OBJECT_OVERLAY, input, VERTEX, FRAGMENT);
        }
    }

    private void createLayout(MemoryStack stack) {
        var handle = stack.mallocLong(1);
        var binding = VkDescriptorSetLayoutBinding.calloc(1, stack);
        binding.get(0).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1).stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
        var descriptorInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default().pBindings(binding);
        VulkanPresenter.check(vkCreateDescriptorSetLayout(device, descriptorInfo, null, handle), "create outline descriptor layout");
        descriptorLayout = handle.get(0);
        var size = VkDescriptorPoolSize.calloc(1, stack);
        size.get(0).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1);
        var poolInfo = VkDescriptorPoolCreateInfo.calloc(stack).sType$Default().maxSets(1).pPoolSizes(size);
        VulkanPresenter.check(vkCreateDescriptorPool(device, poolInfo, null, handle), "create outline descriptor pool");
        descriptorPool = handle.get(0);
        var sets = stack.longs(descriptorLayout);
        var allocate = VkDescriptorSetAllocateInfo.calloc(stack).sType$Default().descriptorPool(descriptorPool).pSetLayouts(sets);
        VulkanPresenter.check(vkAllocateDescriptorSets(device, allocate, handle), "allocate outline descriptor set");
        descriptorSet = handle.get(0);
        var layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default().pSetLayouts(sets);
        VulkanPresenter.check(vkCreatePipelineLayout(device, layoutInfo, null, handle), "create outline pipeline layout");
        layout = handle.get(0);
    }

    private void ensureMask(MemoryStack stack, int width, int height) {
        if (mask != null && this.width == width && this.height == height) {
            return;
        }
        if (mask != null) {
            mask.close();
        }
        this.width = width;
        this.height = height;
        mask = new VulkanTexture(device);
        mask.initializeAttachment(stack, physicalDevice, MASK_FORMAT, width, height);
        var image = VkDescriptorImageInfo.calloc(1, stack);
        image.get(0).imageView(mask.view()).sampler(mask.sampler()).imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        var write = VkWriteDescriptorSet.calloc(1, stack);
        write.get(0).sType$Default().dstSet(descriptorSet).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(1).pImageInfo(image);
        vkUpdateDescriptorSets(device, write, null);
    }

    void recordMask(MemoryStack stack, VkCommandBuffer commands, int width, int height,
                    int viewWidth, int viewHeight, VulkanObjectRenderer objects, VulkanObjectCapture capture, VulkanTerrainScene.Fog fog) {
        ensureMask(stack, width, height);
        var barrier = VkImageMemoryBarrier.calloc(1, stack);
        VkImageMemoryBarrier transition = barrier.get(0).sType$Default().image(mask.image())
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED).newLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
        transition.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).levelCount(1).layerCount(1);
        vkCmdPipelineBarrier(commands, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, 0, null, null, barrier);
        var attachments = VkRenderingAttachmentInfo.calloc(1, stack);
        attachments.get(0).sType$Default().imageView(mask.view()).imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        var info = VkRenderingInfo.calloc(stack).sType$Default().layerCount(1).pColorAttachments(attachments);
        info.renderArea().extent().set(width, height);
        vkCmdBeginRendering(commands, info);
        VulkanGraphics.viewport(stack, commands, (width - viewWidth) / 2, (height - viewHeight) / 2, viewWidth, viewHeight);
        objects.drawOutlineMask(stack, commands, capture, fog);
        vkCmdEndRendering(commands);
        transition.oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL).newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
        vkCmdPipelineBarrier(commands, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, barrier);
    }

    void draw(MemoryStack stack, VkCommandBuffer commands) {
        vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        vkCmdBindDescriptorSets(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, layout, 0, stack.longs(descriptorSet), null);
        vkCmdDraw(commands, 3, 1, 0, 0);
    }

    @Override
    public void close() {
        if (pipeline != 0) vkDestroyPipeline(device, pipeline, null);
        if (layout != 0) vkDestroyPipelineLayout(device, layout, null);
        if (descriptorPool != 0) vkDestroyDescriptorPool(device, descriptorPool, null);
        if (descriptorLayout != 0) vkDestroyDescriptorSetLayout(device, descriptorLayout, null);
        if (mask != null) mask.close();
    }
}
