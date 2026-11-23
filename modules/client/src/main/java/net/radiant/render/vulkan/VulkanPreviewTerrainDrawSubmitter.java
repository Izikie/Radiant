package net.radiant.render.vulkan;

import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.RenderLayer;
import net.radiant.render.TerrainDrawSubmitter;

/** Keeps reference draws active during comparison and skips them in terrain-only mode. */
public final class VulkanPreviewTerrainDrawSubmitter implements TerrainDrawSubmitter {
    private final TerrainDrawSubmitter reference;

    private VulkanPreviewTerrainDrawSubmitter(TerrainDrawSubmitter reference) {
        this.reference = reference;
    }

    public static TerrainDrawSubmitter wrapIfEnabled(TerrainDrawSubmitter reference) {
        return VulkanTerrainPreview.ENABLED ? new VulkanPreviewTerrainDrawSubmitter(reference) : reference;
    }

    @Override
    public void beginFrame(double x, double y, double z) {
        reference.beginFrame(x, y, z);
    }

    @Override
    public boolean acceptsPass(int pass) {
        return (pass != 2 || !VulkanTerrainPreview.replacesTerrainDraws()) && reference.acceptsPass(pass);
    }

    @Override
    public void addChunk(RenderChunk chunk, RenderLayer layer) {
        reference.addChunk(chunk, layer);
    }

    @Override
    public void drawLayer(RenderLayer layer) {
        reference.drawLayer(layer);
    }
}
