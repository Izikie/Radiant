# Fast overworld clouds

At `c23e1a0e` the Vulkan flat-cloud pass draws the same cached textured quads as Radiant's fast-cloud display list. It captures the display list's camera and wind translation, cloud projection, alpha test and fog. Below the layer it draws before terrain. Above the layer it draws after translucent terrain. The comparison replays the OpenGL display list at the matching point in the frame.

On the RX 7900 XT at 854 × 480, all 183 samples across display lists and VBOs with regions off and on matched exactly, including daytime, dusk, partial rain and clouds disabled. Omitting Vulkan clouds produced 640,273 differing pixels below the layer and 55,944 above. The disabled samples stayed exact. Reversing the Vulkan draw order relative to terrain produced 11,594 differing pixels. The fixture and controls are in `renderer-port/local/parity/runs/run-sj79r7sc`, `run-31iju2ig` and `run-2h5jgrqp`, respectively.

The reload fixture exposed an empty translucent re-sort that queued a pooled buffer with zero vertices and 2,240 bytes from an earlier mesh. At `70bed0d6` the worker queues the upload only when the re-sort produced geometry. A glass-panel run recorded 38 populated re-sorts and matched all 27 samples (`run-1lj3cep8`). The four-mode reload fixture verified a 128-pixel-wide, linearly filtered replacement cloud texture with partial alpha and matched all 185 samples (`run-p0x0dj6t`).

The production route matched all 171 samples across the four modes (`run-8788xo7b`). A separate VBO regression for the shared texture bindings matched all 47 sun and moon samples (`run-qi2wngj6`). All these positive runs finished with zero Vulkan validation errors. Repeat the cloud fixture with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/fast-clouds.init.gradle --seconds 6`.

