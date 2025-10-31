# Radiant

## What is Radiant?
Radiant is a community-driven project that builds on the decompiled Minecraft source code provided by MCP (Minecraft Coder Pack) for version 1.8.9, combined with OptiFine M6-Pre2.  
It focuses on pre-optimized tweaks, bug and exploit fixes, and quality-of-life improvements to enhance performance, stability, and the overall Minecraft experience.

## Features
- Increased performance
- Increased efficenty 
- Exploit and bug fixes
- QoL (Quality of Life) improvements and features
- Native Image support
- Code Quality Improvments

## Todo
- [ ] Patch all known packet exploits
- [ ] Optimize network stack heavily without sacrificing security (using velocity networking)
- [ ] Optimize LWJGL3 translation layer

## Requirements
- [JDK/JRE: Oracle GraalVM 25(https://www.graalvm.org/downloads/)
- C/C++ Compiler
   - Clang
   - GCC
   - MSVC (Kinda bad but so is anything made by Microsoft)

## Getting Started
To run the client in development, use the `RunClient` Gradle task located under the `GradleMCP` task group in your IDE or via CLI.

## Native Image

### How to compile:
1. Make sure you have a valid C/C++ compiler installed.<br>
   - Clang
   - GCC
   - MSVC (Kinda bad but so is anything made by Microsoft)
2. Run the `RunClientNativeAgent` gradle task, it is recommended that you load a world to make sure the tracing agent doesn't miss anything.
   **ALWAYS DO THIS BEFORE DOING STEP 3!**
3. Run the `nativeCompile` task. This may take a few minutes (depending on your computing power).

### How to run the executable:
The executable will be created in `build/native/nativeCompile`, it can be run with `Minecraft.exe --gameDir <game directory> --accessToken 0 --userProperties {}`.
The game directory will be different depending on your operating system (example: `%APPDATA%/.minecraft` on Windows).

## License
For legal info and disclaimers, see the [NOTICE](./NOTICE.md) file.  
The full license is in the [LICENSE](./LICENSE) file.

## Changelog
The [CHANGELOG.md](./CHANGELOG.md) file contains major and important changes.  
For a full, detailed history of every change, see the project's commit history in the repository.
