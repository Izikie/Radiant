# Experience orbs and pickups

At `3f87954d`, `RenderXPOrb` submits its camera-facing quad with the resolved sprite, animated tint and lightmap coordinates. Vulkan executes it in the same order as the OpenGL reference, including when `EntityPickupFX` renders it during layer 3. Orb shadows and flames reuse the textured shadow and flame submissions.

All 148 populated frames in `run-ejd3emc7` matched with zero differing RGB pixels and zero Vulkan validation errors across display lists and VBOs with render regions off and on. The fixture exercised different XP icon sizes, bright and dim lightmap samples, animated tint on stationary orbs, partial occlusion by a sandstone post, a burning orb and an isolated pickup. The bright orb crops each showed three distinct tint samples in every mode. The isolated pickup changed 393 to 404 pixels in its crop compared with the expired phase.

Reproduce the fixture in this checkout with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/experience-orbs.init.gradle --warmup 1 --seconds 2`.

After restoring production classes with `python renderer-port/tools/check.py`, run `run-uogzm2ia` matched all 120 populated frames with zero validation errors.

### Next-session checks

Port arrows through `RenderArrow`, including pickups through `EntityPickupFX`. Compare the texture, orientation, shake animation and opaque terrain occlusion.
