# Seeded overworld stars

At `bcb460c6` the Vulkan sky uses Radiant's 780 seeded star quads, the celestial transform and the same rain-adjusted brightness as OpenGL. Like 26.3's star pass, the draw uses a vertex buffer and shared quad indices. It runs between the moon and lower horizon draws. The comparison replays the active OpenGL star VBO or display list in the same frame.

On the RX 7900 XT at 854 × 480, all 183 star-fixture samples and all 172 ordinary-route samples matched across the four terrain configurations. The fixture rotated the camera at midnight, dawn and dusk, with clear sky, partial rain, full rain and the stars setting disabled. Omitting only the Vulkan star draw changed 9,034 pixels across the samples. All samples with full rain or stars disabled still matched in that control.

A separate run switched from display lists to VBOs and back during the night. Each switch rebuilt all 3,120 star vertices, and all 46 samples matched. All nine positive runs reported zero differing pixels and zero Vulkan validation errors. The production client was rebuilt after the last fixture.

Local logs under `renderer-port/local/parity/runs/` are `run-m3t8y756` (star fixture), `run-m57aq6h1` (omission control), `run-z6j2xghq` (ordinary route) and `run-onidtdzx` (live switch). Repeat the star fixture with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/stars.init.gradle --seconds 6`.

