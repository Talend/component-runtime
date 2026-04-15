---
agent: agent
model: claude-sonnet-4-6
tools:
  - codebase
  - editFiles
  - runCommands
  - terminalLastCommand
  - githubRepo
  - github/*
  - jira/*
description: >
  Current prompt version: v0.11
  Quality review and REVIEW.md maintenance for TDI modules (connectors, libraries, and framework modules).
  Invoke explicitly by referencing this file. Requires GitHub MCP and Jira MCP.
---

# Module Quality Review

<!-- ============================================================ -->
<!-- QUICK START — for human readers (not parsed by the AI agent) -->
<!-- ============================================================ -->

> **Quick Start — Human Reference**
>
> **What this does**: Runs a full quality review of a TDI module (connector, library, or framework module) and creates or updates a `REVIEW.md` file in the target module folder. The review covers architecture, features, test coverage (unit, integration, E2E/TUJ), Jira traceability, and quality warnings.
>
> **Requirements**: GitHub MCP · Jira MCP (both must be configured and running)
>
> **How to invoke**
>
> *VS Code — Copilot Chat (Agent mode)*: attach this file with `#review-module`, then type:
> ```
> <MODULE_PATH>
> ```
> *IntelliJ — AI Assistant*: paste the **Command** section from this file into the chat with `<MODULE_PATH>` replaced.
>
> **Input**: `<MODULE_PATH>` — path to the module folder, relative to the repository root (e.g. `kafka`, `http`, `aws-s3`)
>
> **Output**: `<MODULE_PATH>/REVIEW.md` — created from scratch, or incrementally updated if it already exists
>
> **To publish the review to di-documentation**, use the companion prompt `#document-module` instead.
>
> **How to add reviewer notes to a `REVIEW.md`**
>
> Write directly in the `## Reviewer Notes` section using plain markdown. Reference specific items by their stable IDs, visible in the leftmost column of each table:
>
> | What you're referencing | ID format |
> |---|---|
> | A feature row | `F-001`, `F-002`, … |
> | A failure scenario row | `FM-001`, `FM-002`, … |
> | A quality warning item | `W-001`, `W-002`, … |
> | A Jira ticket | `QTDI-1234` |
>
> Example:
> ```markdown
> **jdoe (2026-04-07)**: F-003 — bulk read not yet exposed, tracked in QTDI-9876.
> **alice (2026-04-07)**: FM-002 — retry was removed intentionally in 8.0.
> ```
>
> The agent **never rewrites or removes the `## Reviewer Notes` section**. If an ID you reference is later modified or retired, a warning will appear in `## Quality Warnings > Stale Note References`.

---

> **Scope**: This prompt applies **only** to the module folder given as input. No file outside that folder may be created or modified, except when explicitly running the `document-module` command (which targets the `Talend/di-documentation` repository).
>
> **Never commit or push changes automatically.** All file modifications must remain local only. Do not run `git add`, `git commit`, `git push`, or any equivalent command.
>
> **Always prefer locally available files.** When a file exists in the local workspace, read it directly instead of fetching it from a remote source (GitHub MCP, web, etc.). Only fall back to remote fetching when the file is not present locally.
>
> The main prompt is stored in connectivity-tools and works on all module types across repositories (connectors, libraries, and framework modules), but it may be duplicated in each repository for easier access. Always check the `Family` or `Type` field in the header to confirm the review is for the correct module type.
> When starting the review, the prompt will check if a newer version is available in connectivity-tools. If one is found, it will update the local file and stop — you will need to re-invoke the prompt to continue with the updated version.

---

## Prerequisites

### Required MCP Servers

Two MCP servers must be available before proceeding:

| MCP Server   | Purpose                                                                                       |
| ------------ | --------------------------------------------------------------------------------------------- |
| **GitHub**   | Read cross-repository content (Talend/tuj, Talend/di-documentation), create branches and PRs |
| **Jira**     | Fetch ticket details from commit history to reconcile review features                         |

Before starting, confirm both servers are responsive. If either is unavailable, stop and notify the user.

### Check prompt version
**Only after confirming both MCP servers are available**, check if a newer version of this prompt exists in `Talend/connectivity-tools` at `.github/prompts/review-module.prompt.md` (main branch). Compare the `Current prompt version` field to the one in this loaded file. If the remote version is newer, write the remote content to the local file on disk and **stop** — display a message telling the user a newer version is available and they should re-invoke the prompt. If versions match, continue without interruption.

---

## REVIEW.md Output Contract

All generated `REVIEW.md` files must conform to the following rules regardless of module type and regardless of whether the file is being created or updated. Failures to conform are generation errors — the pre-write consistency gate (Goal 7) enforces them before any file is written.

### Module types

| Type                 | Detection criteria                                                                                           |
| -------------------- | ------------------------------------------------------------------------------------------------------------ |
| **Connector**        | `package-info.java` exists anywhere under the module's `src/main/java/` tree                                 |
| **Framework module** | The artifact ID equals `component-runtime`, OR `src/main/resources/META-INF/services/` contains any of the following SPI files: `org.talend.sdk.component.runtime.manager.ComponentManager$Customizer`, `org.talend.sdk.component.runtime.manager.spi.ContainerListenerExtension`, `org.talend.sdk.component.spi.component.ComponentExtension`, `org.talend.components.extension.register.api.CustomComponentExtension` |
| **Library**          | None of the above criteria match                                                                             |

When detection is ambiguous (multiple criteria match), **Connector** takes precedence over **Framework module**, which takes precedence over **Library**. The detected type is recorded in the header as `Family` (Connector) or `Type` (Library / Framework module).

### Header

Every `REVIEW.md` must open with this exact two-line block:

```
# <Name> <suffix>

> Module: `<MODULE_PATH>` | Maven artifact: `<artifactId>` | Family: `<TCK family name>` (Connector) — or — Type: `Library` / `Framework module`
> Last reviewed: <YYYY-MM-DD> | Reviewed by: GitHub Copilot (<agent model>) | Prompt version: <vX.Y>
```

- `<Name>` is the human-readable module name (e.g. `Kafka`, `HTTP Common`, `Component Runtime`).
- `<suffix>` is `Connector`, `Library`, or `Framework module` based on the detected type.
- All six fields are mandatory. Use `Unknown` for any field that cannot be resolved; never omit a field.
- `Last reviewed` is always the current UTC date in `YYYY-MM-DD` format — refresh on every run.
- `Reviewed by` auto-detects the actual agent model name; never hardcode a value.
- `Prompt version` is the version declared in this prompt's frontmatter.

### Required sections and order

Every `REVIEW.md` must contain these sections in exactly this order:

1. `## Architecture`
2. `## Features`
3. `## Failure Management`
4. `## Test Coverage`
5. `## Jira Traceability`
6. `## Quality Warnings` *(always present; use `No warning detected.` as body text if a sub-section has no content)*
7. `## Reviewer Notes` *(always present; agent never rewrites after first generation)*

Hand-written sections (any heading not in the list above) must appear after `## Reviewer Notes`.

The ID registry is stored in a separate companion file `REVIEW-ID-REGISTRY.md` in the same folder — see **REVIEW-ID-REGISTRY.md Output Contract** below.

### Ordering rules

Ordering rules apply **only when creating a `REVIEW.md` from scratch**. On incremental updates, the existing row order is always preserved — never reorder rows that are already in the file.

New rows (features, scenarios, or tickets not previously present) are inserted in alphabetical position relative to adjacent existing rows.

For reference, the canonical initial order when creating from scratch is:

- `## Features`: alphabetical by feature name (column 1).
- `## Failure Management`: alphabetical by scenario name (column 1).
- `## Test Coverage` (both tables): same order as the upstream section they mirror.
- `## Jira Traceability`: ascending by ticket ID.
- Architecture tree: alphabetical within each directory level.

IDs (`F-NNN`, `FM-NNN`, `W-NNN`) are assigned sequentially starting at `001` in the alphabetical row order when creating from scratch. On incremental updates, existing IDs are carried forward and new rows receive `max(existing IDs for that prefix) + 1`.

### Row and Item IDs

Every row in `## Features`, `## Failure Management`, `## Quality Warnings`, and `## Jira Traceability` carries a stable ID in a dedicated leftmost `ID` column.

| Prefix      | Section            | Example    | Assigned by                          |
| ----------- | ------------------ | ---------- | ------------------------------------ |
| `F-NNN`     | Features           | `F-001`    | Agent                                |
| `FM-NNN`    | Failure Management | `FM-001`   | Agent                                |
| `W-NNN`     | Quality Warnings   | `W-001`    | Agent                                |
| `QTDI-XXXX` | Jira Traceability  | `QTDI-123` | Jira (pre-existing, never modified)  |

**Assignment rules**

- Format: zero-padded 3-digit integer for agent-assigned IDs (e.g. `F-001`, not `F-1`).
- On creation from scratch: assign sequentially starting at `001` in alphabetical row order.
- On incremental update: carry forward all existing IDs unchanged; assign new IDs using `max(existing IDs for that prefix across both Active IDs and Retired IDs tables in `REVIEW-ID-REGISTRY.md`) + 1`.
- IDs are **immutable** once assigned — they survive row renames, rewrites, and content changes.
- IDs are **never reused** — a retired ID stays in the Retired IDs table permanently.

**Row matching on incremental update**: the ID column is the **primary key** for row identity. When a row's ID is found in the prior file, treat it as the same row regardless of name or content changes. Fall back to name-based matching only for rows without an ID (files predating v0.9).

### Missing data placeholders

When external data cannot be retrieved, use these fixed placeholders — never omit the row or section:

| Data source                    | Placeholder                                                 |
| ------------------------------ | ----------------------------------------------------------- |
| Jira ticket fetch failed       | Omit the row silently (do not list as unavailable)          |
| TUJ search returns no results  | `❌` in the E2E / TUJ column                                 |
| `di-documentation` unreachable | `Not available at review time` in the relevant section body |

### Coverage cell semantics

| Symbol         | Meaning                                                                                                                                 |
| -------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| ✅              | At least one test method confirmed for this feature or scenario in this tier                                                            |
| ✅ `<tuj path>` | E2E covered — always include the relative TUJ file path found during the TUJ search. Append `(**disabled**)` if the test is disabled in the TUJ CSV |
| ❌              | No test found                                                                                                                           |

No other symbols may appear in coverage cells.

### Coverage formula

1. **N** = total rows across both tables in `## Test Coverage` (Features table + Failure Scenarios table).
2. **C** = rows where at least one column (UT, IT, or E2E) shows ✅.
3. Report as: `**Overall coverage**: X% (C of N rows covered across features and failure scenarios)`

### Wording preservation on incremental updates

When a `REVIEW.md` already exists and is being updated:

- Match existing feature, scenario, and coverage rows by their **ID column** (primary key). Fall back to identity matching (class name, SPI name, or scenario code path) only for rows without an ID (files predating v0.9).
- If a row's ID matches and its source evidence is unchanged, **preserve the existing wording verbatim**. Do not regenerate.
- Only rewrite a row when the evidence has demonstrably changed (class renamed, annotation added or removed, catch block removed, new Jira ticket, etc.).
- If a prior row can no longer be matched to any current evidence, move it to a `<!-- Orphaned — verify manually -->` comment at the end of its section rather than deleting it.
- The `## Architecture` section is preserved verbatim if the set of source files under `src/main/java/`, `src/main/resources/`, and `src/test/java/` is identical to what is listed in the existing tree (no files added, removed, or renamed). Only regenerate `## Architecture` when the file set has demonstrably changed.
- **Table column widths**: do not reformat existing rows' column padding when inserting or updating rows. Only expand a column if the new row's content strictly requires it — in that case update only the separator line width, never repad existing data rows.
- Warning items in `## Quality Warnings` are matched by their `W-NNN` ID — see **Warning item IDs** below.

### Warning item IDs

Each generated bullet in `## Quality Warnings` carries a stable `W-NNN` ID inline at the start of the bullet text:

```
- **W-001** **<title>**: <description>
```

The agent parses `W-NNN` directly from the bullet text. No HTML comment anchor is used.

On incremental update, for each sub-section (`Coverage Gaps`, `Risk Alerts`, `Coding Rule Violations`, `Stale Note References`):
1. Regenerate all items from current evidence.
2. For each generated item whose `W-NNN` ID matches an item in the prior file and whose source evidence is unchanged, **preserve the existing wording verbatim** (keeping the same `W-NNN`).
3. If an item's evidence has changed, rewrite the bullet but keep the same `W-NNN` ID.
4. If a prior item's `W-NNN` ID no longer matches any current evidence (gap resolved, risk removed from di-documentation), move it to a `<!-- Orphaned — verify manually -->` comment at the end of the sub-section rather than deleting it.
5. If there are no items in a sub-section, write `No warning detected.` with no ID.
6. Assign new `W-NNN` IDs to new warning items using `max(existing W-NNN across Active IDs and Retired IDs tables in `REVIEW-ID-REGISTRY.md`) + 1`.

### Pre-write consistency gate

Before writing the file, verify all of the following. If any check fails, **do not write** — report the specific failure(s) to the user:

1. Header contains all six required fields with non-placeholder values (or `Unknown` only when genuinely unresolvable).
2. All required sections are present in the correct order.
3. Every feature row in `## Features` (matched by `F-NNN` ID) has a matching row in the Features table of `## Test Coverage`, in the same order.
4. Every scenario row in `## Failure Management` (matched by `FM-NNN` ID) has a matching row in the Failure Scenarios table of `## Test Coverage`, in the same order.
5. Every TUJ path listed in `## Test Coverage` was found during the TUJ search (no invented paths).
6. Every Jira ticket in `## Jira Traceability` was successfully fetched from Jira (not invented).
7. The `## Architecture` section contains a fenced code block with a file tree (at least one `├──` or `└──` line).
8. All `F-NNN` IDs within `## Features` are unique; all `FM-NNN` IDs within `## Failure Management` are unique; all `W-NNN` IDs within `## Quality Warnings` are unique; all `QTDI-XXXX` IDs within `## Jira Traceability` are unique.
9. No ID appears in both the Active IDs and Retired IDs tables of `REVIEW-ID-REGISTRY.md`.
10. No retired ID (from the Retired IDs table in `REVIEW-ID-REGISTRY.md`) appears as a live row ID in any section.

---

## REVIEW-ID-REGISTRY.md Output Contract

`REVIEW-ID-REGISTRY.md` is a companion file in the same folder as `REVIEW.md`. It is fully machine-generated and must never be edited manually. Its sole purpose is to store the data the agent needs across runs to maintain ID stability and detect staleness — data that is redundant noise for human reviewers.

### Structure

```markdown
# ID Registry — <Name> <suffix>

> Companion file for `REVIEW.md`. Machine-generated — do not edit manually.
> Module: `<MODULE_PATH>` | Last updated: <YYYY-MM-DD>

## Active IDs

| ID | Last rewritten |
| -- | -------------- |

## Retired IDs

| ID | Section | Former name | Retired on |
| -- | ------- | ----------- | ---------- |
```

### Active IDs table

Contains one row per currently live ID across all sections (`F-NNN`, `FM-NNN`, `W-NNN`, `QTDI-XXXX`).

| Column | Content |
| ------ | ------- |
| `ID` | The stable ID (e.g. `F-001`) |
| `Last rewritten` | Date (`YYYY-MM-DD`) the row was last rewritten by the agent, or blank if never rewritten since creation |

**`Section` and `Row / Item name` columns are intentionally omitted** — they are redundant with `REVIEW.md` itself and would create a maintenance burden.

### Retired IDs table

Contains one row per ID that has been retired (its source row no longer exists in `REVIEW.md`). Retired IDs are never deleted from this table and never reused.

| Column | Content |
| ------ | ------- |
| `ID` | The retired ID |
| `Section` | Section the ID belonged to |
| `Former name` | Row / item name at time of retirement |
| `Retired on` | Date (`YYYY-MM-DD`) of retirement |

### Rebuild rules

On every run:
1. Read the existing `REVIEW-ID-REGISTRY.md` (if present). If absent, warn the user and reconstruct from IDs currently present in `REVIEW.md` — treat `Last rewritten` as blank for all rows.
2. For each active ID still present in `REVIEW.md`: keep the row; update `Last rewritten` if the row was rewritten this run.
3. For each active ID no longer present in `REVIEW.md`: move the row to Retired IDs with today's date.
4. For each new ID assigned this run: append a new row to Active IDs.
5. Write the updated file to `<MODULE_PATH>/REVIEW-ID-REGISTRY.md`.

---

## Command — `<MODULE_PATH>`

**Purpose**: Create or update `<MODULE_PATH>/REVIEW.md` with a full quality review.

### Goal 1 · Verify prerequisites

Run the prerequisite checks described above (model, MCP servers, prompt version). Abort if any check fails.

### Goal 2 · Detect module type and explore structure

**Detect module type** by applying the criteria in the Output Contract module-types table in order (Connector → Framework module → Library). When detection is ambiguous, Connector takes precedence over Framework module, which takes precedence over Library.

Read `<MODULE_PATH>/pom.xml` to extract the artifact ID, packaging, and sub-modules. If sub-modules are declared, treat each as part of the same module and aggregate results across all of them. Locate unit test files (`*Test.java`) and integration test files (`*IT.java`) under `src/test/java/`.

### Goal 3 · Gather Jira traceability

Extract all unique Jira ticket IDs from the git history of `<MODULE_PATH>`, excluding commits whose message contains `[internal]` (case-insensitive). For each ticket ID, fetch its details (summary, status, type, fix versions) using the Jira MCP. Include only tickets from the QTDI project with type `Story`, `Task`, or `Bug`. Skip tickets that cannot be fetched. Build hyperlinks using `https://qlik-dev.atlassian.net/browse/<ID>`.

### Goal 4 · Gather E2E (TUJ) coverage

Search `Talend/tuj` for references to the module (by artifact ID or folder name). This applies to all module types. Two path roots matter:

- **`tuj/java/`** — automated TUJ jobs. Use these paths in coverage cells.
- **`tuj/manual/`** — manual TUJ jobs. Use these paths and append `(**manual**)` in coverage cells.
- **`tuj/javaCycles/`** — state metadata only (CSV files). Do not use these paths in coverage cells.

For each match, note the relative path (preserving `tuj/java/` or `tuj/manual/` prefix). Determine test state from the three canonical CSV files on the `maintenance/8.0.2` branch of `Talend/tuj`: `tuj/javaCycles/approvedTUJ.csv`, `tuj/javaCycles/deprecatedTUJ.csv`, `tuj/javaCycles/disabledTUJ.csv`. A test is **disabled** if it appears in `deprecatedTUJ.csv` or `disabledTUJ.csv` and does *not* appear in `approvedTUJ.csv` — append `(**disabled**)` in that case.

Parse `.item` files only to detect activated non-default options (e.g. SSL, bulk mode) — do not use them to infer the scenario name. Report active options as a parenthetical note alongside the path.

### Goal 5 · Gather quality references

Retrieve risks and coding rules applicable to this module from `Talend/di-documentation`: read files under `11-Risks/` for risks relevant to the module or its technology stack, and files under `12-Coding-Rules/` for rules applicable to the module type. If the repository is unreachable, note it and continue with partial warnings.

### Goal 6 · Build the review

If `<MODULE_PATH>/REVIEW.md` does not exist, create it from scratch using the template below.

If it already exists, perform an incremental update:
- Regenerate each template section from the gathered data.
- **Preserve existing row wording** for any row whose ID matches (primary key) and whose underlying source evidence has not changed. Only rewrite a row when the evidence has demonstrably changed.
- **Preserve row order**: never reorder rows that are already present in the file. New rows are inserted in alphabetical position relative to adjacent existing rows.
- Move unmatched rows to a `<!-- Orphaned — verify manually -->` comment at the end of their section rather than deleting them.
- **Table column widths**: do not reformat existing rows' column padding when inserting or updating rows. Only expand a column if the new row's content strictly requires it.
- Preserve all hand-written sections (headings not in the required list) after `## Reviewer Notes`.
- **Preserve `## Reviewer Notes`**: read the current content byte-for-byte and carry it forward unchanged. Do not modify, reorder, or add any content to this section after the initial generation.
- **Assign row IDs**: on creation, assign `F-NNN`, `FM-NNN`, `W-NNN` IDs sequentially in alphabetical row order. On incremental update, carry forward all existing IDs; assign new IDs using `max(existing for that prefix across both Active IDs and Retired IDs tables in `REVIEW-ID-REGISTRY.md`) + 1`.
- **Rebuild `REVIEW-ID-REGISTRY.md`**: regenerate both sub-tables from current state — append new entries at the bottom of each prefix group, preserving insertion order; move rows to Retired when their source disappears; update `Last confirmed` to today's date; set `Last rewritten` to today's date for any row rewritten this run (leave blank if the row was preserved verbatim). If `REVIEW-ID-REGISTRY.md` does not exist (e.g. deleted accidentally), warn the user and treat it as a fresh start — ID counters reset from the IDs currently present in `REVIEW.md`.
- **Staleness scan**: after rebuilding all sections, scan `## Reviewer Notes` for patterns `F-\d+`, `FM-\d+`, `W-\d+`, and `QTDI-\d+`. For each ID found, classify and emit a warning:
  - **Modified** (active ID, non-blank `Last rewritten` in `REVIEW-ID-REGISTRY.md`): emit `- **W-NNN** **<ID>** *(rewritten <date>)*: Row was modified — your note may be outdated.` This warning is **persistent** — it survives across runs as long as the ID still appears in `## Reviewer Notes`. Preserve it verbatim on subsequent runs if `Last rewritten` is unchanged; update the date if it changes; remove it if the ID is no longer referenced.
  - **Retired** (ID in the Retired IDs table of `REVIEW-ID-REGISTRY.md`): emit `- **W-NNN** **<ID>** *(retired <date>)*: Row no longer exists.`
  - **Unknown** (matches a prefix pattern but exists in neither table of `REVIEW-ID-REGISTRY.md`): emit `- **W-NNN** **<ID>**: ID not found in registry — possible typo.`
  - **Jira retired** (QTDI-XXXX not in `## Jira Traceability`): emit `- **W-NNN** **<ID>** *(removed <date>)*: Ticket no longer in traceability table.`
  Write results to `### Stale Note References` in `## Quality Warnings`. Write `No stale references detected.` when clean.

When populating `## Features`, use entry points as the primary source (TCK annotations for Connectors, public API for Libraries, SPI registrations for Framework modules). Jira stories reconcile but do not drive the table.

Target structure:

````markdown
# <Name> <suffix>

> Module: `<MODULE_PATH>` | Maven artifact: `<artifactId>` | Family: `<TCK family name>` (Connector) — or — Type: `Library` / `Framework module`
> Last reviewed: <YYYY-MM-DD> | Reviewed by: GitHub Copilot (<agent model>) | Prompt version: <vX.Y>

## Architecture

Provide a **file-by-file tree view** of the module, annotating every file with a one-line comment explaining its role. Cover `src/main/java`, `src/main/resources`, and `src/test/java`. For multi-module layouts, repeat a sub-tree for each sub-module. Enumerate actual packages alphabetically — do not assume a fixed package hierarchy. Every file annotation must describe what the file does, not just restate its name. Example:

```
<MODULE_PATH>/
├── pom.xml                          # Maven build descriptor
└── src/
    ├── main/java/org/talend/components/<module>/
    │   ├── common/
    │   │   ├── Constants.java               # API base URL, endpoint paths, default templates
    │   │   └── DSSLCompatibleParameter.java # DSSL-injectable parameter names
    │   ├── datastore/
    │   │   └── MyDatastore.java             # Connection config: credentials, base URL
    │   ├── dataset/
    │   │   └── MyDataset.java               # Dataset config: endpoint, model, options
    │   ├── processor/
    │   │   ├── MyConfiguration.java         # TCK processor configuration
    │   │   └── MyProcessor.java             # @Processor — the TCK entry point
    │   └── service/
    │       ├── I18n.java                    # i18n message interface
    │       ├── MyRestClient.java            # HttpClient interface for management calls
    │       └── UIActionService.java         # @HealthCheck, @Suggestions, schema discovery
    ├── main/resources/
    │   ├── icons/                           # SVG icons (light/dark)
    │   ├── META-INF/services/               # SPI registrations
    │   └── org/talend/components/<module>/  # i18n Messages.properties
    └── test/java/org/talend/components/<module>/
        ├── BaseCredentialOwner.java         # Reads credentials from env / test config
        ├── BaseClient.java                  # Starts mock HTTP server, wires TCK pipeline
        ├── MyProcessorTest.java             # Unit tests for the processor
        └── service/UIActionServiceTest.java # Unit tests for health check and services
```

After the tree, add a short paragraph on the key architectural dependency (e.g. which shared module provides the HTTP runtime).

## Features

| ID      | Feature        | Class / Entry Point | Description            |
| ------- | -------------- | ------------------- | ---------------------- |
| F-001   | <feature name> | `<ClassName>`       | <one-line description> |

## Failure Management

| ID      | Scenario                  | Behaviour / Error Raised          |
| ------- | ------------------------- | --------------------------------- |
| FM-001  | <failure scenario name>   | <expected behaviour or exception> |

<List error conditions handled by the module: network timeouts, authentication failures, malformed payloads, quota exceeded, partial failures, retry logic, etc. Derive from source code (catch blocks, error handlers, custom exception classes) and from Jira Bug tickets.>

## Test Coverage

### Features

| ID      | Feature        | Unit Tests (`*Test.java`)    | Integration Tests (`*IT.java`)    | E2E / TUJ             |
| ------- | -------------- | ---------------------------- | --------------------------------- | --------------------- |
| F-001   | <feature name> | ✅ / ❌                     | ✅ / ❌                          | ✅ `<tuj path>` / ❌ |

### Failure Scenarios

| ID      | Scenario               | Unit Tests (`*Test.java`)    | Integration Tests (`*IT.java`)    | E2E / TUJ             |
| ------- | ---------------------- | ---------------------------- | --------------------------------- | --------------------- |
| FM-001  | <failure scenario>     | ✅ / ❌                     | ✅ / ❌                          | ✅ `<tuj path>` / ❌ |

**Overall coverage**: X% (C of N rows covered across features and failure scenarios — see Output Contract for formula)

## Jira Traceability

| ID          | Ticket                                                              | Summary   | Status   | Type   |
| ----------- | ------------------------------------------------------------------- | --------- | -------- | ------ |
| QTDI-XXXX   | [QTDI-XXXX](https://qlik-dev.atlassian.net/browse/QTDI-XXXX)       | <summary> | <status> | <type> |

<Note any features found in Jira tickets but not reflected in source, or vice versa.>

## Quality Warnings

### Coverage Gaps
- **W-001** **<feature or scenario>**: <description of missing coverage tier(s)>

### Risk Alerts
- **W-NNN** **<risk title>**: <description>

### Coding Rule Violations
- **W-NNN** **<rule title>**: <description>

### Stale Note References
No stale references detected.

## Reviewer Notes

<!-- Add notes here. Reference items using their IDs (e.g. F-001, FM-003, W-002) or Jira ticket IDs (e.g. QTDI-1234). Free-form markdown. -->
<!-- Example: **Author (YYYY-MM-DD)**: F-001 — your note here. -->
````

**`REVIEW-ID-REGISTRY.md` template** (written to `<MODULE_PATH>/REVIEW-ID-REGISTRY.md`):

````markdown
# ID Registry — <Name> <suffix>

> Companion file for `REVIEW.md`. Machine-generated — do not edit manually.
> Module: `<MODULE_PATH>` | Last updated: <YYYY-MM-DD>

## Active IDs

| ID         | Last rewritten |
| ---------- | -------------- |
| F-001      |                |
| FM-001     |                |
| W-001      |                |
| QTDI-XXXX  |                |

## Retired IDs

| ID | Section | Former name | Retired on |
| -- | ------- | ----------- | ---------- |
````

### Goal 7 · Validate and write the file

Run the **pre-write consistency gate** defined in the Output Contract before writing. If any check fails, do not write — report the specific failure(s) to the user and stop.

If all checks pass, write both output files:
- `<MODULE_PATH>/REVIEW.md` — the human-facing review
- `<MODULE_PATH>/REVIEW-ID-REGISTRY.md` — the machine-facing ID registry

Display a summary of what was added or changed.

