# Radiant renderer port

## Objective

Port the rendering engine from Minecraft 26.3 into Radiant's Minecraft 1.8.9 MCP client. Use the installed `%APPDATA%\.minecraft\versions\26.3\26.3.jar` as the source for modern renderer behavior and architecture. Use Radiant 1.8.9 OpenGL as the visual reference. The target is a pixel-identical image. Defer optimizations until parity is established and the user asks for them.

Support both OpenGL and Vulkan. Preserve Radiant's OpenGL display-list and VBO paths, including render regions on and off. For terrain, both backends consume the same ordered section submissions, including the translucent draw order.

Keep dual rendering throughout the port. OpenGL presents the reference frame in the main window. Add each ported pass to the Vulkan comparison window and compare both backends on the same frame. Defer switching the main window to Vulkan until the complete Vulkan frame has verified 1:1 visual parity with Radiant 1.8.9 OpenGL.

## Current checkpoint

The port is on `modern-renderer`. The initial changes are split into three local commits so the OpenGL baseline and the opt-in Vulkan path can be tested independently.

The launcher copy of the 26.3 jar was absent during the underwater verification. A local reference copy is available at `build/renderer-reference-26.3/26.3.jar` (41,483,720 bytes, SHA-1 `e877b6a07acd633fb3bb475002175cec036e7b87`), verified against the client download in Mojang's 26.3 release metadata. Keep the binary in the ignored build directory.

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

The ordinary fog discrepancy at pixel (115, 256) was resolved by computing the planar fog coordinate from the model-view matrix's Z row before projection. Replaying the same 48 air frames with the previous clip-W calculation gave seven differing pixels. Eye-space Z matched every frame. The four production runs on the four-second route matched all 249 populated frames across display lists and VBOs with regions on and off, with zero Vulkan validation errors. See `build/terrain-eye-z-paired-air.log` and `build/terrain-eye-z-{list,list-regions,vbo,vbo-regions}.log`.

The underwater discrepancy occurred on a translucent face crossing the eye plane. Interpolate signed eye-space Z through clipping and take its magnitude in the fragment shader. On the three failing frames, replaying that change removed all 98,724 differing pixels (`build/terrain-water-arrival.log`).

On the RX 7900 XT at 854 × 480, all 265 populated frames on the ordinary route and all 355 populated frames on the liquid fixture matched OpenGL exactly. Both routes used display lists and VBOs with render regions on and off. The fixture sampled the first frames after entering a liquid, covering 203 underwater frames and 52 in lava. All eight runs completed with zero Vulkan validation errors. See `build/terrain-signed-z-{air,liquid}-*.log`.

The additional BGRA8 sRGB liquid run with display lists and regions on matched all 90 populated frames, with zero validation errors (`build/terrain-signed-z-srgb.log`).

During chunk rebuilds, OpenGL replaces each layer's vertices at upload time. The shared submission still selects visible layers from the published compiled section. Vulkan now draws the latest uploaded vertices for each submitted layer, including when a frame falls between solid and translucent uploads. An empty VBO upload clears that layer. An empty display-list upload leaves the section's layer list intact.

The glass-panel fixture moves the camera and edits overlapping terrain across section boundaries. It yields after selected solid uploads to expose the intermediate frames. Before the fix, 62 of 107 populated frames differed across two runs, with a maximum RGB channel error of 202. After the fix, all 216 populated frames across the four OpenGL configurations matched exactly, including runs with 189 split uploads and 51 completed transparency re-sorts in total. All four runs had zero Vulkan validation errors. See `build/terrain-panel-{before,after}-*.log`. To repeat the fixture, use the profile command below with the additional `-I build/terrain-panel-fixture.init.gradle`, four seconds of warmup and six seconds of measurement. Refresh the fixture's instrumented source copies when production code changes, then rebuild production classes without the fixture script.

After rebuilding production classes, the ordinary benchmark route matched all 136 populated frames across the same four configurations, with zero validation errors. See `build/terrain-panel-control-*.log`.

Next, compare the first terrain frames after a resource reload and after leaving and re-entering a world. Check the atlas and section meshes against the same-frame OpenGL reference as terrain becomes visible again.

The RGBA32F probe also exposed a difference with fog disabled. OpenGL wrote the lit color directly, while evaluating the Vulkan fog mix at full visibility changed the blue channel by one floating-point step. The Vulkan shader now skips the mix when fog is disabled. See `build/terrain-float-pixel.log` for the probe and `build/terrain-fog-bypass-*.log` for production comparisons.

All 129 populated fog-off frames in the production runs matched OpenGL exactly across display lists and VBOs, with regions on and off. The enabled-fog control had one differing pixel by one channel level across 33 populated frames. All five runs completed with zero Vulkan validation errors on the RX 7900 XT.

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
