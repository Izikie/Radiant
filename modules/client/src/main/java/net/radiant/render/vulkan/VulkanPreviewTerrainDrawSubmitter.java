package net.radiant.render.vulkan;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraft.util.RenderLayer;
import net.radiant.render.TerrainDrawSubmitter;
import org.joml.Matrix4fc;

import java.util.function.IntFunction;

/** Keeps reference draws active during comparison and skips them in terrain-only mode. */
public final class VulkanPreviewTerrainDrawSubmitter implements TerrainDrawSubmitter {
    private final Minecraft client;
    private final TerrainDrawSubmitter reference;

    private VulkanPreviewTerrainDrawSubmitter(Minecraft client, TerrainDrawSubmitter reference) {
        this.client = client;
        this.reference = reference;
    }

    public static TerrainDrawSubmitter wrapIfEnabled(Minecraft client, TerrainDrawSubmitter reference) {
        return VulkanTerrainPreview.ENABLED ? new VulkanPreviewTerrainDrawSubmitter(client, reference) : reference;
    }

    @Override
    public void beginFrame(double x, double y, double z) {
        reference.beginFrame(x, y, z);
    }

    @Override
    public void extractFrame(Entity view, float partialTicks, Matrix4fc projection, Matrix4fc viewMatrix,
                             int[] lightmap, GlStateManager.FogParameters skyFog,
                             int visibleCount, IntFunction<RenderChunk> visibleChunk) {
        double x = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double y = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double z = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;
        var frame = VulkanTerrainPreview.extract(client, view, x, y, z, partialTicks, projection, viewMatrix, lightmap, skyFog);
        if (frame != null) {
            for (int index = 0; index < visibleCount; index++) {
                frame.add(visibleChunk.apply(index));
            }
            frame.finish();
        }
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
