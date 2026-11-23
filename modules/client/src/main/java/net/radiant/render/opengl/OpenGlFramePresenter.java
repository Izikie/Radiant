package net.radiant.render.opengl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.radiant.lwjgl.opengl.Display;
import net.radiant.render.FramePresenter;

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
    public void present() {
        client.updateDisplay();
    }

    @Override
    public void close() {
        Display.destroy();
    }
}
