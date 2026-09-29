# CFG - Configuration model

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`). Category prefix `CFG-`.
> Machine-readable twin: [index.CFG.json](index.CFG.json). Markdown and JSON MUST agree.
> Related: [DSG](DSG-design.md), [UI](UI-ui.md), [VAL](VAL-validation.md), [ACT](ACT-actions.md), [05 configuration and UI](../05-configuration-and-ui.md), [appendix configuration-types](../10-appendix/configuration-types.md).

## Overview

A component declares its configuration through `@Option` fields/constructor parameters. `ParameterModelService` (`component-runtime-manager`) turns the `@Option` tree into a list of `ParameterMeta` (`path`, `name`, `type`, nested, `metadata` with `tcomp::` keys). The component server flattens that tree into `SimplePropertyDefinition` records (see `CFG-014`); the designer renders a form from them and serializes user input back to a flat `Map<String,String>` (see `CFG-002`) that the runtime turns into an object graph.

Ownership: `@Hidden`, `@DefaultValue`, `@OptionsOrder` are in package `configuration.ui` but classified here (CLAUDE.md table); layouts and widgets are in [UI](UI-ui.md).

### CFG-001 `@Option`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/Option.java` (+ `documentation/src/main/antora/modules/ROOT/pages/component-configuration.adoc`)
- **Category / Sub-area**: CFG / option declaration
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`)
- **Applies to**: input, processor, output, standalone (constructor parameters and nested fields), services (action method parameters)
- **Description**: marks a configuration field or a constructor/action parameter. Only `@Option` fields (non-static, non-synthetic, walking up the class hierarchy) are part of the model; on a first-seen-name basis superclass fields shadowed by a subclass field are skipped. Parameters without `@Option` that are services are injected instead (`ParameterModelService.isService`).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | `""` | Option name. Empty means: the field/parameter name (constructor parameter names need `-parameters` compilation, otherwise `arg0`...). MUST NOT contain `.` and MUST NOT start with `$` (enforced by `OptionNameValidator`). |
| constant `MAX_DURATION_PARAMETER` | `String` | `"maxDurationMs"` | Name of a `@PostConstruct` parameter of a stoppable infinite `@PartitionMapper`/`@Emitter` receiving the max read duration (int/long). |
| constant `MAX_RECORDS_PARAMETER` | `String` | `"maxRecords"` | Same for the max record count. |

- **Contract for the Designer**: MUST build the form from the server `properties`, never from Java classes; MUST NOT invent option names; MUST treat names starting with `$` as technical (see `CFG-016`).
- **Contract for the Runtime**: MUST provide configuration values through the component manager (flat map, see `CFG-002`); `@Option` fields MUST NOT be `final` (`NoFinalOptionValidator`).
- **Server exposure**: `SimplePropertyDefinition.path` / `.name` of `ComponentDetail.properties`.
- **Maturity level**: 0 - no component can be configured or run without it.
- **Example**:

```java
public MyInput(@Option("configuration") final MyConfig config) { }
```

### CFG-002 Option path and flat configuration map syntax
- **Source**: `documentation/src/main/antora/modules/ROOT/pages/component-configuration.adoc` (+ `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/configuration/ConfigurationMapper.java`)
- **Category / Sub-area**: CFG / serialization
- **Kind**: convention
- **Applies to**: all configurable components and action calls
- **Description**: a configuration is a flat `Map<String,String>`. The key is the *dot path* of the option. Rules: nested object = `parent.child`; list element = `list[0].child` (primitive list element = `list[0]`); map = `map.key[0]`, `map.value[0]` (and `map.key[0].field` / `map.value[0].field` for object keys/values); to truncate an array inherited from another layer use `list[length] = N`. In `ParameterMeta` paths the placeholder `${index}` is used (`list[${index}].child`); `PropertiesService` removes `${index}` so the server path is `list[].child` and the nested element path of an array of objects is `list[]` (`.field`). The roots are `configuration.` (or the `@Option` name of the constructor parameter); when a component has no root object the framework creates the umbrella `$configuration`. Values are strings; booleans `true`/`false`, numbers decimal, enums by constant name.
- **Attributes**: none.
- **Contract for the Designer**: MUST serialize user input to this flat form (prefix rules and `[i]` indexes) for `/action/execute` and for saved configuration; MUST use the same paths for `@Suggestable`/`@Updatable`/`@ActiveIf` targets (relative-path syntax in [05](../05-configuration-and-ui.md)).
- **Contract for the Runtime**: MUST convert the flat map to the component object graph via the component manager (`ReflectionService`); unknown keys are ignored (inferred), missing values keep field defaults.
- **Server exposure**: request body of `/action/execute`; `configuration.<path>` keys in runtime URIs (e.g. `file://out?configuration.$maxBatchSize=3&__version=1`).
- **Maturity level**: 0 - configuration serialization is required for correctness.
- **Example**:

```properties
configuration.dataset.datastore.url = jdbc:x
configuration.tables[0].name = a
configuration.tables[1].name = b
configuration.headers.key[0] = X-Id
configuration.headers.value[0] = 12
```

### CFG-003 Parameter types and type mapping
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/reflect/ParameterModelService.java` (+ `component-configuration.adoc`)
- **Category / Sub-area**: CFG / types
- **Kind**: convention
- **Applies to**: all `@Option` values
- **Description**: Java type -> `ParameterMeta.Type` (server `type` string, upper-case in `SimplePropertyDefinition.type`):

| Java type | server `type` | notes |
|---|---|---|
| `boolean`/`Boolean` | `BOOLEAN` | |
| `char`/`Character`, `String`, any type with an xbean `Converter` (`BigDecimal`, `BigInteger`, `File`, `InetAddress`, `ObjectName`, `URI`, `URL`, `Pattern`, `LocalDateTime`, `ZonedDateTime`, ...) | `STRING` | flattened as string |
| other primitives and their wrappers (`int`, `long`, `double`, ...) | `NUMBER` | |
| `enum` | `ENUM` | constants exposed in `validation.enumValues` in declaration order (no sorting), labels in `proposalDisplayNames` |
| array, `Collection<T>` | `ARRAY` | `T` MUST be a concrete class |
| `Map<K,V>` | `OBJECT` | children `key[${index}]` and `value[${index}]`; `K`,`V` MUST be concrete classes; avoid maps |
| any other class | `OBJECT` | children = its `@Option` fields, sorted by name |

`LocalDate`, `LocalTime`, `LocalDateTime`, `ZonedDateTime` additionally receive an implicit `@DateTime` (see `UI-010`). Unsupported (`ParameterizedType` with non-class args) types raise `IllegalArgumentException` at load.
- **Attributes**: none.
- **Contract for the Designer**: MUST support at least `STRING`, `NUMBER`, `BOOLEAN`, `ENUM`, `OBJECT`, `ARRAY`; MUST treat unknown `type` values as `STRING` (inferred).
- **Contract for the Runtime**: MUST rely on the component manager conversion (property editors); MUST NOT reimplement type conversion.
- **Server exposure**: `SimplePropertyDefinition.type`.
- **Maturity level**: 0 - types drive both form fields and value parsing.
- **Example**:

```json
{ "path": "configuration.port", "name": "port", "type": "NUMBER", "defaultValue": "8080" }
```

### CFG-004 `@DefaultValue`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/DefaultValue.java`
- **Category / Sub-area**: CFG / defaults
- **Kind**: annotation (`@Target(FIELD)`, meta `@Ui`)
- **Applies to**: primitive/String fields of configurations
- **Description**: "Provide a default value the UI can use - only for primitive fields." Exposed as property metadata `ui::defaultvalue::value` (`tcomp::ui::defaultvalue::value` in-JVM). It wins over `SimplePropertyDefinition.defaultValue` in `component-form` (`metadata.getOrDefault("ui::defaultvalue::value", defaultValue)`). The framework itself uses it for the built-in options of `CFG-016`.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | (required) | Default value as string. |

- **Contract for the Designer**: MUST pre-fill the field with `ui::defaultvalue::value` when present, else with `defaultValue`; MUST convert per property type.
- **Contract for the Runtime**: none (the runtime receives the value the designer stored; the Java field initializer applies if absent).
- **Server exposure**: property metadata `ui::defaultvalue::value`.
- **Maturity level**: 1 - degraded UX without prefilled values.
- **Example**:

```json
{ "ui::defaultvalue::value": "1000" }
```

### CFG-005 `@Hidden`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/Hidden.java`
- **Category / Sub-area**: CFG / visibility
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, meta `@Ui`)
- **Applies to**: options
- **Description**: "Mark an @Option as being hidden." Metadata `ui::hidden` = `true`. `component-form` renders it by attaching the always-false condition `{"==":[1,-1]}` to the widget.
- **Attributes**: none.
- **Contract for the Designer**: MUST NOT show the field to the user; MUST still keep it in the model and send its value in flat configuration.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `ui::hidden`.
- **Maturity level**: 1 - a shown hidden field is a UX defect, not a correctness one.
- **Example**:

```json
{ "ui::hidden": "true" }
```

### CFG-006 `@OptionsOrder`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/ui/OptionsOrder.java`
- **Category / Sub-area**: CFG / ordering
- **Kind**: annotation (`@Target(TYPE)`, meta `@Ui`)
- **Applies to**: configuration classes
- **Description**: "Allows to sort a class properties." Exclusive with `@GridLayout` (if both exist the validator logs an error and `@OptionsOrder` is ignored). Metadata `ui::optionsorder::value` = comma separated names. Options not listed sort last (`Integer.MAX_VALUE`); default order without it is alphabetical by path.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String[]` | (required) | Ordered option names (joined by `,` in metadata). |

- **Contract for the Designer**: MUST render children in this order when no grid layout applies (one widget per row).
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `ui::optionsorder::value`.
- **Maturity level**: 1 - ordering only.
- **Example**:

```json
{ "ui::optionsorder::value": "url,username,password" }
```

### CFG-007 `@ConfigurationType` (meta-annotation)
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/type/meta/ConfigurationType.java`
- **Category / Sub-area**: CFG / configuration types
- **Kind**: annotation (`@Target(ANNOTATION_TYPE)`)
- **Applies to**: annotations that classify a configuration class (`@DataStore`, `@DataSet`, `@DatasetDiscovery`, `@DynamicDependenciesConfiguration`, and `@Checkpoint` from the RUN catalog)
- **Description**: `value` is the configuration type id. `ConfigurationTypeParameterEnricher` emits two metadata entries for the annotated class: `configurationtype::type` = type id, `configurationtype::name` = the `value()` of the classifying annotation (default `"default"`).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | (required) | Type id: `datastore`, `dataset`, `datasetDiscovery`, `dynamicDependenciesConfiguration`, `checkpoint`. |

- **Contract for the Designer**: MUST use `configurationtype::type` to recognize reusable configuration nodes inside a component form.
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `configurationtype::type`, `configurationtype::name`.
- **Maturity level**: 1 - required for dataset/datastore reuse.
- **Example**:

```json
{ "configurationtype::type": "dataset", "configurationtype::name": "DatasetA" }
```

### CFG-008 `@DataStore`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/type/DataStore.java` (+ `documentation/src/main/antora/modules/ROOT/pages/creating-dataset-datastore.adoc`)
- **Category / Sub-area**: CFG / configuration types
- **Kind**: annotation (`@Target(TYPE)`, `@ConfigurationType("datastore")`)
- **Applies to**: configuration classes (connection to a backend: URL, credentials...)
- **Description**: marks a model as a datastore. Each `@DataSet` MUST reference a datastore (`DatasetValidator`). A datastore is the parameter of `@HealthCheck` (`ACT-003`), `@DiscoverDataset` (`ACT-009`), `@CreateConnection` (`ACT-011`), `@DatabaseSchemaMapping` (`ACT-013`). Display name key: `${family}.datastore.${name}._displayName`.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | `"default"` | Datastore name, unique per family. |

- **Contract for the Designer**: MUST offer datastore reuse (create/edit/pick a saved datastore) and MUST offer a connection test button when a health check of the same name exists.
- **Contract for the Runtime**: MUST feed the datastore values as part of the flat configuration of the referencing component.
- **Server exposure**: property metadata `configurationtype::*`; configuration type tree (`CFG-017`); health-check action reference.
- **Maturity level**: 1 - dataset/datastore reuse is level 1.
- **Example**:

```java
@DataStore("DatastoreA")
public class DatastoreA implements Serializable { @Option private String url; }
```

### CFG-009 `@DataSet`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/type/DataSet.java`
- **Category / Sub-area**: CFG / configuration types
- **Kind**: annotation (`@Target(TYPE)`, `@ConfigurationType("dataset")`)
- **Applies to**: configuration classes (a datastore plus what is needed to read/write data)
- **Description**: marks a model as a dataset. Rules enforced at build time: every input/output component MUST reference a dataset; each dataset MUST reference a datastore; at least one source MUST be instantiable when only the dataset part is filled (other properties optional); dataset names are unique. Parameter of `@DiscoverSchema` (`ACT-007`). Display name key `${family}.dataset.${name}._displayName`.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | `"default"` | Dataset name, unique per family. |

- **Contract for the Designer**: MUST allow selecting/creating a dataset for input/output components and SHOULD pass the dataset configuration to schema discovery.
- **Contract for the Runtime**: MUST inject the dataset values as part of the component configuration.
- **Server exposure**: property metadata `configurationtype::*`; configuration type tree (`CFG-017`).
- **Maturity level**: 1 - dataset reuse is level 1.
- **Example**:

```java
@DataSet("DatasetA")
public class DatasetA implements Serializable { @Option private DatastoreA datastore; }
```

### CFG-010 `@DatasetDiscovery`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/type/DatasetDiscovery.java`
- **Category / Sub-area**: CFG / configuration types
- **Kind**: annotation (`@Target(TYPE)`, `@ConfigurationType("datasetDiscovery")`)
- **Applies to**: configuration classes used by dataset-discovery input components
- **Description**: marks a model as a dataset discovery configuration. The class MUST implement `DatasetDiscoveryConfiguration` (`CFG-011`) and MUST reference a datastore (`DatasetDiscoveryValidator`); i18n key `${family}.datasetdiscovery.${name}._displayName`. A component using it counts as having a dataset (`DatasetValidator` accepts `dataset` or `datasetDiscovery`).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | `"default"` | Name, unique per family. |

- **Contract for the Designer**: MAY use it to let the user browse available datasets of a datastore (pairs with `ACT-009`).
- **Contract for the Runtime**: MUST honor `retrieveDataset` semantics documented in `CFG-011` if it executes discovery components.
- **Server exposure**: property metadata `configurationtype::type=datasetDiscovery`.
- **Maturity level**: 2 - optional discovery capability.
- **Example**:

```java
@DatasetDiscovery("Discovery")
public class Disc extends DatasetDiscoveryConfiguration { @Option private DatastoreA datastore; }
```

### CFG-011 `DatasetDiscoveryConfiguration`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/type/DatasetDiscoveryConfiguration.java`
- **Category / Sub-area**: CFG / configuration types
- **Kind**: class (base of `@DatasetDiscovery` models)
- **Applies to**: dataset discovery configurations
- **Description**: base class with one option `boolean retrieveDataset` (`@DefaultValue("false")`). Semantics of the flag are not described in the API javadoc; from the type name it toggles between listing dataset names and retrieving one dataset (inferred, unverified).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `retrieveDataset` | `boolean` | `false` | Discovery mode flag (option path `<root>.retrieveDataset`). |

- **Contract for the Designer**: MUST render it like any boolean option.
- **Contract for the Runtime**: MUST pass its value like any option.
- **Server exposure**: normal property `retrieveDataset` with `ui::defaultvalue::value=false`.
- **Maturity level**: 2 - belongs to the optional discovery capability.
- **Example**:

```json
{ "configuration.retrieveDataset": "false" }
```

### CFG-012 `@DynamicDependenciesConfiguration`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/type/DynamicDependenciesConfiguration.java`
- **Category / Sub-area**: CFG / configuration types
- **Kind**: annotation (`@Target(TYPE)`, `@ConfigurationType("dynamicDependenciesConfiguration")`)
- **Applies to**: configuration classes
- **Description**: marks the model that is the input of a `@DynamicDependencies` action (`ACT-010`), i.e. the configuration from which extra Maven dependencies (GAV) are computed.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | `"default"` | Name. |

- **Contract for the Designer**: MAY call the dynamic dependencies action with this configuration to resolve extra jars before running.
- **Contract for the Runtime**: SHOULD resolve the returned GAVs into the component classloader (details: SVC/LCM catalogs).
- **Server exposure**: property metadata `configurationtype::type=dynamicDependenciesConfiguration`.
- **Maturity level**: 2 - optional capability.
- **Example**:

```java
@DynamicDependenciesConfiguration("deps")
public class DepsConfig { @Option private String driver; }
```

### CFG-013 `@ConnectorRef` and `ConnectorReference`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/dependency/ConnectorRef.java` (+ `.../dependency/ConnectorReference.java`)
- **Category / Sub-area**: CFG / connector references
- **Kind**: annotation (`@Target(FIELD)`) + deprecated class `ConnectorReference` (since 1.58.0)
- **Applies to**: `String` options that reference another connector
- **Description**: marks a `String` field as one part of a reference to another connector. `ConnectorRefValue` = `MAVEN_REFERENCE` (`mavenReference`), `NAME` (`name`), `FAMILY` (`family`). `DependencyParameterEnricher` emits metadata `dependencies::connector` = the part (`mavenReference`/`name`/`family`) on the field, and `dependencies::connector` = `family` on a class type/collection whose fields carry `@ConnectorRef` (e.g. `ConnectorReference`, `List<ConnectorReference>`).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `ConnectorRefValue` | (required) | Which part of the reference the field holds. |

- **Contract for the Designer**: MAY offer a connector picker filling the fields; MUST otherwise render normal strings.
- **Contract for the Runtime**: none beyond normal option injection.
- **Server exposure**: property metadata `dependencies::connector`.
- **Maturity level**: 2 - optional capability.
- **Example**:

```java
@Option @ConnectorRef(ConnectorRef.ConnectorRefValue.NAME) private String name;
```

### CFG-014 Property definition and metadata model (`SimplePropertyDefinition`)
- **Source**: `component-server-parent/component-server-model/src/main/java/org/talend/sdk/component/server/front/model/SimplePropertyDefinition.java` (+ `component-server-parent/component-server/src/main/java/org/talend/sdk/component/server/service/PropertiesService.java`)
- **Category / Sub-area**: CFG / property payload
- **Kind**: payload field
- **Applies to**: every option
- **Description**: one record per option, sorted by `path`. Fields: `path`, `name`, `displayName` (i18n or name), `type` (`CFG-003`), `defaultValue` (string; see below), `validation` (`PropertyValidation`, see `VAL-007`; `null` when no constraint), `metadata` (map, keys below), `placeholder` (i18n or name), `proposalDisplayNames` (ordered map enum constant -> label, only for `ENUM`). Metadata rules: `tcomp::` prefix stripped; keys starting with `tcomp::validation::` removed (moved to `validation`); `definition::parameter::index` added for root parameters (position in the method signature); `documentation::value` replaced by the bundle value when present; with grid layout translation enabled `ui::gridlayout::<tab>::value` keys are renamed to the translated tab. `defaultValue` is derived by instantiating the configuration class with its no-arg constructor (`DefaultValueInspector`): `null` for objects, for values whose parent could not be instantiated, and for primitives of created-but-uninitialized instances; enum = constant name; collections/maps = JSON string. Metadata key catalogue (all are strings):

| key | source | see |
|---|---|---|
| `ui::gridlayout::<name>::value`, `ui::optionsorder::value`, `ui::autolayout`, `ui::horizontallayout`, `ui::verticallayout` | layouts | `UI-002`..`UI-006` |
| `ui::code::value`, `ui::credential`, `ui::textarea`, `ui::readonly`, `ui::hidden`, `ui::defaultvalue::value`, `ui::modulelist`, `ui::basedonschema`, `ui::path::value`, `ui::structure::{value,discoverSchema,type}`, `ui::datetime`, `ui::datetime::{dateFormat,useSeconds,useUTC}` | widgets | `UI-007`..`UI-015` |
| `condition::if::{target,value,negate,evaluationStrategy}` (`::<i>` suffix when in `ifs`), `condition::ifs::operator` | conditions | `UI-016`, `UI-017` |
| `action::<type>`, `action::<type>::parameters`, `action::<type>::<extra>`, `action::built_in_suggestable` | actions | `ACT-026` |
| `configurationtype::type`, `configurationtype::name` | config types | `CFG-007` |
| `dependencies::connector` | connector refs | `CFG-013` |
| `documentation::value`, `documentation::tooltip` | `@Documentation` | `DSG-007` |
| `definition::parameter::index` | server | this entry |

- **Attributes**: see field list above.
- **Contract for the Designer**: MUST build forms and default values from these records; MUST NOT expect `validation::*` keys in `metadata` (they are in `validation`); note the generated documentation pages (`generated_constraints.adoc`) still show `validation::*` web keys (documented as a discrepancy).
- **Contract for the Runtime**: none (runtime uses in-JVM `ParameterMeta`).
- **Server exposure**: `ComponentDetail.properties`, `ConfigTypeNode.properties`, `ActionItem.properties`, `ActionReference.properties`.
- **Maturity level**: 0 - this payload is the designer's whole view of configuration.
- **Example**:

```json
{
  "path": "configuration.password", "name": "password", "displayName": "Password",
  "type": "STRING", "defaultValue": null, "validation": { "required": true },
  "metadata": { "ui::credential": "true", "definition::parameter::index": "0" },
  "placeholder": "password"
}
```

### CFG-015 `ParameterExtensionEnricher` SPI
- **Source**: `component-spi/src/main/java/org/talend/sdk/component/spi/parameter/ParameterExtensionEnricher.java` (+ `component-runtime-manager/src/main/resources/META-INF/services/org.talend.sdk.component.spi.parameter.ParameterExtensionEnricher`)
- **Category / Sub-area**: CFG / metadata extension
- **Kind**: interface (Java `ServiceLoader` SPI)
- **Applies to**: options
- **Description**: computes property metadata from annotations. `Map<String,String> onParameterAnnotation(String parameterName, Type parameterType, Annotation annotation)` is invoked for each annotation of each option (field annotations plus, for classes, class annotations not overridden at field level); `default Map<Type, Collection<Annotation>> getImplicitAnnotationForTypes()` adds implicit annotations per type (used for `@DateTime` on java.time types and `@Min`/`@Max` on `int`/`Integer`). Two enrichers producing different values for the same key cause `IllegalArgumentException("Ambiguous metadata")`. Built-in enrichers: `CharConstraintEnricher`, `IntegerConstraintEnricher`, `ConfigurationTypeParameterEnricher`, `ValidationParameterEnricher`, `ConditionParameterEnricher`, `ActionParameterEnricher`, `UiParameterEnricher`, `DocumentationParameterEnricher`, `DependencyParameterEnricher`.
- **Attributes**: see description.
- **Contract for the Designer**: none.
- **Contract for the Runtime**: MUST keep `ServiceLoader` discovery of built-in enrichers when embedding `component-runtime-manager`; MAY register custom enrichers (recommended: one key prefix per extension).
- **Server exposure**: resulting keys in property `metadata`.
- **Maturity level**: 2 - extension point.
- **Example**:

```
META-INF/services/org.talend.sdk.component.spi.parameter.ParameterExtensionEnricher
```

### CFG-016 Built-in technical options (`$maxBatchSize`, `$maxRecords`, `$maxDurationMs`, `$lang`)
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/builtinparams/MaxBatchSizeParamBuilder.java` (+ `StreamingLongParamBuilder.java`)
- **Category / Sub-area**: CFG / technical options
- **Kind**: convention (virtual options injected into the model)
- **Applies to**: processors/outputs with an `@AfterGroup` method (`$maxBatchSize`); infinite (streaming) mappers/emitters (`$maxRecords`, `$maxDurationMs`); action calls (`$lang`)
- **Description**: `ComponentManager` appends virtual `NUMBER` options under the root option (path `<root>.$name`, e.g. `configuration.$maxBatchSize`; the umbrella root `$configuration` is created when the component has no root option). They carry `ui::defaultvalue::value` and a minimum (`tcomp::validation::min`): `$maxBatchSize` min `1`, default from local configuration `<ComponentSimpleClassName>._maxBatchSize.value` or `_maxBatchSize.value` (default `1000`; not added when `..._maxBatchSize.active=false` or value `<= 0`); `$maxRecords` and `$maxDurationMs` min `-1`, default from local configuration keys `<ComponentClassName>$maxRecords` / `$maxRecords` (default `-1`; same for `$maxDurationMs`). They are appended to the layout: into `ui::gridlayout::Advanced::value` if the root uses a grid layout (a `Main` layout listing the other options is created if the root had no layout), otherwise into the root's layout marker. Names are translated by the bundle `builtinparams/Messages.properties` (`Max batch size`, `Max records`, `Max duration time (ms)`). `$lang` is not part of the model: the server adds it to action parameters. The API constants `Option.MAX_RECORDS_PARAMETER`/`MAX_DURATION_PARAMETER` (`maxRecords`/`maxDurationMs`) are the parameter names of `@PostConstruct` methods, distinct from the `$`-prefixed configuration keys (the runtime maps both, see RUN catalog).
- **Attributes**:

| option | type | default | meaning |
|---|---|---|---|
| `$maxBatchSize` | `NUMBER` | `1000` (configurable) | Group size for `@AfterGroup` batching. |
| `$maxRecords` | `NUMBER` | `-1` | Max records to read from an infinite source. |
| `$maxDurationMs` | `NUMBER` | `-1` | Max read duration in ms for an infinite source. |

- **Contract for the Designer**: MUST render them like normal options (they appear in `properties`) and MUST send them back in the flat configuration; MUST NOT treat the `$` prefix as an error.
- **Contract for the Runtime**: MUST honor `$maxBatchSize` for group sizing, `$maxRecords`/`$maxDurationMs` for stopping infinite sources (Beam reference: `BaseProcessorFn`, `TalendIO`).
- **Server exposure**: entries in `ComponentDetail.properties` (`configuration.$maxBatchSize`, ...).
- **Maturity level**: 1 - defaults still work without them but batch/stop control is lost.
- **Example**:

```properties
configuration.$maxBatchSize=500
configuration.$maxRecords=10000
configuration.$maxDurationMs=60000
```

### CFG-017 Configuration type tree (`ConfigTypeNode`)
- **Source**: `component-runtime-design-extension/src/main/java/org/talend/sdk/component/design/extension/repository/RepositoryModelBuilder.java` (+ `component-server-parent/component-server-model/src/main/java/org/talend/sdk/component/server/front/model/ConfigTypeNode.java`)
- **Category / Sub-area**: CFG / configuration types
- **Kind**: payload field convention
- **Applies to**: datastores, datasets and other configuration types
- **Description**: for each family the design extension scans the parameter models of mappers, processors and driver runners, flattens them and keeps every parameter whose metadata contains a `configurationtype::*` key. Each becomes a node: key = (family, `configurationtype::name`, `configurationtype::type`), `version` = the `@Version` of the config class (or `-1` when absent), migration handler from `@Version.migrationHandler` (else identity). Config paths are rewritten so the config root is named `configuration` (e.g. dataset property `configuration.datastore.url` in the type tree). Nesting: a config whose model contains another config type becomes its child (edges = child ids; e.g. datastore -> dataset). Server node fields: `id`, `version`, `parentId`, `configurationType`, `name`, `displayName`, `edges`, `properties`, `actions`.
- **Attributes**: see description.
- **Contract for the Designer**: MUST browse the tree to offer reusable datastores/datasets, MUST resolve `parentId`/`edges`; MUST store the node `version` with saved instances and migrate on version change.
- **Contract for the Runtime**: none.
- **Server exposure**: `/configurationtype/index`, `/configurationtype/details`, configuration type migrate endpoint (see [03](../03-component-server-api.md)).
- **Maturity level**: 1 - dataset/datastore reuse.
- **Example**:

```json
{ "id": "idForDstore", "configurationType": "datastore", "name": "DatastoreA", "edges": ["idForDset"] }
```

### CFG-018 `ConfigurationMapper` (object to flat map)
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/configuration/ConfigurationMapper.java`
- **Category / Sub-area**: CFG / serialization
- **Kind**: class (runtime utility)
- **Applies to**: hosts embedding `component-runtime-manager`
- **Description**: `Map<String,String> map(List<ParameterMeta> nestedParameters, Object instance)` converts a configuration instance to the flat key/value map of `CFG-002`: null values are skipped; objects recurse; arrays expand to `path[i]` (indexes resolved from `${index}`); primitives use `String.valueOf`. Fields are found by `@Option` name (superclass walk).
- **Attributes**: none.
- **Contract for the Designer**: none (browser hosts implement the same rules by hand).
- **Contract for the Runtime**: MAY use it to build configuration maps from typed objects (tests, adapters).
- **Server exposure**: none.
- **Maturity level**: 2 - convenience, the flat format is the contract.
- **Example**:

```java
Map<String,String> flat = new ConfigurationMapper().map(metas, configInstance);
```

### CFG-019 Datastore/dataset design rules (build-time)
- **Source**: `component-tools/src/main/java/org/talend/sdk/component/tools/validator/DatasetValidator.java` (+ `DatasetDiscoveryValidator.java`, `DataStoreValidator.java`)
- **Category / Sub-area**: CFG / rules
- **Kind**: convention
- **Applies to**: component authors and hosts that validate third-party plugins
- **Description**: rules checked by `talend-component-maven-plugin validate`: every input (`@PartitionMapper`/`@Emitter`) and output (`@Processor` with only void `@ElementListener`/`@AfterGroup` without `@Output`) MUST have a `dataset` (or `datasetDiscovery`) option; every dataset MUST contain a `datastore`; dataset (and datasetDiscovery) names MUST be unique; at least one source using each dataset MUST be instantiable filling only the dataset (no other required option outside the dataset subtree); a display-name key MUST exist (`${family}.dataset.<name>._displayName`, `${family}.datasetdiscovery.<name>._displayName`); `@DatasetDiscovery` classes MUST extend `DatasetDiscoveryConfiguration`.
- **Attributes**: none.
- **Contract for the Designer**: MAY rely on these invariants (an input always has a dataset which contains a datastore).
- **Contract for the Runtime**: none.
- **Server exposure**: none.
- **Maturity level**: 2 - informational for hosts that do not gate plugin publication.
- **Example**:

```
mvn talend-component:validate
```
