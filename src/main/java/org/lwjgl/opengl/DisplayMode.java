package org.lwjgl.opengl;

/**
 * Created by gudenau on 5/30/2017.
 * <p>
 * LWJGL3
 */
public final class DisplayMode {
    private final int width, height, bpp, freq;
    private final boolean fullscreen;

    public DisplayMode(int width, int height) {
        this(width, height, 0, 0, false);
    }

    DisplayMode(int width, int height, int bpp, int freq) {
        this(width, height, bpp, freq, false);
    }

    private DisplayMode(int width, int height, int bpp, int freq, boolean fullscreen) {
        this.width = width;
        this.height = height;
        this.bpp = bpp;
        this.freq = freq;
        this.fullscreen = fullscreen;
    }

    public boolean isFullscreenCapable() {
        return fullscreen;
    }

    public int getBitsPerPixel() {
        return bpp;
    }

    public int getFrequency() {
        return freq;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DisplayMode dm)) {
            return false;
        }

        return dm.width == width && dm.height == height && dm.bpp == bpp && dm.freq == freq;
    }

    public int hashCode() {
        return width ^ height ^ freq ^ bpp;
    }

    public String toString() {
        return width + " x " + height + " x " + bpp + " @" + freq + "Hz";
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getBpp() {
        return this.bpp;
    }

    public int getFreq() {
        return this.freq;
    }
}