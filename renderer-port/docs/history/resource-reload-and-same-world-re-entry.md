# Resource reload and same-world re-entry

At `61d0784d` the Vulkan window hides when the client leaves a world. The first frame of a new world can have zero terrain submissions. Vulkan now presents a cleared terrain target for that frame. The integrated server handles simultaneous shutdown requests once (`5daad531`), and the client ignores queued play packets for a disconnected handler (`96cafec5`).

On the RX 7900 XT at 854 × 480, all 159 populated samples and all 124 empty samples matched OpenGL exactly across display lists and VBOs with regions on and off. Each run installed a pack with a 64-pixel stone-brick texture, checked the GLFW window was hidden with zero retained sections at the menu, and reopened the same save with a distinct client world. The runs sampled 64 populated frames immediately after reload and 65 after re-entry. All four reported zero client-thread errors and zero Vulkan validation errors. The fixture briefly holds the camera still and then turns it, since the legacy reference can submit no sections while stationary.

Local logs are under `renderer-port/local/parity/runs/run-0ubfap4y/`. To repeat, run `python renderer-port/local/parity/fixtures/terrain-transition-gen.py` and then `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/terrain-transition-fixture.init.gradle --warmup 2`. Rebuild production classes after the fixture.

After rebuilding without overrides, the ordinary production route matched all 129 populated frames across the same four configurations, with zero Vulkan validation errors. See `renderer-port/local/parity/runs/run-58l_41mb/`.
