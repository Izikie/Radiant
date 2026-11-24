package net.radiant.render.vulkan;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.RenderLayer;
import net.radiant.render.terrain.TerrainDrawSubmitter;

import java.util.List;

/** Presents Vulkan terrain alongside the complete OpenGL scene while the port is in progress. */
public final class VulkanTerrainDrawSubmitter implements TerrainDrawSubmitter {
    private final Minecraft client;
    private final TerrainDrawSubmitter openGl;
    private double cameraX, cameraY, cameraZ;

    public VulkanTerrainDrawSubmitter(Minecraft client, TerrainDrawSubmitter openGl) {
        this.client = client;
        this.openGl = openGl;
    }

    @Override
    public void beginFrame(double x, double y, double z) {
        openGl.beginFrame(x, y, z);
        VulkanTerrainPreview.openIfNeeded(client);
        VulkanTerrainPreview.beginFrame();
        cameraX = x;
        cameraY = y;
        cameraZ = z;
    }

    @Override
    public void drawLayer(RenderLayer layer, List<RenderChunk> sections) {
        VulkanTerrainPreview.drawLayer(client, cameraX, cameraY, cameraZ, layer, sections, openGl);
        openGl.drawLayer(layer, sections);
    }
}
