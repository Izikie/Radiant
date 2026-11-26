"""Run Radiant's same-frame terrain comparisons or inspect saved benchmark logs."""

import argparse
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile


PORT = Path(__file__).resolve().parents[1]
ROOT = PORT.parent
MODES = ("list", "list-regions", "vbo", "vbo-regions")
GRADLE = ("java", "-jar", "gradle/wrapper/gradle-wrapper.jar", "--offline", "-q", "--console=plain")
SAMPLE = re.compile(
    r"Terrain comparison (\d+) pixels=\d+ visible=(\d+) mismatched=(\d+) maxChannelError=(\d+)"
)


def summarize(path):
    result = dict(log_path=str(path), exact_frames=0, populated_frames=0, differing_pixels=0)
    try:
        data = path.read_bytes()
    except OSError as error:
        return dict(result, status="FAIL", problems=[error.strerror])
    encoding = "utf-16" if data.startswith((b"\xff\xfe", b"\xfe\xff")) else "utf-8"
    log = re.sub(r"\x1b\[[0-9;]*m", "", data.decode(encoding, errors="replace")).replace("\r\n", "\n")
    samples = [tuple(map(int, match)) for match in SAMPLE.findall(log)]
    populated = sum(visible > 0 for _, visible, _, _ in samples)
    exact = sum(visible > 0 and pixels == 0 for _, visible, pixels, _ in samples)
    differing = sum(pixels for _, _, pixels, _ in samples)
    maximum = max((error for _, _, _, error in samples), default=0)
    errors = re.findall(r"Vulkan terrain preview presented \d+ frames and \d+ terrain draws with (\d+) validation errors", log)
    problems = []
    exit_code = re.search(r"^Gradle exit code: (-?\d+)$", log, re.MULTILINE)
    build_ok = exit_code[1] == "0" if exit_code else "BUILD SUCCESSFUL" in log
    if not build_ok or "Benchmark suite ended: complete" not in log:
        problems.append("incomplete benchmark")
    if not populated:
        problems.append("no populated comparisons")
    if errors != ["0"]:
        problems.append("missing or nonzero Vulkan error count")
    if differing:
        first = next(number for number, _, pixels, _ in samples if pixels)
        problems.append(f"first difference at sample {first}")
    captures = re.search(r"Terrain captures saved in ([^\r\n]+)", log) if differing else None
    return dict(result, status="FAIL" if problems else "PASS", exact_frames=exact,
                populated_frames=populated, differing_pixels=differing, max_rgb_error=maximum,
                validation_errors=sum(map(int, errors)) if errors else None,
                validation_enabled={"true": True, "false": False}.get(
                    log.splitlines()[0].removeprefix("Vulkan validation requested: ") if log else ""
                ),
                capture_path=captures[1] if captures else None, problems=problems)


def report(results, as_json):
    passed = all(result["status"] == "PASS" for result in results)
    if as_json:
        print(json.dumps({"status": "PASS" if passed else "FAIL", "results": results}))
    else:
        print("PASS" if passed else "FAIL")
        for result in results:
            details = list(result["problems"])
            if result.get("validation_enabled") is False:
                details.append("validation off")
            if result.get("capture_path"):
                details.append(f"captures: {result['capture_path']}")
            suffix = f" ({', '.join(details)})" if details else ""
            print(f"{result['status']} {result['exact_frames']}/{result['populated_frames']} exact, "
                  f"{result['differing_pixels']} differing pixels | {result['log_path']}{suffix}")
    return 0 if passed else 1


def run_directory():
    output = PORT / "local" / "parity" / "runs"
    output.mkdir(parents=True, exist_ok=True)
    return Path(tempfile.mkdtemp(dir=output, prefix="run-"))


def compare(directory, modes, *, no_validation=False, init_script=(), warmup=0, seconds=2, progress=False):
    results = []
    validation = str(not no_validation).lower()
    for mode in modes:
        profile = directory / mode
        profile.mkdir()
        vbo = "true" if mode.startswith("vbo") else "false"
        regions = "true" if mode.endswith("-regions") else "false"
        (profile / "options.txt").write_text(f"useVbo:{vbo}\nfboEnable:true\n", encoding="ascii")
        (profile / "optionsof.txt").write_text(f"ofRenderRegions:{regions}\n", encoding="ascii")
        command = list(GRADLE)
        for script in init_script:
            command.extend(["-I", str(script.resolve())])
        command.extend([
            ":dev:RunClientBenchmark", f"-PbenchmarkGameDir={profile}",
            "-PbenchmarkAutoStart=true", "-PbenchmarkBackground=true",
            f"-PbenchmarkWarmup={warmup}", f"-PbenchmarkSeconds={seconds}",
            "-Pradiant.vulkan.terrain=true", "-Pradiant.vulkan.compareTerrain=true",
            f"-Pradiant.vulkan.validation={validation}",
        ])
        path = directory / f"{mode}.log"
        if progress:
            print(f"Running {mode}. Log: {path}", file=sys.stderr, flush=True)
        with path.open("wb") as log:
            log.write(f"Vulkan validation requested: {validation}\n".encode("ascii"))
            log.flush()
            try:
                exit_code = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT).returncode
            except OSError as error:
                log.write(f"{error}\n".encode("utf-8"))
                exit_code = 1
        with path.open("ab") as log:
            log.write(f"\nGradle exit code: {exit_code}\n".encode("ascii"))
        results.append(dict(summarize(path), mode=mode))
        if exit_code:
            break
    return results


def run(args):
    modes = (args.mode,) if args.mode else MODES
    results = compare(run_directory(), modes, no_validation=args.no_validation, init_script=args.init_script,
                      warmup=args.warmup, seconds=args.seconds, progress=True)
    return report(results, args.json)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    launch = commands.add_parser("run", help="compare all four OpenGL configurations")
    launch.add_argument("--mode", choices=MODES, help="compare a single configuration")
    launch.add_argument("--seconds", type=int, default=2)
    launch.add_argument("--warmup", type=int, default=0)
    launch.add_argument("--no-validation", action="store_true", help="run without the Vulkan validation layer")
    launch.add_argument("--init-script", type=Path, action="append", default=[], help="Gradle fixture override")
    launch.add_argument("--json", action="store_true", help="write the final summary as JSON")
    inspect = commands.add_parser("summarize", help="check one completed benchmark per log")
    inspect.add_argument("logs", type=Path, nargs="+")
    inspect.add_argument("--json", action="store_true", help="write the summary as JSON")
    args = parser.parse_args()
    if args.command == "summarize":
        return report([summarize(path) for path in args.logs], args.json)
    if args.seconds < 1 or args.warmup < 0:
        parser.error("seconds must be positive and warmup must be nonnegative")
    return run(args)


if __name__ == "__main__":
    raise SystemExit(main())
