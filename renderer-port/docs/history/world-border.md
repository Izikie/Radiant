# World border

At `7ad19f64` the Vulkan preview draws the world border after precipitation and before translucent terrain. Radiant supplies the camera-relative face vertices, scrolling UVs, status color and distance fade from the same call that draws the OpenGL border. Vulkan prepares the texture and mesh before rendering, then uses additive blending, the alpha test, fog and a read-only depth test.

On the RX 7900 XT at 854 × 480, all 426 same-frame RGB comparisons in `run-8a9hrxl7` were exact, including 313 with visible terrain. Display lists and VBOs each ran with render regions off and on. The face distances of 8.5 and 28.5 blocks produced fade factors of 0.760 and 0.365. A corner 7.5 blocks away showed both faces through stained glass. A second corner exercised late clouds in frames with no terrain submissions. At 159.5 blocks, the border was outside the 128-block range. Consecutive captures from the stationary near-face camera showed the forcefield scrolling.

Omitting the Vulkan border in `run-dk2lt3rz` changed 8,178,652 pixels. Drawing it after translucent terrain in `run-2yt6h8b9` changed 1,796,124. Both controls matched outside the border range. All four fixture runs and both controls reported zero Vulkan validation errors.

The empty-terrain corner exposed cloud ordering that depended on the presence of a terrain submission. The preview now takes the order from the actual cloud pass. Two further pixels where the border met the floor exposed a depth-format difference. At the left pixel, unbiased depths matched. OpenGL's constant offset moved the border 12 D24 steps closer. A separate OpenGL probe reproduced that offset at five window depths from 0.0625 to 0.875. The AMD OpenGL comparison path applies the measured constant in the fragment shader when Vulkan selects a D32 floating-point target.

After the fixture, the production rebuild and ordinary route in `run-b5lo5_p_` matched all 122 populated comparisons across the four modes, with zero differing pixels and zero validation errors.

To repeat the fixture in this checkout, run `python renderer-port/tools/terrain_parity.py run --warmup 2 --seconds 4 --init-script renderer-port/local/parity/fixtures/world-border.init.gradle`. Rebuild production classes with `python renderer-port/tools/check.py` afterward.

### Next-session checks

Start the block selection outline at the two calls to `RenderGlobal.drawSelectionBox` in `EntityRenderer.renderWorldPass` and compare 26.3's `LevelRenderer.submitBlockOutline`. Capture a selected block with partial bounds and an occluded edge, then move the crosshair off the block. Verify the two-pixel line coverage, depth test and blending in all four terrain configurations.
