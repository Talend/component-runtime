# CLAUDE.md — Task: generate the "TCK Integration Guide" documentation

This repository is the Talend Component Kit (TCK) framework. **The only job of this
CLAUDE.md is to drive the generation of a new documentation set.** Do not modify framework
code. Output goes to `documentation-integration/` (create it; plain Markdown, no Antora).

## Goal

Produce a documentation that an **AI (or a developer) can read to build, from scratch, a fully
functional ETL Designer (design-time host) and a fully functional ETL Runtime (run-time host)**
that integrate TCK components. It must therefore be:

- **Exhaustive**: every feature exposed by the framework API is listed, none invented.
- **Normative and precise**: state MUST / SHOULD / MAY, exact names, exact JSON field names,
  exact annotation attributes, exact endpoint paths and HTTP verbs.
- **Self-contained and machine-friendly**: consistent headings, tables, stable feature IDs,
  JSON examples, no marketing prose, no "see the docs" without inlining the essential.
- **Integrator-oriented**: written for the *host application* (the ETL), not for a component
  developer. Component-authoring details appear only as far as the host must understand their
  effect (e.g. what `@Emitter` means for the runtime scheduler).

## Sources of truth (read in this order; code beats prose when they disagree)

1. **Feature definitions — `component-api/src/main/java/org/talend/sdk/component/api/`**
   Sub-packages: `base`, `component`, `configuration` (+ `action`, `condition`, `constraint`,
   `type`, `ui`, `ui.layout`, `ui.widget`), `context`, `exception`, `input`,
   `internationalization`, `meta`, `processor`, `record`, `service` (+ `action`, `asyncvalidation`,
   `cache`, `completion`, `configuration`, `connection`, `dependency`, `discovery`, `factory`,
   `healthcheck`, `http`, `injector`, `interceptor`, `output`, `record`, `schema`, `update`),
   `standalone`. Read every annotation/interface: attributes, javadoc, constraints.
   Also read `component-spi` and `component-runtime-impl` where behaviour is not evident from API.
2. **Framework docs — `documentation/src/main/antora/modules/ROOT/pages/*.adoc`** (and
   `_partials/generated_*.adoc`, which are the generated lists of actions, conditions,
   constraints, configuration types, icons, JUnit environments). Key pages:
   `tck-framework-overview`, `framework_design`, `component-registering`, `component-loading`,
   `component-execution`, `component-configuration`, `component-record`, `record-types`,
   `component-define-input|processor-output|standalone`, `component-partition-mapper`,
   `component-producer`, `component-processor`, `component-output`, `component-combiner`,
   `component-checkpoint`, `component-implementing-streaming`,
   `component-versions-and-migration`, `component-internationalization`,
   `creating-dataset-datastore`, `services-*`, `ref-actions`, `ref-conditions`,
   `ref-constraints`, `ref-configuration-types`, `ref-ui`, `ref-rest-resources`,
   `ref-server-configuration`, `documentation-rest`, `rest-openapi`, `icons`,
   `concept-processor-and-batch-processing`, `compatibility`, `studio*`, `singer`,
   `running-on-remote-engine`, `changelog`.
3. **Server payloads — `component-server-parent/`**:
   - `component-server-api` — JAX-RS interfaces (`ComponentResource`, `ActionResource`,
     `ConfigurationTypeResource`, `DocumentationResource`, `EnvironmentResource`,
     `BulkReadResource`, `CacheResource`): paths, verbs, params, return types.
   - `component-server-model` — the **payload POJOs** (`ComponentIndex(es)`, `ComponentDetail`,
     `ComponentDetailList`, `SimplePropertyDefinition`, `ConfigTypeNode(s)`, `ActionItem`,
     `ActionList`, `Dependencies`, `DependencyDefinition`, `Environment`, `ApiVersion`, `Link`,
     `Bulk*`, …). Field-by-field.
   - `component-server` — real behaviour (sorting, i18n, caching, security, filtering,
     server-side JCache caching; note the server has NO `ETag`/`If-None-Match` support).
     Also the OpenAPI/`ref-rest-resources`.
   - `component-form` — the **UI-schema/JSON-schema generation** from
     `SimplePropertyDefinition` (used to render forms) and action-trigger handling.
4. **Runtime side**: `component-runtime-manager` (plugin/classloader container, `ComponentManager`,
   `Lifecycle`, `Iterator`/`Processor` wrappers, DI, `ConfigurationMapper`),
   `component-runtime-impl` (record impl, JSON/Avro codecs, `Mapper`/`Input`/`Processor`
   wrappers, groups, serialization), `component-runtime-beam` (Beam translation, reference
   runner design), `component-runtime-design-extension`, `component-tools` (CLI
   `deployment-cli`, `web`, validation), `container` (classloader isolation, `.car` /
   `talend-component-maven-plugin` packaging), `singer-parent`, `vault-client`,
   `component-studio`, `remote-engine-customizer`.
5. **Tests as executable examples**: use them to obtain *real* JSON payload samples
   (`component-server*/src/test`, `component-form`, `component-runtime-manager/src/test`).

Never quote a payload from memory. Build examples from the POJOs and, when possible, from
recorded test fixtures. Mark any inference explicitly as `(inferred)`.

## Required output structure

Create these files (keep each focused; cross-link with relative links and feature IDs):

```
documentation-integration/
  README.md                       # purpose, how to read, index, conventions, version covered
  01-overview-and-architecture.md # roles: Designer vs Runtime vs Component Server; flows; glossary
  02-feature-catalog/             # ONE file per category (see "Categories")
  03-component-server-api.md      # every endpoint + every payload (see "Server payloads")
  04-data-model.md                # Record, Schema, types, RecordPointer, JSON/Avro mapping
  05-configuration-and-ui.md      # @Option tree -> properties -> UI schema; layouts, widgets, conditions, validations
  06-runtime-execution.md         # lifecycle, mapper/producer/processor/output/standalone, groups, streaming, checkpoint, migration
  07-designer-blueprint.md        # AI-oriented spec to implement an ETL Designer
  08-runtime-blueprint.md         # AI-oriented spec to implement an ETL Runtime
  09-integration-checklist.md     # maturity levels 0/1/2 (see "Checklist")
  10-appendix/                    # full lists: actions, conditions, constraints, config types, meta-annotations, icons, error codes, server config keys
```

## Categories (file per category in `02-feature-catalog/`)

Group every API feature into one of these (add a category only if something does not fit;
say why). Each feature belongs to exactly **one primary category** and may be cross-referenced.

| ID prefix | Category | Typical content |
|---|---|---|
| `DSG-` | Design (component model & metadata) | `@Components`, `@Icon`, `@Version`, `@Metadatas`, `@Documentation`, families, categories, i18n, `Internationalized`, partial/internal meta |
| `CFG-` | Configuration model | `@Option`, `@ConfigurationType` (datastore/dataset/…), `@DataStore`, `@DataSet`, `@DefaultValue`, `@Hidden`, `@OptionsOrder`, nested/list/map options, path & `$` conventions, `ConfigurationMapper` |
| `UI-`  | UI / forms | layouts (`GridLayout`, `AutoLayout`, `Horizontal/VerticalLayout`), widgets (`TextArea`, `Code`, `Credential`, `DateTime`, `ModuleList`, `Path`, `Structure`, `BasedOnSchema`, `ReadOnly`), `@ActiveIf(s)`, `@Ui` |
| `VAL-` | Validation | constraints (`@Required`, `@Min`, `@Max`, `@Pattern`, `@Uniques`…), validations, `@AsyncValidation`, `ValidationResult` |
| `ACT-` | Design-time actions | `@Action` + all `ActionType`s (healthcheck, suggestions, dynamic_values, update, schema discovery, close_connection, create_connection, discover dataset, dynamic dependencies, user…), `@Suggestable`, `@Proposable`, `@Updatable`, `@Validable`, `@Checkable`, `@BuiltInSuggestable`, `@ActionRef` |
| `DAT-` | Data model | `Record`, `Schema`, `Schema.Type`, `RecordBuilderFactory`, `RecordVisitor`, `RecordPointer`, `SchemaProperty`, `DatabaseSchemaMapping`, `FixedSchema`, `DiscoverSchema(Extended)` |
| `RUN-` | Runtime execution | `@Emitter`, `@PartitionMapper` (`@Assessor`,`@Split`,`@PartitionSize`), `@Producer`, `@Processor`, `@ElementListener`, `@BeforeGroup`/`@AfterGroup`/`@LastGroup`, `@Input`/`@Output`, `OutputEmitter`, `MultiOutputIterator`, `@DriverRunner`/`@RunAtDriver` (standalone), `@Checkpoint*`, streaming, lifecycle (`@PostConstruct`/`@PreDestroy`), `AvailableOutputFlows`, `@ConditionalOutput`, `@ReturnVariables`/`@AfterVariables` |
| `SVC-` | Services (host-provided injection) | `@Service`, `LocalConfiguration`, `Injector`, `Cached`/`LocalCache`, `RecordService`, `ProducerFinder`, `Resolver`/dynamic dependencies, `ObjectFactory`, `RuntimeContext`, health-check service |
| `HTTP-`| HTTP client service | `@Request`, `@HttpClient`, `HttpClientFactory`, `Codec`/`Encoder`/`Decoder`, `Configurer`, OAuth1, headers/query/path, `Response`, `HttpException` |
| `LCM-` | Lifecycle & evolution | `@Version`, `MigrationHandler`, deprecation, `.car`/plugin packaging, classloader isolation, `component-loading`, dependency resolution, multi-version |
| `INT-` | Interceptors & cross-cutting | `@Intercepts`, `InterceptorHandler`, `@CloseConnection`, `ComponentException`, `DiscoverSchemaException`, error handling |
| `SRV-` | Component server capabilities | endpoints, caching, i18n, security/filtering, bulk, environment, documentation rendering, server configuration keys |
| `TST-` | Testing support (informative) | `component-runtime-testing` JUnit4/5 rules, `component-runtime-manager` test utils — only what an integrator can reuse to verify integration |

### Feature entry template (mandatory, one per feature)

```markdown
### <ID> <Name>            e.g. `RUN-012 @PartitionMapper`
- **Source**: <path to API class> (+ doc page)
- **Category / Sub-area**:
- **Kind**: annotation | interface | REST endpoint | payload field | convention
- **Applies to**: component types (input/processor/output/standalone/configuration/service)
- **Description**: what it means, 1–3 sentences, from the host's viewpoint.
- **Attributes**: table (name, type, default, meaning).
- **Contract for the Designer**: what the ETL designer MUST/SHOULD/MAY do with it (or "none").
- **Contract for the Runtime**: what the ETL runtime MUST/SHOULD/MAY do with it (or "none").
- **Server exposure**: which endpoint/field carries it (property metadata key, `meta.*`, action, …).
- **Maturity level**: 0 | 1 | 2  (see Checklist rules)
- **Example**: minimal JSON or Java snippet.
```

Feature IDs are **stable and sequential per category**; never renumber once assigned. Produce a
machine-readable index `02-feature-catalog/index.json` (`id`, `name`, `category`, `level`,
`designer`, `runtime`, `source`) — the checklist is generated from it, so both must agree.

## Server payloads (`03-component-server-api.md`)

For **every endpoint** in `component-server-api` document:

- Method + path (with the `/api/v1` base and any configured prefix), query/path/header params,
  request body schema, response codes, cache behaviour (the server has no `ETag`; document what
  really exists), security/roles.
- The **response payload as a field-by-field table** (name, type, nullable, meaning, who uses
  it: designer/runtime) plus a **full realistic JSON example** derived from the model classes.
- Must cover at least: component index (`/component/index`, incl. `includeIconContent`,
  `language`, `q` query filtering), component detail (`/component/details`, by ids, with
  `SimplePropertyDefinition` list: `path`, `name`, `displayName`, `type`, `defaultValue`,
  `metadata` map — document **every metadata key** such as `ui::gridlayout::*`,
  `ui::optionsorder::value`, `action::*`, `condition::if::*`, `validation::*`,
  `configurationtype::*`, `dataset`/`datastore` references, `documentation::value`,
  `definition::parameter::index`, …; enumerate them from the code, do not guess), component
  migration (`/component/migrate/{id}/{configurationVersion}`), icons (`/component/icon/...`),
  dependencies (`/component/dependencies`, `/component/dependency/{id}`; `.car`/jar streaming),
  documentation (`/documentation/component/{id}`), configuration type index/detail
  (`/configurationtype/index`, `/details`, migrate), actions (`/action/index`,
  `/action/execute?family&type&action&lang` with the free-form body and per-`ActionType`
  result shapes: `HealthCheckStatus`, `Values`, `SuggestionValues`, `Schema`, `ValidationResult`,
  update results, discover dataset…), environment (`/environment`), bulk (`/bulk`), cache
  (`/cache/clear`), plus any `/api/v1` version/`ApiVersion` info.
- Error payload format (`ErrorPayload`, error codes enum, HTTP status mapping).
- A **sequence diagram (mermaid)** for the canonical designer flow: list components → get
  details → render form → run actions (healthcheck, suggestions, update, schema discovery) →
  save configuration → migrate on version change.
- A **flat-properties ↔ nested-configuration** section: how the host serializes form values
  back into the `configuration.<path>=value` map the runtime expects (prefix rules, list
  indexes `[0]`, map keys, `$maxDurationSeconds`-style technical options, `$checkpoint`, …
  — confirm each in `component-runtime-manager`/`component-runtime-impl`).
- Document the **`component-form`** transformation (SimplePropertyDefinition → JSON schema +
  UI schema) with a complete before/after example, since a designer needs it to render forms.

## Blueprints (`07`, `08`) — written for AI code generation

Each blueprint MUST contain: scope & non-goals; module/component decomposition; data
structures (as language-neutral pseudo-schemas); algorithms in numbered steps (e.g. "resolve
`@Option` path → widget"; "translate a mapper into N parallel readers"); state machines
(component lifecycle, group handling `BeforeGroup → element* → AfterGroup`); error handling;
threading/serialization requirements; pluggability points; and an explicit
"feature → module" traceability table referencing catalog IDs. Include acceptance tests
(given/when/then) for the main flows. Designer blueprint additionally: palette, canvas,
form renderer, action trigger wiring (debounce/on-change/on-blur semantics from
`ref-ui`/`ref-actions`), schema propagation between components, dataset/datastore reuse,
migration on load. Runtime blueprint additionally: plugin loading/classloader isolation,
DI of services, mapper splitting, record flow/branching (named outputs `__default__`,
`REJECT`…), batch grouping, streaming/checkpoint, standalone execution, engine adapters
(Beam is the reference).

## Checklist (`09-integration-checklist.md`)

A tickable Markdown checklist (`- [ ] **ID** …`) grouped by **maturity level**, each item
linked to its catalog ID and tagged `[Designer]`, `[Runtime]` or `[Both]`, with a one-line
"how to verify" (observable test). Levels are cumulative:

- **Level 0 — Mandatory / minimal**: the host can discover components from the server, load
  and run them correctly. Only what is required for correctness (component index/details,
  configuration serialization and defaults, plugin loading & classloader isolation, mapper/
  producer/processor/output/standalone lifecycle, Record/Schema handling, required
  validations/constraints, `@Version` value passed through, mandatory services injection,
  error propagation).
- **Level 1 — Normal user experience**: usable product. Adds form rendering with layouts and
  common widgets, display names/i18n, icons, documentation display, conditions (`@ActiveIf`),
  standard validations, health check, suggestions/dynamic values, schema discovery, dataset/
  datastore reuse, configuration migration, batch groups, multi-output/reject flows,
  client-side caching (the server has no `ETag` support), design-time/run-time connection handling.
- **Level 2 — Best experience / full TCK**: everything else — `@Updatable` (update actions),
  async validation, discover-dataset, dynamic dependencies, checkpoint/streaming, record
  pointers, advanced widgets, interceptors, bulk endpoint, environment/feature negotiation,
  API-version negotiation, remote engine/`.car` deployment, Studio/Singer bridges where
  applicable, full i18n, accessibility of documentation, monitoring hooks.

Rules: every catalog feature is assigned **exactly one** level with a justification line
(default to the *lowest* level at which omitting it causes wrong behaviour (0), a degraded but
working UX (1), or a lost optional capability (2)). Provide at the top a **scoring table**
(items per level, pass criteria: a host "is Level N" when 100% of its `[Designer]`/`[Runtime]`
items for levels ≤ N pass) and separate Designer / Runtime / Both totals.

## Workflow to follow when generating

1. Inventory: list every public type in `component-api` and every REST/model class in
   `component-server-parent`; write the inventory to `documentation-integration/.work/inventory.md`
   (working file, gitignored is fine). Nothing may be missing from the final catalog:
   finish with a **coverage check** comparing inventory vs. `index.json` and fix gaps.
2. Read the framework docs listed above; note discrepancies between docs and code (record them
   in `10-appendix/known-discrepancies.md`).
3. Write the catalog category by category, then server API, data model, config/UI, runtime.
4. Write the blueprints, then generate the checklist **from `index.json`**.
5. Validate: every feature ID referenced anywhere exists; every endpoint in
   `component-server-api` is documented; every JSON example parses; every relative link
   resolves; no TODO left. Report anything you could not verify.

Parallelize with subagents where it helps (one per category / per resource group), but a
single agent must own final consistency of IDs, `index.json`, and the checklist.

## Style rules

- English, Markdown, GitHub-flavoured; tables for attribute lists; fenced code with language.
- Use RFC 2119 keywords in uppercase for obligations.
- One concept per section, short paragraphs, no filler; define each term once in the glossary.
- Always name the exact Java type / JSON field / metadata key in backticks.
- State the framework version documented (read it from the root `pom.xml`) in every file header.
- Do not copy large blocks of the Antora docs; synthesize, and link the original page path.
- Never fabricate: unknown → `(unverified)` plus where you looked.

## Repository housekeeping

- Java 8+ multi-module Maven build (`mvn -q -DskipTests`); **this task needs no build**, only
  reading. Do not run the full build unless needed to extract a payload example (prefer
  reading tests).
- **Read freely, exhaustively.** You may read any file in the repository (all documentation,
  `component-api`, `component-server-parent`, runtime modules, tests, poms). Being exhaustive
  matters more than saving reads; the source lists above are a starting point, not a limit.
- **No network access.** Do not use WebFetch/WebSearch, `curl`, or any online source. All
  existing documentation is local in `documentation/src/main/antora/modules/ROOT/pages`
  (plus the code); anything not found there or in the code is `(unverified)`, never fetched.
- Do not edit anything outside `documentation-integration/`. Do not commit; leave changes in
  the working tree for review.
