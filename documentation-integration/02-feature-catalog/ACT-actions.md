# ACT - Design-time actions

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`). Category prefix `ACT-`.
> Machine-readable twin: [index.ACT.json](index.ACT.json). Markdown and JSON MUST agree.
> Related: [CFG](CFG-configuration.md), [UI](UI-ui.md), [VAL](VAL-validation.md), [05 configuration and UI](../05-configuration-and-ui.md), [appendix actions](../10-appendix/actions.md), [03 component server API](../03-component-server-api.md).

## Overview

An *action* is a method of a `@Service` class annotated with an annotation that is itself meta-annotated with `@ActionType(value = <type>, expectedReturnedType = <class>)`. Actions are identified by (`family`, `type`, `name`) where `name` is the annotation `value()`. They are executed by the component server (`POST /action/execute`) at design time. Options are linked to actions with *binding annotations* (`@Suggestable`, `@Proposable`, `@Updatable`, `@Validable`, `@Checkable`, `@BuiltInSuggestable`, `@Structure`) that carry `@ActionRef(<action annotation>, ref = "value")`; `ActionParameterEnricher` turns each binding into property metadata (`ACT-026`), and `component-form` turns metadata plus the component's `ActionReference` list into UI triggers (`ACT-024`).

Rules common to all action methods:

- The declaring class MUST be a `@Service` (or abstract); the return type MUST be assignable to `expectedReturnedType` (`ActionValidator.checkActionType`).
- Method parameters that are services are injected; every other parameter is a *design-time parameter*. Parameters annotated `@Option("x")` are addressed by key path `x[.child...]` in the request; parameters without `@Option` are addressed by their Java parameter name (e.g. `branch`, `incomingSchema`). The action's `ActionReference.properties` list describes them (`SimplePropertyDefinition`, ordered by metadata `definition::parameter::index`).
- `family` defaults to the family of the service package (`@Components`); `value` is unique per (family, type).
- Action types (`ActionType.value`): `user`, `healthcheck`, `suggestions`, `dynamic_values`, `update`, `validation`, `schema`, `schema_extended`, `schema_mapping`, `discoverdataset`, `dynamic_dependencies`, `close_connection`, `create_connection`, `available_output`. The client-side pseudo type `built_in_suggestable` has no server action.

Ownership: `@AsyncValidation`/`ValidationResult` are catalogued in [VAL](VAL-validation.md) (`VAL-010`, `VAL-011`); `@ConditionalOutput` and result shapes of `schema` are in RUN/DAT catalogs. `@AvailableOutputFlows` is here (`ACT-014`).

### ACT-001 `@Action` (type `user`)
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/Action.java` (+ `documentation/src/main/antora/modules/ROOT/pages/services-actions.adoc`)
- **Category / Sub-area**: ACT / custom actions
- **Kind**: annotation (`@Target({METHOD, ANNOTATION_TYPE})`, `@ActionType("user")`)
- **Applies to**: service methods
- **Description**: "Extension point for custom UI integrations and custom actions." Any return type (`expectedReturnedType = Object`). Not bound to an option by any binding annotation, so a designer reaches it only through the action index (`SRV-012`) and custom UI logic.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | `""` | Component family. |
| `value` | `String` | (required) | Action name. |

- **Contract for the Designer**: MAY expose custom actions via `GET /action/index?type=user`; MUST ignore unknown ones.
- **Contract for the Runtime**: none.
- **Server exposure**: `/action/index`, `/action/execute?type=user`.
- **Maturity level**: 2 - optional extension point.
- **Example**:

```java
@Action(family = "mycomp", value = "test") public Status doTest(final IncomingData data) { ... }
```

### ACT-002 `@ActionType` (meta-annotation)
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/ActionType.java`
- **Category / Sub-area**: ACT / action definition
- **Kind**: annotation (`@Target(ANNOTATION_TYPE)`)
- **Applies to**: action annotations
- **Description**: declares an action annotation: `value` = the type id (unique across the application; used as the `type` request parameter and in `action::<type>` metadata), `expectedReturnedType` = required return type contract. Merged from SVC-004 (service-side registration contract): a public method of a `@Service` class annotated with an annotation meta-annotated `@ActionType` (for example `@HealthCheck`, `@DynamicValues`, `@Suggestions`, `@Update`, `@DiscoverSchema`, `@AsyncValidation`, `@DynamicDependencies`, or the generic `@Action(family, value)`) is registered as a `ServiceMeta.ActionMeta` (`family`, `type`, `action`, parameters, invoker) when the plugin container starts (SVC-027). The type name comes from `@ActionType.value()` and the expected return type from `@ActionType.expectedReturnedType()`. `family` defaults to `""` (falls back to the package `@Components` family) and `value` defaults to `"default"` on typed action annotations. A `String` parameter bound with `@Option("$lang")` receives the mapped request language. Health check: `@HealthCheck` returns `HealthCheckStatus{status: OK|KO, comment}` (ACT-003). Server side: `/action/index` and `/action/execute` (SRV-012, SRV-013); errors 400/456/520 per `ErrorOrigin` (SRV-024).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | (required) | Action type id. |
| `expectedReturnedType` | `Class<?>` | `Object.class` | Contract for the return type. |

- **Contract for the Designer**: MUST dispatch response handling on the `type` id (`SRV-013` result table).
- **Contract for the Runtime**: MAY ignore actions (they are design-time); an embedded server exposing them MUST run them inside the plugin classloader with services injected.
- **Server exposure**: `ActionReference.type`, `ActionItem.type`.
- **Maturity level**: 1 - the type id is required to route any action call.
- **Example**:

```java
@ActionType(value = "healthcheck", expectedReturnedType = HealthCheckStatus.class)
```

### ACT-003 `@HealthCheck` and `HealthCheckStatus`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/healthcheck/HealthCheck.java` (+ `.../healthcheck/HealthCheckStatus.java`)
- **Category / Sub-area**: ACT / connection test
- **Kind**: annotation (`@Target(METHOD)`, `@ActionType("healthcheck", HealthCheckStatus)`) + payload class
- **Applies to**: service methods; the single non-service parameter MUST be a `@DataStore` type
- **Description**: "an action doing a connection test". Bound to a datastore class with `@Checkable(value)` (`ACT-020`); the button is only rendered for datastores that carry `@Checkable` (metadata `action::healthcheck`). `component-form` adds a button titled `Validate Connection` (or the action display name) to the datastore's fieldset; the trigger parameters are the datastore option subtree (found by `configurationtype::type=datastore`, name matched to the action name or to `action::healthcheck`).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | `""` | Family. |
| `value` | `String` | `"default"` | Action name (matches `@Checkable.value`). |
| `HealthCheckStatus.status` | `Status` (`OK`, `KO`) | - | Outcome. |
| `HealthCheckStatus.comment` | `String` | - | Message. |

- **Contract for the Designer**: MUST offer a test-connection button on datastores having `action::healthcheck` and MUST display `comment` on `KO`; MUST send the datastore values as flat keys (parameter name from the action's first property, typically `datastore`).
- **Contract for the Runtime**: none.
- **Server exposure**: `POST /action/execute?type=healthcheck`; metadata `action::healthcheck`.
- **Maturity level**: 1 - health check is level 1.
- **Example**:

```json
{ "status": "KO", "comment": "Something went wrong" }
```

### ACT-004 `@Suggestions` and `SuggestionValues`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/completion/Suggestions.java` (+ `.../completion/SuggestionValues.java`)
- **Category / Sub-area**: ACT / completion
- **Kind**: annotation (`@Target(METHOD)`, `@ActionType("suggestions", SuggestionValues)`) + payload class
- **Applies to**: service methods; parameters are whatever `@Suggestable.parameters` selects
- **Description**: computes candidate values of a string option depending on other option values, on demand (focus/click). Linked from the option by `@Suggestable(value)` (`ACT-016`).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | `""` | Family. |
| `value` | `String` | (required) | Action name referenced by `@Suggestable.value`. |
| `SuggestionValues.cacheable` | `boolean` | - | Client MAY cache the list after the first call, else it MUST call each time. |
| `SuggestionValues.items[]` | `Item{id,label}` | - | Pairs to show (`id` is the stored value). |

- **Contract for the Designer**: MUST call the action when the field is focused/changed (`ACT-024`), display `label` (or `label (id)` when `labelDisplayMode=LABEL_ID`), store `id`; MUST honor `cacheable`; MUST allow free text (suggestions are not restrictive).
- **Contract for the Runtime**: none.
- **Server exposure**: `POST /action/execute?type=suggestions`; metadata `action::suggestions`, `action::suggestions::parameters`, `action::suggestions::labelDisplayMode`.
- **Maturity level**: 1 - suggestions are level 1.
- **Example**:

```json
{ "cacheable": false, "items": [ { "id": "value", "label": "label" } ] }
```

### ACT-005 `@DynamicValues` and `Values`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/completion/DynamicValues.java` (+ `.../completion/Values.java`)
- **Category / Sub-area**: ACT / completion
- **Kind**: annotation (`@Target(METHOD)`, `@ActionType("dynamic_values", Values)`) + payload class
- **Applies to**: service methods with no design-time parameter (`ActionValidator`)
- **Description**: provides the fixed-at-runtime value list of a string option (restricted dropdown), resolved by (family, value). Linked by `@Proposable(value)` (`ACT-017`). The list does not depend on other form values.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | `""` | Family. |
| `value` | `String` | (required) | Action name referenced by `@Proposable.value`. |
| `Values.items[]` | `Item{id,label}` | - | Allowed values. |

- **Contract for the Designer**: MUST call it once when building the form (no parameters), render a restricted list (`titleMap` from `label`, value `id`); SHOULD cache the result per form session; on failure MUST fall back to an empty list (the reference implementation logs and continues).
- **Contract for the Runtime**: none.
- **Server exposure**: `POST /action/execute?type=dynamic_values`; metadata `action::dynamic_values`.
- **Maturity level**: 1 - dynamic values are level 1.
- **Example**:

```json
{ "items": [ { "id": "value", "label": "label" } ] }
```

### ACT-006 `@Update`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/update/Update.java`
- **Category / Sub-area**: ACT / update
- **Kind**: annotation (`@Target(METHOD)`, `@ActionType("update")`, no `expectedReturnedType`)
- **Applies to**: service methods returning an object (not a primitive/String, `ActionValidator`)
- **Description**: "an action returning a new instance replacing part of a form/configuration" ("guess my configuration"). Linked by `@Updatable(value)` (`ACT-018`); the returned type MUST be assignable to the decorated field type.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | `""` | Family. |
| `value` | `String` | (required) | Update action name. |

- **Contract for the Designer**: MUST render a button (title = action display name or `<name> (update)`), call the action with the parameters of `@Updatable.parameters`, and replace the value at the trigger `options.path` with the JSON response (merge semantics unspecified; the reference `component-form` sets `options=[{path,type}]`).
- **Contract for the Runtime**: none.
- **Server exposure**: `POST /action/execute?type=update`; metadata `action::update`, `::parameters`, `::after`, `::activeIf`.
- **Maturity level**: 2 - `@Updatable` is level 2 in the checklist.
- **Example**:

```json
{ "url": "http://host/api", "username": "guessed" }
```

### ACT-007 `@DiscoverSchema`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/schema/DiscoverSchema.java` (+ `documentation/src/main/antora/modules/ROOT/pages/studio-schema.adoc`)
- **Category / Sub-area**: ACT / schema discovery
- **Kind**: annotation (`@Target(METHOD)`, `@ActionType("schema", org.talend.sdk.component.api.record.Schema)`, `@Partial`)
- **Applies to**: service methods whose first (only) parameter is a `@DataSet` type
- **Description**: returns the schema of a dataset ("guess schema"). If the component has several datasets, the parameter dataset MUST have the same identifier as the `@DiscoverSchema` value. Linked from a `@Structure(discoverSchema = "<value>")` option; `component-form` adds a "Guess Schema" button for `@Structure(type = OUT)`. Studio lookup order for inputs: `@DiscoverSchemaExtended` named like the dataset, then `@DiscoverSchema` named like the dataset, else a mock job is executed; `DiscoverSchemaException` selects the fallback (`SRV-013`). Merged from DAT-022: a component with several datasets should use a `value` equal to the dataset identifier; referenced by the `@Structure` widget and by `@FixedSchema.value` (DAT-024).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | `""` | Family. |
| `value` | `String` | `"default"` | Identifier referenced by `@Structure.discoverSchema` and `@FixedSchema.value`. |

- **Contract for the Designer**: MUST call the action with the dataset configuration and convert the returned record `Schema` JSON (`entries[]{name,rawName,type,nullable,comment,metadata,errorCapable,valid,props,elementSchema}`, `metadata[]`, `props`, `type`) into the host schema; field semantics in [04](../04-data-model.md).
- **Contract for the Runtime**: MAY call it to obtain design-time schemas; MUST NOT require it for execution.
- **Server exposure**: `POST /action/execute?type=schema`; metadata `action::schema`; component metadata `tcomp::ui::schema::fixed`.
- **Maturity level**: 1 - schema discovery is level 1.
- **Example**:

```json
{ "entries": [ { "name": "column1", "rawName": "column 1", "type": "STRING", "nullable": false } ], "metadata": [], "props": { "talend.fields.order": "column1" }, "type": "RECORD" }
```

### ACT-008 `@DiscoverSchemaExtended`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/schema/DiscoverSchemaExtended.java`
- **Category / Sub-area**: ACT / schema discovery
- **Kind**: annotation (`@Target(METHOD)`, `@ActionType("schema_extended", Schema)`, `@Partial`)
- **Applies to**: service methods (processor/output schema computation)
- **Description**: returns the schema for an outgoing branch of a processor, given the incoming schema and/or the branch name. Valid signatures: `(Schema incomingSchema, @Option("configuration") Conf, String branch)`, `(Schema incomingSchema, @Option Conf)`, `(@Option Conf, String branch)`, `(@Option Conf)`; exactly one `@Option` parameter; parameter names MUST be `incomingSchema` and `branch` (`ActionValidator`). The request carries the incoming schema as a JSON *string* value under the key `incomingSchema` (flat JSON, not a nested object) and the branch name under `branch` (Studio branches: `__default__`/`FLOW`, `MAIN`, `REJECT`). Used with `@FixedSchema` (fixed-schema button suppression, refresh on `watch` paths). Merged from DAT-023: `value` MUST equal the connector (processor) name; schema propagation through processors is a normal-UX capability.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | `""` | Family. |
| `value` | `String` | `"default"` | Identifier, normally the connector/dataset name. |

- **Contract for the Designer**: MAY call it to propagate schemas between components (schema propagation, see [07](../07-designer-blueprint.md)); MUST pass the incoming schema JSON serialized as a string. SHOULD call it to propagate schemas through processors; MUST pass the incoming schema and branch when the method declares them.
- **Contract for the Runtime**: none.
- **Server exposure**: `POST /action/execute?type=schema_extended`.
- **Maturity level**: 1 - schema propagation through processors is a normal-UX capability (lowest level of the merged entries).
- **Example**:

```json
{ "configuration.driver": "jdbc://x", "branch": "REJECT", "incomingSchema": "{\"entries\":[...],\"type\":\"RECORD\"}" }
```

### ACT-009 `@DiscoverDataset` and `DiscoverDatasetResult`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/discovery/DiscoverDataset.java` (+ `.../discovery/DiscoverDatasetResult.java`)
- **Category / Sub-area**: ACT / dataset discovery
- **Kind**: annotation (`@Target(METHOD)`, `@ActionType("discoverdataset", DiscoverDatasetResult)`) + payload class
- **Applies to**: service methods whose only parameter is a `@DataStore` type
- **Description**: "explore a connection to retrieve potential datasets". Merged from DAT-027 and DAT-028: `DiscoverDatasetResult.datasetDescriptionList` is a `List<DatasetDescription>` (default empty); each `DatasetDescription` has a `name` and a sorted (`TreeMap`) `metadata` map of free key/value pairs.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | `""` | Family. |
| `value` | `String` | `"default"` | Action name. |
| `DiscoverDatasetResult.datasetDescriptionList[]` | `DatasetDescription{name, metadata:Map<String,String>}` | - | Discovered datasets. |

- **Contract for the Designer**: MAY offer a "browse datasets" step after a datastore is defined and create a dataset per selection (use `metadata` to prefill). MUST parse the `DiscoverDatasetResult` shape when the action is used.
- **Contract for the Runtime**: none.
- **Server exposure**: `POST /action/execute?type=discoverdataset`.
- **Maturity level**: 2 - listed as level 2 in the checklist.
- **Example**:

```json
{ "datasetDescriptionList": [ { "name": "table_a", "metadata": { "schema": "public" } } ] }
```

### ACT-010 `@DynamicDependencies`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/dependency/DynamicDependencies.java`
- **Category / Sub-area**: ACT / dependencies
- **Kind**: annotation (`@Target(METHOD)`, `@ActionType("dynamic_dependencies", java.util.List)`)
- **Applies to**: service methods with one object `@Option` parameter and return type `List<String>` (`ActionValidator`)
- **Description**: "returns a list of dynamic dependencies with GAV formatting" (`groupId:artifactId:version`), computed from a configuration typically annotated `@DynamicDependenciesConfiguration` (`CFG-012`). Merged from LCM-013: the GAVs are computed from the user's configuration (for example a JDBC driver chosen by the user).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | `""` | Family. |
| `value` | `String` | `"default"` | Action name. |

- **Contract for the Designer**: MAY call it and request the listed artifacts from the dependency endpoints before running. SHOULD call it when the option using it changes, and make the returned GAVs available to the runtime (download via `/component/dependencies`, SRV-006).
- **Contract for the Runtime**: SHOULD resolve the returned GAVs into the component classloader (via `Resolver`, SVC catalog). Resolution may use `Resolver` (SVC-013) or `TALEND-INF/dynamic-dependencies.properties` (unverified) before instantiating the component.
- **Server exposure**: `POST /action/execute?type=dynamic_dependencies`.
- **Maturity level**: 2 - listed as level 2.
- **Example**:

```json
[ "org.postgresql:postgresql:42.7.3" ]
```

### ACT-011 `@CreateConnection`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/connection/CreateConnection.java` (+ `documentation/.../creating-dataset-datastore.adoc`)
- **Category / Sub-area**: ACT / connection reuse (Studio)
- **Kind**: annotation (`@Target(METHOD)`, `@ActionType("create_connection", Object)`)
- **Applies to**: service methods with a `@DataStore` `@Option` parameter
- **Description**: creates a runtime connection object (e.g. JDBC connection) so the Studio can share it between components. The `@Option` value MUST match the runtime class constructor parameter name of the configuration. Studio-only, "no effect for cloud platform". Merged from INT-010: the parameter MUST be a `@DataStore` configuration annotated with `@Option`; Studio only, no effect on cloud platforms.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | `""` | Family. |
| `value` | `String` | `"default"` | Action name. |

- **Contract for the Designer**: Studio-like hosts SHOULD call it once and hand the connection to components using `@Connection`; other hosts MAY ignore.
- **Contract for the Runtime**: Studio-like runtimes MUST inject the created object into `@Connection` fields (`ACT-012`).
- **Server exposure**: `POST /action/execute?type=create_connection` (in-process in the Studio).
- **Maturity level**: 1 - listed as "design-time/run-time connection handling" in level 1.
- **Example**:

```java
@CreateConnection public Object createConn(@Option("configuration") final SomeDataStore ds) { ... }
```

### ACT-012 `@CloseConnection`, `CloseConnectionObject`, `@Connection`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/connection/CloseConnection.java` (+ `.../connection/CloseConnectionObject.java`, `.../connection/Connection.java`)
- **Category / Sub-area**: ACT / connection reuse (Studio)
- **Kind**: annotation + abstract class + field annotation
- **Applies to**: service methods (`@CloseConnection`, returns `CloseConnectionObject`); component fields (`@Connection`)
- **Description**: `@CloseConnection` (type `close_connection`) returns a `CloseConnectionObject` whose `boolean close() throws ComponentException` closes the shared connection; the object exposes the connection through its `@Connection protected Object connection` field. `@Connection` (`@Target(FIELD)`) auto-injects the connection into a mapper/processor field. Studio-only. Merged from INT-011, INT-012, INT-013: `CloseConnectionObject` is an abstract class (Lombok `@Data`) with abstract `boolean close() throws ComponentException`; the shared connection is injected into its `@Connection` protected field `connection`, and `close()` is implemented by casting it to the concrete type. A host MAY inject the connection created by `@CreateConnection` (ACT-011) before calling `close()`.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | `""` | Family. |
| `value` | `String` | `"default"` | Action name. |

- **Contract for the Designer**: Studio-like hosts MUST call the close object at end of job; other hosts MAY ignore.
- **Contract for the Runtime**: Studio-like runtimes MUST inject `@Connection` fields and close via the returned object.
- **Server exposure**: `POST /action/execute?type=close_connection` (in-process in the Studio).
- **Maturity level**: 1 - same rationale as `ACT-011`.
- **Example**:

```java
@CloseConnection public CloseConnectionObject closeConn() { ... }
```

### ACT-013 `@DatabaseSchemaMapping`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/schema/DatabaseSchemaMapping.java`
- **Category / Sub-area**: ACT / schema mapping (Studio)
- **Kind**: annotation (`@Target(METHOD)`, `@ActionType("schema_mapping", String)`)
- **Applies to**: service methods with a `@DataStore` `@Option` parameter returning `String`
- **Description**: returns a database type mapping computed from a datastore, used when `@DatabaseMapping(value = "custom", mapping = "<id>")` (`DSG-014`). Studio-only.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | `""` | Family. |
| `value` | `String` | `"default"` | Identifier referenced by `@DatabaseMapping.mapping`. |

- **Contract for the Designer**: Studio-like hosts MAY call it to map DB column types; others ignore.
- **Contract for the Runtime**: none.
- **Server exposure**: `POST /action/execute?type=schema_mapping`; component metadata `tcomp::ui::schema::mapper`.
- **Maturity level**: 2 - Studio-only.
- **Example**:

```java
@DatabaseSchemaMapping("my_mapping") public String mapping(@Option("datastore") final Store s) { ... }
```

### ACT-014 `@AvailableOutputFlows`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/outputs/AvailableOutputFlows.java`
- **Category / Sub-area**: ACT / dynamic outputs
- **Kind**: annotation (`@Target(METHOD)`, `@ActionType("available_output", Collection)`)
- **Applies to**: service methods with the processor's configuration as parameter, returning `List<String>`; paired by name with `@ConditionalOutput("<name>")` on exactly one `@Processor` using the same configuration type (`ActionValidator`)
- **Description**: "Provide the output flows by some condition": the list of output connection names a processor offers for a given configuration. The processor carries component metadata `conditional_output::value` = the action name (`DSG-008`).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | `""` | Family. |
| `value` | `String` | `"__default__"` | Name matched with `@ConditionalOutput.value` (MUST be non-empty). |

- **Contract for the Designer**: MAY re-query available outputs when the configuration changes and rebuild the output ports; MUST otherwise use static `outputFlows` from `ComponentDetail`.
- **Contract for the Runtime**: MUST accept any flow name the action returned as a valid output (branching rules in RUN catalog).
- **Server exposure**: `POST /action/execute?type=available_output`; `ComponentDetail.outputFlows`; metadata `conditional_output::value`.
- **Maturity level**: 2 - dynamic outputs are a later capability.
- **Example**:

```json
[ "__default__", "REJECT", "audit" ]
```

### ACT-015 `@ActionRef` (meta-annotation)
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/action/meta/ActionRef.java`
- **Category / Sub-area**: ACT / binding definition
- **Kind**: annotation (`@Target(ANNOTATION_TYPE)`)
- **Applies to**: option binding annotations
- **Description**: makes an annotation a binding to an action. `value` = the action annotation class (or `Object.class` for client-side built-in actions), `ref` = name of the method of the binding annotation holding the action name (default `value`). For `Object.class` the metadata key is `action::<snake_case of the annotation simple name>` and the value is the annotation `value()` (or `name()` when `value` is `CUSTOM`).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `Class<?>` | (required) | Action annotation (or `Object.class`). |
| `ref` | `String` | `"value"` | Method carrying the action reference. |

- **Contract for the Designer**: none (consumes the metadata).
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `action::*`.
- **Maturity level**: 1 - mechanism behind all bindings.
- **Example**:

```java
@ActionRef(Suggestions.class) public @interface Suggestable { ... }
```

### ACT-016 `@Suggestable`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/action/Suggestable.java`
- **Category / Sub-area**: ACT / bindings
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, `@ActionRef(Suggestions.class)`)
- **Applies to**: `STRING` options
- **Description**: binds the option to a `@Suggestions` action. Metadata: `action::suggestions` = action name, `action::suggestions::parameters` = parameters joined by `,`, `action::suggestions::labelDisplayMode`. `component-form` renders a `datalist` with two triggers (`onEvent` `focus` and `change`).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | (required) | Action name. |
| `parameters` | `String[]` | `{"."}` | Option references sent to the action, syntax in `ACT-025`; `.` = the decorated option value. |
| `labelDisplayMode` | `LabelDisplayMode` (`LABEL`, `LABEL_ID`) | `LABEL` | `LABEL_ID` shows `label (id)` to disambiguate duplicate labels. |

- **Contract for the Designer**: MUST implement `ACT-004` and honor `labelDisplayMode` (unknown values fall back to `LABEL`).
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata above.
- **Maturity level**: 1 - suggestions are level 1.
- **Example**:

```json
{ "action::suggestions": "loadTables", "action::suggestions::parameters": ".,../datastore", "action::suggestions::labelDisplayMode": "LABEL" }
```

### ACT-017 `@Proposable`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/action/Proposable.java`
- **Category / Sub-area**: ACT / bindings
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, `@ActionRef(DynamicValues.class)`)
- **Applies to**: `STRING` options (not enums, `ActionValidator`)
- **Description**: binds the option to a `@DynamicValues` action ("dynamic value filling depending on server state, not known at coding time"). Metadata `action::dynamic_values` = action name. For per-form dependent proposals use `@Suggestable`.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | (required) | `@DynamicValues` name. |

- **Contract for the Designer**: MUST implement `ACT-005`.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `action::dynamic_values`.
- **Maturity level**: 1 - level 1 feature.
- **Example**:

```json
{ "action::dynamic_values": "regions" }
```

### ACT-018 `@Updatable`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/action/Updatable.java`
- **Category / Sub-area**: ACT / bindings
- **Kind**: annotation (`@Target({PARAMETER, FIELD})`, `@ActionRef(Update.class)`)
- **Applies to**: object options (not primitives, `ActionValidator`); the decorated type MUST be assignable from the `@Update` return type
- **Description**: "Mark the decorated instance as replaceable with an action returning a new instance". Metadata: `action::update`, `action::update::parameters` (default empty), `action::update::after`, `action::update::activeIf`. `component-form` creates a `button` (key `<path>_<n>`) whose trigger has `options=[{path,type}]`; when `after` is set the button is placed with the direct child of that name in a `fieldset`; when `activeIf` is set the button gets a condition: the reference implementation parses the first comma-separated item as `target = <childName>` and compares the child value to `true` (or to `condition::if::value::<childName>` when present).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | (required) | `@Update` name. |
| `after` | `String` | `""` | Direct child primitive option after which the button is appended (no `.` allowed). |
| `parameters` | `String[]` | `{}` | Option references sent to the action (`ACT-025`). |
| `activeIf` | `String` | `""` | Button visibility, format `target = <child>, value = { true }` (javadoc example). |

- **Contract for the Designer**: MUST implement `ACT-006` and the button placement/visibility rules above.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata above.
- **Maturity level**: 2 - `@Updatable` is level 2.
- **Example**:

```json
{ "action::update": "guessConfig", "action::update::after": "url", "action::update::parameters": "." }
```

### ACT-019 `@Validable`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/action/Validable.java`
- **Category / Sub-area**: ACT / bindings
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, `@ActionRef(AsyncValidation.class)`)
- **Applies to**: options (typically strings)
- **Description**: binds the option to an `@AsyncValidation` action. Metadata `action::validation` = name, `action::validation::parameters` = parameters joined by `,`. The server expects the decorated value as first parameter; other parameters "must match the form/parameter path exactly". `component-form` emits a trigger (no `onEvent`).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | (required) | `@AsyncValidation` id. |
| `parameters` | `String[]` | `{"."}` | Option references sent to the validation (`ACT-025`). |

- **Contract for the Designer**: MUST implement `VAL-010`; trigger event when `onEvent` is absent is the renderer's choice (typically value change/blur, unverified).
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata above.
- **Maturity level**: 2 - async validation is level 2.
- **Example**:

```json
{ "action::validation": "checkUrl", "action::validation::parameters": "." }
```

### ACT-020 `@Checkable`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/action/Checkable.java`
- **Category / Sub-area**: ACT / bindings
- **Kind**: annotation (`@Target(TYPE)`, `@ActionRef(HealthCheck.class)`)
- **Applies to**: configuration classes (datastores)
- **Description**: "Mark the decorated type/model as testable with a health check action." Metadata `action::healthcheck` = value. A datastore without `@Checkable` gets no test button even when a `@HealthCheck` exists.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | `"default"` | `@HealthCheck` name. |

- **Contract for the Designer**: MUST add the connection test button only when `action::healthcheck` is present.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `action::healthcheck`.
- **Maturity level**: 1 - level 1 with `ACT-003`.
- **Example**:

```java
@DataStore("DatastoreA") @Checkable public class DatastoreA { ... }
```

### ACT-021 `@BuiltInSuggestable`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/action/BuiltInSuggestable.java`
- **Category / Sub-area**: ACT / bindings (client-side)
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, `@ActionRef(Object.class)`)
- **Applies to**: `STRING` options
- **Description**: suggestions provided by the *host application* rather than by a component service; "it can do nothing in some environments" and there is no guarantee the action exists. Metadata `action::built_in_suggestable` = name (the `name()` when `value` is `CUSTOM`). `component-form` emits a trigger with `family = builtin_client`, `type = built_in_suggestable`, `onEvent = focus`, `remote = false`.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `Name` (`CUSTOM`, `INCOMING_SCHEMA_ENTRY_NAMES`, `CURRENT_SCHEMA_ENTRY_NAMES`) | (required) | Environment action; `CURRENT_SCHEMA_ENTRY_NAMES` is Studio only. |
| `name` | `String` | `""` | Action name when `value` is `CUSTOM`. |

- **Contract for the Designer**: MAY implement `INCOMING_SCHEMA_ENTRY_NAMES` (suggest incoming column names) locally; MUST NOT call the server for it (`remote=false`); MUST degrade to a plain text field when unsupported. Components SHOULD NOT depend on it.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `action::built_in_suggestable`.
- **Maturity level**: 2 - optional host-specific helper.
- **Example**:

```json
{ "action::built_in_suggestable": "INCOMING_SCHEMA_ENTRY_NAMES" }
```

### ACT-022 (moved) -> see SRV-013

### ACT-023 (moved) -> see SRV-012

### ACT-024 Trigger model (`UiSchema.Trigger`) and wiring semantics
- **Source**: `component-form/component-form-core/src/main/java/org/talend/sdk/component/form/internal/converter/impl/widget/AbstractWidgetConverter.java` (+ `ObjectWidgetConverter.java`, `component-form/component-form-model/src/main/java/org/talend/sdk/component/form/model/uischema/UiSchema.java`)
- **Category / Sub-area**: ACT / UI wiring
- **Kind**: payload field convention
- **Applies to**: options with `action::*` metadata
- **Description**: trigger fields: `action` (name), `family`, `type`, `onEvent`, `remote`, `options[]{path,type}`, `parameters[]{key,path}`. Generation rules: `suggestions` -> two triggers (`onEvent` `focus` and `change`); `built_in_suggestable` -> `focus`, `remote=false`, family `builtin_client`; `validation` -> one trigger, no `onEvent`; `dynamic_values` -> resolved at build time (no trigger); `healthcheck` -> button added to the enclosing datastore object; `schema` (from `@Structure(type=OUT)`) -> "Guess Schema" button with `options=[{path=<structure path>, type=array|object}]`; `update` -> button with `options=[{path,type}]`; other `action::<type>` (e.g. `user`-typed bindings by custom annotations) are passed through as triggers. Parameter resolution: for each comma-separated reference in `action::<type>::parameters`, the i-th action parameter (ordered by `definition::parameter::index`) provides `key` prefix; every property under the referenced option becomes `{key: <actionParam path>+<suffix>, path: <form property path>}` (`[]` removed from keys); a reference that resolves to nothing raises `IllegalArgumentException` unless it starts with `$`. `$selfReference`/`$selfReferenceType` pseudo properties are also resolved when present. Absent `onEvent` means "explicit user action or renderer default".
- **Attributes**: see fields above.
- **Contract for the Designer**: MUST build request bodies from `parameters` (`body[key] = value(path)`); MUST fire `focus` triggers on field focus and `change` triggers on value change; MUST treat `remote=false` as client-only.
- **Contract for the Runtime**: none.
- **Server exposure**: derived from `ActionReference` + property metadata.
- **Maturity level**: 1 - action wiring is level 1.
- **Example**:

```json
{ "action": "loadTables", "family": "db", "type": "suggestions", "onEvent": "focus", "parameters": [ { "key": "datastore.url", "path": "configuration.datastore.url" } ] }
```

### ACT-025 Action parameter reference syntax
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/action/Suggestable.java` (javadoc) (+ `component-form/component-form-core/src/main/java/org/talend/sdk/component/form/internal/converter/impl/widget/path/AbsolutePathResolver.java`)
- **Category / Sub-area**: ACT / parameters
- **Kind**: convention
- **Applies to**: `parameters` of `@Suggestable`, `@Updatable`, `@Validable`; `target` of `@ActiveIf`
- **Description**: reference syntax resolved against the path `P` of the decorated option (`AbsolutePathResolver`): `.` = `P` itself; a reference without `.` is normalized to `../<ref>`; `..` removes the last dotted segment of the current path (repeatable, separated by `/`) then the remainder (`/` -> `.`) is appended, so `../foo` = sibling `foo`, `../../other` = `foo` of the grandparent; `./x` or `.x` = child `x` of `P`; anything else is taken as an absolute path. `bar/dummy` = sibling `bar`, its child `dummy`. Javadoc default: `@Suggestable`/`@Validable` = `{"."}`, `@Updatable` = none.
- **Attributes**: none.
- **Contract for the Designer**: MUST implement this resolution identically (Studio counterpart `SettingsCreator.computeTargetPath`).
- **Contract for the Runtime**: none.
- **Server exposure**: raw reference strings in `action::<type>::parameters` and `condition::if::target`.
- **Maturity level**: 1 - needed to build any parameterized action call.
- **Example**:

```
P = configuration.dataset.query ; "../table" -> configuration.dataset.table ; "../../datastore" -> configuration.datastore
```

### ACT-026 Action metadata keys on options
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/reflect/parameterenricher/ActionParameterEnricher.java`
- **Category / Sub-area**: ACT / metadata
- **Kind**: convention (metadata keys)
- **Applies to**: options with a binding annotation
- **Description**: for a binding annotation with `@ActionRef(A.class)`: `tcomp::action::<A's @ActionType value>` = value of the `ref` method; `...::parameters` = the `parameters()` array joined by `,` (only if the annotation has `parameters`); one more key per other declared method: `...::<method>` = `String.valueOf(result)`. For `ActionRef(Object.class)`: `tcomp::action::<snake_case(SimpleName)>` = `value` or `name` (custom). Server strips `tcomp::`. Complete key table:

| binding | keys |
|---|---|
| `@Suggestable` | `action::suggestions`, `action::suggestions::parameters`, `action::suggestions::labelDisplayMode` |
| `@Proposable` | `action::dynamic_values` |
| `@Updatable` | `action::update`, `action::update::parameters`, `action::update::after`, `action::update::activeIf` |
| `@Validable` | `action::validation`, `action::validation::parameters` |
| `@Checkable` | `action::healthcheck` |
| `@BuiltInSuggestable` | `action::built_in_suggestable` |
| `@Structure` | `action::schema` (= `discoverSchema`), `action::schema::discoverSchema`, `action::schema::type` |

- **Attributes**: none.
- **Contract for the Designer**: MUST derive triggers from these keys plus `ActionReference` (`ACT-024`).
- **Contract for the Runtime**: none.
- **Server exposure**: `SimplePropertyDefinition.metadata`.
- **Maturity level**: 1 - action wiring.
- **Example**:

```json
{ "action::update": "guess", "action::update::parameters": ".", "action::update::after": "", "action::update::activeIf": "" }
```

### ACT-027 (moved) -> see DSG-013

