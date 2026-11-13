# Module knowledge — documentation

[2026-11-04 | QTDI-3497] The connector **sample/example** Java sources under `documentation/` (e.g.
`src/main/java/org/talend/runtime/documentation/component/**`,
`src/main/antora/modules/ROOT/pages/_partials/java/*.java`) and under `sample-parent/` use
`jakarta.annotation.*` / `jakarta.json.*`, like the rest of the reactor. `component-runtime-impl`'s
`LifecycleImpl` only discovers `jakarta.annotation.PostConstruct`/`PreDestroy` and
`RecordConverters`/JSON-B decoding only understands `jakarta.json.*` types, so a `javax` import on a
sample silently breaks it at runtime (lifecycle hooks never invoked; JSON values fail to decode)
without any compile-time signal. This replaces the earlier (QTDI-3358) rule that kept samples on
`javax.*` while the runtime was still javax-based: samples and doc snippets (`.adoc` dependency
listings included, e.g. `jakarta.annotation-api`, `jakarta.json-api`, `jakarta.json.bind-api`) must
now follow the jakarta namespace as well. `javax.cache.*` (JSR-107) is the only intentional `javax`
remainder in the reactor since JCache has no jakarta equivalent.
