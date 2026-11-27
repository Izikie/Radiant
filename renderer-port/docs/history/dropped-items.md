# Dropped items and pickup particles

At `ef20df06`, `RenderEntityItem` submits baked item draws with their resolved pose, tinted quads, lightmap coordinates and enchantment glint. Vulkan executes dropped-item submissions after opaque terrain and item pickups after block damage, using the same depth buffer for terrain occlusion. The comparison framebuffer replays the OpenGL model draw at its original point in the frame.

All 203 populated frames in `run-hizppayw` matched with zero differing RGB pixels and zero Vulkan validation errors across display lists and VBOs with render regions off and on. The fixture compared flat and 3D stacks at bright and dim light levels and placed a lapis stack behind an opaque sandstone post. The second phase added overlapping stacks, an enchanted sword and a glass pickup through `EntityPickupFX`. In the fixture area, the first group changed 4,501 to 4,542 OpenGL pixels and the second phase changed another 1,073 to 2,158. Removing the items and expiring the pickup restored the baseline in that area in all four modes.

Reproduce the fixture in this checkout with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/dropped-items.init.gradle --warmup 2 --seconds 2`.

After restoring production classes with `python renderer-port/tools/check.py`, run `run-w5fzxnz_` matched all 121 populated frames with zero validation errors.

## Built-in item models

At `4c49c25d`, stock built-in item renders through `TileEntityItemStackRenderer` submit textured model parts with their resolved transforms, lightmap coordinates and culling state. The OpenGL comparison replays each original display list. Baked and built-in models share the item submission order for dropped items and layer 3 item pickups.

All 148 populated frames in `run-bk77c48o` matched with zero differing RGB pixels and zero Vulkan validation errors. Display lists and VBOs each matched with render regions off and on. The fixture included normal, trapped and ender chests at different light levels, a patterned banner, a player head and a creeper skull pickup. An opaque sandstone post partly occluded the trapped chest. Removing the items and expiring the pickup restored the fixture area to the baseline in all four modes.

Reproduce the fixture in this checkout with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/special-items-visible.init.gradle --warmup 1 --seconds 2`.

After restoring production classes with `python renderer-port/tools/check.py`, run `run-fz628byy` matched all 119 populated frames with zero validation errors.

## Dropped-item shadows

At `174638b0`, `Render.renderShadow` submits its textured shadow quads immediately after each dropped-item or item pickup draw. Vulkan preserves the OpenGL draw order, alpha blend, depth test and disabled depth writes.

All 149 populated frames in `run-bukoi1pa` matched with zero differing RGB pixels and zero Vulkan validation errors. The fixture toggled entity shadows with the same items present, then kept only the pickup and finally removed it. OpenGL darkened 385 pixels near the floor seam, 504 beneath the gold stack, 104 beneath the raised chest and 203 to 207 under the isolated pickup. The area behind the sandstone post showed no darkening.

Reproduce the fixture in this checkout with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/item-shadows.init.gradle --warmup 1 --seconds 2`. In the exploratory run `run-ugfu7okg`, sample 18 differed by one blue level on 61 pixels inside the baked apple model. The shadow fixture uses brick blocks at those locations to isolate the shadow comparison.

After restoring production classes with `python renderer-port/tools/check.py`, run `run-6hkyvs6w` matched all 128 populated frames with zero validation errors.

## Dropped-item fire

At `8c16a9b0`, `Render.renderEntityOnFire` submits the posed flame quads in item draw order. Vulkan uses the animated block atlas and its captured mipmap setting, with alpha testing and depth writes matching OpenGL.

All 148 populated frames in `run-r4cwwvyr` matched with zero differing RGB pixels and zero Vulkan validation errors across display lists and VBOs with render regions off and on. The stationary flame below the gold stack showed three distinct animation crops in every mode. The fixture also showed a flame partly occluded by the sandstone post and a flame on an isolated pickup.

At `891d2c81`, the item pass gained a lightmap fallback for frames without terrain submissions. In `run-59xbsxjh`, VBO sample 6 showed black items while terrain was still loading. The final fixture withheld terrain submissions for one frame with visible items and verified an exact match in every mode.

Reproduce the fixture in this checkout with `python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/item-fire.init.gradle --warmup 1 --seconds 2`.

After restoring production classes with `python renderer-port/tools/check.py`, run `run-qaaciq9q` matched all 125 populated frames with zero validation errors.

### Next-session checks

Port `RenderXPOrb` for experience orbs and their pickups through `EntityPickupFX`. Compare the billboard, animated color, lightmap and terrain occlusion before moving on to arrow pickups and the remaining layer 3 effects.
