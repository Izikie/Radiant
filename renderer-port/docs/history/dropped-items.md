# Dropped items and pickup particles

At `ef20df06`, `RenderEntityItem` submits baked item draws with their resolved pose, tinted quads, lightmap coordinates and enchantment glint. Vulkan executes dropped-item submissions after opaque terrain and item pickups after block damage, using the same depth buffer for terrain occlusion. The comparison framebuffer replays the OpenGL model draw at its original point in the frame.

All 203 populated frames in `run-hizppayw` matched with zero differing RGB pixels and zero Vulkan validation errors across display lists and VBOs with render regions off and on. The fixture compared flat and 3D stacks at bright and dim light levels and placed a lapis stack behind an opaque sandstone post. The second phase added overlapping stacks, an enchanted sword and a glass pickup through `EntityPickupFX`. In the fixture area, the first group changed 4,501 to 4,542 OpenGL pixels and the second phase changed another 1,073 to 2,158. Removing the items and expiring the pickup restored the baseline in that area in all four modes.

Reproduce the fixture in this checkout with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/dropped-items.init.gradle --warmup 2 --seconds 2`.

After restoring production classes with `python renderer-port/tools/check.py`, run `run-w5fzxnz_` matched all 121 populated frames with zero validation errors.

### Next-session checks

Port built-in dropped-item models through `TileEntityItemStackRenderer`. Compare textures, lighting and opaque terrain occlusion for chest, banner and skull items. Entity shadows, fire and layer 3 pickups that use other entity renderers still need parity.
