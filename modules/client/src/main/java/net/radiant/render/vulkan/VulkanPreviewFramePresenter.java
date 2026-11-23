package net.radiant.render.vulkan;

import net.radiant.render.FramePresenter;

/** Presents the Vulkan preview while the OpenGL reference frame is still in the back buffer. */
public final class VulkanPreviewFramePresenter implements FramePresenter {
    private final FramePresenter reference;

    private VulkanPreviewFramePresenter(FramePresenter reference) {
        this.reference = reference;
    }

    public static FramePresenter wrapIfEnabled(FramePresenter reference) {
        return VulkanTerrainPreview.ENABLED ? new VulkanPreviewFramePresenter(reference) : reference;
    }

    @Override
    public void prepareFrame() {
        reference.prepareFrame();
    }

    @Override
    public void present() {
        VulkanTerrainPreview.present();
        reference.present();
    }

    @Override
    public void close() {
        try {
            VulkanTerrainPreview.shutdown();
        } finally {
            reference.close();
        }
    }
}
