package net.minecraft.client.gui;

import net.minecraft.block.material.MapColor;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec4b;
import net.minecraft.world.storage.MapData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MapItemRenderer {
    private static final ResourceLocation MAP_ICONS = new ResourceLocation("textures/map/map_icons.png");
    private final TextureManager textureManager;
    private final Map<String, Instance> loadedMaps = new HashMap<>();

    public MapItemRenderer(TextureManager textureManagerIn) {
        this.textureManager = textureManagerIn;
    }

    public void updateMapTexture(MapData mapdataIn) {
        this.getMapRendererInstance(mapdataIn).updateMapTexture();
    }

    public void renderMap(MapData mapdataIn, boolean p_148250_2_) {
        this.getMapRendererInstance(mapdataIn).render(p_148250_2_);
    }

    private Instance getMapRendererInstance(MapData mapdataIn) {
        Instance mapitemrenderer$instance = this.loadedMaps.get(mapdataIn.mapName);

        if (mapitemrenderer$instance == null) {
            mapitemrenderer$instance = new Instance(mapdataIn);
            this.loadedMaps.put(mapdataIn.mapName, mapitemrenderer$instance);
        }

        return mapitemrenderer$instance;
    }

    public void clearLoadedMaps() {
        for (Instance mapitemrenderer$instance : this.loadedMaps.values()) {
            this.textureManager.deleteTexture(mapitemrenderer$instance.location);
        }

        this.loadedMaps.clear();
    }

    class Instance {
        private final MapData mapData;
        private final DynamicTexture mapTexture;
        private final ResourceLocation location;
        private final int[] mapTextureData;

        private Instance(MapData mapdataIn) {
            this.mapData = mapdataIn;
            this.mapTexture = new DynamicTexture(128, 128);
            this.mapTextureData = this.mapTexture.getTextureData();
            this.location = MapItemRenderer.this.textureManager.getDynamicTextureLocation("map/" + mapdataIn.mapName, this.mapTexture);

            Arrays.fill(this.mapTextureData, 0);
        }

        private void updateMapTexture() {
            for (int i = 0; i < 16384; ++i) {
                int j = this.mapData.colors[i] & 255;

                if (j / 4 == 0) {
                    this.mapTextureData[i] = (i + i / 128 & 1) * 8 + 16 << 24;
                } else {
                    this.mapTextureData[i] = MapColor.MAP_COLOR_ARRAY[j / 4].getMapColor(j & 3);
                }
            }

            this.mapTexture.updateDynamicTexture();
        }

        private void render(boolean noOverlayRendering) {
            int i = 0;
            int j = 0;
            Tessellator tessellator = Tessellator.get();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            float f = 0.0F;
            MapItemRenderer.this.textureManager.bindTexture(this.location);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(1, 771, 0, 1);
            GlStateManager.disableAlpha();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
            worldrenderer.pos(((i) + f), ((j + 128) - f), -0.009999999776482582D).tex(0.0D, 1.0D).endVertex();
            worldrenderer.pos(((i + 128) - f), ((j + 128) - f), -0.009999999776482582D).tex(1.0D, 1.0D).endVertex();
            worldrenderer.pos(((i + 128) - f), ((j) + f), -0.009999999776482582D).tex(1.0D, 0.0D).endVertex();
            worldrenderer.pos(((i) + f), ((j) + f), -0.009999999776482582D).tex(0.0D, 0.0D).endVertex();
            tessellator.draw();
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
            MapItemRenderer.this.textureManager.bindTexture(MapItemRenderer.MAP_ICONS);
            int k = 0;

            for (Vec4b vec4b : this.mapData.mapDecorations.values()) {
                if (!noOverlayRendering || vec4b.x() == 1) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(i + vec4b.y() / 2.0F + 64.0F, j + vec4b.z() / 2.0F + 64.0F, -0.02F);
                    GlStateManager.rotate((vec4b.w() * 360) / 16.0F, 0.0F, 0.0F, 1.0F);
                    GlStateManager.scale(4.0F, 4.0F, 3.0F);
                    GlStateManager.translate(-0.125F, 0.125F, 0.0F);
                    byte b0 = vec4b.x();
                    float f1 = (b0 % 4) / 4.0F;
                    float f2 = (b0 / 4) / 4.0F;
                    float f3 = (b0 % 4 + 1) / 4.0F;
                    float f4 = (b0 / 4 + 1) / 4.0F;
                    worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
                    float f5 = -0.001F;
                    worldrenderer.pos(-1.0D, 1.0D, (k * -0.001F)).tex(f1, f2).endVertex();
                    worldrenderer.pos(1.0D, 1.0D, (k * -0.001F)).tex(f3, f2).endVertex();
                    worldrenderer.pos(1.0D, -1.0D, (k * -0.001F)).tex(f3, f4).endVertex();
                    worldrenderer.pos(-1.0D, -1.0D, (k * -0.001F)).tex(f1, f4).endVertex();
                    tessellator.draw();
                    GlStateManager.popMatrix();
                    ++k;
                }
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.0F, -0.04F);
            GlStateManager.scale(1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }
}
