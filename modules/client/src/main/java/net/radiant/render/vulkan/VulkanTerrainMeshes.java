package net.radiant.render.vulkan;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;

final class VulkanTerrainMeshes implements AutoCloseable {
    static final int STRIDE = 28;
    private static final int PAGE_SIZE = 8 * 1024 * 1024;

    private final VkPhysicalDevice physicalDevice;
    private final VkDevice device;
    private final List<Page> pages = new ArrayList<>();
    private final List<Slice> retired = new ArrayList<>();
    private final List<Slice> awaitingFence = new ArrayList<>();

    VulkanTerrainMeshes(VkPhysicalDevice physicalDevice, VkDevice device) {
        this.physicalDevice = physicalDevice;
        this.device = device;
    }

    Slice upload(ByteBuffer vertices) {
        int size = vertices.remaining();
        for (Page page : pages) {
            int offset = page.allocate(size);
            if (offset >= 0) {
                page.buffer.write(offset, vertices);
                return new Slice(page, offset, size);
            }
        }
        int capacity = Math.max(PAGE_SIZE, size);
        Page page = new Page(VulkanBuffer.mapped(physicalDevice, device, capacity, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT), capacity);
        pages.add(page);
        int offset = page.allocate(size);
        page.buffer.write(offset, vertices);
        return new Slice(page, offset, size);
    }

    void retire(Slice slice) {
        retired.add(slice);
    }

    void releaseRetired() {
        // Keep slices alive through the fence for the last frame that could have extracted them.
        for (Slice slice : awaitingFence) {
            slice.page.release(slice.offset, slice.size);
        }
        awaitingFence.clear();
        awaitingFence.addAll(retired);
        retired.clear();
    }

    @Override
    public void close() {
        for (Page page : pages) {
            page.buffer.close();
        }
        pages.clear();
        retired.clear();
        awaitingFence.clear();
    }

    record Slice(Page page, int offset, int size) {
        long buffer() {
            return page.buffer.handle();
        }

        int quads() {
            return size / (STRIDE * 4);
        }
    }

    private static final class Page {
        private final VulkanBuffer buffer;
        private final TreeMap<Integer, Integer> free = new TreeMap<>();

        private Page(VulkanBuffer buffer, int capacity) {
            this.buffer = buffer;
            free.put(0, capacity);
        }

        private int allocate(int size) {
            var ranges = free.entrySet().iterator();
            while (ranges.hasNext()) {
                Map.Entry<Integer, Integer> range = ranges.next();
                int offset = range.getKey();
                int available = range.getValue();
                if (available >= size) {
                    ranges.remove();
                    if (available > size) {
                        free.put(offset + size, available - size);
                    }
                    return offset;
                }
            }
            return -1;
        }

        private void release(int offset, int size) {
            Map.Entry<Integer, Integer> previous = free.lowerEntry(offset);
            if (previous != null && previous.getKey() + previous.getValue() == offset) {
                offset = previous.getKey();
                size += previous.getValue();
                free.remove(previous.getKey());
            }
            Integer nextSize = free.remove(offset + size);
            free.put(offset, size + (nextSize == null ? 0 : nextSize));
        }
    }
}
