# Radiant renderer port

## Objective

Port Minecraft 26.3's renderer into Radiant's Minecraft 1.8.9 MCP client. Use 26.3 for architecture and renderer behavior. Radiant 1.8.9 OpenGL is the pixel reference. Defer optimizations until parity is established and the user requests them.

Keep dual rendering. OpenGL presents the full game in the main window. Add each ported pass to the Vulkan comparison window and compare the same frame. Switch the main window to Vulkan only after the complete frame has verified 1:1 parity. Preserve OpenGL display lists and VBOs with render regions both on and off. Both backends consume the same ordered section submissions, including translucent order.

## Resume on modern-renderer

Next, port rain and snow using 26.3's `WeatherEffectRenderer` and Radiant's `EntityRenderer.renderRainSnow`. Compare the precipitation columns against OpenGL before translucent terrain, including snow beside rain and under cover.
At `b9825147`, fancy-cloud depth and color draws matched Radiant across all four terrain configurations, including the first frames after mesh rebuilds. See [the parity history](renderer-port/docs/terrain-parity-history.md).
Precipitation is still missing from the Vulkan comparison window.

## Where to look

- Under `modules/client/src/main/java/`, start with `net/minecraft/client/renderer/RenderGlobal.java` and `net/radiant/render/terrain/TerrainDrawSubmitter.java`.
- Backends live in `net/radiant/render/{opengl,vulkan}/*`.
- Never read `Shaders.java`, `RenderGlobal.java` or `EntityRenderer.java` whole. Find the symbol with `rg -n`, then read that range with an offset and limit.
- Skip `net/optifine` and `net/minecraft/world/gen/structure` unless the task names them.
- Run Gradle with `-q --console=plain`, redirect output to a file, and use `rg -i 'error|FAILED' <log>`. Never paste full logs.

## Working on terrain

- Use the [terrain parity skill](.agents/skills/radiant-terrain-parity/SKILL.md) for background runs, fixtures, pixel differences and production rebuilds. From the root, `python renderer-port/tools/terrain_parity.py run` runs the four-mode comparison.
- The Vulkan preview covers overworld sky, fast and fancy clouds, and terrain with shader packs, antialiasing and anisotropic filtering off.
- Search the extracted 26.3 render sources in `renderer-port/local/reference/src/` with `rg --no-ignore-vcs -n` so `.ignore` still excludes assets. See [the class map and extraction setup](renderer-port/docs/class-map.md).
- Use vk-blaze3d for implementation details when helpful. Keep pass structure aligned with 26.3 and verify the image against Radiant OpenGL.
- Commit verified parity fixes separately. Keep fixture overrides, generated profiles, screenshots and logs in ignored build or run directories.
- When a verified result or user decision changes the next step, replace the resume paragraph and record the commit, coverage and remaining gap. Keep older evidence in the history so this file stays short.
