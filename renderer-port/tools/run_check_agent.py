"""Start one ephemeral local Codex runner for the checked-out commit."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]


def main():
    commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    config = "model_instructions_file=" + json.dumps((ROOT / ".agents/runner.md").as_posix())
    executable = shutil.which("codex")
    if not executable:
        print("Codex executable missing from PATH", file=sys.stderr)
        return 1
    command = [executable, "exec", "--ephemeral", "--model", "gpt-6-luna",
               "-c", "model_reasoning_effort=low", "-c", config,
               "--sandbox", "workspace-write", "-c", "approval_policy=never",
               "-c", "shell_environment_policy.set.RADIANT_CHECK_NO_DAEMON='1'"]
    if sys.platform == "win32":
        command.extend(["-c", "features.prefer_mxc=true"])
    gradle_home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
    command.extend(["--add-dir", str(gradle_home.resolve()), commit])
    with tempfile.TemporaryFile(mode="w+b") as errors:
        try:
            result = subprocess.run(command, cwd=ROOT, stdout=subprocess.PIPE, stderr=errors)
        except OSError as error:
            print(error, file=sys.stderr)
            return 1
        if result.returncode:
            errors.seek(0)
            sys.stderr.buffer.writelines(errors.readlines()[-10:])
            return result.returncode
    sys.stdout.buffer.write(result.stdout)
    try:
        summary = json.loads(result.stdout)
        return 0 if summary["commit"] == commit and summary["status"] == "PASS" else 1
    except (KeyError, ValueError, TypeError):
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
