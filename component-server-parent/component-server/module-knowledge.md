# Module knowledge — component-server

## Surefire fork isolation

### A `reuseForks=false` fix for one surefire execution does not extend to sibling executions in the same pom

[2026-09-22 | QTDI-3358] `component-server`'s `pom.xml` has two `maven-surefire-plugin` executions
in the `test` phase — `default-test` and `beam-sample-test` — both of which start a
`ComponentManager`/Meecrowave container keyed by a fixed test container id
(`the-test-component`). Round 0 of this ticket's jakarta migration added
`<reuseForks>false</reuseForks>` to `default-test` to stop a `Container '...' already exists`
collision between test classes sharing one JVM fork, but did not extend the same fix to
`beam-sample-test` — which runs `BeamActionSerializationTest` and `BeamComponentResourceImplTest`
back-to-back and hit the identical collision on CI (intermittent, since it depends on Meecrowave
teardown timing relative to the next test class's startup — not reliably reproduced locally).
**Fix applied**: add the same `<reuseForks>false</reuseForks>` to `beam-sample-test`'s
`<configuration>`. **Takeaway**: when this class of container-id collision shows up in one
surefire execution of this pom, check every execution that boots a `ComponentManager`/Meecrowave
instance with a shared/fixed container id — the collision risk is per-execution, not per-pom, and
a partial fix (one execution only) leaves the others exposed.
