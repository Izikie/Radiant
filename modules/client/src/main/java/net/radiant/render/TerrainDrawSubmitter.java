package net.radiant.render;

import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.RenderLayer;

/** Submits the ordered chunk layers collected by the world renderer. */
public interface TerrainDrawSubmitter {
    void beginFrame(double x, double y, double z);

    boolean acceptsPass(int pass);

    void addChunk(RenderChunk chunk, RenderLayer layer);

    void drawLayer(RenderLayer layer);
}
