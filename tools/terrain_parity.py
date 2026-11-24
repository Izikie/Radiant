"""Run Radiant's same-frame terrain comparisons or inspect saved benchmark logs."""

import argparse
from pathlib import Path
import re
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[1]
MODES = ("list", "list-regions", "vbo", "vbo-regions")
SAMPLE = re.compile(
    r"Terrain comparison (\d+) pixels=\d+ visible=(\d+) mismatched=(\d+) maxChannelError=(\d+)"
)


def summarize(path):
    try:
        data = path.read_bytes()
    except OSError as error:
        print(f"{path}: {error.strerror}")
        return False
    encoding = "utf-16" if data.startswith((b"\xff\xfe", b"\xfe\xff")) else "utf-8"
    log = re.sub(r"\x1b\[[0-9;]*m", "", data.decode(encoding, errors="replace"))
    samples = [tuple(map(int, match)) for match in SAMPLE.findall(log)]
    populated = sum(visible > 0 for _, visible, _, _ in samples)
    exact = sum(visible > 0 and pixels == 0 for _, visible, pixels, _ in samples)
    differing = sum(pixels for _, _, pixels, _ in samples)
    maximum = max((error for _, _, _, error in samples), default=0)
    errors = re.findall(r"Vulkan terrain preview presented \d+ frames and \d+ terrain draws with (\d+) validation errors", log)
    problems = []
    if "BUILD SUCCESSFUL" not in log or "Benchmark suite ended: complete" not in log:
        problems.append("incomplete benchmark")
    if not populated:
        problems.append("no populated comparisons")
    if errors != ["0"]:
        problems.append("missing or nonzero Vulkan error count")
    if differing:
        first = next(number for number, _, pixels, _ in samples if pixels)
        problems.append(f"first difference at sample {first}")
    print(f"{path}: {exact}/{populated} populated frames exact, "
          f"{differing} differing pixels, max RGB error {maximum}; "
          + (", ".join(problems) if problems else "pass"))
    if "Vulkan validation requested: false" in log:
        print("  Vulkan validation was disabled for this run.")
    if differing:
        captures = re.search(r"Terrain captures saved in ([^\r\n]+)", log)
        if captures:
            print(f"  Captures: {captures[1]}")
    return not problems


def run(args):
    output = ROOT / "build" / "terrain-parity"
    output.mkdir(parents=True, exist_ok=True)
    directory = Path(tempfile.mkdtemp(dir=output, prefix="run-"))
    passed = True
    modes = (args.mode,) if args.mode else MODES
    validation = str(not args.no_validation).lower()
    for mode in modes:
        profile = directory / mode
        profile.mkdir()
        vbo = "true" if mode.startswith("vbo") else "false"
        regions = "true" if mode.endswith("-regions") else "false"
        (profile / "options.txt").write_text(f"useVbo:{vbo}\nfboEnable:true\n", encoding="ascii")
        (profile / "optionsof.txt").write_text(f"ofRenderRegions:{regions}\n", encoding="ascii")
        command = ["java", "-jar", "gradle/wrapper/gradle-wrapper.jar", "--offline", "--console=plain"]
        for script in args.init_script:
            command.extend(["-I", str(script.resolve())])
        command.extend([
            ":dev:RunClientBenchmark", f"-PbenchmarkGameDir={profile}",
            "-PbenchmarkAutoStart=true", "-PbenchmarkBackground=true",
            f"-PbenchmarkWarmup={args.warmup}", f"-PbenchmarkSeconds={args.seconds}",
            "-Pradiant.vulkan.terrain=true", "-Pradiant.vulkan.compareTerrain=true",
            f"-Pradiant.vulkan.validation={validation}",
        ])
        path = directory / f"{mode}.log"
        print(f"Running {mode} (validation {'off' if args.no_validation else 'on'}). Log: {path}", flush=True)
        with path.open("wb") as log:
            log.write(f"Vulkan validation requested: {validation}\n".encode("ascii"))
            log.flush()
            result = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
        if not summarize(path) or result.returncode:
            passed = False
        if result.returncode:
            return result.returncode
    return 0 if passed else 1


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    launch = commands.add_parser("run", help="compare all four OpenGL configurations")
    launch.add_argument("--mode", choices=MODES, help="compare a single configuration")
    launch.add_argument("--seconds", type=int, default=2)
    launch.add_argument("--warmup", type=int, default=0)
    launch.add_argument("--no-validation", action="store_true", help="run without the Vulkan validation layer")
    launch.add_argument("--init-script", type=Path, action="append", default=[], help="Gradle fixture override")
    inspect = commands.add_parser("summarize", help="check one completed benchmark per log")
    inspect.add_argument("logs", type=Path, nargs="+")
    args = parser.parse_args()
    if args.command == "summarize":
        results = [summarize(path) for path in args.logs]
        return 0 if all(results) else 1
    if args.seconds < 1 or args.warmup < 0:
        parser.error("seconds must be positive and warmup must be nonnegative")
    return run(args)


if __name__ == "__main__":
    raise SystemExit(main())
