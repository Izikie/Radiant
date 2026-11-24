---
name: radiant-terrain-parity
description: Run and diagnose same-frame Vulkan terrain comparisons against Radiant 1.8.9 OpenGL, including display lists, VBOs, render regions and transient chunk or resource changes.
---

# Compare terrain in Radiant

Read the resume section of the repository's AGENTS.md for the current pass and gap. Run commands below from the repository root.

## Run the comparison

```powershell
python tools/terrain_parity.py run
```

The runner calls the offline Gradle wrapper with the benchmark in background mode. OpenGL and Vulkan both need a graphical session. The four modes use fresh game directories with framebuffers enabled, and shader, AA and AF defaults. The runner prints the log path for each mode under `build/terrain-parity/run-*`. The client writes paired captures under `run/benchmarks/terrain-pair-*`. Gradle dependencies, Minecraft assets and a Vulkan driver must already be available locally.

Use `--mode list-regions` (or `list`, `vbo`, `vbo-regions`) to reproduce one configuration. Validation is on by default. When running with `--no-validation`, record that choice with the result. Log summaries report the client's error counter, which also reads zero when validation is disabled.

For an interactive comparison, launch `.\gradlew.bat :dev:RunClient "-Pradiant.vulkan.terrain=true" "-Pradiant.vulkan.compareTerrain=true"`. For a custom benchmark launch, `-PbenchmarkGameDir=<path>` selects an isolated game directory. The ordinary benchmark defaults to `run/benchmark-game`.

## Locate a difference

```powershell
python tools/terrain_parity.py summarize build/terrain-panel-before-vbo.log
```

Supply one complete run per log. The summary reports exact/populated frames, total differing pixels, maximum channel error and the first differing sample. A failed build, incomplete suite, missing Vulkan shutdown summary, reported validation error, absence of populated samples or any RGB difference fails the check. Follow the capture directory in the log and compare the matching sample numbers in the OpenGL and Vulkan PNGs.

For a transient discrepancy, sample the first frames around the event and log evidence it actually happened, such as a completed transparency re-sort or an atlas reload. Keep the same ordered submissions, camera and dimensions in both renders. The comparison reads Vulkan's presented image and an OpenGL terrain replay. The Vulkan renderer captures the legacy chunk transform and uses a vertical presentation blit.

## Run a diagnostic fixture

For the local glass-panel fixture, refresh its instrumented source copies against current production code before running it. Its Gradle init script replaces classes during compilation. Run it with:

```powershell
python tools/terrain_parity.py run --init-script build/terrain-panel-fixture.init.gradle --warmup 4 --seconds 6
```

After using a source override, rebuild the production classes from the root and repeat the ordinary comparison. Record populated samples, errors and evidence of the exercised event in the checkpoint. The [measurement history](../../../docs/rendering/terrain-parity-history.md) identifies older fixture logs.

```powershell
java -jar gradle/wrapper/gradle-wrapper.jar --offline :client:jar :dev:classes
python tools/terrain_parity.py run
```
