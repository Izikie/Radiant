# Layer 3 footprints

At `530fdfb4`, `EntityFootStepFX` submits its ground-aligned textured quad with the resolved world light tint and age-dependent opacity. Vulkan draws it in layer 3 order with the OpenGL alpha test, blend and depth writes.

All 198 populated frames in `run-8e8cf5fo` matched with zero differing RGB pixels and zero Vulkan validation errors across display lists and VBOs with render regions off and on. The fixture sampled three placements with world light brightness of approximately 1.00, 0.62 and 0.50. It compared their initial opacity, their fade after 166 ticks, and their removal through normal updates. All 994 footprint pixels returned to baseline in each mode. Two render-region runs had one-level color changes elsewhere in the cross-frame OpenGL captures. Those pixels matched Vulkan in the same frame.

Reproduce the fixture in this checkout with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/footprints.init.gradle --warmup 2 --seconds 2`.

After restoring production classes with `python renderer-port/tools/check.py`, run `run-x8bhx8cj` matched all 122 populated frames with zero validation errors.

### Next-session checks

Port `MobAppearance` and compare the guardian model, camera-relative transform, translucency and fade through expiry.
