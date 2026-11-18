package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.util.shaderc.Shaderc.*;
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

            long vertex = createShader(stack, VERTEX_SHADER, shaderc_glsl_vertex_shader, "quad.vert");
            long fragment = 0;
            try {
                fragment = createShader(stack, FRAGMENT_SHADER, shaderc_glsl_fragment_shader, "quad.frag");
                createPipeline(stack, handle, format, vertex, fragment);
            } finally {
                if (fragment != 0) vkDestroyShaderModule(device, fragment, null);
                vkDestroyShaderModule(device, vertex, null);
            }
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

    private void createPipeline(MemoryStack stack, LongBuffer handle, int format, long vertex, long fragment) {
        VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
        stages.get(0).sType$Default().stage(VK_SHADER_STAGE_VERTEX_BIT).module(vertex).pName(stack.UTF8("main"));
        stages.get(1).sType$Default().stage(VK_SHADER_STAGE_FRAGMENT_BIT).module(fragment).pName(stack.UTF8("main"));
        VkPipelineRenderingCreateInfo rendering = VkPipelineRenderingCreateInfo.calloc(stack).sType$Default()
                .pColorAttachmentFormats(stack.ints(format));
        VkPipelineColorBlendAttachmentState.Buffer blend = VkPipelineColorBlendAttachmentState.calloc(1, stack);
        blend.get(0).colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
        VkGraphicsPipelineCreateInfo.Buffer configurations = VkGraphicsPipelineCreateInfo.calloc(1, stack);
        configurations.get(0).sType$Default().pNext(rendering.address()).pStages(stages).layout(pipelineLayout)
                .pVertexInputState(VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default())
                .pInputAssemblyState(VkPipelineInputAssemblyStateCreateInfo.calloc(stack).sType$Default().topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP))
                .pViewportState(VkPipelineViewportStateCreateInfo.calloc(stack).sType$Default().viewportCount(1).scissorCount(1))
                .pRasterizationState(VkPipelineRasterizationStateCreateInfo.calloc(stack).sType$Default().lineWidth(1.0f))
                .pMultisampleState(VkPipelineMultisampleStateCreateInfo.calloc(stack).sType$Default().rasterizationSamples(VK_SAMPLE_COUNT_1_BIT))
                .pColorBlendState(VkPipelineColorBlendStateCreateInfo.calloc(stack).sType$Default().pAttachments(blend))
                .pDynamicState(VkPipelineDynamicStateCreateInfo.calloc(stack).sType$Default()
                        .pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)));
        VulkanPresenter.check(vkCreateGraphicsPipelines(device, 0, configurations, null, handle), "create quad pipeline");
        pipeline = handle.get(0);
    }

    private long createShader(MemoryStack stack, String source, int kind, String name) {
        long compiler = shaderc_compiler_initialize();
        if (compiler == 0) {
            throw new IllegalStateException("Could not initialize shaderc");
        }
        try {
            long result = shaderc_compile_into_spv(compiler, source, kind, name, "main", 0);
            if (result == 0) {
                throw new IllegalStateException("Could not compile " + name);
            }
            try {
                if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
                    throw new IllegalStateException(shaderc_result_get_error_message(result));
                }
                VkShaderModuleCreateInfo info = VkShaderModuleCreateInfo.calloc(stack).sType$Default()
                        .pCode(shaderc_result_get_bytes(result));
                LongBuffer handle = stack.mallocLong(1);
                VulkanPresenter.check(vkCreateShaderModule(device, info, null, handle), "create module for " + name);
                return handle.get(0);
            } finally {
                shaderc_result_release(result);
            }
        } finally {
            shaderc_compiler_release(compiler);
        }
    }

    void draw(MemoryStack stack, VkCommandBuffer commands, int width, int height) {
        VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
        viewport.get(0).width(width).height(height).maxDepth(1.0f);
        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
        scissor.get(0).extent().set(width, height);
        vkCmdSetViewport(commands, 0, viewport);
        vkCmdSetScissor(commands, 0, scissor);
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
