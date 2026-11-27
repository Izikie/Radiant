# Layer 3 explosion billboards

At `e1199593`, `EntityLargeExplodeFX` submits its textured quad with the resolved texture frame, tint and packed lightmap coordinates. Vulkan draws it in the existing layer 3 submission order with pickup effects, preserving the reference depth test and alpha test.

All 150 populated frames in `run-obbab7mr` matched with zero differing RGB pixels and zero Vulkan validation errors across display lists and VBOs with render regions off and on. The fixture sampled three differently tinted billboards at two ages and let normal particle updates expire them. The sandstone post partly occluded one billboard. The first age changed 3,737 to 4,939 OpenGL pixels in the fixture area, the later age changed 8,525 to 11,075 more pixels relative to the first, and expiry restored the baseline in that area in all four modes.

Reproduce the fixture in this checkout with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/large-explosions.init.gradle --warmup 1 --seconds 2`.

After restoring production classes with `python renderer-port/tools/check.py`, run `run-5t7sn0t7` matched all 121 populated frames with zero validation errors.

### Next-session checks

Port `EntityFootStepFX` and compare ground placement, texture, lighting and alpha fade through expiry. Mob appearance and other entity renderers still need parity.
