# tcketl — Development Plan

> Authored in English per `CLAUDE.md` §0 (English-only codebase and generated artifacts). Derived
> from the validated specification in `CLAUDE.md` and the captured REST samples in
> `docs/tck-api-sample/`.

---

## Context

`tcketl` is a JavaFX **ETL designer** (design only — no execution). `CLAUDE.md` is a fully validated
specification: all design questions are resolved (decisions log §11) and the TCK Component Server REST
contract is confirmed against a live server (v1.91.0), with samples in `docs/tck-api-sample/`.

Current repo state: **spec/docs only** — `CLAUDE.md`, four REST sample JSON files, no `pom.xml`, no
`src/`, no Java. This document turns the specification into a **sequenced, bottom-up build plan**:
which Maven modules to create in which order, the key classes/interfaces per step, the design patterns
applied, the module dependency hierarchy, and a **validation milestone** for each step so progress is
checkable end-to-end.

Sequencing principle: build the **JavaFX-free foundation bottom-up** (each layer unit-testable in
isolation against the captured samples), then assemble the app as **vertical slices** so the visible
milestones from the request appear early (e.g. "the app lists connectors").

---

## Module dependency hierarchy

Dependencies always point **toward `domain`** (`CLAUDE.md` §3). `domain` knows nothing of JavaFX,
Jackson or HTTP.

```
tcketl-parent (pom)          ← version & plugin management, internal BOM
        │
        ▼
tcketl-domain                ← pure model (no external dependency)
        ▲   ▲   ▲
        │   │   └───────────── tcketl-config           (domain + jackson)
        │   └───────────────── tcketl-persistence      (domain + jackson)
        └───────────────────── tcketl-component-client (domain + jackson + java.net.http)
                                        │
                                tcketl-forms           (domain + javafx-controls)
                                        │
                                tcketl-app             (ALL of the above + javafx-controls/-fxml/-graphics)
```

**Build order (Maven reactor, topological)** = **implementation order**:

1. `tcketl-parent`
2. `tcketl-domain`
3. `tcketl-config`, `tcketl-persistence`, `tcketl-component-client` (independent siblings; may proceed in parallel)
4. `tcketl-forms`
5. `tcketl-app`

**JPMS:** each module ships `module-info.java`; module names follow the base package
(`com.qlik.tcketl.domain`, `.config`, `.persistence`, `.component.client`, `.forms`, `.app`). Modules
exposing types to Jackson `opens` those packages to `com.fasterxml.jackson.databind`; `tcketl-app`
`opens` its FXML-controller packages to `javafx.fxml`.

---

## Design patterns per module (`CLAUDE.md` §8)

| Module             | Patterns                                                                                      |
| ------------------ | --------------------------------------------------------------------------------------------- |
| `domain`           | Composite (pipeline graph), Builder (Pipeline/Node), Value Object, Visitor + Strategy (validation) |
| `component-client` | Gateway/Adapter (`ComponentServerClient`), DTO + Mapper (anti-corruption), Decorator (cache)  |
| `config`           | Loader/Factory, immutable config object                                                       |
| `persistence`      | Repository/DAO, DTO + Mapper                                                                   |
| `forms`            | Abstract Factory + Strategy (control per property type/widget), Builder (form assembly)        |
| `app` (MVVM)       | MVVM, Observer (Property bindings), Command, Mediator (ViewModel), Facade (services), Composition Root (manual DI) |

---

## Phased plan with milestones

### Phase 0 — Reactor bootstrap (`tcketl-parent` + skeleton)

- `tcketl-parent/pom.xml` (packaging `pom`): `<properties>` for versions; `<dependencyManagement>`
  (Jackson databind 2.18.x, JUnit 6.x BOM, JavaFX 25); `<pluginManagement>` (compiler `--release 25`,
  surefire 3.5.x, `javafx-maven-plugin`, jlink/jpackage); `<modules>` listing the six modules.
- Each child module: minimal `pom.xml` + `module-info.java` + base package directory.
- Coordinates: `groupId = com.qlik.tcketl`, artifacts `tcketl-*`.

**Milestone V0:** `mvn -v` → Maven 3.9.9 / JDK 25; `java -version` → 25; `mvn clean install` green on
the empty reactor.

---

### Phase 1 — `tcketl-domain` (pure business core)

**Connector model**
- `enum ConnectorType { INPUT, PROCESSOR, OUTPUT }`
- `enum PropertyType { STRING, NUMBER, BOOLEAN, ENUM, OBJECT, ARRAY }` (TCK uses `ENUM`)
- `enum PortDirection { IN, OUT }`; `record Port(String name, PortDirection direction)` (from flows; `"__default__"` common)
- `record ConnectorId(String id, String name, String family, String familyId, String plugin)`
- `record IconRef(String icon, String theme, String customIconType)`
- `ConnectorSummary` (palette-oriented): `ConnectorId`, `displayName`, `familyDisplayName`,
  `categories`, `IconRef`, `version`, `detailPath` (from index `links[]`). *No definitive role here —
  the index `type` is not the INPUT/PROCESSOR/OUTPUT role; role is resolved on the detail.*
- `ConnectorDefinition`: `ConnectorId`, `displayName`, `ConnectorType` (**derived** from flows),
  `List<Port>` in/out, `List<ConfigurationProperty>` (flat), `IconRef`.
- `ConfigurationProperty` (value object): `path`, `name`, `displayName`, `PropertyType`,
  `placeholder`, `defaultValue`, `PropertyValidation`, `WidgetHints`, `Map<String,String>
  proposalDisplayNames` (ENUM labels).
- `PropertyValidation`: `required`, `min`, `max`, `pattern`, `enumValues`.
- `WidgetHints` (value object): typed view of the form-relevant metadata (credential, textarea,
  code, datetime, path kind, hidden, readonly, gridlayout tabs/rows, optionsorder, conditions,
  `configurationtype::type`). **Raw TCK metadata key strings are parsed into this object by the
  `component-client` mapper**, so TCK-specific strings stay out of `forms`/`app`.

**Pipeline model** (Composite + Builder)
- `Pipeline` (id UUID, name, `formatVersion`, `List<Node>`, `List<Link>`) + `PipelineBuilder`
- `Node` (id, `ConnectorId`, `ConnectorType`, `displayName`, `Position`, `Map<String,String>
  configuration` = path→value, ports) + `NodeBuilder`
- `Link` (id, `Endpoint from`, `Endpoint to`); `record Endpoint(String nodeId, String port)`
- `record Position(double x, double y)`

**Configuration partitioning** (pure — lives in `domain`, testable without JavaFX; consumed by `forms`)
- `enum ConfigurationSectionKind { DATASTORE, DATASET, EXTRA }`
- `record ConfigurationSection(ConfigurationSectionKind kind, List<ConfigurationProperty> properties)`
- `ConfigurationPartitioner.partition(List<ConfigurationProperty>) → List<ConfigurationSection>` using
  `configurationtype::type` + path-prefix descendants; **Extra is the catch-all**; datastore wins over
  a nesting dataset; empty sections omitted.

**Validation** (Visitor + Strategy)
- `ValidationResult` / `ValidationIssue`; `ValidationRule` (Strategy); `PipelineValidator`
- Rules: no cycle; INPUT has no inbound / OUTPUT no outbound; endpoints reference existing
  nodes/ports; port direction compatibility.
- `PipelineVisitor<T>` traversal reused by validation and future export.

**Milestone V1:** domain unit tests green (no I/O, no JavaFX): builders assemble a pipeline; role
derived from flows; partitioner puts `sampleForm` entirely in Extra and splits a datastore/dataset
case correctly; validator rejects a cycle and invalid links.

---

### Phase 2 — `tcketl-config`

- `ApplicationConfig` immutable, nested: `ComponentServerConfig(url, port, timeoutMs, language)`,
  `StorageConfig(pipelinesDirectory)`, `UiConfig(theme)`.
- `ConfigurationLoader.load(Path) → ApplicationConfig`: Jackson `ObjectMapper` with
  `PropertyNamingStrategies.KEBAB_CASE`; defaults + validation; `ConfigException`.
- `ConfigLocationResolver`: `--config=<path>` else `%APPDATA%\tcketl\config.json`.
- `module-info` `opens` config package to Jackson.

**Milestone V2:** load a sample config JSON; missing file → sensible defaults / clear error; Windows
default-path resolution verified in a unit test (APPDATA injected).

---

### Phase 3 — `tcketl-persistence`

- `PipelineRepository` (Repository/DAO): `save(Pipeline)`, `load(id)`, `list() → List<PipelineRef>`,
  `delete(id)`.
- Pipeline JSON DTOs (kebab-case): `PipelineDto`, `NodeDto`, `LinkDto`, `EndpointDto`, `PositionDto`
  — **`configuration` map kept verbatim** (raw TCK paths, not kebab-cased).
- `PipelineJsonMapper` (DTO ↔ domain); `FileSystemPipelineRepository` (JSON files in storage dir;
  `format-version = "1.0"`).

**Milestone V3:** round-trip `save → load` equals the original; `format-version` present; `list`/
`delete` work in a JUnit `@TempDir`.

---

### Phase 4 — `tcketl-component-client`

- `ComponentServerClient` interface (§5 contract): `isAvailable()`, `listConnectors()`,
  `getConnector(id)`, `getIcon(familyId, componentId)`; optional `environment()`, `documentation(id)`.
- **Internal DTOs** (`.dto`): `EnvironmentDto`; `ComponentIndexDto{components[]}` + `ComponentSummaryDto`
  (`id`, `displayName`, `familyDisplayName`, `categories`, `icon`, `iconFamily`, `links`, `metadata`,
  `type`, `version`), `IdDto`, `LinkDto`, `IconDto`; `ComponentDetailListDto{details[]}` +
  `ComponentDetailDto` (`id`, `displayName`, `icon`, `inputFlows`, `outputFlows`, `metadata`,
  `properties[]`, `actions[]`, `type`, `version`); `PropertyDto`, `ValidationDto`, `ActionDto`,
  `DocumentationDto{source, type}`.
- `ComponentMapper` (anti-corruption): DTO → domain; parses TCK `metadata` into `WidgetHints`; derives
  role from flows; builds ports. **`actions[]` ignored** (dynamic services out of scope for a designer).
- `HttpComponentServerClient` (Gateway/Adapter): `java.net.http.HttpClient`; base URL
  `{url}:{port}/api/v1`; endpoints `/environment`, `/component/index?language=`,
  `/component/details?identifiers=&language=`, `/component/icon/{familyId}?id={componentId}`,
  `/documentation/component/{id}?language=`; **follows the index `links[]` Detail path** when present.
  - **Guardrail:** TCK REST JSON is **camelCase** → this module's `ObjectMapper` must use the **default
    naming** (NOT the kebab-case strategy of `config`/`persistence`).
- `CachingComponentServerClient` (Decorator) over `listConnectors()`/`getConnector()`/icons.
- Errors: unreachable / non-2xx → `isAvailable()` false; `ComponentServerException` for genuine
  failures; documentation **404 tolerated** (`Optional.empty`).

**Milestone V4:** mapper unit tests **offline** against `docs/tck-api-sample/`: `sampleForm` → INPUT
role, `__default__` output port, properties + `WidgetHints`, partition → Extra; index → 132 summaries
groupable by family; `isAvailable()` handles connection-refused gracefully.

---

### Phase 5 — `tcketl-forms`

- `DynamicFormBuilder` (Builder): `build(ConnectorDefinition, Map<String,String> values) →` JavaFX node
  with **three sections** (Datastore / Dataset / Extra) via the domain `ConfigurationPartitioner`;
  empty sections not rendered.
- `PropertyControlFactory` (Abstract Factory) + `PropertyControlStrategy` per type/widget: STRING→
  `TextField`, credential→`PasswordField`, textarea→`TextArea`, code→monospaced `TextArea`, NUMBER→
  `TextField`/`Spinner`, BOOLEAN→`CheckBox`, ENUM→`ComboBox` (labels from `proposalDisplayNames`),
  datetime→date/time controls, path→`TextField` + chooser, hidden→skipped, readonly→disabled.
- `FormModel` / `PropertyBinding`: bind each control to the `path→value` map; expose edits back to the
  caller (ViewModel persists into `Node.configuration`).
- Refinement (later): conditional visibility (`condition::if`/`ifs`), layout ordering
  (`ui::gridlayout`/`ui::optionsorder`).
- Testing: headless JavaFX (Monocle/TestFX); note that partitioning is already domain-tested UI-free.

**Milestone V5:** generate a form from the `sampleForm` detail fixture — controls match property
types/widgets; editing updates the values map.

---

### Phase 5a — Property ordering & dynamic tab panes (`ui::gridlayout`)

> Refines the flat form of Phase 5 into **ordered, tabbed** forms driven entirely by the connector
> metadata. This was deferred as "layout ordering" in the Phase 5 refinement bullet; it is promoted
> here to a first-class sub-phase because correct ordering and the right number of tabs are visible,
> testable acceptance criteria.

**The metadata (observed in `docs/tck-api-sample/component-details.sampleForm.response.json`)**

Each `OBJECT` property (and layout-bearing `ARRAY`) declares its children's arrangement through
`metadata` keys of the form:

```
ui::gridlayout::<PaneName>::value = "<row>|<row>|…"   where <row> = "<name>,<name>,…"
```

- `|` separates **rows**; `,` separates **columns on the same row**.
- Tokens are the child properties' **`name`** (relative to this node), **not** the dotted `path`.
- `<PaneName>` is the **tab** the children belong to. Names are **arbitrary and open-ended** — the
  sample uses only `Main` and `Advanced`, but the implementation MUST enumerate whatever pane names
  appear (there may be more, or different, names on real connectors). **Never hardcode `Main`/`Advanced`.**

Example (the datastore object of `sampleForm`):

```
ui::gridlayout::Main::value      = "login,password|healthcheckOk"
ui::gridlayout::Advanced::value  = "timeout"
```

→ **Main** tab: row 1 = [`login`, `password`] side by side, row 2 = [`healthcheckOk`]; **Advanced**
tab: one row with [`timeout`].

**Key semantics**

- **Tabs are the outermost structure.** The top-level tabs are the pane names declared on the
  connector's **root** configuration object. A tab shows **only** the properties that resolve to it —
  editing the *Advanced* tab never displays *Main* properties, and vice versa.
- **Recursive / nested.** Layout is defined per node for its *direct* children. When a child is
  itself an `OBJECT` placed on a parent's `Main` tab, it renders using *its own* `Main` sub-layout
  (and likewise for other panes). In the sample, the root `config` lists `dataset` under both `Main`
  and `Advanced`, so the same nested object contributes to both parent tabs — but each time through
  its matching sub-pane, so the leaf controls on each tab differ (no property is shown twice).
- **Tab order** follows first appearance of each pane name while walking the properties in server
  order; **`Main` first** if present (convention), remaining panes in encounter order.
- **Row/column order** is exactly the `|` / `,` order in the value string.
- **Fallbacks** (in priority order) for a node whose children are not fully covered by a gridlayout:
  1. `ui::gridlayout::*` entries if present;
  2. else `ui::optionsorder::value` (comma-separated `name` list — flat order, single implicit pane);
  3. else the server's `properties` array order (single implicit pane).
  Children present on the node but absent from every pane are appended, in `properties` order, to the
  first/`Main` pane (logged, never dropped).

**Relationship to the three sections (§5 Datastore/Dataset/Extra)**

Tabs (`ui::gridlayout`) and the `configurationtype::type` sectioning are **orthogonal axes**, and
tabs win as the **outer** structure (per the tab-isolation requirement above). Composition: the
top-level `TabPane` comes first; **within each tab**, the properties that resolve to that tab are
still grouped into the **Datastore / Dataset / Extra** titled sub-groups from Phase 5 (via the domain
`ConfigurationPartitioner`), and empty sub-groups are not rendered. Thus Phase 5's "three-section
form" becomes "three-section grouping **inside** each resolved tab". A connector whose root exposes a
single pane renders without a `TabPane` (just the sectioned form of Phase 5).

**Domain additions (`tcketl-domain`, parsed by the `component-client` mapper — no TCK strings leak out)**

- Extend `WidgetHints` (or add a sibling `LayoutHint`) with a parsed, JavaFX-free model:
  - `record GridLayout(List<Pane> panes)`, `record Pane(String name, List<Row> rows)`,
    `record Row(List<String> propertyNames)`;
  - optional `List<String> optionsOrder`.
- The mapper parses `ui::gridlayout::<pane>::value` / `ui::optionsorder::value` into this model. Raw
  `ui::gridlayout::…` strings never appear in `forms`/`app`.
- Add a pure helper `FormLayoutResolver` in `domain` that, given the root configuration object's
  children + their layout hints, produces the ordered `List<Pane>` for the top-level tabs and, for
  each nested node, the ordered panes/rows for the current tab context (applying the fallback rules
  above). Unit-testable without JavaFX.

**Forms consumption (`tcketl-forms`)**

- `DynamicFormBuilder` renders a `TabPane` when the root exposes more than one pane, one `Tab` per
  pane (in resolved order); a single pane renders as a plain sectioned pane (no tabs).
- Inside a tab, the resolved properties are partitioned into Datastore/Dataset/Extra titled groups;
  each group's rows become an `HBox`-per-row inside a `VBox` (or a `GridPane`), so multiple `name`s on
  one row sit side by side. Controls are still produced by the Phase 5 `PropertyControlFactory`.
- Nested `OBJECT`/`ARRAY` children recurse, honoring their own panes for the current tab context.

**Milestone V5a:** from the `sampleForm` detail fixture, the generated form shows the correct **tabs**
(discovered dynamically, not hardcoded); each control appears in the **right pane, row and column
order**; `login`/`password` share a row and `timeout` appears on the **Advanced** tab and is **absent
from the Main** tab; a connector exposing a third pane name renders a third tab automatically. Layout
resolution is covered by UI-free `domain` unit tests over the fixture.

---

### Phase 5b — Conditional display (`condition::if` / `condition::ifs`)

> Completes the form-generation logic (Phase 5/5a) by honoring TCK **display conditions**: a property
> is shown only when its condition over sibling values holds, updating live as the user edits.
> `VisibilityCondition` already covers one equality clause; this phase generalizes it to evaluation
> strategies and to the AND/OR composite form.

**The metadata (observed in `docs/tck-api-sample/component-details.sampleForm.response.json`)**

Simple clause — `condition::if::*`:
- `condition::if::target` — sibling property **name** (resolved to an absolute sibling path);
- `condition::if::value` — expected value(s), **comma-separated** (OR within the clause);
- `condition::if::negate` — `true`/`false`, inverts the clause result;
- `condition::if::evaluationStrategy` — `DEFAULT` | `CONTAINS` | `LENGTH`.

Composite — `condition::ifs::*` + indexed clauses:
- `condition::ifs::operator` — `AND` | `OR`;
- `condition::if::{target,value,negate,evaluationStrategy}::N` — the Nth simple clause (N = 0, 1, …).

**Evaluation strategy semantics** (comma-listed values are OR-ed inside a clause):
- `DEFAULT` — target value **equals** an expected value;
- `CONTAINS` — target value **contains** an expected value (substring);
- `LENGTH` — target value's **length equals** the numeric expected value.
Unknown strategies are treated as `DEFAULT` and logged (never hide the field on a parsing gap).

Examples (`config.conditionalDisplay.*`): `secondConditionString` = CONTAINS `abc`/`def`;
`thirdConditionString` = LENGTH `10`; `fourthConditionString` = NOT CONTAINS `abc`;
`fifthConditionString` = `aBoolean==true` **AND** `aString` CONTAINS `abc`; `sixthConditionString` =
the same two combined with **OR**. Section objects (`allSupportedTypes`, `configuredWidgets`, …) are
each gated by their `display*` boolean.

**Domain (`tcketl-domain`)**

- `enum EvaluationStrategy { DEFAULT, CONTAINS, LENGTH }` (extensible; unknown → `DEFAULT`).
- Generalize the condition model to a **composite** (Composite pattern):
  - a value object `ConditionClause(String targetPath, List<String> expectedValues, boolean negate,
    EvaluationStrategy strategy)` with `boolean matches(String targetValue)`;
  - evolve `VisibilityCondition` to `(List<ConditionClause> clauses, Operator operator)` with
    `enum Operator { AND, OR }`, plus `boolean isVisible(java.util.function.Function<String,String>
    valueLookup)` and `List<String> targetPaths()`. A simple condition is a one-clause `VisibilityCondition`.
  - keep a `VisibilityCondition.simple(target, values, negate, strategy)` factory for readability/tests.
- **API change:** `isVisible(String)` becomes `isVisible(Function<String,String>)` because a composite
  reads several targets; update the single existing caller (`DynamicFormBuilder`) and the tests.
- **Group (container-level) conditions.** TCK frequently attaches the condition to an enclosing
  configuration **object** (e.g. an "Use a proxy" toggle gating a whole proxy group), not to each
  leaf. Since Phase 5a flattens containers into their leaf rows, `FormLayoutResolver` must **propagate
  each ancestor container's `visibleWhen` down to its descendant leaves**, AND-combined with the
  leaf's own condition. Carry these on the resolved cell: `FormLayout.Row` holds
  `Field(ConfigurationProperty property, List<VisibilityCondition> visibilityConditions)`, and a field
  is shown only when **all** its conditions hold. (Without this, group-gated fields render
  unconditionally — the observed HTTP-connector proxy bug.)

**Component client (`tcketl-component-client`)**

- Rewrite `ComponentMapper.toVisibilityCondition`: if `condition::ifs::operator` is present, parse the
  indexed clauses `condition::if::*::N` (N from 0 until absent) into a composite; else parse the single
  `condition::if::*` clause. Parse `evaluationStrategy` (default `DEFAULT`). Reuse the existing
  `resolveSibling(path, targetName)` to turn each relative target into an absolute path.

**Forms (`tcketl-forms`)**

- `DynamicFormBuilder`: render each `FormLayout.Field` and gate it on **all** of its
  `visibilityConditions` (own + inherited group conditions), evaluated against a value lookup
  (`path -> data.get(path).orElse("")`). The existing "refresh every conditional cell on any edit"
  loop already re-evaluates multi-target composites, so only the cell wiring changes.

**Testing**

- Domain `VisibilityConditionTest`: CONTAINS / LENGTH / negate clauses; AND vs OR composites.
- `ComponentMapperTest` (offline, real sample): `secondConditionString` (CONTAINS list),
  `thirdConditionString` (LENGTH), `fourthConditionString` (negate), `fifthConditionString` (AND),
  `sixthConditionString` (OR) parse with resolved absolute target paths.
- `DynamicFormBuilderTest`: a composite (two targets) shows/hides a field as either target changes.

**Milestone V5b:** conditional fields from the `sampleForm` detail honor evaluation strategies and
AND/OR composites — e.g. `fifthConditionString` appears only when `aBoolean` is `true` **and**
`aString` contains `abc`; toggling either updates visibility live. Condition evaluation is covered by
UI-free `domain` unit tests and offline mapper tests over the fixture.

---

### Phase 6 — `tcketl-app` (MVVM) — vertical slices

Base: `TcketlApplication extends javafx.application.Application` (main class directly on the module
path — no launcher hack); `CompositionRoot` (manual DI — parses `--config`, builds
`ConfigurationLoader`, `HttpComponentServerClient` + caching decorator, `FileSystemPipelineRepository`,
`DynamicFormBuilder`; injects into ViewModels via constructors); MVVM primitives `Command`,
`AsyncCommand` (REST off the FX thread), `ViewModelBase`. `module-info` `opens` controller packages to
`javafx.fxml`. ViewModels reference no View/FXML type and are testable without starting JavaFX.

- **6a — Bootstrap & shell:** `MainView.fxml` + thin `MainController`, `MainViewModel`; startup loads
  config and probes `isAvailable()` on a background thread; **warning dialog + empty palette** if
  unavailable.
  **Milestone V6a:** app launches on the module path, loads config, shows the shell, warns (no crash)
  if the server is down.
- **6b — Palette grouped by family:** `PaletteViewModel` (`ObservableList<ConnectorSummary>` grouped/
  sorted by `familyDisplayName`; `loadConnectorsCommand` async); `PaletteView` collapsible family
  sections with icons (`getIcon`).
  **Milestone V6b: the app lists connectors from the live server, grouped by family** (empty +
  warning if down).
- **6c — Canvas & linking:** `CanvasViewModel` (`ObservableList<NodeViewModel>`/`LinkViewModel`;
  `addNode` = drag from palette → `getConnector` detail → `Node`; `moveNode`; `createLink` validated
  via domain; `selectedNode`); `CanvasView` with drag-and-drop, ports, link drawing, validation
  feedback.
  **Milestone V6c:** drop connectors, position, link ports; domain validation blocks invalid links
  (cycle, direction, INPUT/OUTPUT constraints).
- **6d — Property editor:** `PropertyEditorViewModel` bound to `selectedNode`; embeds the
  `tcketl-forms` three-section form built from the node's `ConnectorDefinition` + configuration;
  writes edits back to `Node.configuration` (path→value).
  **Milestone V6d:** select a node → generated form appears; edits persist to the node's config.
- **6e — Save / Load:** `PipelineViewModel` + commands (new, open from storage dir, save, save-as;
  dirty tracking) → `PipelineRepository`.
  **Milestone V6e: design → save JSON → reopen → identical pipeline** (round-trip through the UI).

---

#### Phase 6f — Connector roles & role-aware ports

> Refines connector-role derivation and the canvas handles so the three roles match TCK semantics.
> The main flow is `__default__`:
> - **INPUT** — empty `inputFlows` (mapper/emitter); no inbound handle in the designer;
> - **PROCESSOR** — has `inputFlows` **and** an `__default__` output flow;
> - **OUTPUT (sink)** — has `inputFlows` **and no `__default__` output** (empty, or only secondary
>   flows like `reject`/`error`); terminal, no outbound handle.
>
> Fixes a misclassification: a sink that declares a non-main output flow is currently typed
> PROCESSOR and offered an outbound handle.

**Domain (`tcketl-domain`)**
- Update `ConnectorType.fromPorts(inputPorts, outputPorts)` to key the PROCESSOR/OUTPUT split on a
  `Port.DEFAULT_FLOW` output rather than "any output":
  - no input flows → `INPUT`;
  - else has a `__default__` output port → `PROCESSOR`;
  - else → `OUTPUT` (sink).
  Keep the standalone case (no input and no output at all) handled by the mapper.
- Keep the `Port` model unchanged (named ports per flow retained for fidelity / future multi-branch).
- `RoleConstraintsRule` is unchanged (INPUT no inbound / OUTPUT no outbound) and now applies to
  correctly-typed sinks.

**Component client (`tcketl-component-client`)**
- `ComponentMapper.toDefinition` still builds ports from `inputFlows`/`outputFlows`; the role comes
  from the refined `fromPorts`. The standalone fallback (both empty → PROCESSOR) stays.

**App / canvas (`tcketl-app`)**
- `CanvasView`: draw a handle for **every declared flow** on each side (`inputFlows` left,
  `outputFlows` right). An INPUT has no input flows → no input handle (its role is purely derived, it
  is not enforced by hiding). A sink's secondary outputs (`reject`, …) and multi-branch flows stay
  visible and connectable. `__default__` displays as `main`. The role drives the **palette icon** and
  the domain link **validation** (`RoleConstraintsRule`), not the set of handles drawn.

**Palette role icons (`tcketl-app`)**
- The palette lists `ConnectorSummary` (index), which has **no reliable role** — the index `type` is
  e.g. `standalone` even for inputs. Resolve each entry's role from the connector **detail**:
  `ComponentServerClient.getConnector(id).type()`, on a background thread via the existing
  `CachingComponentServerClient` (also warms the drop-time cache). Update entries reactively as roles
  resolve; icons fill in progressively.
- Model: wrap each summary in a `PaletteEntry` (summary + `ObjectProperty<ConnectorType>` role, empty
  until resolved); `ConnectorFamily.connectors()` becomes `List<PaletteEntry>`. `PaletteViewModel`
  gains `setRole(ConnectorId, ConnectorType)`; `MainViewModel` drives resolution off the FX thread and
  applies roles on it. ViewModels stay `javafx.base`-only (headless-testable).
- View: `MainController.rebuildPalette` sets a small role-icon **graphic** on each `ListCell`, bound
  to the entry's role. Add a `RoleIcon.of(ConnectorType)` factory returning a ~12 px, theme-neutral
  JavaFX node: **INPUT** → right-pointing arrow (`Path`/`SVGPath`); **PROCESSOR** → gear (`SVGPath`);
  **OUTPUT** → filled `Circle`; unresolved → no icon (or a faint placeholder dot).
- Performance: ~130 background detail calls, cached and progressive. Optional (backlog): resolve a
  family's roles lazily when its accordion pane expands, or add a bulk multi-identifier details call
  to the client.

**Testing**
- `ConnectorTypeTest`: input + `reject`-only output → `OUTPUT`; input + `__default__` output →
  `PROCESSOR`; empty input → `INPUT`.
- `ComponentMapperTest`: a sink-shaped detail (input flow + non-`__default__` output) maps to
  `OUTPUT`.
- Canvas/view: every declared flow renders a handle (an INPUT has none on the input side; a sink's
  `reject`/secondary outputs remain visible).
- `PaletteViewModel`: entries expose a role; `setRole` updates the matching entry (toolkit-free).
- `MainViewModel`: with a stub client returning known definitions, background resolution sets each
  entry's role.
- `RoleIcon`: returns a distinct, non-null node per role (headless).

**Milestone V6f:** a sink (input + non-`__default__` output) is typed **OUTPUT** (palette shows the
output circle) while its `reject`/secondary output flows stay visible on the canvas; an INPUT shows
no input handle (it declares none); domain validation still rejects linking out of the `__default__`
of an OUTPUT or into an INPUT; and the palette shows a per-connector role icon (input arrow /
processor gear / output circle), resolved from the connector detail and cached.

> Implementation note: also update `CLAUDE.md` §5 "Role & ports" (decision Q7) so the documented rule
> matches — INPUT ⇔ no input flow; OUTPUT ⇔ input present and **no `__default__` output**; PROCESSOR
> otherwise.

---

#### Phase 6g — Palette icons (family & connector) + role-icon placement

> Fetches the Component Server icons and lays out each palette row as
> `[connector icon] name … [role glyph]`, with the family icon left of each family header. Icons are
> SVG, rasterized with **jsvg** and cached.

**Build (`tcketl-parent`, `tcketl-app`)**
- Add `com.github.weisj:jsvg` (latest stable), version managed in the parent POM; declared in
  `tcketl-app`. `module-info` of `tcketl-app`: `requires com.github.weisj.jsvg;` (verify the exact
  module name) and `requires java.desktop;` (jsvg renders to an AWT `BufferedImage`). Note for
  Phase 7: the `jlink`/`jpackage` image must include `java.desktop` + jsvg.

**Component client (`tcketl-component-client`)**
- Replace `getIcon(familyId, componentId)` with two endpoint-accurate methods returning bytes,
  tolerating 404 like `documentation(...)`:
  - `Optional<byte[]> componentIcon(String id)` → `GET /component/icon/{id}`;
  - `Optional<byte[]> familyIcon(String familyId)` → `GET /component/icon/family/{familyId}`.
- `HttpComponentServerClient`: build the URLs above; 404 → `Optional.empty`.
- `CachingComponentServerClient`: memoize icon bytes per component id and per family id; clear on
  `invalidateCaches()`. Update stubs/tests (`StubComponentServerClient`, `CachingComponentServerClientTest`).
- Update `CLAUDE.md` §5 icon-endpoint documentation to the two correct endpoints.

**App — rendering (`tcketl-app`)**
- `SvgIconRenderer` (view util): `Image render(byte[] bytes, double size)` — sniff SVG (`<svg`/`<?xml`)
  → rasterize with jsvg to `size`×`size` on a `BufferedImage`, copy pixels into a JavaFX
  `WritableImage` (no `javafx.swing` needed); otherwise decode raster via `new Image(stream)`. Return
  `null` / a blank placeholder on failure (never throw). Cache the resulting `Image` by (kind,id,size).

**App — model + resolution (`tcketl-app`)**
- `PaletteEntry`: add an `ObjectProperty<Image> icon` (empty until resolved).
- Family model: add `familyId` and an `ObjectProperty<Image> icon` to `ConnectorFamily` (or a small
  `PaletteFamily` holder), so the family icon can update after the palette is built.
- `MainViewModel`: after `resolveRoles`, also resolve icons on the background executor (reusing the
  caching client): per connector `componentIcon(id)` → render → set `PaletteEntry.icon`; per family
  (distinct `familyId`) `familyIcon(familyId)` → render → set the family icon; apply on the UI runner.
  Icons fill in progressively; failures leave no icon.

**App — view (`tcketl-app`)**
- `MainController.rebuildPalette`:
  - Connector cell (`ListCell<PaletteEntry>`): `setText(null)`, `setGraphic` an `HBox`:
    `[ImageView ~16px (connector icon)] [name Label] [spacer, HGrow] [RoleIcon (role glyph)]`.
    Bind the icon `ImageView` to `entry.iconProperty()` and the role node to `entry.roleProperty()`
    (listeners, removed on cell reuse — extend the existing `PaletteCell`).
  - Family header: `TitledPane.setText(familyName)` and `setGraphic(ImageView)` bound to the family
    icon property (icon shows left of the title).
- `RoleIcon` is unchanged; only its position moves (now the right-hand node of the cell HBox).

**Testing**
- `SvgIconRendererTest`: a minimal inline `<svg>` → non-null `Image` of the requested size; a 1×1 PNG
  → non-null; garbage bytes → `null` (no throw).
- `CachingComponentServerClientTest`: `componentIcon`/`familyIcon` are fetched once then cached; cleared
  by `invalidateCaches()`.
- `PaletteViewModel`/`MainViewModel`: with a stub client returning bytes, background resolution sets
  each entry's/family's icon (headless where possible; Image creation needs the toolkit).

**Milestone V6g:** palette rows show `[connector icon] name … [role glyph]`, and each family header
shows its icon left of the name; icons come from `/component/icon/{id}` and
`/component/icon/family/{id}`, are rasterized with jsvg, cached, and resolved progressively.

---

### Phase 7 — Packaging & distribution (`tcketl-app`)

- Dev: `javafx-maven-plugin` → `mvn -pl tcketl-app javafx:run` (module path).
- Distribution: `jlink` custom runtime (app + JavaFX + Jackson modules) → `jpackage` Windows
  `.msi`/`.exe`. No fat-jar.

**Milestone V7:** `javafx:run` launches the app; `jpackage` produces an installer that runs
standalone.

---

### Phase 8 — Refinements (backlog, optional)

- Documentation viewer: `/documentation/component/{id}` AsciiDoc render; tolerate 404.
- Extra/advanced widgets not yet covered by Phase 5 (e.g. schema/table editors for `ARRAY`
  structures, `ui::structure` handling). Conditional display is Phase 5b; `gridlayout` ordering is
  Phase 5a.
- Cache refresh/invalidation; richer error surfaces; logging (JUL by default — no new dependency
  decision unless requested).

---

## Cross-cutting concerns

- **Two Jackson configurations:** kebab-case (`PropertyNamingStrategies.KEBAB_CASE`) for **our** files
  (`config`, `persistence`); **default camelCase** for **TCK REST** DTOs (`component-client`). TCK
  property paths inside a node's `configuration` are stored **verbatim**.
- **Single network entry point:** all server access via `ComponentServerClient`; no `HttpClient` or
  hard-coded URL elsewhere. Samples in `docs/tck-api-sample/` are **test fixtures only**, never a
  runtime source (no mock fallback).
- **Testability:** `domain`, `config`, `persistence`, `component-client` are JavaFX-free and fully
  unit-testable (JUnit 6, `@TempDir`, sample fixtures). `forms`/`app` need headless JavaFX (Monocle);
  ViewModels remain testable without the UI.
- **Version centralization:** every version in `tcketl-parent`; modules never redeclare one.
- **English-only** codebase and generated artifacts.

---

## Milestone summary

| ID   | Gate |
| ---- | ---- |
| V0   | Empty reactor builds; JDK 25 / Maven 3.9.9 verified |
| V1   | Domain: builders, role-from-flows, partitioner, validator (unit-tested, UI-free) |
| V2   | Config loads (kebab-case); default `%APPDATA%` path resolves |
| V3   | Pipeline JSON round-trips with `format-version` |
| V4   | Client mapper maps the captured samples; `isAvailable()` degrades gracefully |
| V5   | Dynamic 3-section form generated from a connector detail |
| V5a  | Forms honor `ui::gridlayout`: dynamic tabs (tab-isolated) + correct property order (UI-free resolver tested) |
| V5b  | Conditional display: evaluation strategies + AND/OR composites, live toggling |
| V6a  | App launches, loads config, warns on unreachable server |
| V6b  | **Connectors listed from the live server, grouped by family** |
| V6c  | Canvas drop + validated linking |
| V6d  | Per-node generated property form, edits persisted |
| V6e  | **Pipeline saved to JSON and reopened identically** |
| V6f  | Roles via the `__default__` main flow; sinks terminal; role-aware handles; palette role icons |
| V6g  | Palette shows connector & family SVG icons (jsvg, cached); role glyph moved to the right |
| V7   | `javafx:run` works; `jpackage` installer produced |

---

## Verification

- Per phase, run the module's tests: `mvn -pl <module> test` (e.g. `mvn -pl tcketl-domain test`),
  `mvn clean install` for the whole reactor.
- V4 mapper tests load `docs/tck-api-sample/*.json` from test resources — **no live server needed**.
- V6b/V6d/V6e are validated **against the live Component Server** (URL/port from the config file);
  with the server down, verify the warning + empty palette path instead.
- Final: `mvn -pl tcketl-app javafx:run`, then a `jpackage` build, launching the produced installer.
