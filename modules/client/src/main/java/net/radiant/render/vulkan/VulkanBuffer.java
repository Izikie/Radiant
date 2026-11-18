package net.radiant.render.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanBuffer implements AutoCloseable {
    private final VkDevice device;
    private final int size;
    private long buffer;
    private long memory;
    private long address;

    private VulkanBuffer(VkDevice device, int size) {
        this.device = device;
        this.size = size;
    }

    static VulkanBuffer mapped(VkPhysicalDevice physicalDevice, VkDevice device, int size, int usage) {
        VulkanBuffer buffer = new VulkanBuffer(device, size);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo info = VkBufferCreateInfo.calloc(stack).sType$Default()
                    .size(size).usage(usage).sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            LongBuffer handle = stack.mallocLong(1);
            VulkanPresenter.check(vkCreateBuffer(device, info, null, handle), "create mapped buffer");
            buffer.buffer = handle.get(0);
            VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device, buffer.buffer, requirements);
            buffer.memory = VulkanMemory.allocate(stack, physicalDevice, device, requirements,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            VulkanPresenter.check(vkBindBufferMemory(device, buffer.buffer, buffer.memory, 0), "bind mapped buffer");
            PointerBuffer address = stack.mallocPointer(1);
            VulkanPresenter.check(vkMapMemory(device, buffer.memory, 0, size, 0, address), "map buffer");
            buffer.address = address.get(0);
            return buffer;
        } catch (RuntimeException | Error e) {
            buffer.close();
            throw e;
        }
    }

    void write(int offset, ByteBuffer source) {
        java.util.Objects.checkFromIndexSize(offset, source.remaining(), size);
        memCopy(memAddress(source), address + offset, source.remaining());
    }

    ByteBuffer data() {
        return memByteBuffer(address, size);
    }

    long handle() {
        return buffer;
    }

    @Override
    public void close() {
        if (address != 0) {
            vkUnmapMemory(device, memory);
            address = 0;
        }
        if (buffer != 0) {
            vkDestroyBuffer(device, buffer, null);
            buffer = 0;
        }
        if (memory != 0) {
            vkFreeMemory(device, memory, null);
            memory = 0;
        }
    }
}
