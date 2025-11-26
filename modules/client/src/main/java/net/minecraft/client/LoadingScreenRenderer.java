package net.minecraft.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MinecraftError;
import net.optifine.CustomLoadingScreen;
import net.optifine.CustomLoadingScreens;
import org.lwjgl.opengl.GL11;

public class LoadingScreenRenderer implements IProgressUpdate {
    private String message = "";
    private final Minecraft mc;
    private String currentlyDisplayedText = "";
    private long systemTime = Minecraft.getSystemTime();
    private boolean loadingSuccess;
    private final ScaledResolution scaledResolution;
    private final Framebuffer framebuffer;

    public LoadingScreenRenderer(Minecraft mcIn) {
        this.mc = mcIn;
        this.scaledResolution = new ScaledResolution(mcIn);
        this.framebuffer = new Framebuffer(mcIn.displayWidth, mcIn.displayHeight, false);
        this.framebuffer.setFramebufferFilter(9728);
    }

    @Override
    public void resetProgressAndMessage(String message) {
        this.loadingSuccess = false;
        this.displayString(message);
    }

    @Override
    public void displaySavingString(String message) {
        this.loadingSuccess = true;
        this.displayString(message);
    }

    private void displayString(String message) {
        this.currentlyDisplayedText = message;

        if (!this.mc.running) {
            if (!this.loadingSuccess) {
                throw new MinecraftError();
            }
        } else {
            GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.loadIdentity();

            if (OpenGlHelper.isFramebufferEnabled()) {
                int i = this.scaledResolution.getScaleFactor();
                GlStateManager.ortho(0.0D, (this.scaledResolution.getScaledWidth() * i), (this.scaledResolution.getScaledHeight() * i), 0.0D, 100.0D, 300.0D);
            } else {
                ScaledResolution scaledresolution = new ScaledResolution(this.mc);
                GlStateManager.ortho(0.0D, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(), 0.0D, 100.0D, 300.0D);
            }

            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();
            GlStateManager.translate(0.0F, 0.0F, -200.0F);
        }
    }

    @Override
    public void displayLoadingString(String message) {
        if (!this.mc.running) {
            if (!this.loadingSuccess) {
                throw new MinecraftError();
            }
        } else {
            this.systemTime = 0L;
            this.message = message;
            this.setLoadingProgress(-1);
            this.systemTime = 0L;
        }
    }

    @Override
    public void setLoadingProgress(int progress) {
        if (!this.mc.running) {
            if (!this.loadingSuccess) {
                throw new MinecraftError();
            }
        } else {
            long i = Minecraft.getSystemTime();

            if (i - this.systemTime >= 100L) {
                this.systemTime = i;
                ScaledResolution scaledresolution = new ScaledResolution(this.mc);
                int j = scaledresolution.getScaleFactor();
                int k = scaledresolution.getScaledWidth();
                int l = scaledresolution.getScaledHeight();

                if (OpenGlHelper.isFramebufferEnabled()) {
                    this.framebuffer.framebufferClear();
                } else {
                    GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                }

                this.framebuffer.bindFramebuffer(false);
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.loadIdentity();
                GlStateManager.ortho(0.0D, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(), 0.0D, 100.0D, 300.0D);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.loadIdentity();
                GlStateManager.translate(0.0F, 0.0F, -200.0F);

                if (!OpenGlHelper.isFramebufferEnabled()) {
                    GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                }

                Tessellator tessellator = Tessellator.get();
                WorldRenderer worldrenderer = tessellator.getWorldRenderer();
                CustomLoadingScreen customloadingscreen = CustomLoadingScreens.getCustomLoadingScreen();

                if (customloadingscreen != null) {
                    customloadingscreen.drawBackground(scaledresolution.getScaledWidth(), scaledresolution.getScaledHeight());
                } else {
                    this.mc.getTextureManager().bindTexture(Gui.OPTIONS_BACKGROUND);
                    float f = 32.0F;
                    worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                    worldrenderer.pos(0.0D, l, 0.0D).tex(0.0D, (l / f)).color(64, 64, 64, 255).endVertex();
                    worldrenderer.pos(k, l, 0.0D).tex((k / f), (l / f)).color(64, 64, 64, 255).endVertex();
                    worldrenderer.pos(k, 0.0D, 0.0D).tex((k / f), 0.0D).color(64, 64, 64, 255).endVertex();
                    worldrenderer.pos(0.0D, 0.0D, 0.0D).tex(0.0D, 0.0D).color(64, 64, 64, 255).endVertex();
                    tessellator.draw();
                }

                if (progress >= 0) {
                    int l1 = 100;
                    int i1 = 2;
                    int j1 = k / 2 - l1 / 2;
                    int k1 = l / 2 + 16;
                    GlStateManager.disableTexture2D();
                    worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                    worldrenderer.pos(j1, k1, 0.0D).color(128, 128, 128, 255).endVertex();
                    worldrenderer.pos(j1, (k1 + i1), 0.0D).color(128, 128, 128, 255).endVertex();
                    worldrenderer.pos((j1 + l1), (k1 + i1), 0.0D).color(128, 128, 128, 255).endVertex();
                    worldrenderer.pos((j1 + l1), k1, 0.0D).color(128, 128, 128, 255).endVertex();
                    worldrenderer.pos(j1, k1, 0.0D).color(128, 255, 128, 255).endVertex();
                    worldrenderer.pos(j1, (k1 + i1), 0.0D).color(128, 255, 128, 255).endVertex();
                    worldrenderer.pos((j1 + progress), (k1 + i1), 0.0D).color(128, 255, 128, 255).endVertex();
                    worldrenderer.pos((j1 + progress), k1, 0.0D).color(128, 255, 128, 255).endVertex();
                    tessellator.draw();
                    GlStateManager.enableTexture2D();
                }

                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
                this.mc.fontRendererObj.drawStringWithShadow(this.currentlyDisplayedText, ((k - this.mc.fontRendererObj.getStringWidth(this.currentlyDisplayedText)) / 2), (l / 2 - 4 - 16), 16777215);
                this.mc.fontRendererObj.drawStringWithShadow(this.message, ((k - this.mc.fontRendererObj.getStringWidth(this.message)) / 2), (l / 2 - 4 + 8), 16777215);

                this.framebuffer.unbindFramebuffer();

                if (OpenGlHelper.isFramebufferEnabled()) {
                    this.framebuffer.framebufferRender(k * j, l * j);
                }

                this.mc.updateDisplay();

                try {
                    Thread.yield();
                } catch (Exception _) {
                }
            }
        }
    }

    @Override
    public void setDoneWorking() {
    }
}
