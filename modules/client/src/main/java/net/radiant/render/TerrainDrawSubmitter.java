package net.radiant.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraft.util.RenderLayer;
import org.joml.Matrix4fc;

import java.util.function.IntFunction;

/** Submits the ordered chunk layers collected by the world renderer. */
public interface TerrainDrawSubmitter {
    void beginFrame(double x, double y, double z);

    /** Consumes the camera and visible chunks synchronously after queued uploads have run. */
    default void extractFrame(Entity view, float partialTicks, Matrix4fc projection, Matrix4fc viewMatrix,
                              int[] lightmap, GlStateManager.FogParameters skyFog,
                              int visibleCount, IntFunction<RenderChunk> visibleChunk) {
    }

    boolean acceptsPass(int pass);

    void addChunk(RenderChunk chunk, RenderLayer layer);

    void drawLayer(RenderLayer layer);
}
