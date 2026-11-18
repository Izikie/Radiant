package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanGraphics {
    private VulkanGraphics() {
    }

    static long pipeline(MemoryStack stack, VkDevice device, long layout, int colorFormat, int depthFormat, int topology,
                         VkPipelineVertexInputStateCreateInfo vertexInput, String vertexSource, String fragmentSource) {
        long vertex = shader(stack, device, vertexSource, shaderc_glsl_vertex_shader, "scene.vert");
        long fragment = 0;
        try {
            fragment = shader(stack, device, fragmentSource, shaderc_glsl_fragment_shader, "scene.frag");
            VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
            stages.get(0).sType$Default().stage(VK_SHADER_STAGE_VERTEX_BIT).module(vertex).pName(stack.UTF8("main"));
            stages.get(1).sType$Default().stage(VK_SHADER_STAGE_FRAGMENT_BIT).module(fragment).pName(stack.UTF8("main"));
            VkPipelineRenderingCreateInfo rendering = VkPipelineRenderingCreateInfo.calloc(stack).sType$Default()
                    .pColorAttachmentFormats(stack.ints(colorFormat)).depthAttachmentFormat(depthFormat);
            VkPipelineColorBlendAttachmentState.Buffer blend = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            blend.get(0).colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
            VkGraphicsPipelineCreateInfo.Buffer configurations = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            VkGraphicsPipelineCreateInfo config = configurations.get(0).sType$Default().pNext(rendering.address())
                    .pStages(stages).layout(layout).pVertexInputState(vertexInput)
                    .pInputAssemblyState(VkPipelineInputAssemblyStateCreateInfo.calloc(stack).sType$Default().topology(topology))
                    .pViewportState(VkPipelineViewportStateCreateInfo.calloc(stack).sType$Default().viewportCount(1).scissorCount(1))
                    .pRasterizationState(VkPipelineRasterizationStateCreateInfo.calloc(stack).sType$Default().lineWidth(1.0f))
                    .pMultisampleState(VkPipelineMultisampleStateCreateInfo.calloc(stack).sType$Default().rasterizationSamples(VK_SAMPLE_COUNT_1_BIT))
                    .pColorBlendState(VkPipelineColorBlendStateCreateInfo.calloc(stack).sType$Default().pAttachments(blend))
                    .pDynamicState(VkPipelineDynamicStateCreateInfo.calloc(stack).sType$Default()
                            .pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)));
            if (depthFormat != VK_FORMAT_UNDEFINED) {
                config.pDepthStencilState(VkPipelineDepthStencilStateCreateInfo.calloc(stack).sType$Default()
                        .depthTestEnable(true).depthWriteEnable(true).depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL));
            }
            LongBuffer handle = stack.mallocLong(1);
            VulkanPresenter.check(vkCreateGraphicsPipelines(device, 0, configurations, null, handle), "create graphics pipeline");
            return handle.get(0);
        } finally {
            if (fragment != 0) {
                vkDestroyShaderModule(device, fragment, null);
            }
            vkDestroyShaderModule(device, vertex, null);
        }
    }

    private static long shader(MemoryStack stack, VkDevice device, String source, int kind, String name) {
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

    static void viewport(MemoryStack stack, VkCommandBuffer commands, int width, int height) {
        viewport(stack, commands, 0, 0, width, height);
    }

    static void viewport(MemoryStack stack, VkCommandBuffer commands, int x, int y, int width, int height) {
        VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
        viewport.get(0).x(x).y(y).width(width).height(height).maxDepth(1.0f);
        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
        scissor.get(0).extent().set(width, height);
        scissor.get(0).offset().set(x, y);
        vkCmdSetViewport(commands, 0, viewport);
        vkCmdSetScissor(commands, 0, scissor);
    }
}
