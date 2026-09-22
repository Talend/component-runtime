# Module knowledge — component-runtime-manager

## JSON-B provider resolution

### `createPojoJsonbBuilder`'s Johnzon-specific reflection breaks when another JSON-B provider wins ServiceLoader resolution

[2026-09-22 | QTDI-3358] `DefaultServiceProvider.createPojoJsonbBuilder` calls plain
`javax.json.bind.JsonbBuilder.newBuilder()` and then reflectively reaches into a private
Johnzon-only `MapperBuilder builder` field (via `getDeclaredField("builder")`) to force
`setDoCloseOnStreams(true)`. This silently assumes Johnzon's `JohnzonBuilder` is always the
`JsonbProvider` that `ServiceLoader` resolves. That assumption breaks whenever another module on
the same classpath re-pins the JSON-B provider stack — e.g. `component-server`'s
`dependencyManagement` moving `johnzon-core`/`johnzon-mapper` to the jakarta line while still
needing a javax-line `javax.json.bind.spi.JsonbProvider` (QTDI-3358's jakarta migration pulled in
Yasson + `org.glassfish:javax.json` for that leftover javax lookup). Yasson's builder impl has no
`builder` field, so the reflection throws `NoSuchFieldException`, previously rethrown as a fatal
`IllegalStateException` and crashing any consumer that reaches this code path with a non-Johnzon
provider on the classpath. **Fix applied**: guard the reflective block behind
`jsonbBuilder instanceof org.apache.johnzon.jsonb.JohnzonBuilder` and skip the
`doCloseOnStreams` optimization (with a debug log) when a different provider is resolved, instead
of treating it as an error. Any future change to this method — or to a module's JSON-B
dependency wiring — should re-check which provider actually wins `ServiceLoader` resolution on
that module's classpath before assuming Johnzon-specific internals are reachable.
