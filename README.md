# Radiant

## What is Radiant?

Radiant is a community-driven project that builds on the decompiled Minecraft source code provided by MCP (Minecraft
Coder Pack) for version 1.8.9, combined with OptiFine M6-Pre2.
It focuses on pre-optimized tweaks, bug and exploit fixes, and quality-of-life improvements to enhance performance,
stability, and the overall Minecraft experience.

## Features

- Increased performance
- Increased efficiency
- Exploit and bug fixes
- QoL (Quality of Life) improvements and features
- Native Image support (WIP)
- Code Quality Improvements (Follow Conventions)

## Todo

- [ ] Patch all known packet exploits
- [ ] Optimize network stack heavily without sacrificing security (using tricks from Velocity / no reflection for
  packets)
- [ ] Optimize and clean up LWJGL3 translation layer
- [ ] One-click auto native compile task
- [ ] Remove reflection usage for structure stuff
- [ ] Optimize OpenGL rendering

## Requirements

- [JDK: Oracle GraalVM 25](https://www.graalvm.org/downloads/)
- Python 3.6+
- C/C++ Compiler:
    - Clang
    - GCC
    - MSVC (Kinda bad but so is anything made by Microsoft)

## Getting Started

To run the client in development, use the `RunClient` Gradle task located in the `Dev` module under the `RadiantMCP`
task group in your IDE or via CLI.

## Performance benchmarks

Run `:dev:RunClientBenchmark` to start an instrumented client. On Windows:

```powershell
.\gradlew.bat :dev:RunClientBenchmark -PbenchmarkScenario=terrain -PbenchmarkWarmup=5 -PbenchmarkSeconds=30
```

Click **Benchmark Suite** on the main menu to generate a fresh fixed-seed world and run the same route every time.
The suite teleports to previously unseen terrain, pans across a block gallery, pans across named mobs, paintings,
and block entities with partial occlusion, renders particle emitters, then cycles the creative inventory open and
closed. Leave the window focused and avoid input during the route. The terrain phase warms up at a fixed staging
point and measures the teleport and chunk streaming. Other phases wait for arrival before warming up.
The suite controls player movement and camera input while the route runs.

Suite captures default to 10 seconds per phase and a 3-second warmup. Set `-PbenchmarkAutoStart=true` to start
the suite from the command line and close the client after it writes the report. For a quick smoke run, use
`-PbenchmarkAutoStart=true -PbenchmarkWarmup=0 -PbenchmarkSeconds=2`.
Add `-PbenchmarkBackground=true` for unattended desktop testing. The client opens windowed without taking focus and leaves the mouse cursor free throughout the run.
The unattended run continues if the window loses focus and records unfocused frames so you can identify
captures affected by background rendering.
The report is a single `run/benchmarks/suite-*.json` with metrics for each completed phase and a completion flag.
F8 cancels the suite and writes any completed phases. Each suite creates a new `benchmark-*` save under
`run/benchmark-game/saves`, which you can remove after comparing runs.

The task uses `run/benchmark-game` for worlds, options, and packs and reads shared assets from your regular
Minecraft installation. To benchmark your own world, put a copy in `run/benchmark-game/saves` and keep the same
settings across runs. Press **F8** at your starting position. The client log marks the beginning
and end of the capture. Press F8 again to cancel. Opening a screen, changing worlds, losing focus, or resizing
during the capture discards the session.

Run each scenario several times from the same save and camera position. For a terrain baseline, hold still
and face the same scene. For chunk loading, follow the same route at the same speed once the warmup ends.
Use a separate stationary scene with many entities or particles to measure dynamic rendering. Keep the resource
pack, shaders, resolution, VSync, and frame cap fixed. Restore the save between runs if the scene changes over time.

Each completed capture writes a JSON report under `run/benchmarks`. It includes frame time percentiles, FPS,
Java allocation rate on the client thread when supported, and integrated-server tick time when using single-player.
It also includes aggregate elapsed chunk rebuild and layer upload times, GL draw submissions, and JVM startup to
the first presented main-menu frame. Draw submissions count instrumented OpenGL calls, including one per multi-draw
or display-list call and calls made during list compilation. They reflect Java-side rendering work. Chunk timings
can overlap across workers, and upload timings measure CPU submission time. Record the commit SHA alongside each
report when comparing revisions.

## Native Image

### How to compile:

1. Make sure you have a valid C/C++ compiler installed.<br>
2. Run the `RunClientNativeAgent` gradle task, it is recommended that you load a world to make sure the tracing agent
   doesn't miss anything.
   **ALWAYS DO THIS BEFORE DOING STEP 3!**
3. Run the compile script located in the `nativeimage` folder, depending on your OS. Compilation time depends on your
   system.

### How to run the executable

Native image compilation isn’t fully supported yet, so you can’t currently build a working native executable.

## License

For legal info and disclaimers, see the [NOTICE](./NOTICE.md) file.  
Full license details are in the [LICENSE](./LICENSE) file.

## Changelog

Major changes are documented in [CHANGELOG.md](./CHANGELOG.md).  
For a complete history of all changes, check the project's commit history in the repository.
