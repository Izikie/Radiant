# Radiant renderer port

## Objective

Port Minecraft 26.3's renderer into Radiant's Minecraft 1.8.9 MCP client. Use 26.3 for architecture and renderer behavior. Radiant 1.8.9 OpenGL is the pixel reference. Defer optimizations until parity is established and the user requests them.

Keep dual rendering. OpenGL presents the full game in the main window. Add each ported pass to the Vulkan comparison window and compare the same frame. Switch the main window to Vulkan only after the complete frame has verified 1:1 parity. Preserve OpenGL display lists and VBOs with render regions both on and off. Both backends consume the same ordered section submissions, including translucent order.

## Resume on modern-renderer

**Next comparison:** capture the first populated terrain frames after a resource reload and after leaving and re-entering a world. Confirm each transition actually occurred and compare the rebuilt atlas and section meshes as terrain reappears. The ordinary benchmark samples every 47 frames and can miss the transition.

At `7361ee62`, Vulkan picks up each terrain layer when OpenGL uploads it. On the RX 7900 XT at 854 × 480, all 216 populated glass-panel frames and all 136 ordinary route samples matched exactly across the four configurations. The fixture exercised 189 split uploads and 51 completed transparency re-sorts. All eight runs reported zero validation errors. The preceding signed eye-depth fix at `f0db3ae0` passed the ordinary and liquid routes, including underwater and lava. Earlier measurements, diagnostic log names and known driver coverage are in [the parity history](docs/rendering/terrain-parity-history.md).

## Where to look

- Under `modules/client/src/main/java/`, start with `net/minecraft/client/renderer/RenderGlobal.java` and `net/radiant/render/terrain/TerrainDrawSubmitter.java`.
- Backends live in `net/radiant/render/{opengl,vulkan}/*`.
- Never read `Shaders.java`, `RenderGlobal.java` or `EntityRenderer.java` whole. Find the symbol with `rg -n`, then read that range with an offset and limit.
- Skip `net/optifine` and `net/minecraft/world/gen/structure` unless the task names them.
- Run Gradle with `-q --console=plain`, redirect output to a file, and use `rg -i 'error|FAILED' <log>`. Never paste full logs.

## Working on terrain

- Use the [terrain parity skill](.agents/skills/radiant-terrain-parity/SKILL.md) for background runs, fixtures, pixel differences and production rebuilds. From the root, `python tools/terrain_parity.py run` runs the four-mode comparison.
- The Vulkan preview currently covers terrain with shader packs, antialiasing and anisotropic filtering off.
- Read the modern implementation from `%APPDATA%\.minecraft\versions\26.3\26.3.jar`. This checkout also has an ignored reference copy at `build/renderer-reference-26.3/26.3.jar` matching Mojang's client SHA-1 `e877b6a07acd633fb3bb475002175cec036e7b87`.
- Commit verified parity fixes separately. Keep fixture overrides, generated profiles, screenshots and logs in ignored build or run directories.
- When a verified result or user decision changes the next step, replace the resume paragraph and record the commit, coverage and remaining gap. Keep older evidence in the history so this file stays short.
