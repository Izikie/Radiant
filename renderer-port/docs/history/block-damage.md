# Block damage overlay

At `b6440d4b`, Vulkan draws Radiant's ordered block damage mesh after the selection outline and before particles and weather. The baked model supplies the crack sprite UVs and vertex colors. The pass samples level zero of the shared block atlas, uses Radiant's destination-color/source-color blend and alpha test, and applies the polygon offset before writing depth. Clearing the mesh at the start of each frame removes stale cracks when damage ends.

On the RX 7900 XT at 854 × 480, all 365 populated comparisons in `run-mdc3knxc` matched OpenGL with zero differing RGB pixels and zero Vulkan validation errors. Display lists and VBOs each ran with render regions off and on. A stationary camera selected a quartz slab behind sandstone. The slab progressed through stages 0, 4 and 9, with simultaneous sandstone damage at stages 1 and 6 during the last two phases. The final phase cleared both through `World.sendBlockBreakProgress`.

In the list-regions omission control (`run-flvy429p`), each measured frame differed by 67 pixels at slab stage 0, 1,463 at stage 4 and 9,302 at stage 9. The undamaged baseline and cleared phases both had zero differences.

Removing only the polygon offset in the VBO control (`run-bnaaq5qt`) changed 36,931 pixels while damage was active. Both controls stayed exact during the baseline and cleared phases and reported zero validation errors. After restoring production classes, the ordinary route in `run-hbdtx4c5` matched all 123 populated comparisons with zero validation errors.

To reproduce the fixture in this checkout, run `python renderer-port/tools/terrain_parity.py run --warmup 3 --seconds 3 --init-script renderer-port/local/parity/fixtures/block-damage.init.gradle`. Run `python renderer-port/tools/check.py` afterward to restore production classes.

### Next-session checks

Port the textured quad particle batches in `EffectRenderer.renderParticles`, using 26.3's `QuadParticleFeatureRenderer.prepareGroup` and `executeGroup` as the reference for submission and drawing. Check a particle-sheet sprite and a block-atlas fragment against opaque terrain, their depth-write order, and expiry.
