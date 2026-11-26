---
name: radiant-terrain-parity
description: Run and diagnose same-frame Vulkan terrain comparisons against Radiant 1.8.9 OpenGL, including display lists, VBOs, render regions and transient chunk or resource changes.
---

# Compare terrain in Radiant

Read the resume section of the repository's AGENTS.md for the current pass and gap. Run commands below from the repository root.

## Run the comparison

```sh
python renderer-port/tools/check.py
```

The check builds production classes offline and runs all four modes with fresh game directories, framebuffers enabled, and shader, AA and AF defaults. Its summary names the run directory or failing log under `renderer-port/local/parity/runs/`. The client writes paired captures under `run/benchmarks/terrain-pair-*`. Both backends need a local graphical session, cached Gradle dependencies, Minecraft assets and a Vulkan driver.

For a single-mode rerun use the command below. Other modes are `list`, `vbo` and `vbo-regions`. Validation is on by default. Record any use of `--no-validation` with the result.

```sh
python renderer-port/tools/terrain_parity.py run --mode list-regions
```

For an interactive comparison, run the wrapper JAR. The equivalent wrapper scripts are `./gradlew` on macOS/Linux and `gradlew.bat` on Windows.

```sh
java -jar gradle/wrapper/gradle-wrapper.jar :dev:RunClient "-Pradiant.vulkan.terrain=true" "-Pradiant.vulkan.compareTerrain=true"
```

For a custom benchmark launch, `-PbenchmarkGameDir=<path>` selects an isolated game directory. The ordinary benchmark defaults to `run/benchmark-game`.

## Locate a difference

```sh
python renderer-port/tools/terrain_parity.py summarize renderer-port/local/parity/archive/logs/terrain-panel-before-vbo.log
```

Read only the final summary from `run` or `summarize`. Never tail whole logs. Use `--json` for structured results, including maximum channel error. If a failure needs diagnosis, search the named log for the specific error or sample number and compare matching PNGs in the reported capture directory.

For a transient discrepancy, sample the first frames around the event and log evidence it actually happened, such as a completed transparency re-sort or an atlas reload. Keep the same ordered submissions, camera and dimensions in both renders. The comparison reads the Vulkan image and a same-frame OpenGL reference containing the world clear, sky planes and terrain. A populated frame has at least one terrain pixel that differs from the sky background. Sky-only differences also fail the run. The Vulkan renderer captures the legacy chunk transform and uses a vertical presentation blit.

See [the diagnostic fixture instructions](references/fixture.md) when reproducing a specific terrain transition.
