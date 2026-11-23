package net.radiant.benchmark;

import com.google.gson.JsonObject;
import net.radiant.util.NativeImage;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memFree;

final class BenchmarkFrameComparison {
    private BenchmarkFrameComparison() {
    }

    static void saveReference(Path relativeDir, int width, int height, JsonObject result) throws IOException {
        Path directory = Files.createDirectories(Path.of("benchmarks").resolve(relativeDir));
        writePng(readOpenGl(width, height), directory.resolve("opengl.png"));
        result.addProperty("opengl", relativeDir.toString().replace(File.separatorChar, '/') + "/opengl.png");
        result.addProperty("width", width);
        result.addProperty("height", height);
    }

    static void save(Path relativeDir, NativeImage vulkan, JsonObject result) throws IOException {
        int width = vulkan.getWidth();
        int height = vulkan.getHeight();
        NativeImage openGl = readOpenGl(width, height);
        NativeImage difference = NativeImage.createBlankImage(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gl = openGl.getPixel(x, y);
                int vk = vulkan.getPixel(x, y);
                int red = Math.abs((gl >>> 16 & 255) - (vk >>> 16 & 255));
                int green = Math.abs((gl >>> 8 & 255) - (vk >>> 8 & 255));
                int blue = Math.abs((gl & 255) - (vk & 255));
                difference.setPixel(x, y, 0xFF000000 | red << 16 | green << 8 | blue);
            }
        }

        Path directory = Files.createDirectories(Path.of("benchmarks").resolve(relativeDir));
        writePng(openGl, directory.resolve("opengl.png"));
        writePng(vulkan, directory.resolve("vulkan.png"));
        writePng(difference, directory.resolve("difference.png"));
        String prefix = relativeDir.toString().replace(File.separatorChar, '/') + "/";
        result.addProperty("opengl", prefix + "opengl.png");
        result.addProperty("vulkan", prefix + "vulkan.png");
        result.addProperty("difference", prefix + "difference.png");
        result.addProperty("width", width);
        result.addProperty("height", height);
    }

    private static NativeImage readOpenGl(int width, int height) {
        NativeImage image = NativeImage.createBlankImage(width, height);
        IntBuffer pixels = memAllocInt(width * height);
        int packAlignment = glGetInteger(GL_PACK_ALIGNMENT);
        int readBuffer = glGetInteger(GL_READ_BUFFER);
        try {
            // The OpenGL frame has been composited and the back buffer has yet to be swapped.
            glReadBuffer(GL_BACK);
            glPixelStorei(GL_PACK_ALIGNMENT, 1);
            glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, pixels);
            for (int y = 0; y < height; y++) {
                int row = (height - 1 - y) * width;
                for (int x = 0; x < width; x++) {
                    image.setPixel(x, y, pixels.get(row + x) | 0xFF000000);
                }
            }
            return image;
        } finally {
            glPixelStorei(GL_PACK_ALIGNMENT, packAlignment);
            glReadBuffer(readBuffer);
            memFree(pixels);
        }
    }

    private static void writePng(NativeImage image, Path path) throws IOException {
        if (!image.saveToFile(path.toFile(), NativeImage.FileFormat.PNG)) {
            throw new IOException("Could not write " + path);
        }
    }
}
