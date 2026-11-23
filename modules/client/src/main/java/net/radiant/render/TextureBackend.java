package net.radiant.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;

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
}
