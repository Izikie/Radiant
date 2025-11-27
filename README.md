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
