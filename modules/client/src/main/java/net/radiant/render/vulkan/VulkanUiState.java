package net.radiant.render.vulkan;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanUiState {
    private VulkanUiState() {
    }

    static VulkanGraphics.State capture(boolean depthTest, boolean depthWrite, boolean blending) {
        int cullMode = VK_CULL_MODE_NONE;
        if (glIsEnabled(GL_CULL_FACE)) {
            cullMode = switch (glGetInteger(GL_CULL_FACE_MODE)) {
                case GL_FRONT -> VK_CULL_MODE_FRONT_BIT;
                case GL_FRONT_AND_BACK -> VK_CULL_MODE_FRONT_AND_BACK;
                default -> VK_CULL_MODE_BACK_BIT;
            };
        }
        VulkanGraphics.Blend blend = blending ? new VulkanGraphics.Blend(
                blendFactor(glGetInteger(GL_BLEND_SRC_RGB)), blendFactor(glGetInteger(GL_BLEND_DST_RGB)),
                blendFactor(glGetInteger(GL_BLEND_SRC_ALPHA)), blendFactor(glGetInteger(GL_BLEND_DST_ALPHA))) : null;
        int compare = depthTest ? depthCompare(glGetInteger(GL_DEPTH_FUNC)) : VK_COMPARE_OP_ALWAYS;
        return new VulkanGraphics.State(depthTest, depthTest && depthWrite, compare, cullMode, blend);
    }

    private static int depthCompare(int function) {
        return switch (function) {
            case GL_NEVER -> VK_COMPARE_OP_NEVER;
            case GL_LESS -> VK_COMPARE_OP_LESS;
            case GL_EQUAL -> VK_COMPARE_OP_EQUAL;
            case GL_GREATER -> VK_COMPARE_OP_GREATER;
            case GL_NOTEQUAL -> VK_COMPARE_OP_NOT_EQUAL;
            case GL_GEQUAL -> VK_COMPARE_OP_GREATER_OR_EQUAL;
            case GL_ALWAYS -> VK_COMPARE_OP_ALWAYS;
            default -> VK_COMPARE_OP_LESS_OR_EQUAL;
        };
    }

    private static int blendFactor(int factor) {
        return switch (factor) {
            case GL_ZERO -> VK_BLEND_FACTOR_ZERO;
            case GL_SRC_COLOR -> VK_BLEND_FACTOR_SRC_COLOR;
            case GL_ONE_MINUS_SRC_COLOR -> VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
            case GL_DST_COLOR -> VK_BLEND_FACTOR_DST_COLOR;
            case GL_ONE_MINUS_DST_COLOR -> VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
            case GL_SRC_ALPHA -> VK_BLEND_FACTOR_SRC_ALPHA;
            case GL_ONE_MINUS_SRC_ALPHA -> VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            case GL_DST_ALPHA -> VK_BLEND_FACTOR_DST_ALPHA;
            case GL_ONE_MINUS_DST_ALPHA -> VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA;
            case GL_SRC_ALPHA_SATURATE -> VK_BLEND_FACTOR_SRC_ALPHA_SATURATE;
            default -> VK_BLEND_FACTOR_ONE;
        };
    }
}
