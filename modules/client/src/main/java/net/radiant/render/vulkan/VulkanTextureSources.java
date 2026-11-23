package net.radiant.render.vulkan;

import net.radiant.render.TextureHandle;
import net.radiant.util.NativeImage;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/** CPU copies of standalone textures, owned by the client thread. */
final class VulkanTextureSources {
    private final Map<TextureHandle, Source> sources = new HashMap<>();

    Source get(TextureHandle texture) {
        return sources.get(texture);
    }

    void put(TextureHandle texture, NativeImage image, boolean blur, boolean clamp) {
        sources.put(texture, new Source(image.getWidth(), image.getHeight(), image.getData().clone(), false, blur, clamp));
    }

    void putDynamic(TextureHandle texture, int width, int height, int[] argb) {
        Source source = sources.get(texture);
        if (source == null || !source.dynamic || source.width != width || source.height != height) {
            source = new Source(width, height, new byte[Math.multiplyExact(Math.multiplyExact(width, height), 4)],
                    true, false, false);
            sources.put(texture, source);
        }
        VulkanAtlasUpdates.writeRgba(ByteBuffer.wrap(source.rgba), argb, 0, width * height);
    }

    VulkanTextureUpdates.Region updateRegion(TextureHandle texture, int x, int y, int width, int height, ByteBuffer pixels) {
        Source source = sources.get(texture);
        if (source == null || x < 0 || y < 0 || width <= 0 || height <= 0 || width > source.width - x
                || height > source.height - y || pixels.remaining() < (long) width * height * 4) {
            return null;
        }
        int rowBytes = width * 4;
        for (int row = 0; row < height; row++) {
            int offset = ((y + row) * source.width + x) * 4;
            pixels.get(pixels.position() + row * rowBytes, source.rgba, offset, rowBytes);
        }
        return new VulkanTextureUpdates.Region(source.rgba, source.width, x, y, x + width, y + height);
    }

    Snapshot snapshot(TextureHandle texture) {
        Source source = sources.get(texture);
        if (source == null) {
            return null;
        }
        NativeImage image = NativeImage.createBlankImage(source.width, source.height);
        System.arraycopy(source.rgba, 0, image.getData(), 0, source.rgba.length);
        return new Snapshot(image, source.blur, source.clamp);
    }

    void remove(TextureHandle texture) {
        sources.remove(texture);
    }

    boolean setClamped(TextureHandle texture, boolean clamp) {
        Source source = sources.get(texture);
        return source != null && setSampler(texture, source.blur, clamp);
    }

    boolean setSampler(TextureHandle texture, boolean blur, boolean clamp) {
        Source source = sources.get(texture);
        if (source == null || (source.blur == blur && source.clamp == clamp)) {
            return false;
        }
        sources.put(texture, new Source(source.width, source.height, source.rgba, source.dynamic, blur, clamp));
        return true;
    }

    record Snapshot(NativeImage image, boolean blur, boolean clamp) {
    }

    record Source(int width, int height, byte[] rgba, boolean dynamic, boolean blur, boolean clamp) {
    }
}
