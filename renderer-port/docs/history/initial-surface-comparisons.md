# Initial surface comparisons

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
