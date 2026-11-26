# Block selection outline

At `6bd11fbe`, Radiant supplies the Vulkan pass with the expanded camera-relative bounds from `RenderGlobal.drawSelectionBox`. The preview draws the twelve edges after opaque terrain and before weather. The RX 7900 XT uses two-pixel Bresenham lines with depth testing and 40% black blending. The comparison replays Radiant's OpenGL draw into the same-frame reference. The fallback for devices without native wide Bresenham lines has not been verified.

All 146 populated RGB comparisons in `run-y3ev8nx6` matched at 854 × 480 with zero validation errors across display lists and VBOs, each with render regions off and on. The crosshair hit a quartz slab with bounds (0,193,4) to (1,193.5,5), with sandstone in front of part of its outline. Tilting from 18 to 7 degrees produced a logged miss. The final fixture refreshed the terrain visibility list during warmup. All four modes had visible terrain in both measured phases.

Omitting only the Vulkan outline in `run-ahn52mrz` changed 242 pixels per selected sample, totaling 4,356. At column 420, the exposed edge covered rows 234 and 235. The control recorded zero differences after the crosshair moved off the slab. To reproduce the positive fixture, run `python renderer-port/tools/terrain_parity.py run --warmup 3 --seconds 3 --init-script renderer-port/local/parity/fixtures/block-outline.init.gradle`. Run `python renderer-port/tools/check.py` afterward to restore production classes.

After restoring production classes, the ordinary route in `run-e58ga9r8` matched all 122 populated comparisons with zero differing pixels and zero validation errors.

### Next-session checks

Port the block damage overlay from `LevelRenderer.submitBlockDestroyAnimation` and `RenderGlobal.drawBlockDamageTexture`. Exercise multiple crack stages, check the overlay against the selected block and adjacent terrain, then clear the damage and compare again.
