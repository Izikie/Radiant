package net.radiant.render;

import net.radiant.util.NativeImage;

/** Prepares the rendered frame for capture, then presents it to the output surface. */
public interface FramePresenter extends AutoCloseable {
    void prepareFrame();

    /** Reads the prepared frame of the primary surface before presenting it. */
    NativeImage readFrame();

    void present();

    @Override
    void close();
}
