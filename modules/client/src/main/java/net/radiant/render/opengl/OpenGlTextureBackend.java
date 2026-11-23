package net.radiant.render.opengl;

import net.minecraft.client.renderer.GlStateManager;
import net.radiant.render.TextureBackend;
import net.radiant.render.TextureHandle;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class OpenGlTextureBackend implements TextureBackend {
    @Override
    public TextureHandle create() {
        return new TextureHandle(GlStateManager.generateTexture());
    }

    @Override
    public void delete(TextureHandle texture) {
        GlStateManager.deleteTexture(texture.legacyId());
    }

    @Override
    public void bind(TextureHandle texture) {
        GlStateManager.bindTexture(texture.legacyId());
    }

    @Override
    public void allocateBound(int maxLevel, int width, int height) {
        if (maxLevel >= 0) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, maxLevel);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0.0F);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, maxLevel);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0F);
        }

        for (int level = 0; level <= maxLevel; level++) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, level, GL11.GL_RGBA, width >> level, height >> level,
                    0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer) null);
        }
    }

    @Override
    public void uploadBoundArgb(int level, int x, int y, int width, int height, IntBuffer pixels) {
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, x, y, width, height, GL12.GL_BGRA,
                GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixels);
    }

    @Override
    public void uploadBoundRgba(int level, int x, int y, int width, int height, ByteBuffer pixels) {
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, x, y, width, height, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, pixels);
    }
}
