package net.radiant.render.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.memByteBuffer;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanTexture implements AutoCloseable {
    private static final int SIZE = 8;
    private static final int FORMAT = VK_FORMAT_R8G8B8A8_SRGB;

    private final VkDevice device;
    private long image;
    private long imageMemory;
    private long view;
    private long sampler;

    VulkanTexture(VkDevice device) {
        this.device = device;
    }

    void initialize(VkPhysicalDevice physicalDevice, VkQueue queue, VkCommandBuffer commands) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer handle = stack.mallocLong(1);
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack).sType$Default()
                    .imageType(VK_IMAGE_TYPE_2D).format(FORMAT).mipLevels(1).arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT).tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE).initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.extent().set(SIZE, SIZE, 1);
            VulkanPresenter.check(vkCreateImage(device, imageInfo, null, handle), "create texture image");
            image = handle.get(0);
            VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device, image, requirements);
            imageMemory = allocate(stack, physicalDevice, requirements, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
            VulkanPresenter.check(vkBindImageMemory(device, image, imageMemory, 0), "bind texture memory");
            upload(stack, physicalDevice, queue, commands);
            view = createImageView(stack, device, image, FORMAT);
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack).sType$Default()
                    .magFilter(VK_FILTER_NEAREST).minFilter(VK_FILTER_NEAREST).mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
            VulkanPresenter.check(vkCreateSampler(device, samplerInfo, null, handle), "create texture sampler");
            sampler = handle.get(0);
        }
    }

    private void upload(MemoryStack stack, VkPhysicalDevice physicalDevice, VkQueue queue, VkCommandBuffer commands) {
        int bytes = SIZE * SIZE * 4;
        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack).sType$Default()
                .size(bytes).usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT).sharingMode(VK_SHARING_MODE_EXCLUSIVE);
        LongBuffer handle = stack.mallocLong(1);
        VulkanPresenter.check(vkCreateBuffer(device, bufferInfo, null, handle), "create texture staging buffer");
        long staging = handle.get(0);
        long memory = 0;
        try {
            VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device, staging, requirements);
            memory = allocate(stack, physicalDevice, requirements,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            VulkanPresenter.check(vkBindBufferMemory(device, staging, memory, 0), "bind staging memory");
            PointerBuffer mapped = stack.mallocPointer(1);
            VulkanPresenter.check(vkMapMemory(device, memory, 0, bytes, 0, mapped), "map staging memory");
            try {
                ByteBuffer pixels = memByteBuffer(mapped.get(0), bytes);
                for (int y = 0; y < SIZE; y++) {
                    for (int x = 0; x < SIZE; x++) {
                        boolean bright = ((x ^ y) & 1) == 0;
                        pixels.put((byte) (bright ? 250 : 36)).put((byte) (bright ? 183 : 72))
                                .put((byte) (bright ? 75 : 180)).put((byte) 255);
                    }
                }
            } finally {
                vkUnmapMemory(device, memory);
            }

            VkCommandBufferBeginInfo begin = VkCommandBufferBeginInfo.calloc(stack).sType$Default()
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            VulkanPresenter.check(vkBeginCommandBuffer(commands, begin), "begin texture upload");
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
            VkImageMemoryBarrier transition = barrier.get(0).sType$Default().image(image)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED).newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            transition.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).levelCount(1).layerCount(1);
            vkCmdPipelineBarrier(commands, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, barrier);
            VkBufferImageCopy.Buffer copy = VkBufferImageCopy.calloc(1, stack);
            copy.get(0).imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).layerCount(1);
            copy.get(0).imageExtent().set(SIZE, SIZE, 1);
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

    private long allocate(MemoryStack stack, VkPhysicalDevice physicalDevice, VkMemoryRequirements requirements, int flags) {
        VkPhysicalDeviceMemoryProperties properties = VkPhysicalDeviceMemoryProperties.malloc(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, properties);
        for (int i = 0; i < properties.memoryTypeCount(); i++) {
            if ((requirements.memoryTypeBits() & (1 << i)) != 0 && (properties.memoryTypes(i).propertyFlags() & flags) == flags) {
                VkMemoryAllocateInfo info = VkMemoryAllocateInfo.calloc(stack).sType$Default()
                        .allocationSize(requirements.size()).memoryTypeIndex(i);
                LongBuffer handle = stack.mallocLong(1);
                VulkanPresenter.check(vkAllocateMemory(device, info, null, handle), "allocate texture memory");
                return handle.get(0);
            }
        }
        throw new IllegalStateException("No compatible Vulkan texture memory type");
    }

    static long createImageView(MemoryStack stack, VkDevice device, long image, int format) {
        VkImageViewCreateInfo info = VkImageViewCreateInfo.calloc(stack).sType$Default()
                .image(image).viewType(VK_IMAGE_VIEW_TYPE_2D).format(format);
        info.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).levelCount(1).layerCount(1);
        LongBuffer handle = stack.mallocLong(1);
        VulkanPresenter.check(vkCreateImageView(device, info, null, handle), "create image view");
        return handle.get(0);
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
