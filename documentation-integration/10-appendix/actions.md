# Appendix - Actions

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`). Sources: annotations in `component-api/.../api/service/**` and `.../configuration/action/**`; `documentation/src/main/antora/modules/ROOT/pages/_partials/generated_actions.adoc`; `ActionParameterEnricher`. Catalog: [ACT](../02-feature-catalog/ACT-actions.md). Protocol: [03](../03-component-server-api.md).

## 1. Server-side action types (`@ActionType`)

All annotations below are `@Target(METHOD)` (except `@Action`: `METHOD, ANNOTATION_TYPE`) and live on methods of `@Service` classes. Request: `POST /action/execute?family=<family>&type=<type>&action=<value>&lang=<lang>` with a flat `Map<String,String>` body.

| Annotation (package `org.talend.sdk.component.api.service...`) | `type` | Returned type (contract) | Attributes (default) | Parameter contract | Bound by | Catalog |
|---|---|---|---|---|---|---|
| `Action` | `user` | `Object` | `family` (`""`), `value` | free | none | `ACT-001` |
| `healthcheck.HealthCheck` | `healthcheck` | `healthcheck.HealthCheckStatus` | `family` (`""`), `value` (`"default"`) | one `@DataStore` parameter | `@Checkable` | `ACT-003` |
| `completion.Suggestions` | `suggestions` | `completion.SuggestionValues` | `family` (`""`), `value` | selected by `@Suggestable.parameters` | `@Suggestable` | `ACT-004` |
| `completion.DynamicValues` | `dynamic_values` | `completion.Values` | `family` (`""`), `value` | none | `@Proposable` | `ACT-005` |
| `update.Update` | `update` | (no contract; object assignable to the `@Updatable` field) | `family` (`""`), `value` | selected by `@Updatable.parameters` | `@Updatable` | `ACT-006` |
| `asyncvalidation.AsyncValidation` | `validation` | `asyncvalidation.ValidationResult` | `family` (`""`), `value` | value + others per `@Validable.parameters` | `@Validable` | `VAL-010` |
| `schema.DiscoverSchema` | `schema` | `org.talend.sdk.component.api.record.Schema` | `family` (`""`), `value` (`"default"`) | one `@DataSet` parameter | `@Structure(discoverSchema)`, `@FixedSchema` | `ACT-007` |
| `schema.DiscoverSchemaExtended` | `schema_extended` | `org.talend.sdk.component.api.record.Schema` | `family` (`""`), `value` (`"default"`) | `incomingSchema` (Schema, JSON string), one `@Option`, `branch` (String) | `@FixedSchema` | `ACT-008` |
| `schema.DatabaseSchemaMapping` | `schema_mapping` | `String` | `family` (`""`), `value` (`"default"`) | one `@DataStore` `@Option` | `@DatabaseMapping(mapping)` | `ACT-013` |
| `discovery.DiscoverDataset` | `discoverdataset` | `discovery.DiscoverDatasetResult` | `family` (`""`), `value` (`"default"`) | one `@DataStore` parameter | none (index) | `ACT-009` |
| `dependency.DynamicDependencies` | `dynamic_dependencies` | `java.util.List` (of GAV `String`) | `family` (`""`), `value` (`"default"`) | one object `@Option` | `@DynamicDependenciesConfiguration` | `ACT-010` |
| `connection.CreateConnection` | `create_connection` | `Object` | `family` (`""`), `value` (`"default"`) | one `@DataStore` `@Option` | Studio | `ACT-011` |
| `connection.CloseConnection` | `close_connection` | `connection.CloseConnectionObject` | `family` (`""`), `value` (`"default"`) | none | Studio | `ACT-012` |
| `outputs.AvailableOutputFlows` | `available_output` | `java.util.Collection` (of `String`) | `family` (`""`), `value` (`"__default__"`) | processor configuration | `@ConditionalOutput` | `ACT-014` |

Uniqueness: an action is addressed by (`family`, `type`, `value`); `ActionType.value` is unique across the application.

## 2. Client-side (built-in) action

| Binding | Metadata | Notes | Catalog |
|---|---|---|---|
| `@BuiltInSuggestable(value, name)` | `action::built_in_suggestable` | Names: `CUSTOM`, `INCOMING_SCHEMA_ENTRY_NAMES`, `CURRENT_SCHEMA_ENTRY_NAMES` (Studio only). Provided by the application the UI runs in; "there is no guarantee the specified action is supported". Trigger family `builtin_client`, `remote=false`. | `ACT-021` |

## 3. Binding annotations (option/type level)

| Annotation | Target | `@ActionRef` | Attributes (default) | Metadata keys |
|---|---|---|---|---|
| `@Suggestable` | field, parameter | `Suggestions` | `value`, `parameters` (`{"."}`), `labelDisplayMode` (`LABEL`) | `action::suggestions`, `::parameters`, `::labelDisplayMode` |
| `@Proposable` | field, parameter | `DynamicValues` | `value` | `action::dynamic_values` |
| `@Updatable` | field, parameter | `Update` | `value`, `after` (`""`), `parameters` (`{}`), `activeIf` (`""`) | `action::update`, `::parameters`, `::after`, `::activeIf` |
| `@Validable` | field, parameter | `AsyncValidation` | `value`, `parameters` (`{"."}`) | `action::validation`, `::parameters` |
| `@Checkable` | type | `HealthCheck` | `value` (`"default"`) | `action::healthcheck` |
| `@BuiltInSuggestable` | field, parameter | `Object.class` (client) | `value` (`Name`), `name` (`""`) | `action::built_in_suggestable` |
| `@Structure` | field, parameter | `DiscoverSchema` (`ref = "discoverSchema"`) | `value` (`"__default__"`), `discoverSchema` (`""`), `type` (`IN`) | `action::schema`, `action::schema::discoverSchema`, `action::schema::type`, `ui::structure::*` |

`@ActionRef(value, ref = "value")` (`ACT-015`): generic mechanism; metadata name = `action::<ActionType.value of value>`; for `Object.class` = `action::<snake_case(annotation simple name)>`.

## 4. Result payloads

`HealthCheckStatus` and `ValidationResult`: `{ "status": "OK"|"KO", "comment": "<text>" }`.

`SuggestionValues`: `{ "cacheable": boolean, "items": [ { "id": "<stored value>", "label": "<display>" } ] }`.

`Values`: `{ "items": [ { "id": "<value>", "label": "<display>" } ] }`.

`DiscoverDatasetResult`: `{ "datasetDescriptionList": [ { "name": "<dataset>", "metadata": { "<k>": "<v>" } } ] }`.

`schema` / `schema_extended` (record `Schema`, example from `generated_actions.adoc`):

```json
{
  "entries": [
    { "comment": "The column 1", "errorCapable": false, "metadata": false, "name": "column1",
      "nullable": false, "props": {}, "rawName": "column 1", "type": "STRING", "valid": true },
    { "comment": "The int column", "errorCapable": false, "metadata": false, "name": "column2",
      "nullable": false, "props": {}, "rawName": "column 2", "type": "INT", "valid": true }
  ],
  "metadata": [],
  "props": { "talend.fields.order": "column1,column2" },
  "type": "RECORD"
}
```

Field meaning: [04 data model](../04-data-model.md). Generated samples for `close_connection` (`{"connection": "..."}`), `dynamic_dependencies` and `available_output` are placeholders (empty object) in `generated_actions.adoc`; the actual shapes are `List<String>` and `Collection<String>` respectively; `schema_mapping` generated sample lists `java.lang.String` internals and is not meaningful (the result is a plain `String`).

## 5. Errors

| Situation | HTTP | Body |
|---|---|---|
| missing `family` / `type` / `action` | `400` | `ErrorPayload{code: FAMILY_MISSING \| TYPE_MISSING \| ACTION_MISSING}` |
| action not found | `404` | `ErrorPayload{code: ACTION_MISSING}` |
| `ComponentException(errorOrigin=USER)` | `400` | `ErrorPayload{code: ACTION_ERROR, description: "Action execution failed with: ..."}` |
| `ComponentException(errorOrigin=BACKEND)` | `456` | same |
| other exception / `UNKNOWN` | `520` | same |

Studio guess schema: `DiscoverSchemaException.possibleHandleErrorWith` in {`EXCEPTION` (default), `SILENT` (not implemented), `RETRY` (not implemented), `EXECUTE_MOCK_JOB`, `EXECUTE_LIFECYCLE`}.

## 6. Action index and references

`GET /action/index` -> `ActionList{items: ActionItem{component (= family), type, name, properties[]}}`; per component/config type `ActionReference{family, name, type, displayName, properties[]}`. `displayName` key: `${family}.actions.${type}.${name}._displayName`.

## 7. Parameter reference syntax

See `ACT-025`: `.`, `../x`, `x`, `x/y`, `./x`, absolute.
