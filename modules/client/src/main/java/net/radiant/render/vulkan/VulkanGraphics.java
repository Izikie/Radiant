package net.radiant.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanGraphics {
    private VulkanGraphics() {
    }

    static boolean isSrgb(int format) {
        return format == VK_FORMAT_B8G8R8A8_SRGB || format == VK_FORMAT_R8G8B8A8_SRGB
                || format == VK_FORMAT_A8B8G8R8_SRGB_PACK32;
    }

    static boolean supportsTwoPixelLines(VkPhysicalDevice device, MemoryStack stack) {
        VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.malloc(stack);
        vkGetPhysicalDeviceFeatures(device, features);
        VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(stack);
        vkGetPhysicalDeviceProperties(device, properties);
        return features.wideLines() && properties.limits().lineWidthRange(0) <= 2.0f
                && properties.limits().lineWidthRange(1) >= 2.0f;
    }

    static long pipeline(MemoryStack stack, VkDevice device, long layout, int colorFormat, int depthFormat, int topology, Pass pass,
                         VkPipelineVertexInputStateCreateInfo vertexInput, String vertexSource, String fragmentSource) {
        long vertex = vertexShader(stack, device, vertexSource);
        long fragment = 0;
        try {
            fragment = fragmentShader(stack, device, fragmentSource);
            return pipeline(stack, device, layout, colorFormat, depthFormat, topology, pass.state, vertexInput, vertex, fragment, 1.0f);
        } finally {
            if (fragment != 0) {
                vkDestroyShaderModule(device, fragment, null);
            }
            vkDestroyShaderModule(device, vertex, null);
        }
    }

    static long pipeline(MemoryStack stack, VkDevice device, long layout, int colorFormat, int depthFormat, int topology, State state,
                         VkPipelineVertexInputStateCreateInfo vertexInput, long vertex, long fragment, float lineWidth) {
        VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
        stages.get(0).sType$Default().stage(VK_SHADER_STAGE_VERTEX_BIT).module(vertex).pName(stack.UTF8("main"));
        stages.get(1).sType$Default().stage(VK_SHADER_STAGE_FRAGMENT_BIT).module(fragment).pName(stack.UTF8("main"));
        VkPipelineRenderingCreateInfo rendering = VkPipelineRenderingCreateInfo.calloc(stack).sType$Default()
                .pColorAttachmentFormats(stack.ints(colorFormat)).depthAttachmentFormat(depthFormat);
        VkPipelineColorBlendAttachmentState.Buffer blend = VkPipelineColorBlendAttachmentState.calloc(1, stack);
        blend.get(0).colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
        VkPipelineRasterizationStateCreateInfo rasterization = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                .sType$Default().lineWidth(lineWidth);
        if (state.blend != null) {
            blend.get(0).blendEnable(true)
                    .srcColorBlendFactor(state.blend.sourceColor).dstColorBlendFactor(state.blend.destinationColor)
                    .colorBlendOp(VK_BLEND_OP_ADD)
                    .srcAlphaBlendFactor(state.blend.sourceAlpha).dstAlphaBlendFactor(state.blend.destinationAlpha)
                    .alphaBlendOp(VK_BLEND_OP_ADD);
        }
        rasterization.cullMode(state.cullMode).frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
        if (state.depthBias != null) {
            rasterization.depthBiasEnable(true).depthBiasConstantFactor(state.depthBias.constant)
                    .depthBiasSlopeFactor(state.depthBias.slope);
        }
        VkGraphicsPipelineCreateInfo.Buffer configurations = VkGraphicsPipelineCreateInfo.calloc(1, stack);
        VkGraphicsPipelineCreateInfo config = configurations.get(0).sType$Default().pNext(rendering.address())
                .pStages(stages).layout(layout).pVertexInputState(vertexInput)
                .pInputAssemblyState(VkPipelineInputAssemblyStateCreateInfo.calloc(stack).sType$Default().topology(topology))
                .pViewportState(VkPipelineViewportStateCreateInfo.calloc(stack).sType$Default().viewportCount(1).scissorCount(1))
                .pRasterizationState(rasterization)
                .pMultisampleState(VkPipelineMultisampleStateCreateInfo.calloc(stack).sType$Default().rasterizationSamples(VK_SAMPLE_COUNT_1_BIT))
                .pColorBlendState(VkPipelineColorBlendStateCreateInfo.calloc(stack).sType$Default().pAttachments(blend))
                .pDynamicState(VkPipelineDynamicStateCreateInfo.calloc(stack).sType$Default()
                        .pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)));
        if (depthFormat != VK_FORMAT_UNDEFINED) {
            config.pDepthStencilState(VkPipelineDepthStencilStateCreateInfo.calloc(stack).sType$Default()
                    .depthTestEnable(state.depthTest).depthWriteEnable(state.depthWrite).depthCompareOp(state.depthCompare));
        }
        LongBuffer handle = stack.mallocLong(1);
        VulkanPresenter.check(vkCreateGraphicsPipelines(device, 0, configurations, null, handle), "create graphics pipeline");
        return handle.get(0);
    }

    enum Pass {
        OPAQUE(true, true, false, false),
        TERRAIN_OPAQUE(true, true, false, true),
        TRANSLUCENT(true, false, true, true),
        SKY(false, false, false, false),
        SKY_ADDITIVE(new State(false, false, VK_COMPARE_OP_ALWAYS, VK_CULL_MODE_BACK_BIT,
                new Blend(VK_BLEND_FACTOR_SRC_ALPHA, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO), null)),
        SKY_ADDITIVE_OVERLAY(new State(false, false, VK_COMPARE_OP_ALWAYS, VK_CULL_MODE_NONE,
                new Blend(VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO), null)),
        BLENDED_DEPTH_WRITE(true, true, true, false),
        OBJECT_OVERLAY(false, false, true, false);

        final State state;

        Pass(boolean depthTest, boolean depthWrite, boolean blend, boolean cull) {
            state = new State(depthTest, depthWrite, VK_COMPARE_OP_LESS_OR_EQUAL,
                    cull ? VK_CULL_MODE_BACK_BIT : VK_CULL_MODE_NONE, blend ? Blend.ALPHA : null, null);
        }

        Pass(State state) {
            this.state = state;
        }
    }

    record State(boolean depthTest, boolean depthWrite, int depthCompare, int cullMode, Blend blend, DepthBias depthBias) {
    }

    record DepthBias(float constant, float slope) {
    }

    record Blend(int sourceColor, int destinationColor, int sourceAlpha, int destinationAlpha) {
        private static final Blend ALPHA = new Blend(VK_BLEND_FACTOR_SRC_ALPHA, VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA,
                VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
    }

    static long vertexShader(MemoryStack stack, VkDevice device, String source) {
        return shader(stack, device, source, shaderc_glsl_vertex_shader, "scene.vert");
    }

    static long fragmentShader(MemoryStack stack, VkDevice device, String source) {
        return shader(stack, device, source, shaderc_glsl_fragment_shader, "scene.frag");
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
