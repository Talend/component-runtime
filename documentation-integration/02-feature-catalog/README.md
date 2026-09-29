# Feature catalog - how it is organised

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`; last release tag `1.2610.0`).
> This folder is the single source of truth for feature IDs. Every other document (`03`..`10`, blueprints, checklist) references these IDs; the checklist is generated from [`index.json`](index.json).

## 1. Categories

Each API feature has exactly **one primary entry** in exactly one category. Files are `<PREFIX>-<name>.md` with a machine-readable twin `index.<PREFIX>.json`.

| Prefix | Category | File | Fragment | Typical content |
|---|---|---|---|---|
| `DSG` | Design (component model and metadata) | [DSG-design.md](DSG-design.md) | [index.DSG.json](index.DSG.json) | `@Components`, `@Icon`, `@Metadatas`, `@Documentation`, i18n, `@Internal`, `@Partial`, metadata enrichers |
| `CFG` | Configuration model | [CFG-configuration.md](CFG-configuration.md) | [index.CFG.json](index.CFG.json) | `@Option`, datastore/dataset types, defaults, path syntax, technical `$` options |
| `UI` | UI / forms | [UI-ui.md](UI-ui.md) | [index.UI.json](index.UI.json) | layouts, widgets, `@ActiveIf`, UiSchema generation |
| `VAL` | Validation | [VAL-validation.md](VAL-validation.md) | [index.VAL.json](index.VAL.json) | constraints, `PropertyValidation`, `@AsyncValidation`, runtime validation |
| `ACT` | Design-time actions | [ACT-actions.md](ACT-actions.md) | [index.ACT.json](index.ACT.json) | `@ActionType` and every action annotation, `@Suggestable` family, trigger wiring |
| `DAT` | Data model | [DAT-data-model.md](DAT-data-model.md) | [index.DAT.json](index.DAT.json) | `Record`, `Schema`, builders, `RecordPointer`, JSON/Avro mapping |
| `RUN` | Runtime execution | [RUN-runtime.md](RUN-runtime.md) | [index.RUN.json](index.RUN.json) | `@Emitter`, `@PartitionMapper`, `@Processor`, groups, branches, streaming, checkpoint, engines |
| `LCM` | Lifecycle and evolution | [LCM-lifecycle.md](LCM-lifecycle.md) | [index.LCM.json](index.LCM.json) | `@Version`, migration, plugin container, classloader isolation, dependency resolution, `.car` |
| `INT` | Interceptors and cross-cutting | [INT-interceptors.md](INT-interceptors.md) | [index.INT.json](index.INT.json) | `@Intercepts`, `InterceptorHandler`, `ComponentException`, `DiscoverSchemaException` |
| `SVC` | Services (host-provided injection) and SPI | [SVC-services.md](SVC-services.md) | [index.SVC.json](index.SVC.json) | `@Service`, `LocalConfiguration`, `Injector`, caches, `Resolver`, `ObjectFactory`, `RuntimeContext`, `ComponentExtension` SPI |
| `HTTP` | Declarative HTTP client service | [HTTP-http-client.md](HTTP-http-client.md) | [index.HTTP.json](index.HTTP.json) | `@Request`, `HttpClient`, codecs, configurers, OAuth1, `Response`, `HttpException` |
| `SRV` | Component server capabilities | [SRV-server.md](SRV-server.md) | [index.SRV.json](index.SRV.json) | endpoints, caching, i18n, security, bulk, environment, error payload, server config |
| `TST` | Testing support (informative) | [TST-testing.md](TST-testing.md) | [index.TST.json](index.TST.json) | JUnit 4/5 rules, HTTP mocking, environments, fixtures |

No extra category was added: every feature fits one of the thirteen categories of the brief.

## 2. ID scheme

* Format `<PREFIX>-<NNN>` (three digits), sequential per category, e.g. `RUN-012`.
* **IDs are stable**: never renumbered, never reused. Gaps exist only where an entry became a stub (section 5).
* A new feature takes the next free number of its category (fragment + markdown + `index.json` updated together).
* Headings follow `### <ID> <name>`; the anchor is the GitHub slug of the whole heading.

## 3. Maturity levels

Levels are cumulative and each entry has exactly one (the lowest at which omitting it causes the described problem). Details and pass criteria: `../09-integration-checklist.md` (written after the catalog).

| Level | Meaning | Omitting the feature causes |
|---|---|---|
| 0 | Mandatory / minimal: discover, load and run components correctly | wrong behaviour |
| 1 | Normal user experience: forms, i18n, health check, suggestions, schema discovery, migration, batch groups, branches, client-side caching | degraded but working UX |
| 2 | Best experience / full TCK: updates, async validation, dataset discovery, dynamic dependencies, checkpoint/streaming, pointers, interceptors, bulk, environment, `.car`, Studio bridges, testing helpers | lost optional capability |

## 4. Entry template and `tag`

Every entry follows the template of the brief: Source, Category / Sub-area, Kind, Applies to, Description, Attributes, Contract for the Designer, Contract for the Runtime, Server exposure, Maturity level, Example.

`tag` (added in `index.json`, not present in the fragments) tells which host must act on the feature. Rule, applied by `.work/build_index.py`:

1. A contract counts as **empty** when its text is `none` (optionally followed by a short parenthetical, e.g. `none (consumes the resulting keys)`).
2. Designer contract non-empty and Runtime contract empty -> `Designer`.
3. Runtime contract non-empty and Designer contract empty -> `Runtime`.
4. Both non-empty -> `Both`.
5. Both empty (`DSG-009`, `VAL-013`, `RUN-007`, `RUN-020`, `SRV-021` and the informative `TST-*` entries): judgment by category default - `DSG`/`CFG`/`UI`/`VAL`/`ACT` -> `Designer`; `RUN`/`LCM`/`INT`/`SVC`/`HTTP` -> `Runtime`; `DAT`/`SRV`/`TST` -> `Both` (data and server concepts and test tooling are shared by both hosts).

## 5. Stub convention (deduplication)

When the same API type was catalogued in two categories, one entry is the **primary**; the other becomes a one-line stub and is removed from its `index.<PREFIX>.json`:

```markdown
### LCM-014 (moved) -> see SVC-013
```

* Stubs keep the ID reserved (IDs are stable) but carry no content and are **not** in any JSON file.
* All references anywhere in `documentation-integration/` point to the primary ID.
* Ownership rule used to pick the primary: action annotations -> `ACT`; record/schema/data types -> `DAT`; component lifecycle, migration, classloader/plugin loading -> `LCM`; host-injected services and SPI -> `SVC`; component execution kinds -> `RUN`; validation -> `VAL`; metadata / design-time declarations and i18n -> `DSG`; endpoints -> `SRV`. When two entries covered the same type the richer one is primary, text missing from it was merged in (marked `Merged from <ID>`), and the lowest sensible maturity level was kept.

Moved entries (stub -> primary):

| Stub | Primary | Stub | Primary | Stub | Primary |
|---|---|---|---|---|---|
| `RUN-051` | `SVC-003` | `SVC-010` | `DAT-017` | `SVC-011` | `DAT-020` |
| `LCM-014` | `SVC-013` | `LCM-015` | `SVC-014` | `LCM-016` | `SVC-015` |
| `INT-014` | `SVC-019` | `INT-015` | `SVC-020` | `LCM-017` | `SVC-021` |
| `LCM-019` | `SVC-026` | `LCM-012` | `SVC-027` | `INT-004` | `SVC-009` |
| `LCM-020` | `SVC-005` | `SVC-004` | `ACT-002` | `DAT-022` | `ACT-007` |
| `DAT-023` | `ACT-008` | `DAT-027` | `ACT-009` | `DAT-028` | `ACT-009` |
| `LCM-013` | `ACT-010` | `INT-010` | `ACT-011` | `INT-011` | `ACT-012` |
| `INT-012` | `ACT-012` | `INT-013` | `ACT-012` | `DAT-025` | `ACT-013` |
| `RUN-035` | `ACT-014` | `DSG-004` | `LCM-001` | `LCM-021` | `DSG-006` |
| `DAT-026` | `DSG-014` | `ACT-027` | `DSG-013` | `TST-018` | `RUN-042` |
| `INT-016` | `VAL-012` | `ACT-022` | `SRV-013` | `ACT-023` | `SRV-012` |

## 6. How `index.json` is built

* `index.<PREFIX>.json` (fragments) are the editable data; keep them in sync with the markdown headings (one fragment object per non-stub `### ` heading).
* [`index.json`](index.json) is the concatenation of all fragments, sorted by prefix order `DSG, CFG, UI, VAL, ACT, DAT, RUN, LCM, INT, SVC, HTTP, SRV, TST` and then by number, with the extra `tag` field.
* Fields: `id`, `name`, `category` (the prefix), `level` (0, 1 or 2), `designer`, `runtime`, `source`, `tag`.
* Regenerate with `python ../.work/build_index.py`; verify everything (counts, links, IDs, JSON) with `python ../.work/validate.py`. Coverage of the API types and REST operations is in `../.work/inventory.md` (`python ../.work/inventory.py`).
* The server endpoint labels `EP-*` of [`../03-component-server-api.md`](../03-component-server-api.md) map to `SRV-*` in the table at the top of [SRV-server.md](SRV-server.md#ep-label-to-srv-id-mapping).

## 7. Counts (284 primary entries, 33 stubs)

| Prefix | Level 0 | Level 1 | Level 2 | Total | Tag Designer | Tag Runtime | Tag Both | Stubs |
|---|---|---|---|---|---|---|---|---|
| `DSG` | 1 | 7 | 6 | 14 | 9 | 1 | 4 | 1 |
| `CFG` | 4 | 8 | 7 | 19 | 5 | 1 | 13 | 0 |
| `UI` | 0 | 14 | 8 | 22 | 18 | 0 | 4 | 0 |
| `VAL` | 4 | 5 | 4 | 13 | 4 | 0 | 9 | 0 |
| `ACT` | 0 | 15 | 9 | 24 | 18 | 0 | 6 | 3 |
| `DAT` | 11 | 10 | 9 | 30 | 2 | 16 | 12 | 6 |
| `RUN` | 22 | 7 | 20 | 49 | 3 | 26 | 20 | 2 |
| `LCM` | 7 | 2 | 3 | 12 | 0 | 10 | 2 | 9 |
| `INT` | 2 | 1 | 5 | 8 | 2 | 4 | 2 | 8 |
| `SVC` | 6 | 6 | 12 | 24 | 0 | 22 | 2 | 3 |
| `HTTP` | 21 | 3 | 2 | 26 | 0 | 25 | 1 | 0 |
| `SRV` | 4 | 12 | 9 | 25 | 15 | 0 | 10 | 0 |
| `TST` | 0 | 0 | 18 | 18 | 1 | 3 | 14 | 1 |
| **Total** | **82** | **90** | **112** | **284** | **77** | **108** | **99** | **33** |

Related: [known discrepancies](../10-appendix/known-discrepancies.md), [overview](../01-overview-and-architecture.md).
