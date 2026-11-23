package net.radiant.render.vulkan;

import net.minecraft.client.Minecraft;
import net.optifine.Config;
import net.radiant.benchmark.ClientBenchmark;
import net.radiant.render.FramePresenter;
import net.radiant.util.NativeImage;

import java.util.function.Consumer;

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
    public NativeImage readFrame() {
        return reference.readFrame();
    }

    @Override
    public void present() {
        Consumer<NativeImage> capture = null;
        if (ClientBenchmark.COMPARE_FRAMES) {
            Minecraft client = Minecraft.get();
            if (ClientBenchmark.wantsFrameComparison(client)) {
                float partialTicks = Config.renderPartialTicks;
                capture = vulkan -> ClientBenchmark.saveFrameComparison(client, partialTicks, readFrame(), vulkan);
            }
        }
        VulkanTerrainPreview.present(capture);
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
