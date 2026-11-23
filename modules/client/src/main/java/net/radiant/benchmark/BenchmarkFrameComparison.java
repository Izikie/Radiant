package net.radiant.benchmark;

import com.google.gson.JsonObject;
import net.radiant.util.NativeImage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class BenchmarkFrameComparison {
    private BenchmarkFrameComparison() {
    }

    static void saveReference(Path relativeDir, NativeImage openGl, JsonObject result) throws IOException {
        Path directory = Files.createDirectories(Path.of("benchmarks").resolve(relativeDir));
        writePng(openGl, directory.resolve("opengl.png"));
        result.addProperty("opengl", relativeDir.toString().replace(File.separatorChar, '/') + "/opengl.png");
        result.addProperty("width", openGl.getWidth());
        result.addProperty("height", openGl.getHeight());
    }

    static void save(Path relativeDir, NativeImage openGl, NativeImage vulkan, JsonObject result) throws IOException {
        int width = vulkan.getWidth();
        int height = vulkan.getHeight();
        if (openGl.getWidth() != width || openGl.getHeight() != height) {
            throw new IOException("OpenGL and Vulkan capture dimensions differ");
        }
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

    private static void writePng(NativeImage image, Path path) throws IOException {
        if (!image.saveToFile(path.toFile(), NativeImage.FileFormat.PNG)) {
            throw new IOException("Could not write " + path);
        }
    }
}
