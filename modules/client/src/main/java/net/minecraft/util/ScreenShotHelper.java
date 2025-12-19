package net.minecraft.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.chat.ChatComponentText;
import net.minecraft.util.chat.ChatComponentTranslation;
import net.minecraft.util.chat.ClickEvent;
import net.minecraft.util.chat.IChatComponent;
import net.optifine.Config;
import net.radiant.util.NativeImage;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.IntBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

// TODO: Implement Better Screenshot Functionality
public class ScreenShotHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenShotHelper.class);
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    private static IntBuffer pixelBuffer;
    private static int[] pixelValues;

    public static IChatComponent saveScreenshot(File gameDirectory, int width, int height, Framebuffer buffer) {
        return saveScreenshot(gameDirectory, null, width, height, buffer);
    }

    public static IChatComponent saveScreenshot(File gameDirectory, String screenshotName, int width, int height, Framebuffer buffer) {
        try {
            File folder = new File(gameDirectory, "screenshots");
            folder.mkdir();

            Minecraft minecraft = Minecraft.get();
            int guiScale = Config.getGameSettings().guiScale;
            ScaledResolution scaledresolution = new ScaledResolution(minecraft);
            int scaleFactor = scaledresolution.getScaleFactor();
            int screenshotSize = Config.getScreenshotSize();
            boolean flag = OpenGlHelper.isFramebufferEnabled() && screenshotSize > 1;

            if (flag) {
                Config.getGameSettings().guiScale = scaleFactor * screenshotSize;
                resize(width * screenshotSize, height * screenshotSize);
                GlStateManager.pushMatrix();
                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                minecraft.getFramebuffer().bindFramebuffer(true);
                minecraft.entityRenderer.updateCameraAndRender(Config.renderPartialTicks, System.nanoTime());
            }

            if (OpenGlHelper.isFramebufferEnabled()) {
                width = buffer.framebufferTextureWidth;
                height = buffer.framebufferTextureHeight;
            }

            int l = width * height;

            if (pixelBuffer == null || pixelBuffer.capacity() < l) {
                pixelBuffer = BufferUtils.createIntBuffer(l);
                pixelValues = new int[l];
            }

            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            pixelBuffer.clear();

            if (OpenGlHelper.isFramebufferEnabled()) {
                GlStateManager.bindTexture(buffer.framebufferTexture);
                GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
            } else {
                GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
            }

            pixelBuffer.get(pixelValues);
            TextureUtil.processPixelValues(pixelValues, width, height);

            NativeImage image;
            if (OpenGlHelper.isFramebufferEnabled()) {
                image = NativeImage.createBlankImage(buffer.framebufferWidth, buffer.framebufferHeight);
                int i1 = buffer.framebufferTextureHeight - buffer.framebufferHeight;

                for (int j1 = i1; j1 < buffer.framebufferTextureHeight; ++j1) {
                    for (int k1 = 0; k1 < buffer.framebufferWidth; ++k1) {
                        image.setPixel(k1, j1 - i1, pixelValues[j1 * buffer.framebufferTextureWidth + k1] | 0xFF000000);
                    }
                }
            } else {
                image = NativeImage.createBlankImage(width, height);
                image.setRGBNoAlpha(0, 0, width, height, pixelValues);
            }

            if (flag) {
                minecraft.getFramebuffer().unbindFramebuffer();
                GlStateManager.popMatrix();
                Config.getGameSettings().guiScale = guiScale;
                resize(width, height);
            }

            File file2;

            if (screenshotName == null) {
                file2 = getTimestampedPNGFileForDirectory(folder);
            } else {
                file2 = new File(folder, screenshotName);
            }

            file2 = file2.getCanonicalFile();
            image.saveToFile(file2, NativeImage.FileFormat.PNG);
            IChatComponent component = new ChatComponentText(file2.getName());
            component.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file2.getAbsolutePath()));
            component.getChatStyle().setUnderlined(Boolean.TRUE);
            return new ChatComponentTranslation("screenshot.success", component);
        } catch (Exception exception) {
            LOGGER.warn("Couldn't save screenshot", exception);
            return new ChatComponentTranslation("screenshot.failure", exception.getMessage());
        }
    }

    private static File getTimestampedPNGFileForDirectory(File gameDirectory) {
        String s = DATE_FORMAT.format(new Date());
        int i = 1;

        while (true) {
            File file1 = new File(gameDirectory, s + (i == 1 ? "" : "_" + i) + ".png");

            if (!file1.exists()) {
                return file1;
            }

            ++i;
        }
    }

    private static void resize(int width, int height) {
        Minecraft minecraft = Minecraft.get();
        minecraft.displayWidth = Math.max(1, width);
        minecraft.displayHeight = Math.max(1, height);

        if (minecraft.currentScreen != null) {
            ScaledResolution resolution = new ScaledResolution(minecraft);
            minecraft.currentScreen.onResize(minecraft, resolution.getScaledWidth(), resolution.getScaledHeight());
        }

        updateFramebufferSize();
    }

    private static void updateFramebufferSize() {
        Minecraft mc = Minecraft.get();
        mc.getFramebuffer().createBindFramebuffer(mc.displayWidth, mc.displayHeight);

        if (mc.entityRenderer != null) {
            mc.entityRenderer.updateShaderGroupSize(mc.displayWidth, mc.displayHeight);
        }
    }
}
