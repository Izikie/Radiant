package net.radiant.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.stb.STBImageResize.*;
import static org.lwjgl.stb.STBImageWrite.*;

/**
 * Natively loaded image class
 */
public class NativeImage {

    private byte[] data;
    private int width, height;

    private NativeImage() {
    }

    public byte[] getData() {
        return data;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ByteBuffer getPixelBuffer() {
        return BufferUtils.createByteBuffer(this.data.length)
                .put(this.data)
                .flip();
    }

    /**
     * Gets a pixel from the image at a specified location
     * @param x The x position of the pixel
     * @param y The y position of the pixel
     * @return The pixel in ARGB format
     */
    public int getPixel(int x, int y) {
        return getPixelAtIndex((x + y * this.width) * 4);
    }

    /**
     * Returns the pixel at a specified byte index
     * @param index The index of the data <code>(x + y * width) * 4</code>
     * @return The pixel in ARGB format
     */
    public int getPixelAtIndex(int index) {
        byte r = this.data[index];
        byte g = this.data[index + 1];
        byte b = this.data[index + 2];
        byte a = this.data[index + 3];
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /**
     * Converts the image to an array of ARGB pixels
     * @return The image pixel array
     */
    public int[] getRGB() {
        int size = width * height;
        int[] rgb = new int[size];
        for (int i = 0; i < size; i++) {
            rgb[i] = getPixelAtIndex(i);
        }
        return rgb;
    }

    /**
     * Implementation of {@link java.awt.image.BufferedImage#getRGB(int, int, int, int, int[], int, int) BufferedImage.getRGB}.
     * @param startX The starting X coordinate
     * @param startY The starting Y coordinate
     * @param w width of the region
     * @param h height of the region
     * @param rgbArray if not <code>null</code>, the rgb pixels are written here
     * @param offset offset into the <code>rgbArray</code>
     * @param scansize scanline stride for the <code>rgbArray</code>
     * @return array of ARGB pixels.
     */
    public int[] getRGB(int startX, int startY, int w, int h, int @Nullable [] rgbArray, int offset, int scansize) {
        int yOff = offset;
        int off;

        if (rgbArray == null) {
            rgbArray = new int[offset + h * scansize];
        }

        for (int y = startY; y < startY + h; y++, yOff += scansize) {
            off = yOff;
            for (int x = startX; x < startX + w; x++) {
                rgbArray[off++] = this.getPixel(x, y);
            }
        }

        return rgbArray;
    }

    /**
     * Sets a pixel in the image
     * @param x The X position of the pixel
     * @param y The Y position of the pixel
     * @param color The color to set in ARGB format
     */
    public void setPixel(int x, int y, int color) {
        int index = (x + y * this.width) * 4;
        this.data[index] = (byte) ((color >> 16) & 0xFF);
        this.data[index + 1] = (byte) ((color >> 8) & 0xFF);
        this.data[index + 2] = (byte) (color & 0xFF);
        this.data[index + 3] = (byte) ((color >> 24) & 0xFF);
    }

    /**
     * Sets an RGB region of the image
     * @param startX The starting X coordinate
     * @param startY The starting Y coordinate
     * @param width width of the region
     * @param height height of the region
     * @param pixels The pixels to write to the image
     */
    public void setRGB(int startX, int startY, int width, int height, int[] pixels) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                this.setPixel(startX + x, startY + y, pixels[y * width + x]);
            }
        }
    }

    /**
     * Sets an RGB region of the image with an alpha value of 255
     * @param startX The starting X coordinate
     * @param startY The starting Y coordinate
     * @param width width of the region
     * @param height height of the region
     * @param pixels The pixels to write to the image
     */
    public void setRGBNoAlpha(int startX, int startY, int width, int height, int[] pixels) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                this.setPixel(startX + x, startY + y, pixels[y * width + x] | 0xFF000000);
            }
        }
    }

    public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        int yoff = offset;
        int off;

        for (int y = startY; y < startY + h; y++, yoff += scansize) {
            off = yoff;
            for (int x = startX; x < startX + w; x++) {
                this.setPixel(x, y, rgbArray[off++]);
            }
        }
    }

    public void overlayPixel(int x, int y, int color) {
        int current = getPixel(x, y);
        int blended = blendARGB(color, current);
        setPixel(x, y, blended);
    }

    public NativeImage resized(int width, int height) {

        boolean scaleCleanly = (width % this.width == 0 && height % this.height == 0) || (this.width % width == 0 && this.height % height == 0);
        if (scaleCleanly) {
            NativeImage newImage = createBlankImage(width, height);
            for (int w = 0; w < width; w++) {
                for (int h = 0; h < height; h++) {
                    int relativeW = (int) (((double) w / (double) width) * this.width);
                    int relativeH = (int) (((double) h / (double) height) * this.height);
                    newImage.setPixel(w, h, this.getPixel(relativeW, relativeH));
                }
            }
            return newImage;
        }

        ByteBuffer input = MemoryUtil.memAlloc(this.width * this.height * 4);
        input.put(this.data).flip();

        ByteBuffer output = MemoryUtil.memAlloc(width * height * 4);

        stbir_resize_uint8_linear(
                input, this.width, this.height, this.width * 4,
                output, width, height, width * 4,
                STBIR_RGBA
        );

        NativeImage newImage = createBlankImage(width, height);
        output.get(newImage.data);

        MemoryUtil.memFree(input);
        MemoryUtil.memFree(output);

        return newImage;
    }

    public void copyFrom(NativeImage image, boolean blend) {
        for (int x = 0; x < this.width && x < image.getWidth(); x++) {
            for (int y = 0; y < this.height && y < image.getHeight(); y++) {
                if (blend) {
                    int oldPixel = this.getPixel(x, y);
                    int newPixel = blendARGB(image.getPixel(x, y), oldPixel);
                    this.setPixel(x, y, newPixel | 0xFF000000);
                } else {
                    this.setPixel(x, y, image.getPixel(x, y));
                }
            }
        }
    }

    public void overwritePixels(NativeImage image, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2) {
        int dw = dx2 - dx1;
        int dh = dy2 - dy1;
        int sw = sx2 - sx1;
        int sh = sy2 - sy1;
        NativeImage section = createBlankImage(sw, sh);
        for (int x = 0; x < sw; x++) {
            for (int y = 0; y < sh; y++) {
                section.setPixel(x, y, image.getPixel(sx1 + x, sy1 + y));
            }
        }
        section = section.resized(dw, dh);
        for (int x = 0; x < dw; x++) {
            for (int y = 0; y < dh; y++) {
                setPixel(dx1 + x, dy1 + y, section.getPixel(x, y));
            }
        }
    }

    public boolean saveToFile(File file, FileFormat format) {
        if (this.width <= 0 || this.height <= 0) {
            return false;
        }

        String path = file.getAbsolutePath();
        ByteBuffer data = this.getPixelBuffer();

        return switch (format) {
            case BMP -> stbi_write_bmp(path, this.width, this.height, 4, data);
            case TGA -> stbi_write_tga(path, this.width, this.height, 4, data);
            case PNG -> stbi_write_png(path, this.width, this.height, 4, data, 0);
            case JPG -> stbi_write_jpg(path, this.width, this.height, 4, data, 100);
        };
    }

    /**
     * Creates a new blank image
     * @param width The width of the image
     * @param height The height of the image
     * @return The new blank image
     */
    public static NativeImage createBlankImage(int width, int height) {
        NativeImage image = new NativeImage();
        image.width = width;
        image.height = height;
        image.data = new byte[width * height * 4];
        return image;
    }

    /**
     * Loads a native image from an input stream
     * @param stream The input stream to read from
     * @return The loaded native image
     * @throws IOException If an I/O error occurs
     */
    public static NativeImage loadFromInputStream(InputStream stream) throws IOException {
        byte[] imageBytes = stream.readAllBytes();
        ByteBuffer imageBuffer = MemoryUtil.memAlloc(imageBytes.length);
        imageBuffer.put(imageBytes).flip();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1); // Useless...

            ByteBuffer out = stbi_load_from_memory(imageBuffer, w, h, comp, STBI_rgb_alpha);
            if (out == null) {
                throw new IOException("Failed to load image: " + stbi_failure_reason());
            }

            NativeImage image = new NativeImage();
            image.width = w.get(0);
            image.height = h.get(0);

            int size = image.width * image.height * 4;
            image.data = new byte[size];
            out.get(image.data, 0, size);

            out.position(0);
            stbi_image_free(out);

            return image;
        } finally {
            MemoryUtil.memFree(imageBuffer);
        }
    }

    public static NativeImage loadFromResourceLocation(ResourceLocation location) throws IOException {
        try (InputStream stream = Minecraft.get().getResourceManager().getResource(location).getInputStream()) {
            return loadFromInputStream(stream);
        }
    }

    public static NativeImage loadFromFile(File file) throws IOException {
        try (InputStream stream = new FileInputStream(file)) {
            return loadFromInputStream(stream);
        }
    }

    // I hate this
    public static int blendARGB(int srcARGB, int dstARGB) {
        // Extract channels
        int srcA = (srcARGB >>> 24) & 0xFF;
        int srcR = (srcARGB >>> 16) & 0xFF;
        int srcG = (srcARGB >>> 8) & 0xFF;
        int srcB = srcARGB & 0xFF;

        int dstA = (dstARGB >>> 24) & 0xFF;
        int dstR = (dstARGB >>> 16) & 0xFF;
        int dstG = (dstARGB >>> 8) & 0xFF;
        int dstB = dstARGB & 0xFF;

        // Normalize alpha to 0..1
        float srcAlpha = srcA / 255.0f;
        float invSrcAlpha = 1.0f - srcAlpha;

        // Blend RGB
        int outR = Math.round(srcR * srcAlpha + dstR * invSrcAlpha);
        int outG = Math.round(srcG * srcAlpha + dstG * invSrcAlpha);
        int outB = Math.round(srcB * srcAlpha + dstB * invSrcAlpha);

        // Blend Alpha: GL_ONE, GL_ZERO = source alpha only

        // Clamp to 0..255 (Math.round already ensures that, but extra safety)
        outR = Math.min(255, Math.max(0, outR));
        outG = Math.min(255, Math.max(0, outG));
        outB = Math.min(255, Math.max(0, outB));

        // Pack back into ARGB
        return (dstA << 24) | (outR << 16) | (outG << 8) | outB;
    }

    public enum FileFormat {
        BMP, TGA, PNG, JPG
    }

}
