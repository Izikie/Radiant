# Minecraft 26.3 rendering reference

Use this map to find the Radiant code that serves the same responsibility. Modern classes may split one legacy renderer across several passes.

| 26.3 source | Radiant entry points |
| --- | --- |
| `net.minecraft.client.renderer.GameRenderer`, `LevelRenderer` | `EntityRenderer.renderWorldPass`, `RenderGlobal` |
| `renderer.chunk.SectionRenderDispatcher`, `SectionCompiler`, `CompiledSectionMesh` | `ChunkRenderDispatcher`, `ChunkRenderWorker`, `RenderChunk`, `CompiledChunk` |
| `renderer.chunk.ChunkSectionsToRender`, `ChunkSectionLayer` | `RenderGlobal.renderBlockLayer`, `TerrainDrawSubmitter`, `RenderLayer` |
| `renderer.SkyRenderer`, `renderer.state.level.SkyRenderState` | `RenderGlobal.renderSky`, `generateSky`, `generateSky2`, `generateStars`. Vulkan draws the upper and lower sky planes from `VulkanSkyBackground.State`. |
| `renderer.SkyRenderer.renderSunriseAndSunset` | `RenderGlobal.drawTwilight` supplies the transformed, colored fan to `VulkanSkyBackground.drawTwilight` between the sky planes. |
| `renderer.SkyRenderer.renderSun`, `renderMoon` | `RenderGlobal.drawCelestialBody` supplies the sun or moon quad, including Radiant's moon-phase UVs. `VulkanCelestialPipeline` draws each with its bound texture and rain-dependent color. |
| `renderer.SkyRenderer.renderStars` | `RenderGlobal.renderStars` builds Radiant's seeded star quads. `VulkanSkyBackground.drawStars` draws that mesh with the celestial transform, star brightness and shared quad indices. |
| `renderer.CloudRenderer.prepare`, `render` | `RenderGlobal.renderClouds` and `renderCloudsFancy` build the display-list geometry. `VulkanCloudPipeline` draws fast clouds once and fancy clouds in depth and color passes, using Radiant's translation, projection, fog and texture. |
| `renderer.WeatherEffectRenderer.prepare`, `render` | `EntityRenderer.renderRainSnow` supplies Radiant's lightmapped rain and snow column batches in draw order. `VulkanWeatherPipeline` draws them between opaque and translucent terrain. |
| `renderer.WorldBorderRenderer.prepare`, `render` | `RenderGlobal.renderWorldBorder` supplies the visible sides, scroll phase and fade. `VulkanWorldBorderPipeline` draws the prepared mesh after precipitation and before translucent terrain. |
| `renderer.texture.TextureAtlas` | `TextureMap`, `VulkanTerrainPreview.captureAtlas`, `VulkanAtlasUpdates` |
| `com.mojang.blaze3d.pipeline.RenderTarget`, `platform.Window` | `Framebuffer`, `Display`, `OpenGlWindowBlit`, `VulkanRenderTarget`, `VulkanPresenter` |
| `com.mojang.renderpearl.api.commands.CommandEncoder`, `api.pipeline.RenderPipeline`, `backend.opengl.GlDevice`, `backend.vulkan.VulkanDevice` | `OpenGlTerrainDrawSubmitter`, `VulkanTerrainDrawSubmitter`, `VulkanTerrainScene`, `VulkanTerrainPipeline`, `VulkanPresenter` |

Put the 26.3 client JAR at `renderer-port/local/reference/jars/26.3.jar`. Find the launcher's 26.3 client JAR under `versions/26.3/26.3.jar` in the launcher directory (Windows `%APPDATA%\.minecraft`, macOS `~/Library/Application Support/minecraft`, Linux `~/.minecraft`). The verified client SHA-1 is `e877b6a07acd633fb3bb475002175cec036e7b87`.

The extractor uses the cached `renderer-port/local/reference/jars/vineflower-1.12.0.jar`. On a fresh checkout, download it to that path or pass `--decompiler <path>`. Run the extractor from the repository root. [Vineflower releases](https://github.com/Vineflower/vineflower/releases)

```sh
python renderer-port/tools/extract_reference.py
rg --no-ignore-vcs -n extractRenderState renderer-port/local/reference/src/net/minecraft/client/renderer/SkyRenderer.java
```

The extractor passes only render classes to the decompiler and supplies the full client as a dependency. Generated Java and the decompiler log stay under the ignored reference directory.
