package net.minecraft.client.renderer.texture;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.optifine.Config;
import net.optifine.Mipmaps;
import net.radiant.render.TextureBackend;
import net.radiant.render.TextureHandle;
import net.radiant.render.TextureBackends;
import net.radiant.util.NativeImage;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class TextureUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextureUtil.class);
    private static final TextureBackend BACKEND = TextureBackends.ACTIVE;
    private static final IntBuffer DATA_BUFFER = GLAllocation.createDirectIntBuffer(4194304);
    public static final DynamicTexture MISSING_TEXTURE = new DynamicTexture(16, 16);
    public static final int[] MISSING_TEXTURE_DATA = MISSING_TEXTURE.getTextureData();
    private static final int[] MIPMAP_BUFFER;
    private static final int[] DATA_ARRAY = new int[4194304];

    public static int glGenTextures() {
        return BACKEND.create().legacyId();
    }

    public static void deleteTexture(int textureId) {
        BACKEND.delete(new TextureHandle(textureId));
    }

    public static int uploadTextureImage(int p_110987_0_, NativeImage p_110987_1_) {
        return uploadTextureImageAllocate(p_110987_0_, p_110987_1_, false, false);
    }

    public static void uploadTexture(int textureId, int[] argb, int width, int height) {
        bindTexture(textureId);
        uploadTextureSub(0, argb, width, height, 0, 0, false, false, false);
        BACKEND.dynamicTextureUploaded(new TextureHandle(textureId), width, height, argb);
    }

    public static int[][] generateMipmapData(int p_147949_0_, int p_147949_1_, int[][] p_147949_2_) {
        int[][] aint = new int[p_147949_0_ + 1][];
        aint[0] = p_147949_2_[0];

        if (p_147949_0_ > 0) {
            boolean flag = false;

            for (int i = 0; i < p_147949_2_[0].length; ++i) {
                if (p_147949_2_[0][i] >> 24 == 0) {
                    flag = true;
                    break;
                }
            }

            for (int l1 = 1; l1 <= p_147949_0_; ++l1) {
                if (p_147949_2_[l1] != null) {
                    aint[l1] = p_147949_2_[l1];
                } else {
                    int[] aint1 = aint[l1 - 1];
                    int[] aint2 = new int[aint1.length >> 2];
                    int j = p_147949_1_ >> l1;
                    int k = aint2.length / j;
                    int l = j << 1;

                    for (int i1 = 0; i1 < j; ++i1) {
                        for (int j1 = 0; j1 < k; ++j1) {
                            int k1 = 2 * (i1 + j1 * l);
                            aint2[i1 + j1 * j] = blendColors(aint1[k1], aint1[k1 + 1], aint1[k1 + l], aint1[k1 + 1 + l], flag);
                        }
                    }

                    aint[l1] = aint2;
                }
            }
        }

        return aint;
    }

    private static int blendColors(int p_147943_0_, int p_147943_1_, int p_147943_2_, int p_147943_3_, boolean p_147943_4_) {
        return Mipmaps.alphaBlend(p_147943_0_, p_147943_1_, p_147943_2_, p_147943_3_);
    }

    private static int blendColorComponent(int p_147944_0_, int p_147944_1_, int p_147944_2_, int p_147944_3_, int p_147944_4_) {
        float f = (float) Math.pow(((p_147944_0_ >> p_147944_4_ & 255) / 255.0F), 2.2D);
        float f1 = (float) Math.pow(((p_147944_1_ >> p_147944_4_ & 255) / 255.0F), 2.2D);
        float f2 = (float) Math.pow(((p_147944_2_ >> p_147944_4_ & 255) / 255.0F), 2.2D);
        float f3 = (float) Math.pow(((p_147944_3_ >> p_147944_4_ & 255) / 255.0F), 2.2D);
        float f4 = (float) Math.pow((f + f1 + f2 + f3) * 0.25D, 0.45454545454545453D);
        return (int) (f4 * 255.0D);
    }

    static void atlasStitched(TextureMap atlas) {
        BACKEND.atlasStitched(atlas);
    }

    static void atlasAnimationUploaded(TextureAtlasSprite sprite, int[][] pixels) {
        BACKEND.atlasAnimationUploaded(sprite, pixels);
    }

    public static void uploadTextureMipmap(int[][] p_147955_0_, int p_147955_1_, int p_147955_2_, int p_147955_3_, int p_147955_4_, boolean p_147955_5_, boolean p_147955_6_) {
        for (int i = 0; i < p_147955_0_.length; ++i) {
            int[] aint = p_147955_0_[i];
            uploadTextureSub(i, aint, p_147955_1_ >> i, p_147955_2_ >> i, p_147955_3_ >> i, p_147955_4_ >> i, p_147955_5_, p_147955_6_, p_147955_0_.length > 1);
        }
    }

    private static void uploadTextureSub(int p_147947_0_, int[] p_147947_1_, int p_147947_2_, int p_147947_3_, int p_147947_4_, int p_147947_5_, boolean p_147947_6_, boolean p_147947_7_, boolean p_147947_8_) {
        int i = 4194304 / p_147947_2_;
        setTextureBlurMipmap(p_147947_6_, p_147947_8_);
        setTextureClamped(p_147947_7_);
        int j;

        for (int k = 0; k < p_147947_2_ * p_147947_3_; k += p_147947_2_ * j) {
            int l = k / p_147947_2_;
            j = Math.min(i, p_147947_3_ - l);
            int i1 = p_147947_2_ * j;
            copyToBufferPos(p_147947_1_, k, i1);
            BACKEND.uploadBoundArgb(p_147947_0_, p_147947_4_, p_147947_5_ + l, p_147947_2_, j, DATA_BUFFER);
        }
    }

    public static int uploadTextureImageAllocate(int textureId, NativeImage image, boolean blur, boolean clamp) {
        allocateTexture(textureId, image.getWidth(), image.getHeight());
        uploadTextureImageSubImpl(image, 0, 0, blur, clamp);
        BACKEND.textureUploaded(new TextureHandle(textureId), image, blur, clamp);
        return textureId;
    }

    public static void allocateTexture(int p_110991_0_, int p_110991_1_, int p_110991_2_) {
        allocateTextureImpl(p_110991_0_, 0, p_110991_1_, p_110991_2_);
    }

    public static void allocateTextureImpl(int p_180600_0_, int p_180600_1_, int p_180600_2_, int p_180600_3_) {
        synchronized (TextureUtil.class) {
            deleteTexture(p_180600_0_);
            bindTexture(p_180600_0_);
        }

        BACKEND.allocateBound(p_180600_1_, p_180600_2_, p_180600_3_);
    }

    public static void uploadAnimatedTextureRegion(ResourceLocation location, int textureId, int x, int y, int width,
                                                   int height, ByteBuffer pixels) {
        TextureHandle texture = new TextureHandle(textureId);
        BACKEND.bind(texture);
        BACKEND.uploadBoundRgba(0, x, y, width, height, pixels);
        BACKEND.animatedTextureRegionUploaded(location, texture, x, y, width, height, pixels);
    }

    public static int uploadTextureImageSub(int textureId, NativeImage image, int x, int y, boolean blur, boolean clamp) {
        bindTexture(textureId);
        uploadTextureImageSubImpl(image, x, y, blur, clamp);
        BACKEND.textureRegionUploaded(new TextureHandle(textureId), image, x, y, blur, clamp);
        return textureId;
    }

    private static void uploadTextureImageSubImpl(NativeImage image, int p_110993_1_, int p_110993_2_, boolean p_110993_3_, boolean p_110993_4_) {
        int i = image.getWidth();
        int j = image.getHeight();
        int k = 4194304 / i;
        int[] aint = DATA_ARRAY;
        setTextureBlurred(p_110993_3_);
        setTextureClamped(p_110993_4_);

        for (int l = 0; l < i * j; l += i * k) {
            int i1 = l / i;
            int j1 = Math.min(k, j - i1);
            int k1 = i * j1;
            image.getRGB(0, i1, i, j1, aint, 0, i);
            copyToBuffer(aint, k1);
            BACKEND.uploadBoundArgb(0, p_110993_1_, p_110993_2_ + i1, i, j1, DATA_BUFFER);
        }
    }

    public static void setTextureClamped(boolean p_110997_0_) {
        BACKEND.textureClamped(new TextureHandle(GlStateManager.getBoundTexture()), p_110997_0_);
        if (p_110997_0_) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        } else {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        }
    }

    private static void setTextureBlurred(boolean p_147951_0_) {
        setTextureBlurMipmap(p_147951_0_, false);
    }

    public static void setTextureBlurMipmap(boolean p_147954_0_, boolean p_147954_1_) {
        if (p_147954_0_) {
            GlStateManager.textureMinFilter(p_147954_1_ ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        } else {
            int i = Config.getMipmapType();
            GlStateManager.textureMinFilter(p_147954_1_ ? i : GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }
    }

    private static void copyToBuffer(int[] p_110990_0_, int p_110990_1_) {
        copyToBufferPos(p_110990_0_, 0, p_110990_1_);
    }

    private static void copyToBufferPos(int[] p_110994_0_, int p_110994_1_, int p_110994_2_) {
        DATA_BUFFER.clear();
        DATA_BUFFER.put(p_110994_0_, p_110994_1_, p_110994_2_);
        DATA_BUFFER.position(0).limit(p_110994_2_);
    }

    static void bindTexture(int p_94277_0_) {
        BACKEND.bind(new TextureHandle(p_94277_0_));
    }

    public static int[] readImageData(IResourceManager resourceManager, ResourceLocation imageLocation) throws IOException {
        NativeImage image = readNativeImage(resourceManager.getResource(imageLocation).getInputStream());

        if (image == null) {
            return null;
        } else {
            int i = image.getWidth();
            int j = image.getHeight();
            int[] aint = new int[i * j];
            image.getRGB(0, 0, i, j, aint, 0, i);
            return aint;
        }
    }

    public static NativeImage readNativeImage(InputStream imageStream) throws IOException {
        if (imageStream == null) {
            return null;
        }

        NativeImage image;
        try {
            image = NativeImage.loadFromInputStream(imageStream);
        } finally {
            IOUtils.closeQuietly(imageStream);
        }
        return image;
    }

    public static void processPixelValues(int[] pixels, int width, int height) {
        int[] aint = new int[width];
        int i = height / 2;

        for (int j = 0; j < i; ++j) {
            System.arraycopy(pixels, j * width, aint, 0, width);
            System.arraycopy(pixels, (height - 1 - j) * width, pixels, j * width, width);
            System.arraycopy(aint, 0, pixels, (height - 1 - j) * width, width);
        }
    }

    static {
        int i = -16777216;
        int j = -524040;
        int[] aint = new int[]{-524040, -524040, -524040, -524040, -524040, -524040, -524040, -524040};
        int[] aint1 = new int[]{-16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216};
        int k = aint.length;

        for (int l = 0; l < 16; ++l) {
            System.arraycopy(l < k ? aint : aint1, 0, MISSING_TEXTURE_DATA, 16 * l, k);
            System.arraycopy(l < k ? aint1 : aint, 0, MISSING_TEXTURE_DATA, 16 * l + k, k);
        }

        MISSING_TEXTURE.updateDynamicTexture();
        MIPMAP_BUFFER = new int[4];
    }
}
