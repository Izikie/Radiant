package net.radiant.render.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.memByteBuffer;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanTexture implements AutoCloseable {
    private final VkDevice device;
    private long image;
    private long imageMemory;
    private long view;
    private long sampler;

    VulkanTexture(VkDevice device) {
        this.device = device;
    }

    void initialize(VkPhysicalDevice physicalDevice, VkQueue queue, VkCommandBuffer commands,
                    int width, int height, ByteBuffer[] levels, boolean srgb) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int format = srgb ? VK_FORMAT_R8G8B8A8_SRGB : VK_FORMAT_R8G8B8A8_UNORM;
            LongBuffer handle = stack.mallocLong(1);
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack).sType$Default()
                    .imageType(VK_IMAGE_TYPE_2D).format(format).mipLevels(levels.length).arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT).tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE).initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.extent().set(width, height, 1);
            VulkanPresenter.check(vkCreateImage(device, imageInfo, null, handle), "create texture image");
            image = handle.get(0);
            VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device, image, requirements);
            imageMemory = VulkanMemory.allocate(stack, physicalDevice, device, requirements, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
            VulkanPresenter.check(vkBindImageMemory(device, image, imageMemory, 0), "bind texture memory");
            upload(stack, physicalDevice, queue, commands, width, height, levels);
            view = VulkanMemory.createImageView(stack, device, image, format, VK_IMAGE_ASPECT_COLOR_BIT, levels.length);
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack).sType$Default()
                    .magFilter(VK_FILTER_NEAREST).minFilter(VK_FILTER_NEAREST).mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .maxLod(levels.length - 1);
            VulkanPresenter.check(vkCreateSampler(device, samplerInfo, null, handle), "create texture sampler");
            sampler = handle.get(0);
        }
    }

    private void upload(MemoryStack stack, VkPhysicalDevice physicalDevice, VkQueue queue, VkCommandBuffer commands,
                        int width, int height, ByteBuffer[] levels) {
        int bytes = 0;
        for (ByteBuffer level : levels) {
            bytes = Math.addExact(bytes, level.remaining());
        }
        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack).sType$Default()
                .size(bytes).usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT).sharingMode(VK_SHARING_MODE_EXCLUSIVE);
        LongBuffer handle = stack.mallocLong(1);
        VulkanPresenter.check(vkCreateBuffer(device, bufferInfo, null, handle), "create texture staging buffer");
        long staging = handle.get(0);
        long memory = 0;
        try {
            VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device, staging, requirements);
            memory = VulkanMemory.allocate(stack, physicalDevice, device, requirements,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            VulkanPresenter.check(vkBindBufferMemory(device, staging, memory, 0), "bind staging memory");
            PointerBuffer mapped = stack.mallocPointer(1);
            VulkanPresenter.check(vkMapMemory(device, memory, 0, bytes, 0, mapped), "map staging memory");
            try {
                ByteBuffer pixels = memByteBuffer(mapped.get(0), bytes);
                for (ByteBuffer level : levels) {
                    pixels.put(level.duplicate());
                }
            } finally {
                vkUnmapMemory(device, memory);
            }

            VkCommandBufferBeginInfo begin = VkCommandBufferBeginInfo.calloc(stack).sType$Default()
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            VulkanPresenter.check(vkResetCommandBuffer(commands, 0), "reset texture upload commands");
            VulkanPresenter.check(vkBeginCommandBuffer(commands, begin), "begin texture upload");
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
            VkImageMemoryBarrier transition = barrier.get(0).sType$Default().image(image)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED).newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            transition.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).levelCount(levels.length).layerCount(1);
            vkCmdPipelineBarrier(commands, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, barrier);
            VkBufferImageCopy.Buffer copy = VkBufferImageCopy.calloc(levels.length, stack);
            long offset = 0;
            for (int i = 0; i < levels.length; i++) {
                VkBufferImageCopy region = copy.get(i).bufferOffset(offset);
                region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(i).layerCount(1);
                region.imageExtent().set(Math.max(1, width >> i), Math.max(1, height >> i), 1);
                offset += levels[i].remaining();
            }
            vkCmdCopyBufferToImage(commands, staging, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copy);
            transition.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL).newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            vkCmdPipelineBarrier(commands, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, barrier);
            VulkanPresenter.check(vkEndCommandBuffer(commands), "end texture upload");
            VkSubmitInfo submit = VkSubmitInfo.calloc(stack).sType$Default().pCommandBuffers(stack.pointers(commands));
            VulkanPresenter.check(vkQueueSubmit(queue, submit, 0), "submit texture upload");
            VulkanPresenter.check(vkQueueWaitIdle(queue), "finish texture upload");
        } finally {
            vkDestroyBuffer(device, staging, null);
            if (memory != 0) {
                vkFreeMemory(device, memory, null);
            }
        }
    }

    long view() {
        return view;
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
