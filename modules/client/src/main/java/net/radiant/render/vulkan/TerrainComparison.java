package net.radiant.render.vulkan;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.RenderLayer;
import net.radiant.render.terrain.TerrainDrawSubmitter;
import net.radiant.util.NativeImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER;
import static org.lwjgl.opengl.GL30.*;

/** Replays the ported passes into an OpenGL reference target in world draw order. */
final class TerrainComparison implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerrainComparison.class);
    private final int framebuffer = glGenFramebuffers();
    private final int color = glGenRenderbuffers();
    private final int depth = glGenRenderbuffers();
    private int width, height, samples;
    private boolean active;
    private int[] background, beforeLateClouds;
    private int[] beforeOverlays, afterOverlays;
    private final Tessellator overlayReference = new Tessellator(4096);
    private boolean savedMismatch;
    private Path directory;

    void begin(int w, int h) {
        int previous = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);
        try {
            if (width != w || height != h) {
                int rb = glGetInteger(GL_RENDERBUFFER_BINDING);
                glBindRenderbuffer(GL_RENDERBUFFER, color);
                glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA8, w, h);
                glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, color);
                glBindRenderbuffer(GL_RENDERBUFFER, depth);
                glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, w, h);
                glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depth);
                glBindRenderbuffer(GL_RENDERBUFFER, rb);
                if (glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                    throw new IllegalStateException("Incomplete terrain comparison framebuffer");
                }
                width = w;
                height = h;
            }
            glPushAttrib(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_SCISSOR_BIT);
            try {
                glDisable(GL_SCISSOR_TEST);
                glColorMask(true, true, true, true);
                glDepthMask(true);
                glClearBufferfv(GL_DEPTH, 0, new float[]{1});
            } finally {
                glPopAttrib();
            }
            active = true;
            background = null;
            beforeLateClouds = null;
            beforeOverlays = afterOverlays = null;
        } finally {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, previous);
        }
    }

    void copyBackground() {
        int source = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int previousRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, source);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);
        glPushAttrib(GL_SCISSOR_BIT);
        try {
            glDisable(GL_SCISSOR_TEST);
            glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        } finally {
            glPopAttrib();
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, source);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, previousRead);
        }
    }

    void drawOverlay(int[] vertices, VertexFormat format) {
        if (background != null && beforeOverlays == null) beforeOverlays = readColors();
        drawSky(() -> {
            var mesh = overlayReference.getWorldRenderer();
            mesh.begin(GL_QUADS, format);
            mesh.addVertexData(vertices);
            overlayReference.draw();
        });
    }

    void drawClouds(Runnable reference, boolean afterTerrain) {
        if (afterTerrain && background != null && beforeLateClouds == null) beforeLateClouds = readColors();
        drawSky(reference);
    }

    void drawSky(Runnable reference) {
        int previous = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);
        try {
            reference.run();
        } finally {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, previous);
        }
    }

    void draw(TerrainDrawSubmitter reference, RenderLayer layer, List<RenderChunk> sections) {
        int previous = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);
        try {
            if (background == null) background = readColors();
            if (beforeOverlays != null && afterOverlays == null) afterOverlays = readColors();
            reference.drawLayer(layer, sections);
        } finally {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, previous);
        }
    }

    boolean active() {
        return active;
    }

    void end() {
        active = false;
    }

    private int[] readColors() {
        int[] pixels = new int[Math.multiplyExact(width, height)];
        int previous = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);
        glPushClientAttrib(GL_CLIENT_PIXEL_STORE_BIT);
        try {
            glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
            glPixelStorei(GL_PACK_ROW_LENGTH, 0);
            glPixelStorei(GL_PACK_SKIP_ROWS, 0);
            glPixelStorei(GL_PACK_SKIP_PIXELS, 0);
            glPixelStorei(GL_PACK_SWAP_BYTES, GL_FALSE);
            glPixelStorei(GL_PACK_ALIGNMENT, 4);
            glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, pixels);
        } finally {
            glPopClientAttrib();
            glBindFramebuffer(GL_READ_FRAMEBUFFER, previous);
        }
        return pixels;
    }

    void compare(NativeImage vk) {
        int[] pixels = readColors();
        int[] terrainPixels = beforeLateClouds == null ? pixels : beforeLateClouds;
        int[] opaquePixels = beforeOverlays == null ? terrainPixels : beforeOverlays;
        int mismatches = 0, maximum = 0, visible = 0;
        long absoluteError = 0;
        NativeImage gl = NativeImage.createBlankImage(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = (height - 1 - y) * width + x;
                int expected = pixels[index];
                int actual = vk.getPixel(x, y);
                gl.setPixel(x, y, expected | 0xff000000);
                if ((background != null && ((opaquePixels[index] ^ background[index]) & 0xffffff) != 0)
                        || (afterOverlays != null && ((terrainPixels[index] ^ afterOverlays[index]) & 0xffffff) != 0)) visible++;
                if (((expected ^ actual) & 0xffffff) == 0) continue;
                mismatches++;
                for (int shift = 0; shift <= 16; shift += 8) {
                    int error = Math.abs((expected >> shift & 255) - (actual >> shift & 255));
                    maximum = Math.max(maximum, error);
                    absoluteError += error;
                }
            }
        }
        LOGGER.info("Terrain comparison {} pixels={} visible={} mismatched={} maxChannelError={} absoluteError={}",
                ++samples, pixels.length, visible, mismatches, maximum, absoluteError);
        if (visible > 0 && (directory == null || samples % 20 == 0) || mismatches > 0 && !savedMismatch) {
            try {
                if (directory == null) directory = Files.createTempDirectory(Files.createDirectories(Path.of("benchmarks")), "terrain-pair-");
                if (!gl.saveToFile(directory.resolve(samples + "-opengl.png").toFile(), NativeImage.FileFormat.PNG)
                        || !vk.saveToFile(directory.resolve(samples + "-vulkan.png").toFile(), NativeImage.FileFormat.PNG)) {
                    throw new IOException("Could not write terrain pair in " + directory);
                }
                LOGGER.info("Terrain captures saved in {}", directory.toAbsolutePath());
                savedMismatch |= mismatches > 0;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void close() {
        glDeleteRenderbuffers(color);
        glDeleteRenderbuffers(depth);
        glDeleteFramebuffers(framebuffer);
    }
}
