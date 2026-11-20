package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

final class VulkanTexture implements AutoCloseable {
    private final VkDevice device;
    private long image;
    private long imageMemory;
    private long view;
    private long sampler;
    private int mipLevels;

    VulkanTexture(VkDevice device) {
        this.device = device;
    }

    void initialize(VkPhysicalDevice physicalDevice, VkQueue queue, VkCommandBuffer commands,
                    int width, int height, ByteBuffer[] levels, boolean srgb) {
        initialize(physicalDevice, queue, commands, width, height, levels, srgb,
                VK_FILTER_NEAREST, VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE, VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
    }

    void initialize(VkPhysicalDevice physicalDevice, VkQueue queue, VkCommandBuffer commands,
                    int width, int height, ByteBuffer[] levels, boolean srgb, int filter, int addressModeU, int addressModeV) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int format = srgb ? VK_FORMAT_R8G8B8A8_SRGB : VK_FORMAT_R8G8B8A8_UNORM;
            initializeImage(stack, physicalDevice, format, width, height, levels.length, VK_IMAGE_USAGE_TRANSFER_DST_BIT,
                    filter, addressModeU, addressModeV);
            upload(stack, physicalDevice, queue, commands, width, height, levels);
        }
    }

    void initializeAttachment(MemoryStack stack, VkPhysicalDevice physicalDevice, int format, int width, int height) {
        initializeImage(stack, physicalDevice, format, width, height, 1, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                VK_FILTER_NEAREST, VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE, VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
    }

    private void initializeImage(MemoryStack stack, VkPhysicalDevice physicalDevice, int format, int width, int height,
                                 int levels, int usage, int filter, int addressModeU, int addressModeV) {
        mipLevels = levels;
        LongBuffer handle = stack.mallocLong(1);
        VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack).sType$Default()
                .imageType(VK_IMAGE_TYPE_2D).format(format).mipLevels(levels).arrayLayers(1)
                .samples(VK_SAMPLE_COUNT_1_BIT).tiling(VK_IMAGE_TILING_OPTIMAL)
                .usage(usage | VK_IMAGE_USAGE_SAMPLED_BIT)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE).initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
        imageInfo.extent().set(width, height, 1);
        VulkanPresenter.check(vkCreateImage(device, imageInfo, null, handle), "create texture image");
        image = handle.get(0);
        VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
        vkGetImageMemoryRequirements(device, image, requirements);
        imageMemory = VulkanMemory.allocate(stack, physicalDevice, device, requirements, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
        VulkanPresenter.check(vkBindImageMemory(device, image, imageMemory, 0), "bind texture memory");
        view = VulkanMemory.createImageView(stack, device, image, format, VK_IMAGE_ASPECT_COLOR_BIT, levels);
        VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack).sType$Default()
                .magFilter(filter).minFilter(filter).mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
                .addressModeU(addressModeU)
                .addressModeV(addressModeV)
                .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                .maxLod(levels - 1);
        VulkanPresenter.check(vkCreateSampler(device, samplerInfo, null, handle), "create texture sampler");
        sampler = handle.get(0);
    }

    private void upload(MemoryStack stack, VkPhysicalDevice physicalDevice, VkQueue queue, VkCommandBuffer commands,
                        int width, int height, ByteBuffer[] levels) {
        int bytes = 0;
        for (ByteBuffer level : levels) {
            bytes = Math.addExact(bytes, level.remaining());
        }
        try (VulkanBuffer staging = VulkanBuffer.mapped(physicalDevice, device, bytes, VK_BUFFER_USAGE_TRANSFER_SRC_BIT)) {
            ByteBuffer pixels = staging.data();
            for (ByteBuffer level : levels) {
                pixels.put(level.duplicate());
            }

            VkCommandBufferBeginInfo begin = VkCommandBufferBeginInfo.calloc(stack).sType$Default()
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            VulkanPresenter.check(vkResetCommandBuffer(commands, 0), "reset texture upload commands");
            VulkanPresenter.check(vkBeginCommandBuffer(commands, begin), "begin texture upload");
            VkBufferImageCopy.Buffer copy = VkBufferImageCopy.calloc(levels.length, stack);
            long offset = 0;
            for (int i = 0; i < levels.length; i++) {
                VkBufferImageCopy region = copy.get(i).bufferOffset(offset);
                region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(i).layerCount(1);
                region.imageExtent().set(Math.max(1, width >> i), Math.max(1, height >> i), 1);
                offset += levels[i].remaining();
            }
            recordCopies(stack, commands, staging.handle(), copy, true);
            VulkanPresenter.check(vkEndCommandBuffer(commands), "end texture upload");
            VkSubmitInfo submit = VkSubmitInfo.calloc(stack).sType$Default().pCommandBuffers(stack.pointers(commands));
            VulkanPresenter.check(vkQueueSubmit(queue, submit, 0), "submit texture upload");
            VulkanPresenter.check(vkQueueWaitIdle(queue), "finish texture upload");
        }
    }

    void recordCopies(MemoryStack stack, VkCommandBuffer commands, long staging, VkBufferImageCopy.Buffer copies, boolean initial) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
        VkImageMemoryBarrier transition = barrier.get(0).sType$Default().image(image)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(initial ? VK_IMAGE_LAYOUT_UNDEFINED : VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                .newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                .srcAccessMask(initial ? 0 : VK_ACCESS_SHADER_READ_BIT)
                .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
        transition.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).levelCount(mipLevels).layerCount(1);
        int previousStage = initial ? VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT : VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
        vkCmdPipelineBarrier(commands, previousStage, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, barrier);
        vkCmdCopyBufferToImage(commands, staging, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copies);
        transition.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL).newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
        vkCmdPipelineBarrier(commands, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, barrier);
    }

    int mipLevels() {
        return mipLevels;
    }

    long view() {
        return view;
    }

    long image() {
        return image;
    }

    long sampler() {
        return sampler;
    }

    @Override
    public void close() {
        if (sampler != 0) vkDestroySampler(device, sampler, null);
        if (view != 0) vkDestroyImageView(device, view, null);
        if (image != 0) vkDestroyImage(device, image, null);
        if (imageMemory != 0) vkFreeMemory(device, imageMemory, null);
    }
}
