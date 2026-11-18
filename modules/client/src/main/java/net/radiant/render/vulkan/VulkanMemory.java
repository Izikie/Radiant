package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

final class VulkanMemory {
    private VulkanMemory() {
    }

    static long allocate(MemoryStack stack, VkPhysicalDevice physicalDevice, VkDevice device,
                         VkMemoryRequirements requirements, int flags) {
        VkPhysicalDeviceMemoryProperties properties = VkPhysicalDeviceMemoryProperties.malloc(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, properties);
        for (int i = 0; i < properties.memoryTypeCount(); i++) {
            if ((requirements.memoryTypeBits() & (1 << i)) != 0 && (properties.memoryTypes(i).propertyFlags() & flags) == flags) {
                VkMemoryAllocateInfo info = VkMemoryAllocateInfo.calloc(stack).sType$Default()
                        .allocationSize(requirements.size()).memoryTypeIndex(i);
                LongBuffer handle = stack.mallocLong(1);
                VulkanPresenter.check(vkAllocateMemory(device, info, null, handle), "allocate Vulkan memory");
                return handle.get(0);
            }
        }
        throw new IllegalStateException("No compatible Vulkan memory type for flags " + flags);
    }

    static long createImageView(MemoryStack stack, VkDevice device, long image, int format, int aspect) {
        return createImageView(stack, device, image, format, aspect, 1);
    }

    static long createImageView(MemoryStack stack, VkDevice device, long image, int format, int aspect, int levels) {
        VkImageViewCreateInfo info = VkImageViewCreateInfo.calloc(stack).sType$Default()
                .image(image).viewType(VK_IMAGE_VIEW_TYPE_2D).format(format);
        info.subresourceRange().aspectMask(aspect).levelCount(levels).layerCount(1);
        LongBuffer handle = stack.mallocLong(1);
        VulkanPresenter.check(vkCreateImageView(device, info, null, handle), "create image view");
        return handle.get(0);
    }
}
