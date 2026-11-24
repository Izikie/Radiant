package net.radiant.render.terrain;

import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.RenderLayer;

import java.util.List;

public interface TerrainDrawSubmitter {
    void beginFrame(double cameraX, double cameraY, double cameraZ);

    /** Draw the sections in the supplied order. The list is valid only during this call. */
    void drawLayer(RenderLayer layer, List<RenderChunk> sections);
}
