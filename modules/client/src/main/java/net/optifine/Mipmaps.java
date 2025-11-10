package net.optifine;

import net.minecraft.client.renderer.GLAllocation;
import net.optifine.util.TextureUtils;
import org.joml.Vector2i;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class Mipmaps {
    private final String iconName;
    private final int width;
    private final int height;
    private final int[] data;
    private final boolean direct;
    private final int[][] mipmapDatas;
    private final Vector2i[] mipmapDimensions;
    private IntBuffer[] mipmapBuffers;

    public Mipmaps(String iconName, int width, int height, int[] data, boolean direct) {
        this.iconName = iconName;
        this.width = width;
        this.height = height;
        this.data = data;
        this.direct = direct;
        this.mipmapDimensions = makeMipmapDimensions(width, height, iconName);
        this.mipmapDatas = generateMipMapData(data, width, height, this.mipmapDimensions);

        if (direct) {
            this.mipmapBuffers = makeMipmapBuffers(this.mipmapDimensions, this.mipmapDatas);
        }
    }

    public static Vector2i[] makeMipmapDimensions(int width, int height, String iconName) {
        int i = TextureUtils.ceilPowerOfTwo(width);
        int j = TextureUtils.ceilPowerOfTwo(height);

        if (i == width && j == height) {
            List<Vector2i> list = new ArrayList<>();
            int k = i;
            int l = j;

            while (true) {
                k /= 2;
                l /= 2;

                if (k <= 0 && l <= 0) {
                    return list.toArray(Vector2i[]::new);
                }

                if (k <= 0) {
                    k = 1;
                }

                if (l <= 0) {
                    l = 1;
                }

                int i1 = k * l * 4;
                list.add(new Vector2i(k, l));
            }
        } else {
            Log.error("Mipmaps not possible (power of 2 dimensions needed), texture: " + iconName + ", dim: " + width + "x" + height);
            return new Vector2i[0];
        }
    }

    public static int[][] generateMipMapData(int[] data, int width, int height, Vector2i[] mipmapDimensions) {
        int[] aint = data;
        int i = width;
        boolean flag = true;
        int[][] aint1 = new int[mipmapDimensions.length][];

        for (int j = 0; j < mipmapDimensions.length; ++j) {
            Vector2i dimension = mipmapDimensions[j];
            int k = dimension.x;
            int l = dimension.y;
            int[] aint2 = new int[k * l];
            aint1[j] = aint2;
            int i1 = j + 1;

            if (flag) {
                for (int j1 = 0; j1 < k; ++j1) {
                    for (int k1 = 0; k1 < l; ++k1) {
                        int l1 = aint[j1 * 2 + (k1 * 2) * i];
                        int i2 = aint[j1 * 2 + 1 + (k1 * 2) * i];
                        int j2 = aint[j1 * 2 + 1 + (k1 * 2 + 1) * i];
                        int k2 = aint[j1 * 2 + (k1 * 2 + 1) * i];
                        int l2 = alphaBlend(l1, i2, j2, k2);
                        aint2[j1 + k1 * k] = l2;
                    }
                }
            }

            aint = aint2;
            i = k;

            if (k <= 1 || l <= 1) {
                flag = false;
            }
        }

        return aint1;
    }

    public static int alphaBlend(int c1, int c2, int c3, int c4) {
        int i = alphaBlend(c1, c2);
        int j = alphaBlend(c3, c4);
        return alphaBlend(i, j);
    }

    private static int alphaBlend(int c1, int c2) {
        int i = (c1 & -16777216) >> 24 & 255;
        int j = (c2 & -16777216) >> 24 & 255;
        int k = (i + j) / 2;

        if (i == 0 && j == 0) {
            i = 1;
            j = 1;
        } else {
            if (i == 0) {
                c1 = c2;
                k /= 2;
            }

            if (j == 0) {
                c2 = c1;
                k /= 2;
            }
        }

        int l = (c1 >> 16 & 255) * i;
        int i1 = (c1 >> 8 & 255) * i;
        int j1 = (c1 & 255) * i;
        int k1 = (c2 >> 16 & 255) * j;
        int l1 = (c2 >> 8 & 255) * j;
        int i2 = (c2 & 255) * j;
        int j2 = (l + k1) / (i + j);
        int k2 = (i1 + l1) / (i + j);
        int l2 = (j1 + i2) / (i + j);
        return k << 24 | j2 << 16 | k2 << 8 | l2;
    }

    public static IntBuffer[] makeMipmapBuffers(Vector2i[] mipmapDimensions, int[][] mipmapDatas) {
        if (mipmapDimensions == null) {
            return null;
        } else {
            IntBuffer[] aintbuffer = new IntBuffer[mipmapDimensions.length];

            for (int i = 0; i < mipmapDimensions.length; ++i) {
                Vector2i dimension = mipmapDimensions[i];
                int j = dimension.x * dimension.y;
                IntBuffer intbuffer = GLAllocation.createDirectIntBuffer(j);
                int[] aint = mipmapDatas[i];
                intbuffer.clear();
                intbuffer.put(aint);
                intbuffer.clear();
                aintbuffer[i] = intbuffer;
            }

            return aintbuffer;
        }
    }

    public static void allocateMipmapTextures(int width, int height, String name) {
        Vector2i[] adimension = makeMipmapDimensions(width, height, name);

        for (int i = 0; i < adimension.length; ++i) {
            Vector2i dimension = adimension[i];
            int j = dimension.x;
            int k = dimension.y;
            int l = i + 1;
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, l, GL11.GL_RGBA, j, k, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer) null);
        }
    }

    private int averageColor(int i, int j) {
        int k = (i & -16777216) >> 24 & 255;
        int p = (j & -16777216) >> 24 & 255;
        return (k + j >> 1 << 24) + ((k & 16711422) + (p & 16711422) >> 1);
    }
}
