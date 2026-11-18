package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

final class VulkanDepthTarget implements AutoCloseable {
    private final VkDevice device;
    private long image;
    private long memory;
    private long view;

    VulkanDepthTarget(VkDevice device) {
        this.device = device;
    }

    void initialize(MemoryStack stack, VkPhysicalDevice physicalDevice, int format, int width, int height) {
        VkImageCreateInfo info = VkImageCreateInfo.calloc(stack).sType$Default()
                .imageType(VK_IMAGE_TYPE_2D).format(format).mipLevels(1).arrayLayers(1)
                .samples(VK_SAMPLE_COUNT_1_BIT).tiling(VK_IMAGE_TILING_OPTIMAL)
                .usage(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT).sharingMode(VK_SHARING_MODE_EXCLUSIVE);
        info.extent().set(width, height, 1);
        LongBuffer handle = stack.mallocLong(1);
        VulkanPresenter.check(vkCreateImage(device, info, null, handle), "create depth image");
        image = handle.get(0);
        VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
        vkGetImageMemoryRequirements(device, image, requirements);
        memory = VulkanMemory.allocate(stack, physicalDevice, device, requirements, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
        VulkanPresenter.check(vkBindImageMemory(device, image, memory, 0), "bind depth image");
        view = VulkanMemory.createImageView(stack, device, image, format, VK_IMAGE_ASPECT_DEPTH_BIT);
    }

    VkRenderingAttachmentInfo attachment(MemoryStack stack, VkCommandBuffer commands) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
        VkImageMemoryBarrier transition = barrier.get(0).sType$Default().image(image)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED).newLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                .dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
        transition.subresourceRange().aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT).levelCount(1).layerCount(1);
        int tests = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT;
        vkCmdPipelineBarrier(commands, tests, tests, 0, null, null, barrier);

        VkRenderingAttachmentInfo attachment = VkRenderingAttachmentInfo.calloc(stack).sType$Default()
                .imageView(view).imageLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
        attachment.clearValue().depthStencil().depth(1.0f);
        return attachment;
    }

    @Override
    public void close() {
        if (view != 0) {
            vkDestroyImageView(device, view, null);
        }
        if (image != 0) {
            vkDestroyImage(device, image, null);
        }
        if (memory != 0) {
            vkFreeMemory(device, memory, null);
        }
    }
}
