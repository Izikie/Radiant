# Fancy overworld clouds

At `b9825147` Vulkan draws the cached cloud faces with their legacy UVs and face colors, first with color writes disabled and then with blending enabled for the visible color draw. Both draws use the same alpha test, transform and geometry as the Radiant display list. Radiant rebuilds fancy clouds after drawing the old list, so Vulkan uses the replacement mesh starting on the following frame.

On the RX 7900 XT at 854 × 480, all 283 samples matched across display lists and VBOs with render regions off and on. The fixture positioned the cloud layer below, around and above the camera, switched among fancy, fast and disabled clouds, and sampled 68 first frames after fancy mesh rebuilds. Omitting only the depth draw produced 701,000 differing pixels, including 247,681 inside the layer. Omitting both fancy draws produced 2,136,238 differing pixels.

A VBO render-regions run reloaded a 128-pixel-wide, linearly filtered cloud texture with partial alpha. All 61 samples matched. After rebuilding production classes, all 175 ordinary-route samples matched in the four configurations. All nine positive runs completed with zero Vulkan validation errors.

Local logs under `renderer-port/local/parity/runs/` are `run-rfil0usk` (four-mode sweep), `run-mh8s9p_8` (depth omitted), `run-6br42dej` (both draws omitted), `run-hdhi_8ak` (texture reload) and `run-etf4uy3b` (production). Repeat the sweep with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/fancy-clouds.init.gradle --seconds 8`.

