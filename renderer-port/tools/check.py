"""Build the production client and report same-frame parity for the selected modes."""

import argparse
from itertools import islice
import json
import re
import subprocess

import terrain_parity as parity


def build(path):
    command = [*parity.GRADLE, ":client:jar", ":dev:classes"]
    with path.open("wb") as log:
        try:
            return subprocess.run(command, cwd=parity.ROOT, stdout=log, stderr=subprocess.STDOUT).returncode == 0
        except OSError as error:
            log.write(f"Build error: {error}\n".encode())
            return False


def check(modes, no_validation):
    directory = parity.run_directory()
    log = directory / "build.log"
    commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=parity.ROOT, text=True).strip()
    built = build(log)
    summary = dict(build="OK" if built else "FAIL", status="FAIL", commit=commit, modes=modes,
                   validation_enabled=not no_validation, log_path=str(log))
    if not built:
        with log.open(encoding="utf-8", errors="replace") as stream:
            matches = (line.strip() for line in stream if re.search("error|FAILED", line, re.I))
            summary["errors"] = list(islice(matches, 5))
        return summary

    results = parity.compare(directory, modes, no_validation=no_validation)
    failures = [result for result in results if result["status"] != "PASS"]
    counts = [result.get("validation_errors") for result in results]
    summary.update(status="PASS" if not failures and len(results) == len(modes) else "FAIL",
                   log_path=failures[0]["log_path"] if failures else str(directory),
                   exact_frames=sum(result["exact_frames"] for result in results),
                   populated_frames=sum(result["populated_frames"] for result in results),
                   differing_pixels=sum(result["differing_pixels"] for result in results),
                   max_channel_error=max(result.get("max_rgb_error", 0) for result in results),
                   validation_errors=None if None in counts else sum(counts))
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--mode", choices=parity.MODES)
    parser.add_argument("--no-validation", action="store_true")
    args = parser.parse_args()
    summary = check((args.mode,) if args.mode else parity.MODES, args.no_validation)
    if args.json:
        print(json.dumps(summary))
    else:
        print(f"Build {summary['build']} | {summary['status']} | {summary['commit']}")
        if summary["build"] == "FAIL":
            for error in summary["errors"]:
                print(error)
        else:
            print(f"Modes: {', '.join(summary['modes'])}")
            print(f"Exact/populated frames: {summary['exact_frames']}/{summary['populated_frames']}")
            print(f"Differing pixels: {summary['differing_pixels']} | Max channel error: {summary['max_channel_error']}")
            count = summary["validation_errors"]
            validation = "disabled" if args.no_validation else count if count is not None else "unknown"
            print(f"Validation errors: {validation}")
        print(f"Log: {summary['log_path']}")
    return 0 if summary["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
