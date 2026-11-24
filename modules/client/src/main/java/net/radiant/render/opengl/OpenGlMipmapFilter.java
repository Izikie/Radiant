package net.radiant.render.opengl;

import org.lwjgl.opengl.GL;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LOD;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;

/** Measures the current driver's transition between adjacent RGBA8 atlas mip levels. */
public final class OpenGlMipmapFilter {
    private static final int STEPS = 256;
    private static final int WIDTH = 2 * (STEPS + 1);

    private OpenGlMipmapFilter() {
    }

    public static float[] measure(int width, int height, int levels) {
        float[] weights = new float[STEPS + 1];
        for (int i = 0; i <= STEPS; i++) {
            weights[i] = (float) i / STEPS;
        }
        if (levels < 2 || !GL.getCapabilities().OpenGL30) {
            return weights;
        }

        int previousDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int previousRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int previousRenderbuffer = glGetInteger(GL_RENDERBUFFER_BINDING);
        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int previousMode = glGetInteger(GL_MATRIX_MODE);
        int texture = glGenTextures();
        int framebuffer = glGenFramebuffers();
        int target = glGenRenderbuffers();
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glPushClientAttrib(GL_CLIENT_PIXEL_STORE_BIT);
        glActiveTexture(GL_TEXTURE0);
        for (int mode : new int[]{GL_PROJECTION, GL_MODELVIEW, GL_TEXTURE}) {
            glMatrixMode(mode);
            glPushMatrix();
            glLoadIdentity();
        }
        try {
            glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
            for (int param : new int[]{GL_PACK_ROW_LENGTH, GL_PACK_SKIP_PIXELS, GL_PACK_SKIP_ROWS,
                    GL_UNPACK_ROW_LENGTH, GL_UNPACK_SKIP_PIXELS, GL_UNPACK_SKIP_ROWS}) {
                glPixelStorei(param, 0);
            }
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            glBindRenderbuffer(GL_RENDERBUFFER, target);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA32F, WIDTH, 2);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, target);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                return weights;
            }

            glUseProgram(0);
            for (int unit = glGetInteger(GL_MAX_TEXTURE_UNITS) - 1; unit >= 0; --unit) {
                glActiveTexture(GL_TEXTURE0 + unit);
                glDisable(GL_TEXTURE_2D);
            }
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, texture);
            // Filtering can depend on the dimensions and mip count of the bound texture.
            glDisable(GL_SCISSOR_TEST);
            glColorMask(true, true, true, true);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, levels - 1);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, levels - 1);
            for (int mip = 0; mip < levels; mip++) {
                glTexImage2D(GL_TEXTURE_2D, mip, GL_RGBA8, Math.max(1, width >> mip), Math.max(1, height >> mip),
                        0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            }
            for (int mip = 0; mip < levels; mip++) {
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, mip);
                if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                    return weights;
                }
                float value = mip % 2;
                glClearBufferfv(GL_COLOR, 0, new float[]{value, value, value, 1});
            }
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, target);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
            for (int cap : new int[]{GL_LIGHTING, GL_BLEND, GL_FOG, GL_ALPHA_TEST, GL_DEPTH_TEST,
                    GL_CULL_FACE, GL_SCISSOR_TEST, GL_STENCIL_TEST, GL_COLOR_LOGIC_OP, GL_DITHER,
                    GL_TEXTURE_GEN_S, GL_TEXTURE_GEN_T}) {
                glDisable(cap);
            }
            glColorMask(true, true, true, true);
            glViewport(0, 0, WIDTH, 2);
            glMatrixMode(GL_PROJECTION);
            glOrtho(0, WIDTH, 0, 2, -1, 1);

            glBegin(GL_QUADS);
            for (int i = 0; i <= STEPS; i++) {
                float spanX = (float) Math.pow(2, (double) i / STEPS) * 2 / width;
                float spanY = spanX * width / height;
                int x = 2 * i;
                glTexCoord2f(0, 0);
                glVertex2i(x, 0);
                glTexCoord2f(spanX, 0);
                glVertex2i(x + 2, 0);
                glTexCoord2f(spanX, spanY);
                glVertex2i(x + 2, 2);
                glTexCoord2f(0, spanY);
                glVertex2i(x, 2);
            }
            glEnd();
            float[] row = new float[WIDTH];
            glReadPixels(0, 0, WIDTH, 1, GL_RED, GL_FLOAT, row);
            if (row[0] == 0 && row[2 * STEPS] == 1) {
                for (int i = 0; i <= STEPS; i++) {
                    weights[i] = row[2 * i];
                }
            }
            return weights;
        } finally {
            glActiveTexture(GL_TEXTURE0);
            for (int mode : new int[]{GL_TEXTURE, GL_MODELVIEW, GL_PROJECTION}) {
                glMatrixMode(mode);
                glPopMatrix();
            }
            glMatrixMode(previousMode);
            glUseProgram(previousProgram);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, previousDraw);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, previousRead);
            glBindRenderbuffer(GL_RENDERBUFFER, previousRenderbuffer);
            glPopClientAttrib();
            glPopAttrib();
            glDeleteFramebuffers(framebuffer);
            glDeleteRenderbuffers(target);
            glDeleteTextures(texture);
        }
    }
}
