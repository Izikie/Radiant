# Textured sun and moon

At `c59f5405`, Vulkan draws the sun and moon after twilight and before the lower horizon. Both backends consume the same textured quads, including Radiant's moon-phase coordinates. Vulkan uses the bound texture, the celestial transform and the rain-dependent color with source-alpha additive blending.

On the RX 7900 XT at 854 × 480, all 191 samples of the rotating-camera fixture matched across display lists and VBOs with regions on and off. Each mode sampled the noon sun and all eight midnight moon phases, alternating clear sky and 0.375 rain strength. The camera then lowered to include terrain. Omitting the Vulkan bodies changed 526,811 pixels during the sun segment and 233,855 during the moon segment, with differences in every phase. The ordinary production route matched all 175 samples, including 127 with visible terrain.

A VBO render-regions run replaced both textures during a resource reload with asymmetric RGBA images using linear filtering and clamped edges. The fixture verified the 32-pixel sun and its filter after reload. All 47 samples matched. All nine positive runs reported zero differing pixels and zero Vulkan validation errors.

Local logs under `renderer-port/local/parity/runs/` are `run-6nxf5143` (celestial fixture), `run-ohwu3shw` (omission control), `run-4r1y_rh7` (ordinary route) and `run-1ge17_fz` (resource reload). Repeat the celestial fixture with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/celestial.init.gradle --seconds 6`.

