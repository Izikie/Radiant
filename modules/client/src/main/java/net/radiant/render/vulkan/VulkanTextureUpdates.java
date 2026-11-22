package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;

/** Batches base-level texture updates in staging memory owned by the frame fence. */
final class VulkanTextureUpdates implements AutoCloseable {
    private final Map<VulkanTexture, Region> pending = new LinkedHashMap<>();
    private VulkanBuffer staging;
    private int stagingCapacity;

    void add(VulkanTexture texture, Region region) {
        pending.put(texture, region);
    }

    void record(MemoryStack stack, VkPhysicalDevice physicalDevice, VkDevice device, VkCommandBuffer commands) {
        if (pending.isEmpty()) {
            return;
        }
        int bytes = 0;
        for (Region region : pending.values()) {
            bytes = Math.addExact(bytes, region.width() * region.height() * 4);
        }
        if (stagingCapacity < bytes) {
            int capacity = Math.max(4096, bytes);
            VulkanBuffer replacement = VulkanBuffer.mapped(physicalDevice, device, capacity, VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
            if (staging != null) {
                staging.close();
            }
            staging = replacement;
            stagingCapacity = capacity;
        }

        ByteBuffer output = staging.data();
        VkBufferImageCopy.Buffer copy = VkBufferImageCopy.calloc(1, stack);
        copy.get(0).imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).layerCount(1);
        for (var entry : pending.entrySet()) {
            Region region = entry.getValue();
            copy.get(0).bufferOffset(output.position());
            copy.get(0).imageOffset().set(region.left, region.top, 0);
            copy.get(0).imageExtent().set(region.width(), region.height(), 1);
            for (int y = region.top; y < region.bottom; y++) {
                output.put(region.pixels, (y * region.stride + region.left) * 4, region.width() * 4);
            }
            try (MemoryStack local = MemoryStack.stackPush()) {
                entry.getKey().recordCopies(local, commands, staging.handle(), copy, false);
            }
        }
        pending.clear();
    }

    @Override
    public void close() {
        pending.clear();
        if (staging != null) {
            staging.close();
        }
    }

    record Region(byte[] pixels, int stride, int left, int top, int right, int bottom) {
        int width() {
            return right - left;
        }

        int height() {
            return bottom - top;
        }

        Region include(Region previous) {
            if (previous == null || previous.pixels != pixels) {
                return this;
            }
            return new Region(pixels, stride, Math.min(left, previous.left), Math.min(top, previous.top),
                    Math.max(right, previous.right), Math.max(bottom, previous.bottom));
        }
    }
}
