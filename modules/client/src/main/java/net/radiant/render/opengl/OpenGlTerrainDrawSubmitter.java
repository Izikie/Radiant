package net.radiant.render.opengl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderList;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.RenderLayer;
import net.optifine.Config;
import net.optifine.shaders.ShadersRender;
import net.radiant.benchmark.ClientBenchmark;
import net.radiant.render.TerrainDrawSubmitter;
import org.lwjgl.opengl.GL11;

public final class OpenGlTerrainDrawSubmitter implements TerrainDrawSubmitter {
    private final Minecraft client;
    private final ChunkRenderContainer container;

    public OpenGlTerrainDrawSubmitter(Minecraft client, boolean vboEnabled) {
        this.client = client;
        this.container = vboEnabled ? new VboRenderList() : new RenderList();
    }

    @Override
    public void beginFrame(double x, double y, double z) {
        container.initialize(x, y, z);
    }

    @Override
    public boolean acceptsPass(int pass) {
        return true;
    }

    @Override
    public void addChunk(RenderChunk chunk, RenderLayer layer) {
        container.addRenderChunk(chunk, layer);
    }

    @Override
    public void drawLayer(RenderLayer layer) {
        if (Config.isFogOff() && client.entityRenderer.fogStandard) {
            GlStateManager.disableFog();
        }

        client.entityRenderer.enableLightmap();

        if (OpenGlHelper.useVbo()) {
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        }

        if (Config.isShaders()) {
            ShadersRender.preRenderChunkLayer(layer);
        }

        container.renderChunkLayer(layer);

        if (Config.isShaders()) {
            ShadersRender.postRenderChunkLayer(layer);
        }

        if (OpenGlHelper.useVbo()) {
            for (VertexFormatElement element : DefaultVertexFormats.BLOCK.getElements()) {
                switch (element.usage()) {
                    case POSITION:
                        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
                        break;

                    case UV:
                        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit + element.index());
                        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                        break;

                    case COLOR:
                        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
                        GlStateManager.resetColor();
                }
            }
        }

        client.entityRenderer.disableLightmap();
        ClientBenchmark.recordGlTerrainBatch();
    }
}
