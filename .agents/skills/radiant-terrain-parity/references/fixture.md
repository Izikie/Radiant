# Run a diagnostic fixture

For the local glass-panel fixture, refresh its instrumented source copies against current production code before running it. Its Gradle init script replaces classes during compilation. Run it with:

```powershell
python renderer-port/tools/terrain_parity.py run --init-script renderer-port/local/parity/fixtures/terrain-panel-fixture.init.gradle --warmup 4 --seconds 6
```

After using a source override, rebuild the production classes from the root and repeat the ordinary comparison. Record populated samples, errors and evidence of the exercised event in the checkpoint. The [measurement history](../../../../renderer-port/docs/terrain-parity-history.md) identifies older fixture logs.

```powershell
java -jar gradle/wrapper/gradle-wrapper.jar --offline -q --console=plain :client:jar :dev:classes *> renderer-port/local/parity/terrain-production.log
if ($LASTEXITCODE) { rg -i "error|FAILED" renderer-port/local/parity/terrain-production.log; exit $LASTEXITCODE }
python renderer-port/tools/terrain_parity.py run
```
