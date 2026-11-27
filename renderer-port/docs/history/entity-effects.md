# Entity shadows and fire

At `3c115be1`, the world entity render pass submits the shadow and fire meshes produced by `Render`. The shared feature draw preserves entity order, block-derived shadow geometry and light tint, flame billboarding and animated atlas UVs, and the depth behavior of each draw.

All 240 populated frames in `run-_scvlhix` matched same-frame OpenGL with zero differing RGB pixels and zero Vulkan validation errors across display lists and VBOs with render regions off and on. The fixture used two adult pigs and a baby pig at different heights and light levels, with a sandstone post in the scene. It toggled shadows, ignited and extinguished the pigs, and removed them. OpenGL changed between 26,664 and 26,705 pixels when enabling shadows, and the extinguished captures matched their shadows-only captures. Burning captures changed as the atlas animated. Two removal captures had 41 one-level differences along a terrain edge relative to the earlier baseline. Those pixels matched Vulkan in the same frame.

Reproduce the fixture in this checkout with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/entity-effects.init.gradle --warmup 2 --seconds 2`.

The production rebuild and comparison from `python renderer-port/tools/check.py` matched all 124 populated frames in `run-xsc8kq16` with zero differing RGB pixels and zero Vulkan validation errors.

### Next-session checks

Port the base living-entity model draw from `RendererLivingEntity.renderModel` and compare posed adult and child geometry, lightmap, and cutouts. Compare damage tint, translucency and render layers before claiming parity for living entities.
