# Radiant renderer port

## Objective

Port Minecraft 26.3's renderer into Radiant's Minecraft 1.8.9 MCP client. Use 26.3 for architecture and renderer behavior. Radiant 1.8.9 OpenGL is the pixel reference. Defer optimizations until parity is established and the user requests them.

Keep dual rendering. OpenGL presents the full game in the main window. Add each ported pass to the Vulkan comparison window and compare the same frame. Switch the main window to Vulkan only after the complete frame has verified 1:1 parity. Preserve OpenGL display lists and VBOs with render regions both on and off. Both backends consume the same ordered section submissions, including translucent order.

## Resume on modern-renderer

Next, port the world border using 26.3's `WorldBorderRenderer.prepare` / `render` and Radiant's `RenderGlobal.renderWorldBorder`. Submit it after precipitation and before translucent terrain.
At `975895a7`, rain and snow matched same-frame Radiant OpenGL across all four configurations. The production build is restored. See [the parity history and next-session checks](renderer-port/docs/terrain-parity-history.md#rain-and-snow).
Verify the border's fade, scrolling texture and blending at a face and corner, then move outside its visible range.

## Where to look

- Under `modules/client/src/main/java/`, start with `net/minecraft/client/renderer/RenderGlobal.java` and `net/radiant/render/terrain/TerrainDrawSubmitter.java`.
- Backends live in `net/radiant/render/{opengl,vulkan}/*`.
- Never read `Shaders.java`, `RenderGlobal.java` or `EntityRenderer.java` whole. Find the symbol with `rg -n`, then read that range with an offset and limit.
- Skip `net/optifine` and `net/minecraft/world/gen/structure` unless the task names them.
- Run Gradle with `-q --console=plain`, capture output to a file and search it for errors or FAILED lines. Never paste full logs.

## Working on terrain

- Use the [terrain parity skill](.agents/skills/radiant-terrain-parity/SKILL.md) for background runs, fixtures, pixel differences and production rebuilds.
- The Vulkan preview covers overworld sky, fast and fancy clouds, precipitation, and terrain with shader packs, antialiasing and anisotropic filtering off.
- Search the extracted 26.3 render sources in `renderer-port/local/reference/src/` with `rg --no-ignore-vcs -n` so `.ignore` still excludes assets. See [the class map and extraction setup](renderer-port/docs/class-map.md).
- Use the older [vk-blaze3d port](https://github.com/Izikie/Radiant/tree/vk-blaze3d) for ideas only. Its implementation may be outdated or inaccurate. Check ideas against `renderer-port/local/reference/src/` and Radiant OpenGL output before using them.
- Commit verified parity fixes separately. Keep fixture overrides, generated profiles, screenshots and logs in ignored build or run directories.
- When a verified result or user decision changes the next step, update only the next step, commit under test and remaining gap in the resume block.
- Read only the history index and the relevant pass file. Append results to that file and update its index entry.
