# Radiant

## What is Radiant?
Radiant is a community-driven project that builds on the decompiled Minecraft source code provided by MCP (Minecraft Coder Pack) for version 1.8.9, combined with OptiFine M6-Pre2.  
It focuses on pre-optimized tweaks, bug and exploit fixes, and quality-of-life improvements to enhance performance, stability, and the overall Minecraft experience.

## Features
- Increased performance
- Lowered/reduced memory usage
- Exploit and bug fixes
- QoL (Quality of Life) improvements and features

## Todo
- [ ] Patch all known packet exploits
- [ ] Optimize network stack heavily without sacrificing security (using velocity networking)
- [ ] Replace Gson with FastJson2 for better speed
- [ ] Update to a native LWJGL3 implementation, dropping the translation layer
- [ ] Make build compatible with GraalVM native image

## Requirements
- [JDK/JRE: GraalVM 23](https://www.graalvm.org/downloads/)

## Getting Started
To run the client in development, use the `RunClient` Gradle task located under the `GradleMCP` task group in your IDE or via CLI:

## Native Image

### How to compile:
1. Make sure you have a valid C/C++ compiler installed.<br>
   - **Windows:** [Microsoft Visual C++](https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist?view=msvc-170)
   - **MacOS:** Clang
   - **Linux:** GCC
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
