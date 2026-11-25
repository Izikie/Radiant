# Minecraft 26.3 rendering reference

Use this map to find the Radiant code that serves the same responsibility. Modern classes may split one legacy renderer across several passes.

| 26.3 source | Radiant entry points |
| --- | --- |
| `net.minecraft.client.renderer.GameRenderer`, `LevelRenderer` | `EntityRenderer.renderWorldPass`, `RenderGlobal` |
| `renderer.chunk.SectionRenderDispatcher`, `SectionCompiler`, `CompiledSectionMesh` | `ChunkRenderDispatcher`, `ChunkRenderWorker`, `RenderChunk`, `CompiledChunk` |
| `renderer.chunk.ChunkSectionsToRender`, `ChunkSectionLayer` | `RenderGlobal.renderBlockLayer`, `TerrainDrawSubmitter`, `RenderLayer` |
| `renderer.SkyRenderer`, `renderer.state.level.SkyRenderState` | `RenderGlobal.renderSky`, `generateSky`, `generateSky2`, `generateStars` |
| `renderer.texture.TextureAtlas` | `TextureMap`, `VulkanTerrainPreview.captureAtlas`, `VulkanAtlasUpdates` |
| `com.mojang.blaze3d.pipeline.RenderTarget`, `platform.Window` | `Framebuffer`, `Display`, `OpenGlWindowBlit`, `VulkanRenderTarget`, `VulkanPresenter` |
| `com.mojang.renderpearl.api.commands.CommandEncoder`, `api.pipeline.RenderPipeline`, `backend.opengl.GlDevice`, `backend.vulkan.VulkanDevice` | `OpenGlTerrainDrawSubmitter`, `VulkanTerrainDrawSubmitter`, `VulkanTerrainScene`, `VulkanTerrainPipeline`, `VulkanPresenter` |

Put the 26.3 client JAR at `renderer-port/local/reference/jars/26.3.jar`. The launcher copy is at `%APPDATA%\.minecraft\versions\26.3\26.3.jar` and the verified client SHA-1 is `e877b6a07acd633fb3bb475002175cec036e7b87`.

The extractor uses the cached `renderer-port/local/reference/jars/vineflower-1.12.0.jar`. On a fresh checkout, download it to that path or pass `--decompiler <path>`. Run the extractor from the repository root. [Vineflower releases](https://github.com/Vineflower/vineflower/releases)

```powershell
python renderer-port/tools/extract_reference.py
rg --no-ignore-vcs -n 'extractRenderState' renderer-port/local/reference/src/net/minecraft/client/renderer/SkyRenderer.java
```

The extractor passes only render classes to the decompiler and supplies the full client as a dependency. Generated Java and the decompiler log stay under the ignored reference directory.
