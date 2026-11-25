package net.radiant.render.vulkan;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanTextureBindings implements AutoCloseable {
    record Image(int width, int height, ByteBuffer pixels, int filter, int wrapU, int wrapV) {
        static Image capture() {
            int width = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH);
            int height = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT);
            ByteBuffer pixels = BufferUtils.createByteBuffer(Math.multiplyExact(Math.multiplyExact(width, height), 4));
            glPushClientAttrib(GL_CLIENT_PIXEL_STORE_BIT);
            try {
                glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
                glPixelStorei(GL_PACK_ROW_LENGTH, 0);
                glPixelStorei(GL_PACK_SKIP_ROWS, 0);
                glPixelStorei(GL_PACK_SKIP_PIXELS, 0);
                glPixelStorei(GL_PACK_ALIGNMENT, 4);
                glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            } finally {
                glPopClientAttrib();
            }
            int filter = glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER) == GL_LINEAR ? VK_FILTER_LINEAR : VK_FILTER_NEAREST;
            return new Image(width, height, pixels, filter, wrap(GL_TEXTURE_WRAP_S), wrap(GL_TEXTURE_WRAP_T));
        }

        private static int wrap(int axis) {
            return glGetTexParameteri(GL_TEXTURE_2D, axis) == GL_REPEAT ? VK_SAMPLER_ADDRESS_MODE_REPEAT : VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
        }
    }

    private final VkDevice device;
    private final VulkanTexture[] textures;
    private final Image[] uploaded;
    private final long[] sets;
    private long setLayout, pool;

    VulkanTextureBindings(VkDevice device, int count) {
        this.device = device;
        textures = new VulkanTexture[count];
        uploaded = new Image[count];
        sets = new long[count];
    }

    void initialize(MemoryStack stack) {
        LongBuffer handle = stack.mallocLong(1);
        var binding = VkDescriptorSetLayoutBinding.calloc(1, stack);
        binding.get(0).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1).stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
        VulkanPresenter.check(vkCreateDescriptorSetLayout(device, VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default()
                .pBindings(binding), null, handle), "create texture descriptor layout");
        setLayout = handle.get(0);
        var size = VkDescriptorPoolSize.calloc(1, stack);
        size.get(0).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(sets.length);
        VulkanPresenter.check(vkCreateDescriptorPool(device, VkDescriptorPoolCreateInfo.calloc(stack).sType$Default()
                .maxSets(sets.length).pPoolSizes(size), null, handle), "create texture descriptor pool");
        pool = handle.get(0);
        LongBuffer handles = stack.mallocLong(sets.length);
        LongBuffer layouts = stack.mallocLong(sets.length);
        while (layouts.hasRemaining()) layouts.put(setLayout);
        layouts.flip();
        VulkanPresenter.check(vkAllocateDescriptorSets(device, VkDescriptorSetAllocateInfo.calloc(stack).sType$Default()
                .descriptorPool(pool).pSetLayouts(layouts), handles), "allocate texture descriptors");
        handles.get(sets);
    }

    long layout() { return setLayout; }

    void prepare(VkPhysicalDevice gpu, VkQueue queue, VkCommandBuffer commands, int slot, Image image) {
        if (uploaded[slot] == image) return;
        if (textures[slot] != null) textures[slot].close();
        var texture = textures[slot] = new VulkanTexture(device);
        texture.initialize(gpu, queue, commands, image.width, image.height, new ByteBuffer[]{image.pixels},
                image.filter, image.wrapU, image.wrapV);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var info = VkDescriptorImageInfo.calloc(1, stack);
            info.get(0).sampler(texture.sampler()).imageView(texture.view()).imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            var write = VkWriteDescriptorSet.calloc(1, stack);
            write.get(0).sType$Default().dstSet(sets[slot]).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1).pImageInfo(info);
            vkUpdateDescriptorSets(device, write, null);
        }
        uploaded[slot] = image;
    }

    void bind(MemoryStack stack, VkCommandBuffer commands, long layout, int slot) {
        vkCmdBindDescriptorSets(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, layout, 0, stack.longs(sets[slot]), null);
    }

    @Override
    public void close() {
        if (pool != 0) vkDestroyDescriptorPool(device, pool, null);
        if (setLayout != 0) vkDestroyDescriptorSetLayout(device, setLayout, null);
        for (VulkanTexture texture : textures) if (texture != null) texture.close();
    }
}
