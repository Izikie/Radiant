# Changes

<details>
  <summary>Bug & Exploit Fixes</summary>

- **Fix Resource Pack Exploit** — Patched directory traversal exploit.
- **Fix Demo Troll** — Ignored demo screen requests from servers.
- **Fix Rain Strength** — Fixed crashes and lag caused by extreme rain values. Also fixed brightness issues.
- **Fix Alex Arm** — Corrected item rendering position for Alex model.
- **Fix Void Box Rendering** — Removed visual artifact when falling in void.
- **Fix Perspective Shader Reset** — Fixed shader reset when change perspective.
</details>

<details>
  <summary>Improvements & Features</summary>

- **Better F3 Debug Screen** — Made F3 display closer to 1.7 style with less clutter.
- **Faster Language Selection** — Optimized language selection to reload only language data.
- **Case-Insensitive Commands** — Commands now ignore case for easier use.
- **1.7 Potion Positioning** — Prevents inventory offset when potion effects are active.
- **LWJGL3 Translation Layer** — Switched to an optimized LWJGL3 translation layer.
</details>

<details>
  <summary>Performance & Cleanup</summary>

- **Removed Timer Thread Fix** — Dropped legacy timer workaround for older Java versions.
- **Removed Reflectors** — Cleaned out unneeded Forge reflector code.
- **Removed Profiler** — Removed built-in profiling; use external profiler instead.
- **Removed Demo** — Enforces ownership; users must own the game.
- **Removed Twitch Integration** — Removed Twitch support.
- **Removed Realms Integration** — Removed Realms support.
- **Removed Touch Support** — Removed touchscreen support.
- **Removed Snooper/Telemetry** — Removed telemetry data collection.
- **Removed Anaglyph Mode** — Removed the old red/blue 3D stereoscopic rendering.
- **Switched to SLF4J** — Improved logging with SLF4J framework.
- **Switched to JOML** — Replaced math library with JOML for better performance.
- **Use FastUtil** — Replaced collections with FastUtil for faster performance.
</details>
