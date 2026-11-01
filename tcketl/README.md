# tcketl - TCK ETL pipeline designer

`tcketl` is a JavaFX **design-time ETL pipeline designer** built on top of a Talend Component Kit
(TCK) **component-server**. It lets you:

- connect to a component-server (scheme, host, port, base path - no authentication),
- browse the palette of connectors (families, search, icons),
- drag and drop connectors on a canvas, with their input and output ports visible,
- link an output port to an input port to build a pipeline,
- click a node to configure it with a form generated from the `properties` returned by the server
  (tabs = `ui::gridlayout` layouts, `@ActiveIf` conditional display, calls to the server actions).

The design (decisions, specification, architecture) follows `documentation-integration/07-designer-blueprint.md`.

> Status: phases **P0**-**P9** are done and **P10** is partly done (configuration migration on load, 30 s environment polling). The designer connects to a component-server, shows its palette, lets you build, save and reopen a pipeline, and clicking a node shows its form (one tab per layout, conditional fields, healthcheck / suggestions / update / schema buttons). Remaining and deferred work is in the [Roadmap](#roadmap).

## Requirements

- **JDK 17** (the repository JDK, see `.tool-versions`). The code is compiled with `--release 17`: records, sealed
  types, text blocks and `instanceof` patterns are fine, pattern matching for `switch`, virtual threads and
  `SequencedCollection` methods are not available.
- Maven 3.9+.
- A reachable component-server (only needed at run time).

## Build and run

`tcketl/` is a standalone aggregator (`tcketl-parent`) whose parent is the root `component-runtime` pom. It is
**not** listed in the root `<modules>` and the root pom is untouched.

Always build **from the repository root**: the root pom resolves the formatter files from
`${maven.multiModuleProjectDirectory}`, an enforcer rule fails fast with an explicit message otherwise.

```bash
# build, run the tests and the quality gates (Spotless, Checkstyle, RAT, JaCoCo, enforcer)
mvn -f tcketl/pom.xml clean verify

# a single module and what it depends on
mvn -f tcketl/pom.xml -pl tcketl-domain -am test

# run the designer (install first: javafx:run resolves the sibling modules from the local repository)
mvn -f tcketl/pom.xml -pl tcketl-app -am install -DskipTests
mvn -f tcketl/pom.xml -pl tcketl-app javafx:run
```

The entry point is `org.talend.sdk.component.tcketl.app.Launcher`, a class that does not extend
`javafx.application.Application` so that the application also starts from the classpath.

## Using the designer

- **Connection**: *File > Connection settings* (`Ctrl+,`) sets the scheme, host, port, base path and language, and
  tests the connection (`GET /environment`). Saving the settings reloads the palette. An unreachable server shows a
  banner with a *Retry* button; the window stays usable.
- **Palette** (left): the connectors grouped by family, with a search on name, family and categories (`Ctrl+F`) and a
  refresh (`F5`). An entry shows the icon of the connector and its role: a green triangle for an input, a blue
  diamond for a processor, an orange square for an output.
- **Canvas** (centre):
  - drag a connector from the palette onto the canvas, or double-click it, to add a node. The definition of the
    connector is loaded from the server and the ports are drawn from its flows: inputs on the left, outputs on the
    right, `__default__` shown as *main*, `REJECT` dashed and orange. A node whose definition cannot be loaded stays
    on the canvas, flagged;
  - drag from an output port to an input port to link them. A ghost link follows the pointer, and letting go on an
    invalid target (an output, the node itself, a cycle, a duplicate, an input that already has a link) is refused
    with an explanation. `Esc` gives up;
  - click selects a node or a link, dragging a node moves it (snapped to a 20 px grid), a double click on its title
    renames it, `Del` removes the selection (a node takes its links with it);
  - dragging the background pans, `Ctrl` + wheel or the zoom buttons (bottom right) zoom between 25% and 200%;
  - the collapsed *Problems* panel (bottom) lists what is wrong with the pipeline (no input node, input ports left
    unconnected, connectors the server does not know); clicking a problem selects its node.
- **Pipelines** are files `*.tcketl.json` (*File*: *New* `Ctrl+N`, *Open* `Ctrl+O`, *Save* `Ctrl+S`, *Save as*
  `Ctrl+Shift+S`). The window title shows `name *` while there are unsaved changes, and closing the window asks
  before dropping them. The configuration of each node is stored verbatim, and a connector that the server no longer
  offers is kept untouched.

## Modules

```text
tcketl-app ──┬──► tcketl-forms ──────────────┐
             ├──► tcketl-component-client ───┤
             ├──► tcketl-persistence ────────┼──► tcketl-domain (JDK only)
             └──► tcketl-config ──► tcketl-persistence
```

`tcketl-config` reuses the JSON file primitives of `tcketl-persistence` (atomic write, strict parsing, format
version check), so both kinds of file share one reading and writing behaviour.

| Module | Responsibility |
|---|---|
| `tcketl-domain` | Pure core (JDK only): connector and pipeline model, link rules, form engine, ports. |
| `tcketl-config` | Application settings: server connection, language, timeouts. Settings file located by `--config <path>`, then `TCKETL_CONFIG`, then the per-user default; an unusable file falls back to defaults with a warning and is never overwritten silently. |
| `tcketl-persistence` | Pipeline files (`*.tcketl.json`, versioned, atomically written) and the shared JSON file primitives. |
| `tcketl-component-client` | The only module that talks to the component-server: HTTP transport (gzip, timeouts, error mapping), wire DTOs, the metadata parser and mapper to the domain, and a caching catalog invalidated by the environment stamp. Its entry point is `ComponentServerClient`. |
| `tcketl-forms` | JavaFX renderer of the generated forms. |
| `tcketl-app` | JavaFX application: composition root, connection, palette, canvas, inspector. |

Packages are `org.talend.sdk.component.tcketl.<domain|config|persistence|client|forms|app>`. Modules are on the
classpath (no `module-info.java`), like the rest of the repository.

## Form engine

`tcketl-domain` (package `org.talend.sdk.component.tcketl.domain.form`) turns the `properties` of a connector into
everything a form needs, with no UI dependency:

- `PropertyTree`: the properties as a tree (parents come from the paths, `p[]` is the element of the list `p`, a
  map is an object with a `key[]` and a `value[]`);
- `LayoutResolver`: the tabs, one per `ui::gridlayout` pane found in the payload (`Main` first, `Checkpoint` never),
  with their rows, titled groups, list editors and the healthcheck, update and *Guess schema* buttons. A child that
  no pane lists is appended to the first pane rather than dropped;
- `FormValues`: the values of one node as an immutable flat map, defaults applied, with the edits (`set`, `append`,
  `remove`, `move`). `toConfiguration()` is the map the runtime expects: `<path>[i]` indexes, `$` options kept,
  `$<name>_name` companions dropped, keys the form does not know preserved;
- `Validator`: `required`, `min`/`max`, `minLength`/`maxLength`, `pattern`, `minItems`/`maxItems`, `uniqueItems` and
  enum values. It skips what a `Visibility` hides (`@ActiveIf`, P8).

It is tested on the recorded payloads (`FormEngineFixturesTest` in `tcketl-component-client`), including a real
sampleForm `Input` and the `the_family` output.

## Conventions

- Every file carries the Talend license header (Spotless adds it to Java, RAT checks FXML/CSS/properties).
  Spotless **rewrites** Java files at `process-classes`: commit the result.
- Checkstyle (main sources): `final` parameters, no tabs, braces everywhere, imports ordered
  (`java`, `javafx`, `javax`, ..., `org`).
- No blocking call on the JavaFX Application Thread; every server response is delivered back on it.
- JavaFX is pinned by the `openjfx.version` property of `tcketl/pom.xml` (21.0.x LTS, requires JDK 17+). A 17.0.x
  version is a drop-in fallback.
- Tests are JUnit 5 with hand-written fakes and recorded payloads. Fixtures live in
  `tcketl-component-client/src/test/resources/fixtures`: `sample-*` files are derived from real server responses
  (cleaned: no BOM, no truncation marker), `synthetic-*` files are hand built to cover cases the samples do not.
- When the root version changes, bump the parent version of the `tcketl` poms
  (`mvn -f tcketl/pom.xml versions:set -DnewVersion=<version>`): the root release tooling does not see this folder.

## Looking at the UI without a screen

Tests that need the JavaFX toolkit are **opt-in**: they only run when `-Dtcketl.ui.snapshots=<directory>` is given,
and then write a PNG of what they render in that directory. They render the scene off screen (`Scene.snapshot`), so
they also work on a locked desktop or a CI agent, where screen captures come out blank.

```bash
mvn -f tcketl/pom.xml -pl tcketl-app -am test -Dtcketl.ui.snapshots=tcketl/tcketl-app/target/ui

# also render the window against a real component-server: the settings file (see tcketl-config) points at it
mvn -f tcketl/pom.xml -pl tcketl-app -am test -Dtcketl.ui.snapshots=tcketl/tcketl-app/target/ui \
    -Dtcketl.ui.config=/path/to/config.json
```

A stage that is never shown gets no layout pulses: what the layout creates (tree and list cells, the menu accelerators,
and the icon loads the cells trigger) only exists once the scene has been laid out. The tests therefore wait with
`FxHarness.await(scene, timeout, condition)`, which lays the scene out before each check, never with a sleep.
A wrapped `Label` that is given less height than its text needs keeps one line and ends it with an ellipsis: reserve
the room, or let its container honour the height-for-width layout.

The tests do not call view methods. `FxHarness` feeds the **input pipeline of the real scene** (mouse press, drag,
release and click with the real click counting, key presses and accelerators, and a palette drag and drop through the
scene's drop target), so what they prove is what a user gets: hit-testing (what the pointer reaches, shadows
included), double clicks, focus, shortcuts. The bridge to the toolkit internals (`SceneHelper`) relies on the
classpath mode and is test code only.

## Roadmap

| Phase | Content | Milestone | Status |
|---|---|---|---|
| P0 | Bootstrap, build, fixtures | **M0** empty window, `verify` green | done |
| P1 | Domain core: connector roles, ports, pipeline, link rules | | done |
| P2 | Settings and pipeline persistence | | done |
| P3 | Component-server client | | done |
| P4 | App shell, connection dialog, palette | **M1** connect and browse the palette | done |
| P5 | Canvas, ports, links, save/open | **M2** build, save and reopen a pipeline | done |
| P6 | Form engine (pure, headless) | | done |
| P7 | Form renderer and inspector | **M3** click a node, get a tabbed form | done |
| P8 | `@ActiveIf` conditional display | **M4** | done (node validity badge deferred) |
| P9 | Service calls: healthcheck, suggestions, dynamic values, validation, update, schema discovery | **M5** | done |
| P10 | Migration, environment refresh, documentation, error UX | | partly done: migration on load and 30 s environment polling; documentation window done; **deferred**: node validity badge, undo/redo, credential masking |
| P11 | Packaging and hardening | | docs written (`docs/`); `jpackage` image not produced |
