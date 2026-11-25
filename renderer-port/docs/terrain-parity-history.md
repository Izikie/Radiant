# Terrain parity measurements

## Initial port

| Commit | Scope |
| --- | --- |
| `095b0ec6` | OpenGL window framebuffer presentation and the existing quad fallback |
| `acdb0d03` | Ordered terrain submission with the legacy OpenGL display-list and VBO implementations |
| `9c37f53c` | Vulkan terrain preview, its integration with the shared interface, and same-frame RGB comparison |

## Initial surface comparisons

The Vulkan path measures the OpenGL atlas mip transition, captures the legacy chunk transform and preserves the OpenGL raster orientation during terrain rendering. A vertical blit presents the Vulkan image in window orientation.

The integrated client ran on a Radeon RX 7900 XT at 854 × 480. All four runs completed with zero Vulkan validation errors. The default OpenGL-only benchmark also completed.

The table below records the initial UNORM surface baseline at `9c37f53c`. The preceding presentation and shared-submission commits also built independently from clean source snapshots using `:client:jar :dev:classes`.

| OpenGL reference | Render regions | Exact frames / sampled frames with terrain | Total differing pixels across samples | Largest RGB channel difference |
| --- | --- | ---: | ---: | ---: |
| Display lists | Off | 30 / 34 | 6 | 1 |
| Display lists | On | 26 / 33 | 9 | 1 |
| VBOs | Off | 30 / 33 | 3 | 1 |
| VBOs | On | 29 / 33 | 6 | 1 |

The integrated logs are local artifacts in `renderer-port/local/parity/archive/logs/terrain-parity-integrated-*.log`. Each log identifies its capture directory. These results cover terrain in the sampled scenes on this driver.

The sRGB presentation follow-up uses a compatible UNORM attachment view so translucent terrain blends in Radiant's encoded RGB space. With display lists and regions on, forcing the BGRA8 sRGB surface produced 1,220,887 differing pixels across 33 populated samples before the fix and 5 across 33 afterward. Across all four sRGB configurations after the fix, 119 of 128 populated frames matched exactly. The remaining 9 frames each differed at one pixel by one channel level. The production surface-selection run had 7 such pixels in 32 populated frames. All five runs reported zero validation errors. See `renderer-port/local/parity/archive/logs/terrain-srgb-before.log` and `renderer-port/local/parity/archive/logs/terrain-srgb-after-*.log`.

## Terrain fog and chunk rebuilds

The ordinary fog discrepancy at pixel (115, 256) was resolved by computing the planar fog coordinate from the model-view matrix's Z row before projection. Replaying the same 48 air frames with the previous clip-W calculation gave seven differing pixels. Eye-space Z matched every frame. The four production runs on the four-second route matched all 249 populated frames across display lists and VBOs with regions on and off, with zero Vulkan validation errors. See `renderer-port/local/parity/archive/logs/terrain-eye-z-paired-air.log` and `renderer-port/local/parity/archive/logs/terrain-eye-z-{list,list-regions,vbo,vbo-regions}.log`.

At `f0db3ae0`, the underwater discrepancy occurred on a translucent face crossing the eye plane. Interpolate signed eye-space Z through clipping and take its magnitude in the fragment shader. On the three failing frames, replaying that change removed all 98,724 differing pixels (`renderer-port/local/parity/archive/logs/terrain-water-arrival.log`).

On the RX 7900 XT at 854 × 480, all 265 populated frames on the ordinary route and all 355 populated frames on the liquid fixture matched OpenGL exactly. Both routes used display lists and VBOs with render regions on and off. The fixture sampled the first frames after entering a liquid, covering 203 underwater frames and 52 in lava. All eight runs completed with zero Vulkan validation errors. See `renderer-port/local/parity/archive/logs/terrain-signed-z-{air,liquid}-*.log`.

The additional BGRA8 sRGB liquid run with display lists and regions on matched all 90 populated frames, with zero validation errors (`renderer-port/local/parity/archive/logs/terrain-signed-z-srgb.log`).

At `7361ee62`, Vulkan began tracking per-layer uploads. During chunk rebuilds, OpenGL replaces each layer's vertices at upload time. The shared submission still selects visible layers from the published compiled section. Vulkan now draws the latest uploaded vertices for each submitted layer, including when a frame falls between solid and translucent uploads. An empty VBO upload clears that layer. An empty display-list upload leaves the section's layer list intact.

The glass-panel fixture moves the camera and edits overlapping terrain across section boundaries. It yields after selected solid uploads to expose the intermediate frames. Before the fix, 62 of 107 populated frames differed across two runs, with a maximum RGB channel error of 202. After the fix, all 216 populated frames across the four OpenGL configurations matched exactly, including runs with 189 split uploads and 51 completed transparency re-sorts in total. All four runs had zero Vulkan validation errors. See `renderer-port/local/parity/archive/logs/terrain-panel-{before,after}-*.log`.

After rebuilding production classes, the ordinary benchmark route matched all 136 populated frames across the same four configurations, with zero validation errors. See `renderer-port/local/parity/archive/logs/terrain-panel-control-*.log`.

The RGBA32F probe also exposed a difference with fog disabled. OpenGL wrote the lit color directly, while evaluating the Vulkan fog mix at full visibility changed the blue channel by one floating-point step. The Vulkan shader now skips the mix when fog is disabled. See `renderer-port/local/parity/archive/logs/terrain-float-pixel.log` for the probe and `renderer-port/local/parity/archive/logs/terrain-fog-bypass-*.log` for production comparisons.

All 129 populated fog-off frames in the production runs matched OpenGL exactly across display lists and VBOs, with regions on and off. The enabled-fog control had one differing pixel by one channel level across 33 populated frames. All five runs completed with zero Vulkan validation errors on the RX 7900 XT.

## Resource reload and same-world re-entry

At `61d0784d` the Vulkan window hides when the client leaves a world. The first frame of a new world can have zero terrain submissions. Vulkan now presents a cleared terrain target for that frame. The integrated server handles simultaneous shutdown requests once (`5daad531`), and the client ignores queued play packets for a disconnected handler (`96cafec5`).

On the RX 7900 XT at 854 × 480, all 159 populated samples and all 124 empty samples matched OpenGL exactly across display lists and VBOs with regions on and off. Each run installed a pack with a 64-pixel stone-brick texture, checked the GLFW window was hidden with zero retained sections at the menu, and reopened the same save with a distinct client world. The runs sampled 64 populated frames immediately after reload and 65 after re-entry. All four reported zero client-thread errors and zero Vulkan validation errors. The fixture briefly holds the camera still and then turns it, since the legacy reference can submit no sections while stationary.

Local logs are under `renderer-port/local/parity/runs/run-0ubfap4y/`. To repeat, run `python renderer-port/local/parity/fixtures/terrain-transition-gen.py` and then `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/terrain-transition-fixture.init.gradle --warmup 2`. Rebuild production classes after the fixture.

After rebuilding without overrides, the ordinary production route matched all 129 populated frames across the same four configurations, with zero Vulkan validation errors. See `renderer-port/local/parity/runs/run-58l_41mb/`.

## Overworld sky background

At `dde18013` Vulkan clears to the world fog color and draws the upper sky and lower horizon before terrain. The reference copies the same frame's OpenGL clear, then replays Radiant's sky planes and ordered terrain layers. Every RGB pixel enters the comparison, including frames with only sky. A frame counts as terrain-populated when terrain changes the background color of at least one pixel.

On the RX 7900 XT at 854 × 480, all 117 populated ordinary-route samples and all 110 populated sunset and night pitch-sweep samples matched exactly across display lists and VBOs with regions on and off. The runs reported zero differing pixels and zero Vulkan validation errors. Under `renderer-port/local/parity/runs/`, see `run-01f_vcsq` for the sweep, `run-jy4n3deb` for both display-list results, and `run-q84t5k_s` and `run-nj2el1l1` for the completed VBO runs.

Omitting the Vulkan upper plane produced 3,764,548 differing pixels (`run-dbjb3opv`). Near sea level, a fixture compared the sky before terrain could obscure the lower plane. Omitting the lower plane produced 10,177,803 differing pixels (`run-ss98twud`), and restoring it gave an exact match (`run-l_is9wn_`).

With VBOs and render regions on, the transition fixture installed the 64-pixel stone-brick texture, hid the preview at the menu with zero retained sections, and reopened the same save in a distinct client world. All 40 populated samples matched, with zero differing pixels and zero Vulkan validation errors (`run-92qukdhr`). Twilight gradients, celestial bodies and clouds remain outside this comparison.

## Sunrise and sunset

At `b6fa98c4` the Vulkan sky draws Radiant's 18-vertex twilight fan between the upper and lower sky planes. The comparison replays the OpenGL fan in that same position with its original transform, packed vertex colors and blend factors.

On the RX 7900 XT at 854 × 480, the horizon sweep matched all 53 terrain-populated samples across display lists and VBOs with regions off and on. The fixture fixed the client time at 14,000 ticks for dusk and 22,000 for dawn and rotated the camera through both horizons. Omitting only the Vulkan fan produced 484,145 differing pixels at dusk and 435,696 at dawn. Under `renderer-port/local/parity/runs/`, see `run-ax1lk5do` for the sweep and `run-9rrlb4yc` for the control. The local fixture is `renderer-port/local/parity/fixtures/twilight-angles.init.gradle`.

The ordinary production route matched all 124 populated samples across the same four configurations (`run-b55wnl8u`). Both four-mode suites reported zero differing pixels across all samples and zero Vulkan validation errors.

## Textured sun and moon

At `c59f5405`, Vulkan draws the sun and moon after twilight and before the lower horizon. Both backends consume the same textured quads, including Radiant's moon-phase coordinates. Vulkan uses the bound texture, the celestial transform and the rain-dependent color with source-alpha additive blending.

On the RX 7900 XT at 854 × 480, all 191 samples of the rotating-camera fixture matched across display lists and VBOs with regions on and off. Each mode sampled the noon sun and all eight midnight moon phases, alternating clear sky and 0.375 rain strength. The camera then lowered to include terrain. Omitting the Vulkan bodies changed 526,811 pixels during the sun segment and 233,855 during the moon segment, with differences in every phase. The ordinary production route matched all 175 samples, including 127 with visible terrain.

A VBO render-regions run replaced both textures during a resource reload with asymmetric RGBA images using linear filtering and clamped edges. The fixture verified the 32-pixel sun and its filter after reload. All 47 samples matched. All nine positive runs reported zero differing pixels and zero Vulkan validation errors.

Local logs under `renderer-port/local/parity/runs/` are `run-6nxf5143` (celestial fixture), `run-ohwu3shw` (omission control), `run-4r1y_rh7` (ordinary route) and `run-1ge17_fz` (resource reload). Repeat the celestial fixture with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/celestial.init.gradle --seconds 6`.

## Seeded overworld stars

At `bcb460c6` the Vulkan sky uses Radiant's 780 seeded star quads, the celestial transform and the same rain-adjusted brightness as OpenGL. Like 26.3's star pass, the draw uses a vertex buffer and shared quad indices. It runs between the moon and lower horizon draws. The comparison replays the active OpenGL star VBO or display list in the same frame.

On the RX 7900 XT at 854 × 480, all 183 star-fixture samples and all 172 ordinary-route samples matched across the four terrain configurations. The fixture rotated the camera at midnight, dawn and dusk, with clear sky, partial rain, full rain and the stars setting disabled. Omitting only the Vulkan star draw changed 9,034 pixels across the samples. All samples with full rain or stars disabled still matched in that control.

A separate run switched from display lists to VBOs and back during the night. Each switch rebuilt all 3,120 star vertices, and all 46 samples matched. All nine positive runs reported zero differing pixels and zero Vulkan validation errors. The production client was rebuilt after the last fixture.

Local logs under `renderer-port/local/parity/runs/` are `run-m3t8y756` (star fixture), `run-m57aq6h1` (omission control), `run-z6j2xghq` (ordinary route) and `run-onidtdzx` (live switch). Repeat the star fixture with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/stars.init.gradle --seconds 6`.

## Fast overworld clouds

At `c23e1a0e` the Vulkan flat-cloud pass draws the same cached textured quads as Radiant's fast-cloud display list. It captures the display list's camera and wind translation, cloud projection, alpha test and fog. Below the layer it draws before terrain. Above the layer it draws after translucent terrain. The comparison replays the OpenGL display list at the matching point in the frame.

On the RX 7900 XT at 854 × 480, all 183 samples across display lists and VBOs with regions off and on matched exactly, including daytime, dusk, partial rain and clouds disabled. Omitting Vulkan clouds produced 640,273 differing pixels below the layer and 55,944 above. The disabled samples stayed exact. Reversing the Vulkan draw order relative to terrain produced 11,594 differing pixels. The fixture and controls are in `renderer-port/local/parity/runs/run-sj79r7sc`, `run-31iju2ig` and `run-2h5jgrqp`, respectively.

The reload fixture exposed an empty translucent re-sort that queued a pooled buffer with zero vertices and 2,240 bytes from an earlier mesh. At `70bed0d6` the worker queues the upload only when the re-sort produced geometry. A glass-panel run recorded 38 populated re-sorts and matched all 27 samples (`run-1lj3cep8`). The four-mode reload fixture verified a 128-pixel-wide, linearly filtered replacement cloud texture with partial alpha and matched all 185 samples (`run-p0x0dj6t`).

The production route matched all 171 samples across the four modes (`run-8788xo7b`). A separate VBO regression for the shared texture bindings matched all 47 sun and moon samples (`run-qi2wngj6`). All these positive runs finished with zero Vulkan validation errors. Repeat the cloud fixture with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/fast-clouds.init.gradle --seconds 6`.

## Fancy overworld clouds

At `b9825147` Vulkan draws the cached cloud faces with their legacy UVs and face colors, first with color writes disabled and then with blending enabled for the visible color draw. Both draws use the same alpha test, transform and geometry as the Radiant display list. Radiant rebuilds fancy clouds after drawing the old list, so Vulkan uses the replacement mesh starting on the following frame.

On the RX 7900 XT at 854 × 480, all 283 samples matched across display lists and VBOs with render regions off and on. The fixture positioned the cloud layer below, around and above the camera, switched among fancy, fast and disabled clouds, and sampled 68 first frames after fancy mesh rebuilds. Omitting only the depth draw produced 701,000 differing pixels, including 247,681 inside the layer. Omitting both fancy draws produced 2,136,238 differing pixels.

A VBO render-regions run reloaded a 128-pixel-wide, linearly filtered cloud texture with partial alpha. All 61 samples matched. After rebuilding production classes, all 175 ordinary-route samples matched in the four configurations. All nine positive runs completed with zero Vulkan validation errors.

Local logs under `renderer-port/local/parity/runs/` are `run-rfil0usk` (four-mode sweep), `run-mh8s9p_8` (depth omitted), `run-6br42dej` (both draws omitted), `run-hdhi_8ak` (texture reload) and `run-etf4uy3b` (production). Repeat the sweep with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/fancy-clouds.init.gradle --seconds 8`.
