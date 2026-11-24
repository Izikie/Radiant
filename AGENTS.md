# Radiant renderer port

## Objective

Port the rendering engine from Minecraft 26.3 into Radiant's Minecraft 1.8.9 MCP client. Use the installed `%APPDATA%\.minecraft\versions\26.3\26.3.jar` as the source for modern renderer behavior and architecture. Use Radiant 1.8.9 OpenGL as the visual reference. The target is a pixel-identical image. Defer optimizations until parity is established and the user asks for them.

Support both OpenGL and Vulkan. Preserve Radiant's OpenGL display-list and VBO paths, including render regions on and off. For terrain, both backends consume the same ordered section submissions, including the translucent draw order.

## Current checkpoint

The port is on `modern-renderer`. The initial changes are split into three local commits so the OpenGL baseline and the opt-in Vulkan path can be tested independently.

| Commit | Scope |
| --- | --- |
| `095b0ec6` | OpenGL window framebuffer presentation and the existing quad fallback |
| `acdb0d03` | Ordered terrain submission with the legacy OpenGL display-list and VBO implementations |
| `9c37f53c` | Vulkan terrain preview, its integration with the shared interface, and same-frame RGB comparison |

Keep subsequent verified parity fixes in focused commits. Keep temporary shader overrides, benchmark profiles, PNGs, and logs in their local ignored directories.

- `RenderGlobal` submits terrain through `TerrainDrawSubmitter`. The implementations live under `modules/client/src/main/java/net/radiant/render/`.
- OpenGL is the default and draws the full game. Enabling Vulkan terrain opens a second, terrain-only window. It uses the same chunk meshes, atlas updates, lightmap, fog, and chunk transforms. The preview currently requires shader packs, antialiasing, and anisotropic filtering to be off.
- The Vulkan path measures the OpenGL atlas mip transition, uses the legacy matrix stack to capture the final chunk transform, and preserves OpenGL's raster orientation through terrain rendering. A vertical blit puts the presented Vulkan image in window orientation.
- `TerrainComparison` replays the same terrain submissions into an OpenGL target and compares RGB against the Vulkan presentation image at the same dimensions. It logs exact mismatch counts and maximum channel error, and writes paired PNGs under `run/benchmarks/terrain-pair-*`.

## Last verified result

The integrated client ran on a Radeon RX 7900 XT at 854 × 480. All four runs completed with zero Vulkan validation errors. The default OpenGL-only benchmark also completed.

The table below records the initial UNORM surface baseline at `9c37f53c`. The preceding presentation and shared-submission commits also built independently from clean source snapshots using `:client:jar :dev:classes`.

| OpenGL reference | Render regions | Exact frames / sampled frames with terrain | Total differing pixels across samples | Largest RGB channel difference |
| --- | --- | ---: | ---: | ---: |
| Display lists | Off | 30 / 34 | 6 | 1 |
| Display lists | On | 26 / 33 | 9 | 1 |
| VBOs | Off | 30 / 33 | 3 | 1 |
| VBOs | On | 29 / 33 | 6 | 1 |

Full terrain parity is still open. Keep the exact RGB comparison and retain differing pairs for diagnosis. The integrated logs are local artifacts in `build/terrain-parity-integrated-*.log`. Each log identifies its capture directory. These results cover terrain in the sampled scenes on this driver.

The sRGB presentation follow-up uses a compatible UNORM attachment view so translucent terrain blends in Radiant's encoded RGB space. With display lists and regions on, forcing the BGRA8 sRGB surface produced 1,220,887 differing pixels across 33 populated samples before the fix and 5 across 33 afterward. Across all four sRGB configurations after the fix, 119 of 128 populated frames matched exactly. The remaining 9 frames each differed at one pixel by one channel level. The production surface-selection run had 7 such pixels in 32 populated frames. All five runs reported zero validation errors. See `build/terrain-srgb-before.log` and `build/terrain-srgb-after-*.log`.

## Resume terrain work

Start with the first differing PNG pair listed in each integrated log. The fog-off and no-mipmap controls in `build/terrain-rounding-{fogoff,nomip}.log` each still had a differing sample. Three completed shader experiments tested fused fog blending, a rearranged fog factor, and distance from fragment W. All three still had one-level differences in the display-list/render-region run. See `build/resume-terrain-*.log`. The production shader retains its original fog equation. Rebuild without any temporary source override after running a diagnostic.

Run the production build from the repository root.

```powershell
java -jar gradle/wrapper/gradle-wrapper.jar --offline :client:jar :dev:classes
```

Launch the interactive comparison from the same directory.

```powershell
.\gradlew.bat :dev:RunClient "-Pradiant.vulkan.terrain=true" "-Pradiant.vulkan.compareTerrain=true"
```

For the unattended benchmark, use `:dev:RunClientBenchmark` with the same two properties and `-PbenchmarkAutoStart=true -PbenchmarkWarmup=0 -PbenchmarkSeconds=2`. Add `"-Pradiant.vulkan.validation=true"` when running with the validation layer installed. The four existing local reference profiles are `run/terrain-pass-parity-{list,list-regions,vbo,vbo-regions}`. Select each with `-I build/sky-validation.init.gradle` and `-PskyVerificationProfile=terrain-pass-parity-<mode>`.

For background AI testing, pass `-PbenchmarkBackground=true` alongside `-PbenchmarkAutoStart=true`. The benchmark stays windowed, shows its windows without requesting focus, and leaves the desktop mouse free.

Update this checkpoint when a verified result, implementation decision, or user instruction changes the next step.
