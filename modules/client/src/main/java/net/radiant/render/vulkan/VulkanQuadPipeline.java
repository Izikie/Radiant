package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

final class VulkanQuadPipeline implements AutoCloseable {
    private static final String VERTEX_SHADER = """
            #version 450
            layout(location = 0) out vec2 uv;
            void main() {
                uv = vec2(gl_VertexIndex & 1, gl_VertexIndex >> 1);
                gl_Position = vec4(uv * 1.2 - 0.6, 0.0, 1.0);
            }
            """;
    private static final String FRAGMENT_SHADER = """
            #version 450
            layout(set = 0, binding = 0) uniform sampler2D checker;
            layout(location = 0) in vec2 uv;
            layout(location = 0) out vec4 color;
            void main() {
                color = texture(checker, uv);
            }
            """;

    private final VkDevice device;
    private long descriptorLayout;
    private long descriptorPool;
    private long descriptorSet;
    private long pipelineLayout;
    private long pipeline;

    VulkanQuadPipeline(VkDevice device) {
        this.device = device;
    }

    void initialize(int format, VulkanTexture texture) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            createDescriptors(stack, texture);
            LongBuffer handle = stack.mallocLong(1);
            VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
                    .pSetLayouts(stack.longs(descriptorLayout));
            VulkanPresenter.check(vkCreatePipelineLayout(device, layoutInfo, null, handle), "create graphics pipeline layout");
            pipelineLayout = handle.get(0);

            pipeline = VulkanGraphics.pipeline(stack, device, pipelineLayout, format, VK_FORMAT_UNDEFINED,
                    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP, false, VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default(),
                    VERTEX_SHADER, FRAGMENT_SHADER);
        }
    }

    private void createDescriptors(MemoryStack stack, VulkanTexture texture) {
        int descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
        LongBuffer handle = stack.mallocLong(1);
        VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
        bindings.get(0).descriptorType(descriptorType).descriptorCount(1).stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
        VkDescriptorSetLayoutCreateInfo layout = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default().pBindings(bindings);
        VulkanPresenter.check(vkCreateDescriptorSetLayout(device, layout, null, handle), "create quad descriptor layout");
        descriptorLayout = handle.get(0);
        VkDescriptorPoolSize.Buffer sizes = VkDescriptorPoolSize.calloc(1, stack);
        sizes.get(0).type(descriptorType).descriptorCount(1);
        VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack).sType$Default()
                .pPoolSizes(sizes).maxSets(1);
        VulkanPresenter.check(vkCreateDescriptorPool(device, poolInfo, null, handle), "create quad descriptor pool");
        descriptorPool = handle.get(0);
        VkDescriptorSetAllocateInfo allocateInfo = VkDescriptorSetAllocateInfo.calloc(stack).sType$Default()
                .descriptorPool(descriptorPool).pSetLayouts(stack.longs(descriptorLayout));
        VulkanPresenter.check(vkAllocateDescriptorSets(device, allocateInfo, handle), "allocate quad descriptor set");
        descriptorSet = handle.get(0);
        VkDescriptorImageInfo.Buffer image = VkDescriptorImageInfo.calloc(1, stack);
        image.get(0).imageView(texture.view()).sampler(texture.sampler()).imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(1, stack);
        writes.get(0).sType$Default().dstSet(descriptorSet).descriptorType(descriptorType).descriptorCount(1).pImageInfo(image);
        vkUpdateDescriptorSets(device, writes, null);
    }

    void draw(MemoryStack stack, VkCommandBuffer commands, int width, int height) {
        VulkanGraphics.viewport(stack, commands, width, height);
        vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        vkCmdBindDescriptorSets(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, stack.longs(descriptorSet), null);
        vkCmdDraw(commands, 4, 1, 0, 0);
    }

    @Override
    public void close() {
        if (pipeline != 0) vkDestroyPipeline(device, pipeline, null);
        if (pipelineLayout != 0) vkDestroyPipelineLayout(device, pipelineLayout, null);
        if (descriptorPool != 0) vkDestroyDescriptorPool(device, descriptorPool, null);
        if (descriptorLayout != 0) vkDestroyDescriptorSetLayout(device, descriptorLayout, null);
    }
}
