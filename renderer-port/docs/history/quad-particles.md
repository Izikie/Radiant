# Textured quad particles

At `ce99e35e`, Vulkan stages the textured quads from `EffectRenderer.renderParticles` and draws them after the block damage overlay and before precipitation. Each batch retains its OpenGL submission order, texture, and depth-write setting. The pass uses the same lightmap and alpha test, and the block-atlas batches use the shared atlas and mipmap calibration.

All 297 populated frames in `run-kufq06qc` matched the same-frame OpenGL reference with zero RGB differences or Vulkan validation errors. The fixture ran display lists and VBOs with render regions off and on. A stationary camera captured a baseline, five sheet sprites at distinct light levels, four additional overlapping block fragments, and expiry through the normal particle update. The sheet sprites changed 11,645 OpenGL pixels in the fixture area. The fragments changed another 11,549 to 13,934 pixels across the runs. After expiry, the same area matched the baseline in each mode.

Reproduce the fixture in this checkout with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/quad-particles.init.gradle --warmup 3 --seconds 3`. After restoring production classes with `python renderer-port/tools/check.py`, run `run-a0vc8kb4` matched all 118 populated frames with zero validation errors.

### Next-session checks

Port dropped-item rendering from `RenderEntityItem`, using 26.3's `ItemFeatureRenderer` for model submission and execution. Compare model transforms, atlas lighting and occlusion against opaque terrain. The item renderer also supplies draws for pickup particles in `EffectRenderer.renderLitParticles`.
