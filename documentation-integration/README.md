# TCK Integration Guide - README

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`; last release tag `1.2610.0`). Code beats prose: where the Antora docs and the code disagree, this guide follows the code and records the difference in [10-appendix/known-discrepancies.md](10-appendix/known-discrepancies.md).

## 1. Purpose

This folder is a normative, self-contained specification of what a **host application** (an ETL product) must do to integrate Talend Component Kit (TCK) components. It is written to let an AI agent or a developer build, from scratch:

- a **Designer**: the design-time host that discovers components from the Component Server, renders configuration forms, runs design-time actions, stores configurations and migrates them;
- a **Runtime**: the run-time host that loads plugins, instantiates components from a flat configuration map and executes them (mapper / producer / processor / output / standalone).

It is integrator-oriented: component-authoring details appear only where they change what the host must do. It is not a component developer guide.

## 2. Audience

| Reader | Uses it to |
|---|---|
| AI code generator | Get exact names, JSON fields, endpoints, algorithms and acceptance tests as prompts/specs |
| Host developer | Look up a feature contract (MUST/SHOULD/MAY) and verify the integration level reached |
| Reviewer / QA | Run the tickable checklist and fill the integration report |

## 3. Reading order and file map

| Order | File | Content |
|---|---|---|
| 1 | [01-overview-and-architecture.md](01-overview-and-architecture.md) | Roles (Component Server, Designer, Runtime), flows, glossary, responsibility matrix |
| 2 | [02-feature-catalog/](02-feature-catalog/README.md) | One file per category (`DSG`, `CFG`, `UI`, `VAL`, `ACT`, `DAT`, `RUN`, `LCM`, `INT`, `SVC`, `HTTP`, `SRV`, `TST`), each feature with contract for Designer and Runtime; machine index [index.json](02-feature-catalog/index.json) |
| 3 | [03-component-server-api.md](03-component-server-api.md) | Every endpoint and payload (field tables, JSON examples), errors, flat-property mapping, `component-form` transformation |
| 4 | [04-data-model.md](04-data-model.md) | `Record`, `Schema`, types, `RecordPointer`, JSON/Avro mapping |
| 5 | [05-configuration-and-ui.md](05-configuration-and-ui.md) | `@Option` tree to properties to UI schema; layouts, widgets, conditions, validations |
| 6 | [06-runtime-execution.md](06-runtime-execution.md) | Lifecycle, mapper/producer/processor/output/standalone, groups, streaming, checkpoint, migration |
| 7 | [07-designer-blueprint.md](07-designer-blueprint.md) | AI-oriented spec to implement a Designer (written separately) |
| 8 | [08-runtime-blueprint.md](08-runtime-blueprint.md) | AI-oriented spec to implement a Runtime (written separately) |
| 9 | [09-integration-checklist.md](09-integration-checklist.md) | Generated tickable checklist by maturity level 0/1/2, scoring, report template; twin [09-integration-checklist.json](09-integration-checklist.json) |
| 10 | [10-appendix/](10-appendix/) | Full lists and reference tables (below) |
| - | `.work/` | Generator and validation scripts, inventory (working files, section 6) |

Appendix files (`10-appendix/`): [actions](10-appendix/actions.md), [built-in-services](10-appendix/built-in-services.md), [conditions](10-appendix/conditions.md), [configuration-types](10-appendix/configuration-types.md), [constraints](10-appendix/constraints.md), [error-codes](10-appendix/error-codes.md), [icons](10-appendix/icons.md), [known-discrepancies](10-appendix/known-discrepancies.md), [lifecycle-hooks](10-appendix/lifecycle-hooks.md), [meta-annotations](10-appendix/meta-annotations.md), [property-metadata-keys](10-appendix/property-metadata-keys.md), [runtime-configuration-keys](10-appendix/runtime-configuration-keys.md), [server-configuration](10-appendix/server-configuration.md).

Files `07` and `08` are produced independently of the files above; if a link to them does not resolve yet, the file has not been delivered.

## 4. Feature catalog, IDs, levels and tags

- **ID**: `<PREFIX>-<NNN>`, stable and never renumbered or reused. The prefix is the primary category (13 categories). Features moved between categories leave a one-line stub (`(moved) -> see ID`) that is not in `index.json`; always use the primary ID. Rules: [02-feature-catalog/README.md](02-feature-catalog/README.md).
- **Entry**: source path, kind, applies-to, description, attributes, `Contract for the Designer`, `Contract for the Runtime`, server exposure, maturity level, example.
- **Level** (exactly one per feature, cumulative):

| Level | Meaning | Omitting causes |
|---|---|---|
| 0 | Mandatory / minimal: discover, load and run correctly | wrong behaviour |
| 1 | Normal user experience | degraded but working UX |
| 2 | Best experience / all of TCK | lost optional capability |

- **Tag** (`index.json`, derived): `Designer` (only the Designer contract is non-empty), `Runtime` (only the Runtime one), `Both` (both non-empty). If both are empty the category default applies (rule in the catalog README).
- **`index.json`** fields: `id`, `name`, `category`, `level`, `designer`, `runtime`, `source`, `tag` (284 primary entries).

## 5. Conventions

- **MUST / MUST NOT** = required for correctness of the described behaviour; **SHOULD** = recommended, omit only with a reason; **MAY** = optional (RFC 2119, uppercase).
- Exact Java types, JSON fields, metadata keys, endpoint paths and verbs are in backticks. Server base path is `/api/v1`.
- `(inferred)` = deduced from code, not directly stated; `(unverified)` = not found in the repository. Both are aggregated in known-discrepancies section 3.
- Examples come from POJOs and test fixtures, not from memory. Antora pages are referenced, not copied.
- No network sources were used; anything absent from the repository is `(unverified)`.

## 6. Regenerating and validating (scripts in `.work/`)

Run from the repository root (Python 3, standard library only):

| Command | Effect |
|---|---|
| `python documentation-integration/.work/build_index.py` | Rebuilds `02-feature-catalog/index.json` from the per-category fragments `index.<PREFIX>.json` (adds `tag`) |
| `python documentation-integration/.work/build_checklist.py` | Rebuilds `09-integration-checklist.md` and `.json` from `index.json` (hand-written verification texts are in its `V` dictionary) |
| `python documentation-integration/.work/inventory.py` | Rewrites `.work/inventory.md` (coverage of API types, REST operations and payload types against catalog IDs) |
| `python documentation-integration/.work/unverified.py` | Refreshes the "Unverified items" table in `known-discrepancies.md` |
| `python documentation-integration/.work/validate.py` | Consistency checks (JSON parse, IDs, links, counts) |

Order after any catalog edit: edit fragment and markdown, then `build_index.py`, `build_checklist.py`, `inventory.py`, `validate.py`.

## 7. How an AI should use this guide

Feed files in the order below; each step adds only what the previous ones do not contain. Keep the tables and IDs verbatim in the prompt; drop prose sections if the context budget is small.

**Build a Designer**

1. [01-overview-and-architecture.md](01-overview-and-architecture.md) (roles, glossary).
2. [07-designer-blueprint.md](07-designer-blueprint.md) (decomposition, algorithms, acceptance tests).
3. [03-component-server-api.md](03-component-server-api.md) and [05-configuration-and-ui.md](05-configuration-and-ui.md) (payloads, flat-property mapping, form generation).
4. Catalog entries with `tag` in `Designer`/`Both` (filter `index.json` on `tag` and `level`; start with level 0), categories `SRV`, `CFG`, `UI`, `VAL`, `ACT`, `DSG` first; [04-data-model.md](04-data-model.md) for schemas.
5. Appendix: [property-metadata-keys](10-appendix/property-metadata-keys.md), [actions](10-appendix/actions.md), [conditions](10-appendix/conditions.md), [constraints](10-appendix/constraints.md), [error-codes](10-appendix/error-codes.md), [icons](10-appendix/icons.md).
6. [09-integration-checklist.md](09-integration-checklist.md) (Designer lines) to self-verify and report.

**Build a Runtime**

1. [01-overview-and-architecture.md](01-overview-and-architecture.md).
2. [08-runtime-blueprint.md](08-runtime-blueprint.md).
3. [06-runtime-execution.md](06-runtime-execution.md) and [04-data-model.md](04-data-model.md).
4. Catalog entries with `tag` in `Runtime`/`Both`: `RUN`, `LCM`, `SVC`, `HTTP`, `DAT`, `INT`, `CFG` (flat map syntax).
5. Appendix: [built-in-services](10-appendix/built-in-services.md), [lifecycle-hooks](10-appendix/lifecycle-hooks.md), [runtime-configuration-keys](10-appendix/runtime-configuration-keys.md), [meta-annotations](10-appendix/meta-annotations.md).
6. [09-integration-checklist.md](09-integration-checklist.md) (Runtime lines).

Always add [known-discrepancies.md](10-appendix/known-discrepancies.md) section 1 (six confirmed facts) to the context: they correct commonly assumed behaviour (no `ETag`, no default authentication, `GET /cache/clear` mutates, `$maxDurationMs`, version as argument, `talend.checkpoint.enabled`).

## 8. Scope and known limits

- Covered: the public API of `component-api` / `component-spi`, the REST API and payloads of `component-server-parent`, the runtime behaviour of `component-runtime-manager` / `component-runtime-impl`, and the testing helpers an integrator can reuse.
- Out of scope: writing components, Antora rendering, framework build, Beam/Studio/Singer details beyond what a host must know (see the relevant catalog entries).
- The Component Server has **no ETag/conditional-request support** (caching is client-side), **no authentication by default**, and `GET /api/v1/cache/clear` **mutates state**. See [known-discrepancies.md](10-appendix/known-discrepancies.md) (C1, C4, C5) and the "Server-side prerequisites" section of the checklist.
- Items marked `(unverified)` or `(inferred)` are listed in known-discrepancies section 3; treat them as needing a test in the target environment.
- Checklist "How to verify" texts for level 0 are hand-written; for levels 1 and 2 many are generated from the first MUST clause of the contract and describe the observable check in general terms.
- The `-SNAPSHOT` version is a development iteration after `1.2610.0`; behaviour may differ in released versions.
