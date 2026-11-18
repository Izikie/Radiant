package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_UNDEFINED;

final class VulkanQuadScene implements VulkanScene {
    private VkDevice device;
    private VulkanTexture texture;
    private VulkanQuadPipeline pipeline;

    @Override
    public void initialize(VkPhysicalDevice physicalDevice, VkDevice device, VkQueue queue, VkCommandBuffer commands) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int size = 8;
            ByteBuffer pixels = stack.malloc(size * size * 4);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    boolean bright = ((x ^ y) & 1) == 0;
                    pixels.put((byte) (bright ? 250 : 36)).put((byte) (bright ? 183 : 72))
                            .put((byte) (bright ? 75 : 180)).put((byte) 255);
                }
            }
            texture = new VulkanTexture(device);
            texture.initialize(physicalDevice, queue, commands, size, size, new ByteBuffer[]{pixels.flip()}, true);
        }
    }

    @Override
    public int depthFormat() {
        return VK_FORMAT_UNDEFINED;
    }

    @Override
    public void configureTarget(int colorFormat) {
        if (pipeline != null) {
            pipeline.close();
        }
        pipeline = new VulkanQuadPipeline(device);
        pipeline.initialize(colorFormat, texture);
    }

    @Override
    public void prepareFrame() {
    }

    @Override
    public void draw(MemoryStack stack, VkCommandBuffer commands, int width, int height) {
        pipeline.draw(stack, commands, width, height);
    }

    @Override
    public void close() {
        if (pipeline != null) {
            pipeline.close();
        }
        if (texture != null) {
            texture.close();
        }
    }
}
