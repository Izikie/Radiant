# Overworld sky background

At `dde18013` Vulkan clears to the world fog color and draws the upper sky and lower horizon before terrain. The reference copies the same frame's OpenGL clear, then replays Radiant's sky planes and ordered terrain layers. Every RGB pixel enters the comparison, including frames with only sky. A frame counts as terrain-populated when terrain changes the background color of at least one pixel.

On the RX 7900 XT at 854 × 480, all 117 populated ordinary-route samples and all 110 populated sunset and night pitch-sweep samples matched exactly across display lists and VBOs with regions on and off. The runs reported zero differing pixels and zero Vulkan validation errors. Under `renderer-port/local/parity/runs/`, see `run-01f_vcsq` for the sweep, `run-jy4n3deb` for both display-list results, and `run-q84t5k_s` and `run-nj2el1l1` for the completed VBO runs.

Omitting the Vulkan upper plane produced 3,764,548 differing pixels (`run-dbjb3opv`). Near sea level, a fixture compared the sky before terrain could obscure the lower plane. Omitting the lower plane produced 10,177,803 differing pixels (`run-ss98twud`), and restoring it gave an exact match (`run-l_is9wn_`).

With VBOs and render regions on, the transition fixture installed the 64-pixel stone-brick texture, hid the preview at the menu with zero retained sections, and reopened the same save in a distinct client world. All 40 populated samples matched, with zero differing pixels and zero Vulkan validation errors (`run-92qukdhr`). Twilight gradients, celestial bodies and clouds remain outside this comparison.

