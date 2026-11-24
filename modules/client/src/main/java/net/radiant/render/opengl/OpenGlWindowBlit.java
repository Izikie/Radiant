package net.radiant.render.opengl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.optifine.Config;
import net.radiant.lwjgl.opengl.Display;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class OpenGlWindowBlit {
    private OpenGlWindowBlit() {
    }

    public static boolean canBlit() {
        // The blit copies all color channels, so use the quad if the window displays alpha.
        // The quad can also be affected by fixed-function state that a blit bypasses.
        return GL.getCapabilities().OpenGL30
                && !Config.isShaders()
                && GLFW.glfwGetWindowAttrib(Display.getHandle(), GLFW.GLFW_TRANSPARENT_FRAMEBUFFER) == GLFW.GLFW_FALSE
                && GlStateManager.getActiveTextureUnit() == OpenGlHelper.defaultTexUnit
                && GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) == 0
                && GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) == 0
                && GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING) == 0
                && GL11.glGetInteger(GL11.GL_DRAW_BUFFER) == GL11.GL_BACK
                && GL11.glGetInteger(GL11.GL_RED_BITS) == 8
                && GL11.glGetInteger(GL11.GL_GREEN_BITS) == 8
                && GL11.glGetInteger(GL11.GL_BLUE_BITS) == 8
                && GL11.glGetInteger(GL13.GL_SAMPLE_BUFFERS) == 0
                && !GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB)
                && !GL11.glIsEnabled(GL11.GL_FOG)
                && !GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP)
                && !GL11.glIsEnabled(GL11.GL_STENCIL_TEST)
                && !GL11.glIsEnabled(GL11.GL_POLYGON_SMOOTH)
                && (!GL11.glIsEnabled(GL11.GL_CULL_FACE) || GL11.glGetInteger(GL11.GL_CULL_FACE_MODE) == GL11.GL_BACK);
    }

    public static void copyColor(int sourceFramebuffer, int width, int height) {
        // The draw framebuffer is the window. Bind the source for reading only.
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceFramebuffer);
        try {
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
        }
    }
}
