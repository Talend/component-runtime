# UI - User interface and forms

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`). Category prefix `UI-`.
> Machine-readable twin: [index.UI.json](index.UI.json). Markdown and JSON MUST agree.
> Related: [CFG](CFG-configuration.md), [VAL](VAL-validation.md), [ACT](ACT-actions.md), [05 configuration and UI](../05-configuration-and-ui.md), [appendix conditions](../10-appendix/conditions.md).

## Conventions used by all UI entries

- Every UI annotation is meta-annotated with `@Ui` (`UI-001`). In the JVM the resulting property metadata key is `tcomp::ui::<annotationSimpleNameLowerCase>[::<method>]`; the server strips `tcomp::`, so hosts see `ui::<name>[::<method>]`.
- An annotation without attributes yields `ui::<name>` = `"true"`. An annotation with attributes yields one key per attribute (`String[]` joined by `,`, enums/`Class` by `toString()`/lower-cased simple class name).
- Values starting with `local_configuration:` in a string attribute are resolved against the server `LocalConfiguration` at model build time (`UiParameterEnricher`).
- The reference form generator is `component-form` (`component-form/component-form-core`): `UiSpecService.convert(ComponentDetail | ConfigTypeNode, lang, context)` returns `Ui{jsonSchema, uiSchema, properties}`. Widgets that `component-form` does not render (`ModuleList`, `Path`, `BasedOnSchema`, `AutoLayout`, `HorizontalLayout`, `VerticalLayout`) are hints for other hosts (Studio); their metadata is still delivered.

### UI-001 `@Ui` (meta-annotation)
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/meta/Ui.java`
- **Category / Sub-area**: UI / metadata mapping
- **Kind**: annotation (`@Target(ANNOTATION_TYPE)`)
- **Applies to**: UI annotations (`DefaultValue`, `Hidden`, `OptionsOrder`, layouts, widgets)
- **Description**: marks an annotation as producing `ui::*` metadata through `UiParameterEnricher`. No attributes.
- **Attributes**: none.
- **Contract for the Designer**: MUST read UI hints only from `metadata` keys starting with `ui::` and MUST ignore unknown ones.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata keys `ui::*`.
- **Maturity level**: 1 - the mapping rule underlies every rendering hint.
- **Example**:

```json
{ "ui::textarea": "true", "ui::code::value": "sql" }
```

### UI-002 `@GridLayout`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/layout/GridLayout.java` (+ `documentation/src/main/antora/modules/ROOT/pages/ref-ui.adoc`)
- **Category / Sub-area**: UI / layouts
- **Kind**: annotation (`@Target(TYPE)`, `@Repeatable(GridLayouts.class)`)
- **Applies to**: configuration classes
- **Description**: places child options by row and column; exclusive with `@OptionsOrder`. Metadata: one key per form name `ui::gridlayout::<name>::value` = rows joined by `|`, options of a row joined by `,` (e.g. `first|second,third`). Form selection (`component-form`): if a single layout exists use it without tabs; else if a `Main` layout exists render tabs `Main` then `Advanced` (other layouts, e.g. `Checkpoint`, are not shown as tabs); else render all layouts as tabs in case-insensitive alphabetical order. A `gridLayoutFilter` (form name) restricts rendering to one layout.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `Row[]` | (required) | Ordered rows; `@GridLayout.Row(String[] value)` lists option names of the row. |
| `names` | `String[]` | `{"Main"}` | Form names that use this definition. Constants `FormType.MAIN="Main"`, `ADVANCED="Advanced"`, `CHECKPOINT="Checkpoint"`, `CITIZEN="CitizenUser"` (deprecated). |

- **Contract for the Designer**: MUST render one row per `|` segment and one column per `,` item; MUST render tabs as above; SHOULD show an option that is missing from every layout nowhere (validator logs an error for such options).
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `ui::gridlayout::<name>::value` (tab names translated when server option `talend.component.server.gridlayout.translation.support=true`, see `UI-022`).
- **Maturity level**: 1 - without layouts the form falls back to one widget per line.
- **Example**:

```json
{ "ui::gridlayout::Main::value": "url|username,password", "ui::gridlayout::Advanced::value": "timeout" }
```

### UI-003 `@GridLayouts`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/layout/GridLayouts.java`
- **Category / Sub-area**: UI / layouts
- **Kind**: annotation (`@Target(TYPE)`), container of `@GridLayout`
- **Applies to**: configuration classes
- **Description**: "Allow to configure multiple grid layouts on the same class, qualified with a classifier (name)". Each contained `@GridLayout` produces `ui::gridlayout::<name>::value` for each of its `names`.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `GridLayout[]` | (required) | Layout definitions. |

- **Contract for the Designer**: same as `UI-002`.
- **Contract for the Runtime**: none.
- **Server exposure**: several `ui::gridlayout::*::value` keys on the same property.
- **Maturity level**: 1 - main/advanced tab support.
- **Example**:

```json
{ "ui::gridlayout::Main::value": "first|second,third", "ui::gridlayout::Advanced::value": "another" }
```

### UI-004 `@AutoLayout`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/layout/AutoLayout.java`
- **Category / Sub-area**: UI / layouts
- **Kind**: annotation (`@Target(TYPE)`, marker)
- **Applies to**: configuration classes
- **Description**: "Request the rendered to do what it thinks is best." Metadata `ui::autolayout` = `true`. Not consumed by `component-form`.
- **Attributes**: none.
- **Contract for the Designer**: MAY choose its own layout; SHOULD keep declaration order (inferred).
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `ui::autolayout`.
- **Maturity level**: 2 - a hint only.
- **Example**:

```json
{ "ui::autolayout": "true" }
```

### UI-005 `@HorizontalLayout`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/layout/HorizontalLayout.java`
- **Category / Sub-area**: UI / layouts
- **Kind**: annotation (`@Target(TYPE)`, marker)
- **Applies to**: configuration classes
- **Description**: notifies that a horizontal layout is preferred. Metadata `ui::horizontallayout` = `true`. Not consumed by `component-form`.
- **Attributes**: none.
- **Contract for the Designer**: MAY lay children out in one row.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `ui::horizontallayout`.
- **Maturity level**: 2 - a hint only.
- **Example**:

```json
{ "ui::horizontallayout": "true" }
```

### UI-006 `@VerticalLayout`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/layout/VerticalLayout.java`
- **Category / Sub-area**: UI / layouts
- **Kind**: annotation (`@Target(TYPE)`, marker)
- **Applies to**: configuration classes
- **Description**: notifies that a vertical layout is preferred. Metadata `ui::verticallayout` = `true`. Not consumed by `component-form` (which renders vertically by default).
- **Attributes**: none.
- **Contract for the Designer**: SHOULD stack children vertically (default).
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `ui::verticallayout`.
- **Maturity level**: 2 - a hint only.
- **Example**:

```json
{ "ui::verticallayout": "true" }
```

### UI-007 `@TextArea`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/widget/TextArea.java`
- **Category / Sub-area**: UI / widgets
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, marker)
- **Applies to**: `STRING` options
- **Description**: multiline text input. Metadata `ui::textarea` = `true`; `component-form` emits widget `textarea`.
- **Attributes**: none.
- **Contract for the Designer**: MUST render a multiline input for `ui::textarea=true`.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `ui::textarea`.
- **Maturity level**: 1 - degraded (single-line) rendering without it.
- **Example**:

```json
{ "widget": "textarea", "key": "configuration.query" }
```

### UI-008 `@Code`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/widget/Code.java`
- **Category / Sub-area**: UI / widgets
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`)
- **Applies to**: `STRING` options
- **Description**: code editor widget instead of a text area. Metadata `ui::code::value` = language; `component-form` emits widget `code` with `options.language`.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | (required) | Language identifier (e.g. `sql`, `java`, `javascript`). |

- **Contract for the Designer**: SHOULD render a code editor with the given language; MAY fall back to a text area.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `ui::code::value`.
- **Maturity level**: 1 - degraded rendering without it.
- **Example**:

```json
{ "widget": "code", "options": { "language": "sql" } }
```

### UI-009 `@Credential`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/widget/Credential.java` (+ `documentation/src/main/antora/modules/ROOT/pages/tutorial-configuration-sensitive-data.adoc`)
- **Category / Sub-area**: UI / widgets
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, marker)
- **Applies to**: `STRING` options
- **Description**: marks a secret. Metadata `ui::credential` = `true`; `component-form` emits widget `text` with `type: "password"`. Values may be vault-encrypted (`vault:v1:...`) and are decrypted server-side at action execution (see SRV-013).
- **Attributes**: none.
- **Contract for the Designer**: MUST mask the value (password input) and MUST NOT log or display it in clear; SHOULD store it encrypted when the host has a secret store.
- **Contract for the Runtime**: SHOULD keep the value out of logs.
- **Server exposure**: property metadata `ui::credential`.
- **Maturity level**: 1 - leaking a secret on screen is a severe UX/security defect, correctness is unaffected.
- **Example**:

```json
{ "widget": "text", "type": "password", "key": "configuration.password" }
```

### UI-010 `@DateTime`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/widget/DateTime.java`
- **Category / Sub-area**: UI / widgets
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`)
- **Applies to**: `java.time` options (implicit on `ZonedDateTime`, `LocalDate`, `LocalDateTime`, `LocalTime`); using it on another type throws `IllegalArgumentException("Unsupported type for @DateTime option")` except `Object`
- **Description**: date/time picker. Metadata (`UiParameterEnricher`): `ui::datetime` = `time` (`LocalTime`), `date` (`LocalDate`), `datetime` (`LocalDateTime`), `zoneddatetime` (`ZonedDateTime` or `Object`); plus `ui::datetime::dateFormat` (date, datetime, zoneddatetime), `ui::datetime::useSeconds` (time, datetime, zoneddatetime), `ui::datetime::useUTC` (datetime, zoneddatetime). Explicit annotation overrides the implicit defaults. `component-form` widgets: `date` (options `dateFormat`), `datetime` (options `useSeconds`, `useUTC`, `dateFormat`); `time` uses widget `datetime` with `useSeconds` only.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `dateFormat` | `String` | `"YYYY/MM/DD"` | Display format. |
| `useSeconds` | `boolean` | `true` | Show seconds. |
| `useUTC` | `boolean` | `true` | Interpret/serialize in UTC. |

- **Contract for the Designer**: MUST render a picker and MUST serialize to the string form the runtime converter accepts (ISO-like, per Java type; inferred); SHOULD honor the three options.
- **Contract for the Runtime**: MUST parse the string with the xbean converters of the component manager.
- **Server exposure**: property metadata `ui::datetime`, `ui::datetime::dateFormat`, `ui::datetime::useSeconds`, `ui::datetime::useUTC`.
- **Maturity level**: 1 - dates are common; a plain text fallback is a degraded UX.
- **Example**:

```json
{ "ui::datetime": "datetime", "ui::datetime::dateFormat": "YYYY/MM/DD", "ui::datetime::useSeconds": "true", "ui::datetime::useUTC": "true" }
```

### UI-011 `@ModuleList`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/widget/ModuleList.java`
- **Category / Sub-area**: UI / widgets
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, marker)
- **Applies to**: `STRING` options
- **Description**: "selected module list widget, only for studio". Metadata `ui::modulelist` = `true`. Not rendered by `component-form`.
- **Attributes**: none.
- **Contract for the Designer**: Studio-like hosts MAY render a module chooser; others MUST render a plain text field.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `ui::modulelist`.
- **Maturity level**: 2 - Studio-only advanced widget.
- **Example**:

```json
{ "ui::modulelist": "true" }
```

### UI-012 `@Path`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/widget/Path.java`
- **Category / Sub-area**: UI / widgets
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`)
- **Applies to**: `STRING` options
- **Description**: "file or directory widget. Only for studio." Metadata `ui::path::value` = `FILE` or `DIRECTORY` (`String.valueOf(enum)`; the generated doc sample shows `null` because it is generated without a value). Not rendered by `component-form`.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `Path.Type` (`FILE`, `DIRECTORY`) | `FILE` | Kind of path to pick. |

- **Contract for the Designer**: Studio-like hosts MAY render a file/directory chooser; others MUST render a text field.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `ui::path::value`.
- **Maturity level**: 2 - Studio-only advanced widget.
- **Example**:

```json
{ "ui::path::value": "DIRECTORY" }
```

### UI-013 `@Structure`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/widget/Structure.java`
- **Category / Sub-area**: UI / widgets (schema binding)
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, `@ActionRef(value = DiscoverSchema.class, ref = "discoverSchema")`)
- **Applies to**: `List<String>` or `List<Object>` options (enforced by `ModelValidator`)
- **Description**: marks the option that holds the component schema/columns (the "data selector"). Metadata `ui::structure::value`, `ui::structure::discoverSchema`, `ui::structure::type`, and (through `@ActionRef`) `action::schema` = the `discoverSchema` name and `action::schema::discoverSchema` / `action::schema::type`. `component-form`: for an option with `type = OUT` it adds a "Guess Schema" button (widget `button`) to the enclosing object, wired to the `schema` action named by `discoverSchema` (default `"default"`) or, when several datasets exist, matched by dataset name; the trigger `options` carry `{path, type: array|object}` and the trigger `parameters` are the dataset paths.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | `"__default__"` | Name of the input/output flow. |
| `discoverSchema` | `String` | `""` | Id of the `@DiscoverSchema` action to use. |
| `type` | `Structure.Type` (`IN`, `OUT`) | `IN` | Connection type modelled. |

- **Contract for the Designer**: MUST offer schema discovery (button) for `OUT` structures and MUST write the returned schema entries into the option (inferred from `options.path`/`type`); SHOULD keep IN structures editable.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `ui::structure::*`, `action::schema*`.
- **Maturity level**: 1 - schema discovery is level 1.
- **Example**:

```json
{ "ui::structure::value": "__default__", "ui::structure::type": "OUT", "ui::structure::discoverSchema": "default", "action::schema": "default" }
```

### UI-014 `@BasedOnSchema`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/widget/BasedOnSchema.java`
- **Category / Sub-area**: UI / widgets
- **Kind**: annotation (`@Target({PARAMETER, FIELD})`, marker)
- **Applies to**: table columns (option inside an array-of-object) filled from the component schema
- **Description**: "Mark a table column filled by component's schema auto. Only for studio." Metadata `ui::basedonschema` = `true`. Not rendered by `component-form`. Related built-in suggestion names: `BuiltInSuggestable.Name.INCOMING_SCHEMA_ENTRY_NAMES`, `CURRENT_SCHEMA_ENTRY_NAMES` (see `ACT-021`).
- **Attributes**: none.
- **Contract for the Designer**: Studio-like hosts MAY auto-fill the column from the schema.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `ui::basedonschema`.
- **Maturity level**: 2 - Studio-only.
- **Example**:

```json
{ "ui::basedonschema": "true" }
```

### UI-015 `@ReadOnly`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/widget/ReadOnly.java`
- **Category / Sub-area**: UI / widgets
- **Kind**: annotation (`@Target({PARAMETER, FIELD})`, marker)
- **Applies to**: options
- **Description**: "read-only widget. User cannot modify widget." Metadata `ui::readonly` = `true`; `component-form` sets `UiSchema.readOnly = true`.
- **Attributes**: none.
- **Contract for the Designer**: MUST NOT let the user edit the value; MUST keep sending the value.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `ui::readonly`.
- **Maturity level**: 1 - editing a read-only field is a UX defect.
- **Example**:

```json
{ "readOnly": true, "key": "configuration.id" }
```

### UI-016 `@ActiveIf`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/condition/ActiveIf.java` (+ `documentation/src/main/antora/modules/ROOT/pages/ref-conditions.adoc`, `component-configuration.adoc`)
- **Category / Sub-area**: UI / conditions
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, `@Repeatable(ActiveIfs.class)`, `@Condition("if")`)
- **Applies to**: options
- **Description**: the option is active (visible and validated) only if the value at `target` matches. Metadata: `condition::if::target`, `condition::if::value` (values joined by `,`), `condition::if::negate`, `condition::if::evaluationStrategy` (`DEFAULT`, `LENGTH`, `CONTAINS`, with `evaluationStrategyOptions` appended as `CONTAINS(lowercase=true)`); in an `ActiveIfs` group each key gets suffix `::<index>`. `target` is a path relative to the option: `..` = parent object, `/` separator, a name without `.` is a sibling (normalized to `../name`), `.` = itself; the special target `ui.scope` compares against the UI scope (`UI-019`). Evaluation semantics (`VisibilityService`): `DEFAULT` compares the stringified target value with each `value` (any match), `LENGTH` compares size of collection/array/string (missing = `0`), `CONTAINS` tests substring of string / any element of collection/array (optional lowercase), `negate` inverts the any-match result. `component-form` converts to JSON-logic conditions (see `UI-021`).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `target` | `String` | (required) | Path to evaluate. |
| `value` | `String[]` | (required) | Values to compare to. |
| `negate` | `boolean` | `false` | Invert the match. |
| `evaluationStrategy` | `EvaluationStrategy` (`DEFAULT`, `LENGTH`, `CONTAINS`) | `DEFAULT` | How the target value is compared. |
| `evaluationStrategyOptions` | `EvaluationStrategyOption[]` | `{}` | Options (`name`, `value` default `"true"`); only `lowercase` for `CONTAINS`. |

- **Contract for the Designer**: MUST hide inactive options, re-evaluate on every change of the target, and SHOULD exclude inactive options from validation; MUST keep hidden values consistent (do not fail on them).
- **Contract for the Runtime**: MUST skip constraint validation of inactive options (the framework does via `VisibilityService`; hosts validating themselves MUST replicate this).
- **Server exposure**: property metadata `condition::if::*`.
- **Maturity level**: 1 - conditions are listed in level 1 of the checklist; ignoring them shows irrelevant fields.
- **Example**:

```json
{ "condition::if::target": "checkbox1", "condition::if::value": "true", "condition::if::negate": "false", "condition::if::evaluationStrategy": "DEFAULT" }
```

### UI-017 `@ActiveIfs`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/condition/ActiveIfs.java`
- **Category / Sub-area**: UI / conditions
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, `@Condition("ifs")`)
- **Applies to**: options
- **Description**: several visibility conditions on one option. Metadata `condition::ifs::operator` (`AND`/`OR`, default `AND`) plus the indexed `condition::if::*::<i>` keys of the contained `@ActiveIf`s.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `operator` | `ActiveIfs.Operator` (`AND`, `OR`) | `AND` | How conditions are combined. |
| `value` | `ActiveIf[]` | (required) | The conditions. |

- **Contract for the Designer**: MUST combine the conditions with the operator.
- **Contract for the Runtime**: same as `UI-016`.
- **Server exposure**: property metadata `condition::ifs::operator`, `condition::if::target::<i>`, `condition::if::value::<i>`, `condition::if::negate::<i>`, `condition::if::evaluationStrategy::<i>`.
- **Maturity level**: 1 - same rationale as `UI-016`.
- **Example**:

```json
{ "condition::ifs::operator": "OR", "condition::if::target::0": "checkbox2", "condition::if::value::0": "true", "condition::if::target::1": "checkbox3", "condition::if::value::1": "true" }
```

### UI-018 `@Condition` (meta-annotation)
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/condition/meta/Condition.java`
- **Category / Sub-area**: UI / conditions
- **Kind**: annotation (`@Target(ANNOTATION_TYPE)`)
- **Applies to**: condition annotations (`@ActiveIf` -> `if`, `@ActiveIfs` -> `ifs`)
- **Description**: `value` is the condition type used in the metadata key `condition::<type>::<attribute>`. `ConditionParameterEnricher` turns each attribute of the annotation into a metadata entry (`String[]` joined by `,`).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | (required) | Condition type id. |

- **Contract for the Designer**: MUST support at least the types `if` and `ifs`.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `condition::<type>::*`.
- **Maturity level**: 1 - part of the conditions mechanism.
- **Example**:

```json
{ "condition::if::target": "foo" }
```

### UI-019 `UIScope`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/condition/UIScope.java`
- **Category / Sub-area**: UI / conditions
- **Kind**: interface (constants)
- **Applies to**: `@ActiveIf(target = UIScope.TARGET, ...)`
- **Description**: constants for scope-dependent visibility: `TARGET="ui.scope"`, values `studio`, `cloud`, `studio.component`, `studio.component.connection`, `studio.metadata`. `component-form` treats a condition on `ui.scope` as a constant: the option is shown only when the condition value contains `cloud` (and not negated), or when a `negate=true` condition does not contain `cloud`; i.e. the web form acts as the `cloud` scope. The in-JVM `VisibilityService` resolves `ui.scope` as a normal (absent) path (inferred: evaluates to not visible), so validation of such options is skipped at runtime.
- **Attributes**: constants above.
- **Contract for the Designer**: MUST declare its scope (Studio-like hosts: `studio*`, web hosts: `cloud`) and show/hide options accordingly; unknown scopes SHOULD be treated as not matching.
- **Contract for the Runtime**: none.
- **Server exposure**: `condition::if::target=ui.scope`, `condition::if::value=<scope list>`.
- **Maturity level**: 2 - only needed for options that differ between Studio and web.
- **Example**:

```java
@ActiveIf(target = UIScope.TARGET, value = UIScope.CLOUD_SCOPE)
```

### UI-020 Type-driven default widgets
- **Source**: `component-form/component-form-core/src/main/java/org/talend/sdk/component/form/internal/converter/impl/UiSchemaConverter.java` (+ `.../widget/*WidgetConverter.java`)
- **Category / Sub-area**: UI / widget selection
- **Kind**: convention
- **Applies to**: all options
- **Description**: widget chosen by the reference generator, in priority order for `STRING`: `ui::credential` -> `text`+`password`; `ui::code::value` -> `code`; `action::suggestions` or `action::built_in_suggestable` -> `datalist` (not restricted, triggers on focus and change); `action::dynamic_values` -> `datalist` restricted, `titleMap` filled at build time by calling the dynamic-values action with no parameters; `ui::textarea=true` -> `textarea`; `ui::datetime` -> `date`/`datetime`; else `text`. `NUMBER` -> `text` (numeric via JSON schema). `BOOLEAN` -> `toggle`. `ENUM` -> `datalist` restricted with `titleMap` from `validation.enumValues`/`proposalDisplayNames` (sorted by value when no display names). `ARRAY` of primitives -> `multiSelect` (`restricted=false`, values from `action::dynamic_values` if any); `ARRAY` of objects -> nested schema with `itemWidget: collapsibleFieldset`. `OBJECT` -> `fieldset` (children ordered by `ui::optionsorder::value`, else alphabetical by path) or, with grid layouts, `tabs`/`columns`/`fieldset`.
- **Attributes**: none.
- **Contract for the Designer**: MUST offer at least a text control per type and MUST honor `enumValues` restriction; SHOULD implement the priority order above.
- **Contract for the Runtime**: none.
- **Server exposure**: derived from `type`, `validation.enumValues`, `metadata`.
- **Maturity level**: 1 - form rendering.
- **Example**:

```json
{ "widget": "datalist", "restricted": true, "titleMap": [ { "name": "Insert", "value": "Insert" } ] }
```

### UI-021 UiSchema and JSON Schema generation contract
- **Source**: `component-form/component-form-core/src/main/java/org/talend/sdk/component/form/api/UiSpecService.java` (+ `component-form/component-form-model/src/main/java/org/talend/sdk/component/form/model/uischema/UiSchema.java`, `.../jsonschema/JsonSchema.java`)
- **Category / Sub-area**: UI / form generation
- **Kind**: payload field convention (output of `component-form`)
- **Applies to**: component and configuration-type forms
- **Description**: `UiSpecService.convert` builds three artifacts: `jsonSchema` (types, `required`, `default`, `minimum`, `maximum`, `minLength`, `maxLength`, `minItems`, `maxItems`, `uniqueItems`, `pattern`, `enum`), `uiSchema` (rendering tree) and `properties` (initial values from defaults; a string with suggestions/dynamic values also gets a sibling `$<name>_name`). `UiSchema` fields: `key`, `title`, `widget`, `itemWidget`, `type`, `description`, `tooltip`, `items`, `options`, `autoFocus`, `disabled`, `readOnly`, `required`, `restricted`, `placeholder`, `triggers[]{action,family,type,onEvent,remote,options[]{path,type},parameters[]{key,path}}`, `titleMap[]{name,value}` (or grouped `{title,suggestions}`), `condition` (JSON-logic map). Conditions: `DEFAULT` -> `{"===":[{"var":path},value]}`; `LENGTH` -> `{"===":[{"var":"<path>.length"},n]}`; `CONTAINS` -> `{"in":[value,{"var":path}]}` (`{"in":[{"var":path},value]}` when the target is an array; lower-cased value for `lowercase=true`); several values -> `{"or":[...]}`; `negate` -> `{"==":[cond,false]}`; several `@ActiveIf` -> `and`/`or` per `condition::ifs::operator`; `ui::hidden` -> `{"==":[1,-1]}`. Documentation is put in `description` only when `PropertyContext.Configuration.includeDocumentationMetadata` is true, and in `tooltip` when `documentation::tooltip=true`.
- **Attributes**: see description.
- **Contract for the Designer**: MAY reuse `component-form` (Java) or MUST reimplement the same mapping; a custom renderer MUST honor `condition`, `triggers`, `titleMap`, `restricted`, `readOnly`, `required` semantics.
- **Contract for the Runtime**: none.
- **Server exposure**: none (computed client-side or in a UI backend from `ComponentDetail`/`ConfigTypeNode`); worked before/after example in [03](../03-component-server-api.md) and [05](../05-configuration-and-ui.md).
- **Maturity level**: 1 - needed for a usable form.
- **Example**:

```json
{ "key": "configuration.url", "title": "URL", "widget": "text", "condition": { "===": [ { "var": "configuration.useUrl" }, true ] } }
```

### UI-022 Layout tab name translation
- **Source**: `component-server-parent/component-server/src/main/java/org/talend/sdk/component/server/service/PropertiesService.java` (+ `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/internationalization/ParameterBundle.java`)
- **Category / Sub-area**: UI / i18n of layouts
- **Kind**: convention
- **Applies to**: grid layouts
- **Description**: when the server configuration `getTranslateGridLayoutTabNames()` (documented option `talend.component.server.gridlayout.translation.support`) is true, each key `ui::gridlayout::<tab>::value` is renamed to `ui::gridlayout::<translated>::value` where `<translated>` is the bundle value of `<property_path or simple_class_name>._gridlayout.<tab>._displayName`. Consequence: tab names in metadata may no longer be `Main`/`Advanced`, so `component-form` Main/Advanced detection then falls back to alphabetical tabs.
- **Attributes**: none.
- **Contract for the Designer**: SHOULD NOT hard-code `Main`/`Advanced` when the server may translate tab names; the Studio does not support this option.
- **Contract for the Runtime**: none.
- **Server exposure**: renamed `ui::gridlayout::*::value` keys.
- **Maturity level**: 2 - optional i18n refinement.
- **Example**:

```properties
MyConfig._gridlayout.Main._displayName = Principal
```
