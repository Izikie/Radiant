package net.radiant.render;

/** Prepares the rendered frame for capture, then presents it to the output surface. */
public interface FramePresenter extends AutoCloseable {
    void prepareFrame();

    void present();

    @Override
    void close();
}
