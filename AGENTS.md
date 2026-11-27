# Radiant renderer port

## Objective

Port Minecraft 26.3's renderer into Radiant's Minecraft 1.8.9 MCP client. Use 26.3 for architecture and renderer
behavior. Radiant 1.8.9 OpenGL is the pixel reference. Defer optimizations until parity is established and the user
requests them.

Keep dual rendering. OpenGL presents the full game in the main window. Add each ported pass to the Vulkan comparison
window and compare the same frame. Switch the main window to Vulkan only after the complete frame has verified 1:1
parity. Preserve OpenGL display lists and VBOs with render regions both on and off. Both backends consume the same
ordered section submissions, including translucent order.

## Resume on modern-renderer

- **Next step.** Port built-in dropped-item models from `TileEntityItemStackRenderer` and verify their textures,
  lighting and terrain occlusion.
- **Last verified renderer commit.** At `ef20df06`, baked dropped-item models, enchantment glint and baked item
  pickup particles matched same-frame Radiant OpenGL across all four configurations. The production build is restored.
  See [the parity history and next-session checks](renderer-port/docs/history/dropped-items.md).
- **Remaining gap.** Built-in item models, entity shadows and fire, and layer 3 pickups rendered by other entity
  renderers still need parity.

## Where to look

- Under `modules/client/src/main/java/`, start with `net/minecraft/client/renderer/RenderGlobal.java` and
  `net/radiant/render/terrain/TerrainDrawSubmitter.java`.
- Backends live in `net/radiant/render/{opengl,vulkan}/*`.
- Never read `Shaders.java`, `RenderGlobal.java` or `EntityRenderer.java` whole. Find the symbol with `rg -n`, then read
  that range with an offset and limit.
- Skip `net/optifine` and `net/minecraft/world/gen/structure` unless the task names them.
- Run Gradle with `-q --console=plain`, capture output to a file and search it for errors or FAILED lines. Never paste
  full logs.

## Working on terrain

- Use the [terrain parity skill](.agents/skills/radiant-terrain-parity/SKILL.md) for background runs, fixtures, pixel
  differences and production rebuilds.
- The Vulkan preview covers overworld sky, fast and fancy clouds, precipitation, and terrain with shader packs,
  antialiasing and anisotropic filtering off.
- Search the extracted 26.3 render sources in `renderer-port/local/reference/src/` with `rg --no-ignore-vcs -n` so
  `.ignore` still excludes assets. See [the class map and extraction setup](renderer-port/docs/class-map.md).
- Use the older [vk-blaze3d port](https://github.com/Izikie/Radiant/tree/vk-blaze3d) for ideas only. Its implementation
  may be outdated or inaccurate. Check ideas against `renderer-port/local/reference/src/` and Radiant OpenGL output
  before using them.
- Commit verified parity fixes separately. Keep fixture overrides, generated profiles, screenshots and logs in ignored
  build or run directories.
- When a verified result or user decision changes the next step, update only the next step, last verified renderer
  commit and remaining gap in the resume block. Keep the last verified renderer commit through later workflow-only
  commits.
- Read only the history index and the relevant pass file. Append results to that file and update its index entry.

## Orchestration

- Handle tiny edits directly. When spawning a subagent for other work, set its model and reasoning effort explicitly
  from the task's row in [agent routing](renderer-port/agent-routing.md). Never inherit the lead's model. If the spawn
  can't set the model, or the row names the lead's own model, do the work directly.
- Never delegate a Vulkan pass, a parity diagnosis or `check.py`. Delegate only bounded edits with a written brief that
  names files and symbols.
- Start a fresh session without chat history. Supply the goal, commit hash, paths and symbols, done condition and
  expected short result. Reuse a session only for dependent steps in the same port or diagnosis.
- After a build, renderer or parity code change, commit and run `python renderer-port/tools/check.py` directly in the
  current session. Run it in the background when other work can proceed. Read only its summary (at most 10 lines).
- On parity FAIL the lead searches the named log for the specific error or sample and compares matching PNGs.
- For other lower-tier failures or unexpected replies, escalate one tier once. Return the outcome without retrying the
  same tier.
