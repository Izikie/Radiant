# **Changes**

<details>
  <summary>Bug & Exploit Fixes</summary>

- **Fix Resource Pack Exploit** — Patched a directory traversal exploit.
- **Fix Demo Troll** — Ignored demo screen requests from servers.
- **Fix Rain Strength** — Fixed crashes, lag, and brightness issues caused by extreme rain values.
- **Fix Alex Arm** — Corrected item positioning for the Alex model.
- **Fix Void Box Rendering** — Removed the visual artifact that appeared when falling into the void.
- **Fix Perspective Shader Reset** — Shaders no longer reset when changing perspective.

</details>

<details>
  <summary>Improvements & Features</summary>

- **Better Show FPS** — Render text with a shadow for improved readability.
- **Better F3 Debug Screen** — Redesigned to be closer to 1.7 style with less clutter.
- **Faster Language Selection** — Only reloads language data for quicker switching.
- **Case-Insensitive Commands** — Commands now ignore capitalization.
- **1.7 Potion Positioning** — Inventory no longer shifts when potion effects are active.
- **LWJGL3** — Switched to LWJGL3 via a translation layer.

</details>

<details>
  <summary>Performance & Cleanup</summary>

- **Removed Timer Thread Fix** — Dropped an outdated Java timer workaround.
- **Removed Reflectors** — Cleaned out unnecessary Forge reflector code.
- **Removed Profiler** — Built-in profiler removed; use external profiling tools.
- **Removed Demo** — Demo mode removed; ownership is required to play.
- **Removed Twitch Integration** — Twitch support removed.
- **Removed Realms Integration** — Realms support removed.
- **Removed Touch Support** — Touchscreen input no longer supported.
- **Removed Snooper/Telemetry** — Telemetry/data collection removed.
- **Removed Anaglyph 3D Rendering** — Red/Blue 3D stereoscopic rendering removed.
- **Switched to JOML** — Replaced the vector library with JOML for performance.
- **Use FastUtil** — Replaced standard collections with FastUtil for performance.

</details>
