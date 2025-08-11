package net.minecraft.client.renderer.texture;

import net.minecraft.client.resources.IResourceManager;
import net.radiant.util.NativeImage;

import java.io.IOException;

public class DynamicTexture extends AbstractTexture {
    private final int[] dynamicTextureData;
    private final int width;
    private final int height;

    public DynamicTexture(NativeImage image) {
        this(image.getWidth(), image.getHeight());
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), this.dynamicTextureData, 0, image.getWidth());
        this.updateDynamicTexture();
    }

    public DynamicTexture(int textureWidth, int textureHeight) {
        this.width = textureWidth;
        this.height = textureHeight;
        this.dynamicTextureData = new int[textureWidth * textureHeight];
        TextureUtil.allocateTexture(this.getGlTextureId(), textureWidth, textureHeight);
    }

    public void loadTexture(IResourceManager resourceManager) throws IOException {
    }

    public void updateDynamicTexture() {
        TextureUtil.uploadTexture(this.getGlTextureId(), this.dynamicTextureData, this.width, this.height);
    }

    public int[] getTextureData() {
        return this.dynamicTextureData;
    }
}
