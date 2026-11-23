package net.radiant.render.vulkan;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.radiant.render.opengl.OpenGlTextureBackend;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;

import static org.lwjgl.system.MemoryUtil.memCalloc;
import static org.lwjgl.system.MemoryUtil.memFree;

/** Keeps the OpenGL texture uploads and supplies the Vulkan preview from CPU texture data. */
public final class VulkanPreviewTextureBackend extends OpenGlTextureBackend {
    public static final VulkanPreviewTextureBackend INSTANCE = new VulkanPreviewTextureBackend();

    private AtlasSource atlas;

    private VulkanPreviewTextureBackend() {
    }

    @Override
    public void atlasStitched(TextureMap texture) {
        Minecraft mc = Minecraft.get();
        if (mc == null || texture != mc.getTextureMapBlocks()) {
            return;
        }

        // Model baking releases the frame lists for static sprites after stitching.
        Map<TextureAtlasSprite, int[][]> frames = new IdentityHashMap<>();
        for (TextureAtlasSprite sprite : texture.getUploadedSprites()) {
            frames.put(sprite, sprite.getFrameTextureData(0));
        }
        atlas = new AtlasSource(texture.atlasWidth, texture.atlasHeight, texture.getMipmapLevels(), frames);
        VulkanTerrainPreview.atlasStitched();
    }

    @Override
    public void atlasAnimationUploaded(TextureAtlasSprite sprite, int[][] pixels) {
        if (atlas != null && atlas.frames().replace(sprite, pixels) != null) {
            VulkanTerrainPreview.updateSprite(sprite, pixels);
        }
    }

    VulkanTerrainScene.Atlas snapshotAtlas() {
        AtlasSource source = this.atlas;
        int width = source.width();
        int height = source.height();
        if (width <= 0 || height <= 0) {
            throw new IllegalStateException("Block atlas has not been stitched");
        }
        ByteBuffer[] levels = new ByteBuffer[source.mipLevels() + 1];
        try {
            for (int level = 0; level < levels.length; level++) {
                int mipWidth = width >> level;
                int mipHeight = height >> level;
                levels[level] = memCalloc(Math.multiplyExact(Math.multiplyExact(mipWidth, mipHeight), 4));
            }
            for (var entry : source.frames().entrySet()) {
                TextureAtlasSprite sprite = entry.getKey();
                int[][] pixels = entry.getValue();
                for (int level = 0; level < Math.min(levels.length, pixels.length); level++) {
                    int mipWidth = width >> level;
                    int spriteWidth = sprite.getIconWidth() >> level;
                    int spriteHeight = sprite.getIconHeight() >> level;
                    int x = sprite.getOriginX() >> level;
                    int y = sprite.getOriginY() >> level;
                    ByteBuffer mip = levels[level];
                    for (int row = 0; row < spriteHeight; row++) {
                        mip.position(((y + row) * mipWidth + x) * 4);
                        VulkanAtlasUpdates.writeRgba(mip, pixels[level], row * spriteWidth, spriteWidth);
                    }
                }
            }
            for (ByteBuffer mip : levels) {
                mip.clear();
            }
            return new VulkanTerrainScene.Atlas(width, height, levels);
        } catch (RuntimeException | Error e) {
            for (ByteBuffer mip : levels) {
                memFree(mip);
            }
            throw e;
        }
    }

    private record AtlasSource(int width, int height, int mipLevels, Map<TextureAtlasSprite, int[][]> frames) {
    }
}
