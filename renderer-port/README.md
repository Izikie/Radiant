# Minecraft 26.3 renderer port

| Path | Contents |
| --- | --- |
| [docs/](docs/) | Class map, reference setup, and terrain parity history |
| [tools/](tools/) | Reference extraction and terrain parity commands |
| `local/reference/jars/` | Local client and decompiler JARs |
| `local/reference/src/` | Extracted render sources |
| `local/parity/fixtures/` | Local Gradle overrides and fixture inputs |
| `local/parity/runs/` | Benchmark profiles and logs |
| `local/parity/archive/` | Earlier logs, probes, and diagnostic scripts |

Run these commands from the repository root.

```powershell
python renderer-port/tools/extract_reference.py
python renderer-port/tools/terrain_parity.py run
```

Git ignores `local/`. The client writes paired screenshots to `run/benchmarks/`. Archived logs retain their original capture paths, and archived scripts may contain paths from earlier runs.
