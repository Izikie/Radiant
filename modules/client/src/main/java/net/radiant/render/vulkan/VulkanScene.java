package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;

interface VulkanScene extends AutoCloseable {
    void initialize(VkPhysicalDevice physicalDevice, VkDevice device, VkQueue queue, VkCommandBuffer commands);

    int depthFormat();

    void configureTarget(int colorFormat);

    default void releaseCompletedResources() {
    }

    void prepareFrame();

    default void recordUploads(MemoryStack stack, VkCommandBuffer commands) {
    }

    default void recordOffscreen(MemoryStack stack, VkCommandBuffer commands, int width, int height) {
    }

    void draw(MemoryStack stack, VkCommandBuffer commands, int width, int height);

    @Override
    void close();
}
