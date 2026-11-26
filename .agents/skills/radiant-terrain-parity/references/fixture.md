# Run a diagnostic fixture

For the local glass-panel fixture, refresh its instrumented source copies against current production code before running it. Its Gradle init script replaces classes during compilation. Run it with:

```sh
python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/terrain-panel-fixture.init.gradle --warmup 4 --seconds 6
```

After using a source override, rebuild the production classes from the root and repeat the ordinary comparison. Record populated samples, errors and evidence of the exercised event in the checkpoint. The [measurement history](../../../../renderer-port/docs/terrain-parity-history.md) identifies older fixture logs.

```sh
python renderer-port/tools/check.py
```
