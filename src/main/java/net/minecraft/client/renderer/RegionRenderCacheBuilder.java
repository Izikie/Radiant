package net.minecraft.client.renderer;

import net.minecraft.util.RenderLayer;

public class RegionRenderCacheBuilder {
    private final WorldRenderer[] worldRenderers = new WorldRenderer[RenderLayer.values().length];

    public RegionRenderCacheBuilder() {
        this.worldRenderers[RenderLayer.SOLID.ordinal()] = new WorldRenderer(2097152);
        this.worldRenderers[RenderLayer.CUTOUT.ordinal()] = new WorldRenderer(131072);
        this.worldRenderers[RenderLayer.CUTOUT_MIPPED.ordinal()] = new WorldRenderer(131072);
        this.worldRenderers[RenderLayer.TRANSLUCENT.ordinal()] = new WorldRenderer(262144);
    }

    public WorldRenderer getWorldRendererByLayer(RenderLayer layer) {
        return this.worldRenderers[layer.ordinal()];
    }

    public WorldRenderer getWorldRendererByLayerId(int id) {
        return this.worldRenderers[id];
    }
}
