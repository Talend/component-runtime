# Module knowledge — documentation

[2026-09-23 | QTDI-3358] The connector **sample/example** Java sources under `documentation/` (e.g.
`src/main/java/org/talend/runtime/documentation/component/**`,
`src/main/antora/modules/ROOT/pages/_partials/java/*.java`) must keep `javax.annotation.*` and
`javax.json.*` imports, **not** `jakarta.*` — even though this module also contains the doc-site's
own REST-doc-generator tooling (`SearchIndexation.java`, `Github.java`, `Gravatars.java`,
`Generator.java`, under `org.talend.runtime.documentation` build-tooling packages) which correctly
uses `jakarta.json`/`jakarta.ws.rs` for its own unrelated purposes. The two groups look similar
(same module, same top-level package prefix) but must not be migrated together: the sample files
are compiled and copy-pasted by connector authors into the still-`javax`-based
`component-runtime` — `component-runtime-impl`'s `LifecycleImpl` only discovers
`javax.annotation.PostConstruct`/`PreDestroy` (not the `jakarta` equivalents), and
`RecordConverters`/JSON-B decoding only understands `javax.json.*` types. A `jakarta` import on a
sample silently breaks it at runtime (lifecycle hooks never invoked; JSON values fail to
decode) without any compile-time signal, since the file compiles fine as a standalone
`documentation`-module `.java` source — the breakage only shows up when someone copies the sample
into a real connector plugin executed by `component-runtime`. Round 2 of this ticket's jakarta
migration incorrectly moved 8 such sample files to `jakarta.*`; caught by GitHub's automated PR
review (5 files) plus manual inspection for the same defect (3 more), fixed in Round 5. When
migrating `documentation`'s imports for any future jakarta-related work, classify each file by
*what it is* (connector sample vs. doc-generator tooling) before touching its imports, not by
module or package-prefix alone.
