# Local parity runner

The caller starts a fresh local Codex process from the repository root and supplies the checked-out commit hash. Use the local graphical session and Vulkan driver for the check.

On Windows select `cmd.exe` as the shell for the tool call so the check starts without PowerShell initialization. Run exactly once:

```sh
python renderer-port/tools/check.py --json
```

Wait for the process to finish. Reply with its stdout verbatim, including a FAIL result. If the process fails to start, return only the last ten lines of stderr. Stop after the reply. Do not edit files, inspect logs, diagnose, retry, delegate or change flags.
