package net.radiant;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.io.File;
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
        ByteBuffer buffer = BufferUtils.createByteBuffer(this.data.length);
        buffer.put(this.data);
        buffer.flip();
        return buffer;
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
    public int[] getRGBSection(int startX, int startY, int w, int h, int @Nullable [] rgbArray, int offset, int scansize) {
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
        this.data[index] = (byte)((color >> 16) & 0xFF);
        this.data[index + 1] = (byte)((color >> 8) & 0xFF);
        this.data[index + 2] = (byte)(color & 0xFF);
        this.data[index + 3] = (byte)((color >> 24) & 0xFF);
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

    public NativeImage resized(int width, int height) {
        ByteBuffer inputPixels = ByteBuffer.wrap(this.data);

        NativeImage newImage = createBlankImage(width, height);
        ByteBuffer buffer = ByteBuffer.wrap(newImage.data);
        stbir_resize(
                inputPixels, this.width, this.height, this.width * 4,
                buffer, width, height, width * 4,
                STBIR_ARGB, STBIR_TYPE_UINT8, STBIR_EDGE_CLAMP, STBIR_FILTER_DEFAULT
        );
        return newImage;
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
        try (MemoryStack stack = MemoryStack.stackPush()) {
            byte[] imageBytes = stream.readAllBytes();
            ByteBuffer imageBuffer = stack.malloc(imageBytes.length);
            imageBuffer.put(imageBytes);
            imageBuffer.flip();

            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            ByteBuffer out = stbi_load_from_memory(imageBuffer, w, h, comp, STBI_rgb_alpha);
            if (out == null) {
                throw new IOException("Failed to load image: " + stbi_failure_reason());
            }

            NativeImage image = new NativeImage();
            image.width = w.get(0);
            image.height = h.get(0);

            image.data = new byte[out.remaining()];
            out.get(image.data);

            out.position(0);
            stbi_image_free(out);

            return image;
        }
    }

    public static NativeImage loadFromResourceLocation(ResourceLocation location) throws IOException {
        try (InputStream stream = Minecraft.get().getResourceManager().getResource(location).getInputStream()) {
            return loadFromInputStream(stream);
        }
    }

    public enum FileFormat {
        BMP, TGA, PNG, JPG
    }

}
