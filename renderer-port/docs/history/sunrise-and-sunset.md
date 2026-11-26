# Sunrise and sunset

At `b6fa98c4` the Vulkan sky draws Radiant's 18-vertex twilight fan between the upper and lower sky planes. The comparison replays the OpenGL fan in that same position with its original transform, packed vertex colors and blend factors.

On the RX 7900 XT at 854 × 480, the horizon sweep matched all 53 terrain-populated samples across display lists and VBOs with regions off and on. The fixture fixed the client time at 14,000 ticks for dusk and 22,000 for dawn and rotated the camera through both horizons. Omitting only the Vulkan fan produced 484,145 differing pixels at dusk and 435,696 at dawn. Under `renderer-port/local/parity/runs/`, see `run-ax1lk5do` for the sweep and `run-9rrlb4yc` for the control. The local fixture is `renderer-port/local/parity/fixtures/twilight-angles.init.gradle`.

The ordinary production route matched all 124 populated samples across the same four configurations (`run-b55wnl8u`). Both four-mode suites reported zero differing pixels across all samples and zero Vulkan validation errors.

