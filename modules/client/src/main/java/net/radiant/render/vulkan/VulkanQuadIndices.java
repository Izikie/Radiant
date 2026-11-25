package net.radiant.render.vulkan;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;

final class VulkanQuadIndices {
    static final int CAPACITY = 16_384;

    private VulkanQuadIndices() {}

    static VulkanBuffer create(VkPhysicalDevice physicalDevice, VkDevice device) {
        VulkanBuffer indices = VulkanBuffer.mapped(physicalDevice, device, CAPACITY * 6 * Integer.BYTES, VK_BUFFER_USAGE_INDEX_BUFFER_BIT);
        var indexData = indices.data().asIntBuffer();
        for (int quad = 0; quad < CAPACITY; quad++) {
            int base = quad * 4;
            // Match the OpenGL reference diagonal so smooth lighting interpolates across the same triangles.
            indexData.put(base).put(base + 1).put(base + 3);
            indexData.put(base + 1).put(base + 2).put(base + 3);
        }
        return indices;
    }
}
