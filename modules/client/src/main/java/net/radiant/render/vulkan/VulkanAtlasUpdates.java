package net.radiant.render.vulkan;

import net.radiant.benchmark.ClientBenchmark;
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

/** Updates only the atlas rectangles touched by texture ticks. The presenter owns the staging fence. */
final class VulkanAtlasUpdates implements AutoCloseable {
    private final Map<String, Sprite> pending = new LinkedHashMap<>();
    private VkBufferImageCopy.Buffer regions;
    private VulkanBuffer staging;
    private int stagingCapacity;

    void update(String name, int x, int y, int width, int height, int[][] pixels) {
        // Interpolated frames reuse their input arrays. Keep an owned snapshot of the latest frame per sprite.
        int[][] snapshot = new int[pixels.length][];
        for (int level = 0; level < pixels.length; level++) {
            snapshot[level] = pixels[level].clone();
        }
        pending.put(name, new Sprite(x, y, width, height, snapshot));
    }

    void clear() {
        pending.clear();
    }

    void record(MemoryStack stack, VkPhysicalDevice physicalDevice, VkDevice device, VkCommandBuffer commands,
                VulkanTexture atlas) {
        if (pending.isEmpty()) {
            return;
        }
        int count = 0;
        int bytes = 0;
        for (Sprite sprite : pending.values()) {
            int levels = Math.min(sprite.pixels.length, atlas.mipLevels());
            count += levels;
            for (int level = 0; level < levels; level++) {
                bytes = Math.addExact(bytes, Math.multiplyExact(sprite.widthAt(level) * sprite.heightAt(level), 4));
            }
        }
        if (regions == null || regions.capacity() < count) {
            VkBufferImageCopy.Buffer replacement = VkBufferImageCopy.calloc(Math.max(32, count));
            if (regions != null) {
                regions.free();
            }
            regions = replacement;
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
        regions.clear().limit(count);
        int regionIndex = 0;
        int water = 0;
        int lava = 0;
        for (Map.Entry<String, Sprite> entry : pending.entrySet()) {
            Sprite sprite = entry.getValue();
            int levels = Math.min(sprite.pixels.length, atlas.mipLevels());
            for (int level = 0; level < levels; level++) {
                int width = sprite.widthAt(level);
                int height = sprite.heightAt(level);
                VkBufferImageCopy region = regions.get(regionIndex++).bufferOffset(output.position());
                region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(level).layerCount(1);
                region.imageOffset().set(sprite.x >> level, sprite.y >> level, 0);
                region.imageExtent().set(width, height, 1);
                int[] pixels = sprite.pixels[level];
                for (int i = 0; i < width * height; i++) {
                    int argb = pixels[i];
                    output.put((byte) (argb >>> 16)).put((byte) (argb >>> 8)).put((byte) argb).put((byte) (argb >>> 24));
                }
            }
            switch (entry.getKey()) {
                case "minecraft:blocks/water_still", "minecraft:blocks/water_flow" -> water++;
                case "minecraft:blocks/lava_still", "minecraft:blocks/lava_flow" -> lava++;
                default -> {
                }
            }
        }
        atlas.recordCopies(stack, commands, staging.handle(), regions, false);
        ClientBenchmark.recordVulkanAtlasUpload(pending.size(), water, lava, bytes);
        pending.clear();
    }

    @Override
    public void close() {
        clear();
        if (regions != null) {
            regions.free();
        }
        if (staging != null) {
            staging.close();
        }
    }

    private record Sprite(int x, int y, int width, int height, int[][] pixels) {
        int widthAt(int level) {
            return width >> level;
        }

        int heightAt(int level) {
            return height >> level;
        }
    }
}
