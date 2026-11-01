# tcketl - Specification

Derived from the living plan (`copilot/plan.md`); where code and this file disagree, the code wins.

## 1. Decisions and conventions

| # | Topic | Decision |
|---|---|---|
| D1 | JDK / language level | **(confirmed, twice: "utilise java 17, ne change pas le jdk")** Java 17 on the existing JDK; never change the JDK, `.tool-versions` or the root pom. `maven.compiler.release=17` is inherited, so code MUST compile with `--release 17`: records, sealed types, text blocks, `instanceof` patterns, `Stream.toList()`, `var` and arrow `switch` expressions are fine; **not available**: pattern matching for `switch`, record patterns, virtual threads, `SequencedCollection` methods, unnamed variables. Dev machine: OpenJDK 17.0.2. |
| D2 | UI toolkit | **(confirmed)** JavaFX **21.0.2** (LTS, needs JDK 17+): `javafx-controls`, `javafx-fxml`, pinned by `openjfx.version`. Smoke-tested on JDK 17.0.2: works. |
| D3 | Repo integration | **(confirmed)** `tcketl/` is a **standalone aggregator** (`tcketl-parent`, parent = root pom through `<relativePath>../pom.xml</relativePath>`), NOT in the root `<modules>`, root pom untouched, local builds only. An enforcer rule (`tcketl-build-from-repository-root`) fails any build that does not start from the repository root: always `mvn -f tcketl/pom.xml ...`. |
| D4 | Server model | **(confirmed)** Own immutable DTO records (Jackson) + anti-corruption mapper; **no compile dependency** on other component-runtime modules (`component-server-model` and `component-form` are not reused; `component-form-core` is javax.ws.rs based). `tcketl/` can be extracted into its own repository. |
| D5 | Coordinates | `groupId=org.talend.sdk.component`, version = root version (`1.2611.0-SNAPSHOT`), packages `org.talend.sdk.component.tcketl.<module>`, Talend license header on every file. |
| D6 | Modularity | Classpath mode, no `module-info.java` (as the rest of the repository). `talend.build.name` per module gives the automatic module name. |
| D7 | Tests | JUnit 5 (inherited, with params), hand-written fakes, JDK `HttpServer` stubs. No AssertJ. Off-screen snapshot harness for the GUI (see 9). TestFX/Monocle only as an optional late phase. |
| D8 | JSON | Jackson databind (root-managed). Own files (config, pipeline) = kebab-case; TCK wire DTOs = camelCase with `FAIL_ON_UNKNOWN_PROPERTIES=false`. |
| D9 | HTTP | `java.net.http.HttpClient`, HTTP/1.1, manual gzip decoding, timeouts connect 5 s / request 30 s / action 60 s (`ServerTimeouts`, configurable). |
| D10 | "Calls to services" | = TCK **actions** executed by the server through `POST /api/v1/action/execute`: healthcheck, suggestions, dynamic values, async validation, update, schema discovery. In scope (P9). |
| D11 | Scope | **Design-time only**: no pipeline execution or export to a runtime engine in v1. |
| D12 | UI construction | MVVM (view models use only `javafx.beans`, so they run without a toolkit). FXML + CSS for static shells, plain code for dynamic parts (canvas, forms). `System.Logger`. UI language: English. |

Reconciling `docs/development-plan.md` (written for a standalone JDK 25 project). **Kept**: module split and dependency
direction, pure domain, roles derived from flows (not from `type`), `ui::gridlayout` tab semantics (5a), group-condition
propagation (5b), fixtures as offline tests, one network entry point. **Adapted**: JDK 25 -> 17, JavaFX 25 -> 21, JPMS
-> classpath, `com.qlik.tcketl` -> `org.talend.sdk.component.tcketl`, JUnit 6 -> 5, "actions out of scope" -> in scope
(P9). **Deferred**: datastore/dataset/extra grouping inside tabs, jlink/JPMS, TestFX/Monocle.

## 2. Scope

**In scope (v1)**: FR-01..FR-16 (3.1).
**Out of scope**: pipeline execution/export, authentication/vault/tenant header, WebSocket transport, `/bulk`
(documented defects), `/cache/clear` (mutating, never called), dependency download endpoints, `.car`/deployment
awareness, Studio/Singer bridges, datastore/dataset reuse (`/configurationtype/*`, blueprint A9), schema propagation
across the graph (blueprint A12), i18n other than English, collaborative editing.

## 3. Specification

### 3.1 Functional requirements

Level = maturity level of `documentation-integration` (0 minimal, 1 usable, 2 full). Blueprint = section of
`07-designer-blueprint.md`.

| ID | Requirement | Level | Blueprint | Phase | Status |
|---|---|---|---|---|---|
| FR-01 | **Connection settings**: host, port, base URL path (default `/api/v1`), scheme `http`/`https`, UI language. "Test connection" calls `GET /environment` and shows server `version`/`commit`. No authentication. Settings persisted; saving them reloads the palette. Unreachable server => non-blocking banner (URL + Retry), UI stays responsive. | 0 | A17, M01 | P2, P4 | **done** |
| FR-02 | **Palette**: `GET /component/index?language=<L>&includeIconContent=false`; grouped by family (`familyDisplayName`), sorted case-insensitively by family then `displayName`; client-side search on name/family/categories; tooltip = `metadata["documentation::value"]`; manual refresh; streaming badge when `mapper::infinite=true`. | 0/1 | A1 | P4 | **done** |
| FR-03 | **Drag and drop**: dragging a palette entry onto the canvas creates a node at the drop point; its definition (flows, properties, version) is fetched on demand (`GET /component/details`); loading/failed states are visible on the node. | 0 | A2, A11 | P5 | **done** |
| FR-04 | **Visible ports**: one input handle per `inputFlows` entry (left), one output handle per `outputFlows` entry (right), labelled (`__default__` shown as "main", `REJECT` styled distinctly); standalone components show none; role glyph. Roles derive from flows, never from index `type`. | 0 | A11 | P4, P5 | **done** |
| FR-05 | **Linking**: drag from an output handle to an input handle creates a link; invalid targets are refused with visual feedback (rules LR1-LR5); links are selectable/deletable; moving a node re-routes its links. | 0 | A11 | P5 | **done** |
| FR-06 | **Selection and inspector**: clicking a node selects it and shows its configuration form in the inspector; node label editable; node deletable (removes its links). | 0 | M14 | P5, P7 | **partial**: select / rename / delete done (P5); the inspector form is 7d |
| FR-07 | **Form generated from `properties`**: tabs from layouts, rows/columns, nested object groups, array editors, widgets by metadata (3.5.4), defaults, required markers, tooltips, read-only, hidden. | 0/1 | A2-A5, A14 | P6, P7 | **partial**: engine done (P6), widget selection + view model done (7a); rendering is 7b-7c |
| FR-08 | **ActiveIf**: `condition::if::*`, `condition::ifs::operator`, strategies `DEFAULT`/`LENGTH`/`CONTAINS`, `negate`, relative target paths, group-condition propagation; re-evaluated on every change; hidden fields are not validated. | 1 | A6 | P8 | to do (conditions are already parsed into `VisibilityCondition`) |
| FR-09 | **Validation**: `required`, `min`, `max`, `minLength`, `maxLength`, `pattern`, `minItems`, `maxItems`, `uniqueItems`, `enumValues`; inline messages; node-level problem indicator. | 0/1 | A7 | P6, P7, P8 | **partial**: `Validator` + view-model messages done; inline display 7b-7c; node badge P8 |
| FR-10 | **Service calls (actions)**: (a) healthcheck button, (b) suggestions on focus/change, (c) dynamic values at form build, (d) async validation, (e) update button, (f) schema discovery ("Guess schema"). Stale responses discarded, UI never blocked, timeouts and errors displayed. | 1 (a,b,c,f) / 2 (d,e) | A8 | P9 | to do (bindings already parsed into `ActionBinding`) |
| FR-11 | **Pipeline persistence**: new/open/save/save-as, dirty flag, format-versioned JSON; node configuration kept as the flat TCK map + component `version`; loading re-fetches details; unresolved components kept intact and flagged. | 0 | A14, A15 | P5 | **done** |
| FR-12 | **Component migration on load**: if stored `version` < current, `POST /component/migrate/{id}/{version}` with the stored map. | 1 | A15 | P10 | to do |
| FR-13 | **Icons**: component and family icons (`GET /component/icon/{id}`, `/icon/family/{id}`, `Accept: application/octet-stream`, `theme`), PNG/SVG rendering, placeholder on `404`. | 1 | A1 step 8 | P4 | **done** |
| FR-14 | **Documentation**: component documentation (`GET /documentation/component/{id}`, asciidoc source shown as text in v1). | 1 | A13 | P10 | **partial**: client side done (`DocumentationSource`, `HttpComponentCatalog.documentation`); the viewer is P10 |
| FR-15 | **Environment awareness**: poll `GET /environment` (30 s), offer palette refresh when the stamp changes. | 2 | A17 | P10 | **partial**: stamp comparison done in `CachingComponentCatalog`; the polling timer and the prompt are P10 |
| FR-16 | **Error handling**: decode `ErrorPayload{code, description}`, non-modal notifications, no network on the FX thread. | 0 | A16 | all | **partial**: error mapping, banner and notices done; UX polish P10 |

### 3.2 Non-functional requirements

- NFR-1 Responsiveness: no blocking call on the JavaFX Application Thread; every response is delivered back on it.
- NFR-2 Offline testability: all parsing/mapping/form logic is verified against recorded fixtures without a server.
- NFR-3 Robustness: unknown metadata keys, unknown property types (treated as `STRING`), missing optional fields are tolerated.
- NFR-4 Build hygiene: Spotless, Checkstyle, RAT and enforcer gates of the root pom stay green; Talend license header everywhere.
- NFR-5 Scale: palette of 130+ connectors and forms of several hundred properties stay fluid (lazy tab content, cached details).
- NFR-6 Security: HTTPS uses the JDK trust store (never disable verification); credentials are never logged.

### 3.3 Server contract used (base `/api/v1` + optional prefix; verified in `component-server-api`)

| Endpoint | Used for | Notes |
|---|---|---|
| `GET /environment` | status, version, change stamp | `lastUpdated` is `ZonedDateTime.toString()` (`2026-07-17T21:37:06.859Z[UTC]`): an **opaque string**; stamp = `lastUpdated` + `connectors.pluginsHash` |
| `GET /component/index?language&includeIconContent=false[&theme][&q]` | palette | `components[]`: `categories`, `displayName`, `familyDisplayName`, `icon`, `iconFamily`, `id{family,familyId,id,name,plugin,pluginLocation}`, `links`, `metadata`, `type`, `version`. `type` is **not reliable** for roles |
| `GET /component/details?identifiers=<id>[&identifiers=..]&language` | node definition | `inputFlows`, `outputFlows`, `version`, `properties[]`, `actions[]`, `metadata`. On `400` the body is a **map** `{id: ErrorPayload}`, not a single payload |
| `GET /component/icon/{id}`, `/icon/family/{id}`, `/icon/custom/{familyId}/{iconKey}` | icons | `Accept: application/octet-stream`; `Content-Type` `image/png` or `image/svg+xml`; `404` `ICON_MISSING`/`FAMILY_MISSING`/`COMPONENT_MISSING`/`PLUGIN_MISSING` |
| `GET /documentation/component/{id}?language[&segment]` | docs | `{source, type:"asciidoc"}` |
| `POST /component/migrate/{id}/{configurationVersion}` | migration | body = flat `Map<String,String>` |
| `POST /action/execute?family=<technical family>&type&action&lang` | service calls | flat JSON body keyed `<paramRoot>[.<sub>]`; **`lang`** here, `language` elsewhere; `family` = `id.family`, not `familyId` |
| `GET /action/index?type&family&language` | actions not referenced by options | schema discovery, available outputs |

Never called: `POST /bulk`, `GET /cache/clear`, `/component/dependencies`, `/component/dependency/{id}`,
`/component/icon/index`. The server has no `ETag`/`If-None-Match`; it normalises `language` (`en`, `fr`, `zh_CN`, `ja`,
`de`; anything else becomes `en`).

### 3.4 Domain rules (implemented in P1)

**Roles** (from flows only, `ConnectorRole.fromFlows`): `in=[] and out=[]` -> `STANDALONE` (no ports); `in=[]` ->
`INPUT`; `out=[]` -> `OUTPUT` (sink); otherwise `PROCESSOR` (a component whose only output is `REJECT` is a
`PROCESSOR`). The index `type` is only a provisional hint until the details are loaded.

**Link rules** (single source of truth: `PipelineEditor`):
- LR1 `from.flow` MUST be an output flow of the source node and `to.flow` an input flow of the target node (verbatim names).
- LR2 No self-link, no cycle (the job graph is a DAG).
- LR3 An input port accepts **at most one** incoming link (blueprint A11.5); an output port MAY fan out.
- LR4 No duplicate link.
- LR5 `OUTPUT` has no outgoing port; `STANDALONE` has no ports.
- LR6 Warnings (non-blocking, "Problems" list): unconnected input port of a `PROCESSOR`/`OUTPUT`; `INPUT` without outgoing link (relaxed when `mapper::optionalRow=true`).
- LR7 Warning: no `INPUT` node in the pipeline.

**Configuration paths**: server `path` values are dotted from the root option (`config.dataset.datastore.login`); array
element definitions use `p[]`, values use `p[0]`; technical options `$maxRecords`, `$maxDurationMs`, `$maxBatchSize`
are ordinary properties (relative to the root option); `<slotPath>.__version=<n>` for versioned nested classes (only
preserved). A node persists `{component ref, version, configuration (flat Map<String,String>)}`; `version` is never
part of the map; `$checkpoint.*` is runtime state and is never invented; the runtime applies no UI defaults.

### 3.5 Form generation specification

#### 3.5.1 Pipeline of transformations (pure, headless, in `tcketl-domain`, package `...domain.form`)

| Step | Input -> Output | Blueprint | Status |
|---|---|---|---|
| F1 | `properties[]` -> `PropertyTree` (parent = path without last segment; `[]`-aware; unknown `type` -> `STRING`; duplicate or malformed paths ignored with a tree warning) | A2 | done (P6, `PropertyTreeBuilder`) |
| F2 | defaults: `ui::defaultvalue::value` wins over `defaultValue`; typed; only fills keys absent from the stored map | A3 | done (P6, `Defaults`) |
| F3 | `LayoutResolver`: tree -> `FormLayout{tabs -> rows -> cells}` | A4 + dev-plan 5a | done (P6) |
| F4 | `VisibilityEngine`: values -> effective visibility per path | A6 + dev-plan 5b | done (P8, `VisibilityEngine`); the `Visibility` oracle interface exists, `Visibility.ALL` is used until then |
| F5 | `Validator`: values + visibility -> issues (invisible nodes skipped) | A7 | done (P6) |
| F6 | `ConfigurationCodec` / `ConfigurationValues`: tree <-> flat map (`[i]`, map `.key[i]`/`.value[i]`, `$` options) | A14 | done (P6; no `decode`, see 8) |
| F7 | `ActionBindings`: metadata `action::<type>` + `actions[]` -> triggers and request bodies | A8 | **to do (P9)**; metadata already parsed into `ActionBinding` / `ActionDefinition` |

#### 3.5.2 Layout rules (tabs = layouts) - implemented in P6

Syntax (value of `ui::gridlayout::<Pane>::value`, tokens are child `name`s relative to the object, not paths):

```text
ui::gridlayout::Main::value     = "login,password|healthcheckOk"   # '|' = rows, ',' = cells on one row
ui::gridlayout::Advanced::value = "timeout"
```

1. **Tabs are the outermost structure**; their names are discovered dynamically from the root object(s), never hard-coded. Order: `Main` first, then encounter order. Layout `Checkpoint` is runtime state and is excluded (A10.3). One pane => no `TabPane`. Tabs with no leaf at all are dropped.
2. A cell naming an `OBJECT` renders as a **titled group** using **that object's own pane with the same name as the current tab** (case-insensitive); a nested object lacking that pane contributes nothing to this tab; a nested object with no gridlayout at all is an implicit single pane rendered where its parent references it.
3. A cell naming a non-existent child is skipped. Rows = `|`, columns = `,` in the written order.
4. Fallbacks for an object without gridlayout: `ui::optionsorder::value` order, then remaining children sorted (stably) by `path`.
5. **Unlisted children** (D-L1): appended to the first pane and logged, never dropped (the blueprint says "not rendered", which matches `component-form`); switchable by `FormOptions.appendUnlistedToFirstPane`.
6. Technical `$` options go to an **Advanced** pane (created if absent); `ui::hidden` fields stay in the model but are never displayed.
7. Buttons are appended once per object container, in the order guess-schema (`ui::structure::type=OUT` descendant), healthcheck (`action::healthcheck`), update (`action::update`, after the child named by `::after`).

#### 3.5.3 Conditions (`@ActiveIf`) - to implement in P8

Keys per node: `condition::if::target[::i]`, `::value[::i]` (comma separated), `::negate[::i]` (default false),
`::evaluationStrategy[::i]` (`DEFAULT` string equality; `LENGTH` size equality, `null` only matches `"0"`; `CONTAINS`
substring / list membership), combinator `condition::ifs::operator` (`AND` default, `OR`). Target path resolution
(blueprint A5): bare name = sibling, `../x` = parent-relative, `.`/`./x` = relative to self, otherwise absolute.
`result_i = negate_i != any(hit(v))`, combined by the operator; no condition = visible. **Effective visibility = own
visibility AND the parent's effective visibility** (an invisible object hides its whole subtree: the group-condition
propagation observed on the HTTP connector). The target `ui.scope` is a constant, not a real path. Evaluation works on
**concrete** paths (list elements included). Invisible nodes are not validated and do not block saving. Pipeline files
keep **all** values (lossless); a future export applies `omitInvisible` (host policy, blueprint A14.4).

#### 3.5.4 Widget selection (first match wins; implemented as `WidgetSelector` in 7a)

| Property | JavaFX control |
|---|---|
| `ui::hidden=true` | none (kept in model; never displayed by the layout) |
| `OBJECT` | titled group / tabs / columns |
| `BOOLEAN` | `CheckBox` |
| `ENUM` | restricted `ComboBox` (labels from `proposalDisplayNames`, else constants) |
| `NUMBER` | numeric `TextField` (`min`/`max` from `validation`) |
| `ARRAY` of objects | repeatable group per element (add/remove/reorder) |
| `ARRAY` of primitives | multi-value editor |
| map (`key[]` + `value[]`) | key/value rows (add/remove) |
| `STRING` + `ui::credential` | `PasswordField` |
| `STRING` + `ui::code::value` | monospaced `TextArea` |
| `STRING` + `action::suggestions` / `built_in_suggestable` | editable `ComboBox` + triggers |
| `STRING` + `action::dynamic_values` | restricted `ComboBox`, options loaded at form build |
| `STRING` + `ui::textarea=true` | `TextArea` |
| `STRING` + `ui::datetime` | `TextField` + format hint (`dateFormat`, `useSeconds`, `useUTC`); the stored format is unverified |
| `STRING` + `ui::path::value` | `TextField` + file/directory chooser button |
| other | `TextField` |

Common: title = `displayName`, `placeholder`, read-only = `ui::readonly`, required = `validation.required`, tooltip =
`documentation::value` (`documentation::tooltip=true` -> tooltip, otherwise visible help text).

#### 3.5.5 Actions (service calls) - to implement in P9

| Metadata | Trigger | Result handling |
|---|---|---|
| `action::healthcheck` (on object) | button, `type=healthcheck`, body `datastore.*` | `{status:OK}` success badge; `{status:KO, comment}` failure text |
| `action::suggestions` | `focus` and `change` (debounce 300 ms) | `items[]` `{id,label}` -> options (stored value = `id`); `cacheable` allows session reuse |
| `action::dynamic_values` | at form build, body `{}` | restricted options; cached per `(family, action, lang)` |
| `action::validation` | change/blur | `status=KO` -> field error `comment` (separate from sync errors) |
| `action::update` | button (`::after`, `::activeIf`) | response replaces the sub-tree at `options[0].path`, then re-run F4/F5 |
| `action::schema` + `ui::structure::type=OUT` | "Guess schema" button | `SchemaWire.entries[].name` populate the structure option |
| `action::built_in_suggestable` | local, no call | host-side suggestions |

Request body: for the i-th reference of `action::<type>::parameters` (default `.`), resolve its absolute path, then
`body[<paramRoot>] = value` for a primitive or `body[<paramRoot>.<sub>] = value` per leaf for an object/array, where
`<paramRoot>` comes from `ActionReference.properties` ordered by `definition::parameter::index`. Never send `$lang`.
Scheduling: monotonic `seq` per `(path, type)`, stale responses dropped; editing never blocked while pending; client
timeout 60 s.

### 3.6 Persistence formats (own files, kebab-case)

`%APPDATA%\tcketl\config.json` (Windows) / `${XDG_CONFIG_HOME:-~/.config}/tcketl/config.json`; override with
`--config <path>` or `TCKETL_CONFIG`:

```json
{
  "format-version": "1.0",
  "server": { "scheme": "http", "host": "localhost", "port": 8080, "base-path": "/api/v1" },
  "language": "en",
  "timeouts": { "connect-ms": 5000, "request-ms": 30000, "action-ms": 60000 }
}
```

Pipeline file `*.tcketl.json` (atomic write: temp file + move; `format-version` is REQUIRED, an unknown major is
refused with a clear message):

```json
{
  "format-version": "1.0",
  "name": "demo",
  "nodes": [
    { "id": "n-1", "label": "Input",
      "component": { "id": "Y29uZmlndXJhdGlvbkZvcm1TYW1wbGUjc2FtcGxlRm9ybSNJbnB1dA",
                     "family": "sampleForm", "name": "Input", "plugin": "configurationFormSample", "version": 1 },
      "position": { "x": 120.0, "y": 80.0 },
      "configuration": { "config.dataset.datastore.login": "john" } }
  ],
  "links": [ { "from": { "node": "n-1", "flow": "__default__" }, "to": { "node": "n-2", "flow": "__default__" } } ]
}
```

