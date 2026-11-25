"""Decompile the 26.3 rendering reference into the ignored build directory."""

import argparse
from pathlib import Path
import shutil
import subprocess
import tempfile
from zipfile import ZipFile


REFERENCE = Path(__file__).resolve().parents[1] / "build" / "renderer-reference-26.3"
RENDER_PACKAGES = (
    "net/minecraft/client/renderer/",
    "com/mojang/blaze3d/buffers/",
    "com/mojang/blaze3d/framegraph/",
    "com/mojang/blaze3d/pipeline/",
    "com/mojang/blaze3d/platform/",
    "com/mojang/blaze3d/resource/",
    "com/mojang/blaze3d/systems/",
    "com/mojang/blaze3d/vertex/",
    "com/mojang/renderpearl/",
)
RENDER_CLASSES = ("com/mojang/blaze3d/ProjectionType",)


def is_render_class(name):
    if not name.endswith(".class"):
        return False
    outer_class = name.removesuffix(".class").split("$", 1)[0]
    return name.startswith(RENDER_PACKAGES) or outer_class in RENDER_CLASSES


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--decompiler", type=Path, default=REFERENCE / "vineflower-1.12.0.jar",
                        help="path to a Vineflower JAR")
    args = parser.parse_args()
    client = REFERENCE / "26.3.jar"
    for jar in (client, args.decompiler):
        if not jar.is_file():
            parser.error(f"missing {jar}. See docs/rendering/class-map.md for setup.")

    output = REFERENCE / "src"
    log = REFERENCE / "extract.log"
    with tempfile.TemporaryDirectory(dir=REFERENCE) as temporary:
        selected = Path(temporary) / "render-classes.jar"
        staging = Path(temporary) / "src"
        staging.mkdir()
        with ZipFile(client) as source, ZipFile(selected, "w") as target:
            for name in source.namelist():
                if is_render_class(name):
                    target.writestr(name, source.read(name))
        command = ["java", "-jar", str(args.decompiler.resolve()), "--folder", "--log-level=WARN",
                   f"--add-external={client}", str(selected), str(staging)]
        with log.open("wb") as stream:
            subprocess.run(command, stdout=stream, stderr=subprocess.STDOUT, check=True)
        count = sum(1 for _ in staging.rglob("*.java"))
        if not (staging / "net/minecraft/client/renderer/LevelRenderer.java").is_file():
            parser.error(f"LevelRenderer.java was not generated. See {log}.")
        shutil.copytree(staging, output, dirs_exist_ok=True)
    print(f"Extracted {count} Java sources to {output}")


if __name__ == "__main__":
    main()
