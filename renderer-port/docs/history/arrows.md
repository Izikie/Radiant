# Arrows and pickups

At `17eea5b9`, `RenderArrow` submits the two end faces and four shaft faces with their current poses, texture, normals and lightmap coordinates. Vulkan preserves their OpenGL order for world arrows and for arrow pickups rendered by `EntityPickupFX`. Burning arrows use the shared flame submissions.

All 149 populated frames in `run-viso4g70` matched with zero differing RGB pixels and zero Vulkan validation errors across display lists and VBOs with render regions off and on. The fixture exercised distinct yaw and pitch, dim and bright lighting, shake animation, an arrow behind a sandstone post, a burning arrow and an isolated pickup. The two shake crops showed three to four distinct samples in every mode. The isolated pickup changed 637 to 675 pixels in its crop compared with the expired phase.

Reproduce the fixture in this checkout with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/arrows.init.gradle --warmup 1 --seconds 2`.

After restoring production classes with `python renderer-port/tools/check.py`, run `run-mu41711x` matched all 122 populated frames with zero validation errors.

### Next-session checks

Port the layer 3 explosion billboards from `EntityLargeExplodeFX` and compare their animation, tint, lightmap and depth through expiry. Footsteps, mob appearance and other entity renderers still need parity.
