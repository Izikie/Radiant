package net.radiant.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.radiant.util.NativeImage;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public interface TextureBackend {
    TextureHandle create();

    void delete(TextureHandle texture);

    void bind(TextureHandle texture);

    void allocateBound(int maxLevel, int width, int height);

    void uploadBoundArgb(int level, int x, int y, int width, int height, IntBuffer pixels);

    void uploadBoundRgba(int level, int x, int y, int width, int height, ByteBuffer pixels);

    default void atlasStitched(TextureMap atlas) {
    }

    default void atlasAnimationUploaded(TextureAtlasSprite sprite, int[][] pixels) {
    }

    default void textureUploaded(TextureHandle texture, NativeImage image, boolean blur, boolean clamp) {
    }

    default void dynamicTextureUploaded(TextureHandle texture, int width, int height, int[] argb) {
    }

    default void textureRegionUploaded(TextureHandle texture, NativeImage image, int x, int y, boolean blur, boolean clamp) {
    }

    default void animatedTextureRegionUploaded(ResourceLocation location, TextureHandle texture, int x, int y,
                                               int width, int height, ByteBuffer pixels) {
    }

    default void textureClamped(TextureHandle texture, boolean clamp) {
    }

    default void legacyTextureDeleted(TextureHandle texture) {
    }
}
