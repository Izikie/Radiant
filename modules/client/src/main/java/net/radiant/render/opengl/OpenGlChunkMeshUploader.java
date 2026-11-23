package net.radiant.render.opengl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.VertexBufferUploader;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.ListedRenderChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.RenderLayer;
import net.radiant.benchmark.ClientBenchmark;
import net.radiant.render.ChunkMeshUploader;
import org.lwjgl.opengl.GL11;

public final class OpenGlChunkMeshUploader implements ChunkMeshUploader {
    private final WorldVertexBufferUploader displayListUploader = new WorldVertexBufferUploader();
    private final VertexBufferUploader vertexBufferUploader = new VertexBufferUploader();

    @Override
    public void upload(RenderChunk chunk, CompiledChunk compiled, RenderLayer layer, WorldRenderer mesh) {
        if (OpenGlHelper.useVbo()) {
            vertexBufferUploader.setVertexBuffer(chunk.getVertexBufferByLayer(layer.ordinal()));
            vertexBufferUploader.draw(mesh);
        } else {
            int displayList = ((ListedRenderChunk) chunk).getDisplayList(layer, compiled);
            GL11.glNewList(displayList, GL11.GL_COMPILE);
            GlStateManager.pushMatrix();
            chunk.multModelviewMatrix();
            displayListUploader.draw(mesh);
            GlStateManager.popMatrix();
            GL11.glEndList();
        }
        ClientBenchmark.recordTerrainUpload(false);
    }
}
