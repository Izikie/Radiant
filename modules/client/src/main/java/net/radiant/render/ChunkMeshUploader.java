package net.radiant.render;

import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.RenderLayer;

/** Uploads a completed chunk layer on the client thread before its builder is recycled. */
public interface ChunkMeshUploader {
    void upload(RenderChunk chunk, CompiledChunk compiled, RenderLayer layer, WorldRenderer mesh);
}
