package net.radiant.render.vulkan;

import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.RenderLayer;
import net.radiant.render.ChunkMeshUploader;

public final class VulkanPreviewChunkMeshUploader implements ChunkMeshUploader {
    private final ChunkMeshUploader reference;

    private VulkanPreviewChunkMeshUploader(ChunkMeshUploader reference) {
        this.reference = reference;
    }

    public static ChunkMeshUploader wrapIfEnabled(ChunkMeshUploader reference) {
        return VulkanTerrainPreview.ENABLED ? new VulkanPreviewChunkMeshUploader(reference) : reference;
    }

    @Override
    public void upload(RenderChunk chunk, CompiledChunk compiled, RenderLayer layer, WorldRenderer mesh) {
        // Read the builder before the legacy OpenGL uploader resets it.
        if (!VulkanTerrainPreview.upload(chunk, compiled, layer, mesh)) {
            reference.upload(chunk, compiled, layer, mesh);
        }
    }
}
