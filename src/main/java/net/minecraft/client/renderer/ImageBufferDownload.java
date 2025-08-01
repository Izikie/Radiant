package net.minecraft.client.renderer;

import net.radiant.NativeImage;

public class ImageBufferDownload implements IImageBuffer {
    private int[] imageData;
    private int imageWidth;
    private int imageHeight;

    public NativeImage parseUserSkin(NativeImage image) {
        if (image == null) {
            return null;
        }
        this.imageWidth = 64;
        this.imageHeight = 64;
        int w = image.getWidth();
        int h = image.getHeight();

        int s;
        for (s = 1; this.imageWidth < w || this.imageHeight < h; s *= 2) {
            this.imageWidth *= 2;
            this.imageHeight *= 2;
        }

        NativeImage image1 = NativeImage.createBlankImage(this.imageWidth, this.imageHeight);
        image1.copyFrom(image);

//        if (image.getHeight() == 32 * s) { // I hate this part, it's retarded
//            image1.overwritePixels(image1, 24 * s, 48 * s, 20 * s, 52 * s, 4 * s, 16 * s, 8 * s, 20 * s);
//            image1.overwritePixels(image1, 28 * s, 48 * s, 24 * s, 52 * s, 8 * s, 16 * s, 12 * s, 20 * s);
//            image1.overwritePixels(image1, 20 * s, 52 * s, 16 * s, 64 * s, 8 * s, 20 * s, 12 * s, 32 * s);
//            image1.overwritePixels(image1, 24 * s, 52 * s, 20 * s, 64 * s, 4 * s, 20 * s, 8 * s, 32 * s);
//            image1.overwritePixels(image1, 28 * s, 52 * s, 24 * s, 64 * s, 0, 20 * s, 4 * s, 32 * s);
//            image1.overwritePixels(image1, 32 * s, 52 * s, 28 * s, 64 * s, 12 * s, 20 * s, 16 * s, 32 * s);
//            image1.overwritePixels(image1, 40 * s, 48 * s, 36 * s, 52 * s, 44 * s, 16 * s, 48 * s, 20 * s);
//            image1.overwritePixels(image1, 44 * s, 48 * s, 40 * s, 52 * s, 48 * s, 16 * s, 52 * s, 20 * s);
//            image1.overwritePixels(image1, 36 * s, 52 * s, 32 * s, 64 * s, 48 * s, 20 * s, 52 * s, 32 * s);
//            image1.overwritePixels(image1, 40 * s, 52 * s, 36 * s, 64 * s, 44 * s, 20 * s, 48 * s, 32 * s);
//            image1.overwritePixels(image1, 44 * s, 52 * s, 40 * s, 64 * s, 40 * s, 20 * s, 44 * s, 32 * s);
//            image1.overwritePixels(image1, 48 * s, 52 * s, 44 * s, 64 * s, 52 * s, 20 * s, 56 * s, 32 * s);
//        }

        this.imageData = image1.getRGB();
        this.setAreaOpaque(0, 0, 32 * s, 16 * s);
        this.setAreaTransparent(32 * s, 0, 64 * s, 32 * s);
        this.setAreaOpaque(0, 16 * s, 64 * s, 32 * s);
        this.setAreaTransparent(0, 32 * s, 16 * s, 48 * s);
        this.setAreaTransparent(16 * s, 32 * s, 40 * s, 48 * s);
        this.setAreaTransparent(40 * s, 32 * s, 56 * s, 48 * s);
        this.setAreaTransparent(0, 48 * s, 16 * s, 64 * s);
        this.setAreaOpaque(16 * s, 48 * s, 48 * s, 64 * s);
        this.setAreaTransparent(48 * s, 48 * s, 64 * s, 64 * s);
        return image1;
    }

    public void skinAvailable() {
    }

    private void setAreaTransparent(int startX, int startY, int endX, int endY) {
        if (!this.hasTransparency(startX, startY, endX, endY)) {
            for (int x = startX; x < endX; ++x) {
                for (int y = startY; y < endY; ++y) {
                    this.imageData[x + y * this.imageWidth] &= 0xffffff;
                }
            }
        }
    }

    private void setAreaOpaque(int startX, int startY, int endX, int endY) {
        for (int x = startX; x < endX; ++x) {
            for (int y = startY; y < endY; ++y) {
                this.imageData[x + y * this.imageWidth] |= 0xff000000;
            }
        }
    }

    private boolean hasTransparency(int startX, int startY, int endX, int endY) {
        for (int x = startX; x < endX; ++x) {
            for (int y = startY; y < endY; ++y) {
                int k = this.imageData[x + y * this.imageWidth];

                if ((k >> 24 & 255) < 128) {
                    return true;
                }
            }
        }

        return false;
    }
}
