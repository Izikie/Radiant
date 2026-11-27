# Guardian apparition

At `25104d0d`, `MobAppearance` submits the elder guardian's model parts in layer 3 order. The Vulkan draw uses the same animated geometry, camera-relative transforms, texture, lightmap and per-frame opacity as the OpenGL draw. The model uses two-sided rendering with source-alpha blending, alpha testing and depth writes.

All 244 populated frames in `run-9upa6k7v` matched same-frame OpenGL with zero differing RGB pixels and zero Vulkan validation errors across display lists and VBOs with render regions off and on. The fixture held the camera steady and sampled ages 7, 15 and 24, then let the particle expire through normal updates. The OpenGL captures showed the model sweep across the frame and fade. The live particle and renderer counts both reached zero at age 31. The expired capture returned to baseline in three modes. In VBO render regions, one pixel differed by one channel level between the OpenGL baseline and expired captures, and both captures matched their same-frame Vulkan images.

Reproduce the fixture in this checkout with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/mob-appearance.init.gradle --warmup 2 --seconds 2`.

The production rebuild and comparison from `python renderer-port/tools/check.py` matched all 128 populated frames in `run-zuyemu9h` with zero differing RGB pixels and zero Vulkan validation errors.

### Next-session checks

Extend parity to the remaining entity shadows and fire. Compare the shadow against the same supporting blocks and the flame layers through animation and occlusion.
