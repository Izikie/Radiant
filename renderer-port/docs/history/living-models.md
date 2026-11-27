# Base living-entity models

At `6fd23fc7`, `RendererLivingEntity.renderModel` submits the posed model's textured parts in entity draw order. Vulkan uses the same model geometry, current lightmap, camera-relative transforms and alpha test as the OpenGL draw. The visible-to-player invisibility branch uses source-alpha blending with depth writes disabled.

All 242 populated frames in `run-8vi7_ngs` matched same-frame OpenGL with zero differing RGB pixels and zero Vulkan validation errors across display lists and VBOs with render regions off and on. The fixture spawned pigs at different distances, changed their head and leg poses, switched one to the child model, added a flying bat, and made the closest pig translucent. The first visible model captures differed from baseline by 51,694 to 51,822 pixels. Three removal captures showed one-level terrain differences relative to the earlier baseline. Those pixels matched Vulkan in the same frame.

Reproduce the fixture in this checkout with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/living-models.init.gradle --warmup 2 --seconds 2`.

The production rebuild and comparison from `python renderer-port/tools/check.py` matched all 116 populated frames in `run-37617_it` with zero differing RGB pixels and zero Vulkan validation errors.

### Next-session checks

Port the damage and color-multiplier overlay from `RendererLivingEntity.setBrightness` and the textured model draws from `renderLayers`. Compare a hurt entity and recovery, a cutout saddle, and dyed sheep wool. Specialized layer materials and other entity render paths need their own checks.
