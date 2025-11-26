import sys
import os
import shutil
import subprocess
from pathlib import Path
from typing import List, Union
from enum import Enum

# ===============================
#   ENUMS
# ===============================
class CompileType(Enum):
    DEV = 0
    RELEASE = 2
    UNKNOWN = 69


compile_type = CompileType.UNKNOWN

# ===============================
#   BEAUPY IMPORT
# ===============================
try:
    from beaupy import select
    from rich.console import Console as _Console
except ImportError:
    subprocess.run(
        [sys.executable, "-m", "pip", "install", "--disable-pip-version-check", "-q", "beaupy"],
        check=True
    )
    from beaupy import select
    from rich.console import Console as _Console

console = _Console()
console.clear()

# ===============================
#   CONSTANTS
# ===============================
CURSOR = ">"
CURSOR_STYLE = "bright_cyan"

SRC_DIR = Path("src")
OUT_DIR = Path("out")
NATIVES_DIR = OUT_DIR / "natives"

CLIENT_JAR = SRC_DIR / "client.jar"
CLIENT_SOURCE = Path("../modules/client/build/libs/client-all-1.0.jar")

# ===============================
#   HELPERS
# ===============================
def print_info(msg: str):
    console.print(f"[bold yellow][!][/bold yellow] {msg}")

def print_error(msg: str):
    console.print(f"[bold red][*][/bold red] {msg}")

def select_prompt(question: str, options: List[Union[str, tuple]]) -> int:
    console.print(question + "?")
    return select(
        options,
        cursor=CURSOR,
        cursor_style=CURSOR_STYLE,
        return_index=True,
    )

def run_cmd(cmd: list):
    try:
        subprocess.run(cmd, check=True)
    except subprocess.CalledProcessError as e:
        print_error(f"Command failed: {e}")
        raise

# ===============================
#   FILE OPS
# ===============================
def copy_sources():
    print_info("Copying files...")

    shutil.rmtree(OUT_DIR, ignore_errors=True)
    OUT_DIR.mkdir(exist_ok=True)
    SRC_DIR.mkdir(exist_ok=True)

    if not CLIENT_SOURCE.exists():
        print_error(f"Missing client JAR at: {CLIENT_SOURCE}")
        raise FileNotFoundError(CLIENT_SOURCE)

    shutil.copyfile(CLIENT_SOURCE, CLIENT_JAR)

# ===============================
#   BUILD
# ===============================
def compile_native_image():
    print_info("Compiling native-image...")

    exe = "native-image.cmd" if os.name == "nt" else "native-image"

    opt_map = {
        CompileType.DEV: "-Ob",
        CompileType.RELEASE: "-O3",
    }

    opt_flag = opt_map.get(compile_type, "")

    cmd = [
        exe,
        "-jar", str(CLIENT_JAR),
        "-o", str(OUT_DIR / "Radiant"),
        "--verbose",
        "--no-fallback",
        "--emit", "build-report",
        "--allow-incomplete-classpath",
        "--enable-native-access=ALL-UNNAMED",
        "--enable-url-protocols=http,https",
        "--enable-all-security-services",
        "--color=always",
        opt_flag,
        "-march=x86-64-v2",
        "-Djava.net.preferIPv4Stack=true",
        "-Dio.netty.allocator.maxOrder=9",
        "-Ddnsjava.inetaddressprovider.enable=false",
        "-H:+UnlockExperimentalVMOptions",
        "-H:+SharedArenaSupport"
    ]

    run_cmd(cmd)

# ===============================
#   RELEASE SETUP
# ===============================
def setup_build():
    print_info("Setting up build...")

    natives = {
        # Windows
        "lwjgl.dll", "glfw.dll", "lwjgl_opengl.dll",
        "OpenAL.dll", "lwjgl_stb.dll",

        # macOS
        "lwjgl.dylib", "glfw.dylib", "lwjgl_opengl.dylib",
        "OpenAL.dylib", "lwjgl_stb.dylib",

        # Linux
        "liblwjgl.so", "libglfw.so", "liblwjgl_opengl.so",
        "libopenal.so", "liblwjgl_stb.so",
    }

    NATIVES_DIR.mkdir(parents=True, exist_ok=True)

    shutil.rmtree("logs", ignore_errors=True)

    for n in natives:
        src = Path(f"../run/natives/{n}")
        if src.exists():
            shutil.copy(src, NATIVES_DIR)

    (OUT_DIR / "run.bat").write_text(
        '@ECHO off\n'
        '"%cd%\\Radiant.exe" -Xmx4g -Xms4g -Xmn2g '
        '-XX:MaximumHeapSizePercent=50 -XX:MaximumYoungGenerationSizePercent=30 '
        '-XX:+CollectYoungGenerationSeparately -XX:MaxHeapFree=3072m '
        '"-Djava.library.path=%cd%\\natives"'
    )

    (OUT_DIR / "run.sh").write_text(
        '#!/bin/sh\n'
        '"$PWD/Radiant.exe" -Xmx4g -Xms4g -Xmn2g '
        '-XX:MaximumHeapSizePercent=50 -XX:MaximumYoungGenerationSizePercent=30 '
        '-XX:+CollectYoungGenerationSeparately -XX:MaxHeapFree=3072m '
        '"-Djava.library.path=$PWD/natives"'
    )

# ===============================
#   MAIN
# ===============================
def main():
    global compile_type

    result = select_prompt(
        "Select compile type",
        ["Dev", "Release", "Cancel"]
    )

    if result == 3:
        return

    try:
        compile_type = CompileType(result)
    except ValueError:
        print_error("Invalid compile type selected.")
        return

    copy_sources()
    compile_native_image()
    setup_build()

    print_info("Done!")

if __name__ == "__main__":
    main()
