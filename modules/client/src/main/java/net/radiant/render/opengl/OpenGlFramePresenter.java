package net.radiant.render.opengl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.radiant.lwjgl.opengl.Display;
import net.radiant.render.FramePresenter;
import net.radiant.util.NativeImage;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memFree;

public final class OpenGlFramePresenter implements FramePresenter {
    private final Minecraft client;

    public OpenGlFramePresenter(Minecraft client) {
        this.client = client;
    }

    @Override
    public void prepareFrame() {
        GlStateManager.pushMatrix();
        client.getFramebuffer().framebufferRender(client.displayWidth, client.displayHeight);
        GlStateManager.popMatrix();
    }

    @Override
    public NativeImage readFrame() {
        int width = client.displayWidth;
        int height = client.displayHeight;
        NativeImage image = NativeImage.createBlankImage(width, height);
        IntBuffer pixels = memAllocInt(width * height);
        int packAlignment = glGetInteger(GL_PACK_ALIGNMENT);
        int readBuffer = glGetInteger(GL_READ_BUFFER);
        try {
            // The OpenGL frame has been composited and the back buffer has yet to be swapped.
            glReadBuffer(GL_BACK);
            glPixelStorei(GL_PACK_ALIGNMENT, 1);
            glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, pixels);
            for (int y = 0; y < height; y++) {
                int row = (height - 1 - y) * width;
                for (int x = 0; x < width; x++) {
                    image.setPixel(x, y, pixels.get(row + x) | 0xFF000000);
                }
            }
            return image;
        } finally {
            glPixelStorei(GL_PACK_ALIGNMENT, packAlignment);
            glReadBuffer(readBuffer);
            memFree(pixels);
        }
    }

    @Override
    public void present() {
        client.updateDisplay();
    }

    @Override
    public void close() {
        Display.destroy();
    }
}
