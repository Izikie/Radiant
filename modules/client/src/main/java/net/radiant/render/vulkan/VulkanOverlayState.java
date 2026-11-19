package net.radiant.render.vulkan;

import net.minecraft.client.renderer.GlStateManager;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanOverlayState {
    private VulkanOverlayState() {
    }

    static VulkanGraphics.State capture(GlStateManager.DrawState draw) {
        int cullMode = switch (draw.cullFace()) {
            case GL_FRONT -> VK_CULL_MODE_FRONT_BIT;
            case GL_BACK -> VK_CULL_MODE_BACK_BIT;
            case GL_FRONT_AND_BACK -> VK_CULL_MODE_FRONT_AND_BACK;
            default -> VK_CULL_MODE_NONE;
        };
        VulkanGraphics.Blend blend = draw.blending() ? new VulkanGraphics.Blend(
                blendFactor(draw.sourceColor()), blendFactor(draw.destinationColor()),
                blendFactor(draw.sourceAlpha()), blendFactor(draw.destinationAlpha())) : null;
        int compare = draw.depthTest() ? depthCompare(draw.depthFunction()) : VK_COMPARE_OP_ALWAYS;
        VulkanGraphics.DepthBias bias = draw.polygonOffset()
                ? new VulkanGraphics.DepthBias(draw.polygonUnits(), draw.polygonFactor()) : null;
        return new VulkanGraphics.State(draw.depthTest(), draw.depthTest() && draw.depthWrite(), compare, cullMode, blend, bias);
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
