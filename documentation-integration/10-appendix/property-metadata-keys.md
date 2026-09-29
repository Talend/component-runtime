# Appendix: `metadata` keys of `SimplePropertyDefinition` and component payloads

- **Framework version documented**: `1.2611.0-SNAPSHOT` (root `pom.xml`; last release tag in git: `component-runtime-1.2610.0`).
- **Scope**: every key that can appear in the `metadata` map of a `SimplePropertyDefinition` (returned inside `ComponentDetail.properties`, `ActionReference.properties`, `ActionItem.properties`, `ConfigTypeNode.properties`) and in the component-level `metadata` map of `ComponentIndex` / `ComponentDetail`.
- **Sources of truth (code)**: `component-runtime-manager/.../reflect/parameterenricher/*Enricher.java`, `component-runtime-manager/.../extension/*MetadataEnricher.java`, `component-runtime-manager/.../reflect/ComponentMetadataService.java`, `component-runtime-manager/.../builtinparams/*`, `component-server/.../service/PropertiesService.java`, `component-server/.../service/PropertyValidationService.java`, `component-form-core/.../AbstractWidgetConverter.java` (consumer side).
- Related: [03-component-server-api.md](../03-component-server-api.md) (payload tables), [server-configuration.md](server-configuration.md), [error-codes.md](error-codes.md).

## 1. How the map is produced (normative for the host)

1. Each `ParameterExtensionEnricher` (registered in `META-INF/services/org.talend.sdk.component.spi.parameter.ParameterExtensionEnricher`) turns an annotation on an `@Option` into raw keys prefixed with `tcomp::`. The nine enrichers are: `CharConstraintEnricher`, `IntegerConstraintEnricher`, `ConfigurationTypeParameterEnricher`, `ValidationParameterEnricher`, `ConditionParameterEnricher`, `ActionParameterEnricher`, `UiParameterEnricher`, `DocumentationParameterEnricher`, `DependencyParameterEnricher`.
2. `PropertiesService.buildProperties` (server) then post-processes each property:
   - **Prefix stripping**: the literal `tcomp::` is removed from every key (`"tcomp::ui::credential"` becomes `"ui::credential"`).
   - **Validation keys are removed from `metadata`**: every key starting with `tcomp::validation::` is dropped from the map and folded into the `validation` object (`PropertyValidation`, see section 8).
   - **`definition::parameter::index`** is added **only to top-level parameters** of the owning component/action/config (`parent == null`): its value is the 0-based index of the parameter in the method/constructor signature, as a string. Nested properties do not carry it.
   - **i18n**: `documentation::value` is replaced by the resource-bundle value when it differs; when `talend.component.server.gridlayout.translation.support=true`, the tab name inside `ui::gridlayout::<Tab>::value` keys is replaced by its translation (see [server-configuration.md](server-configuration.md)).
3. Values are always `String`. Arrays of strings are joined with `,` unless stated otherwise.
4. **Component-level metadata (`ComponentIndex.metadata`, `ComponentDetail.metadata`) is NOT prefix-stripped.** Keys coming from `ComponentSchemaEnricher` keep `tcomp::` (asserted by `ComponentResourceImplTest#getDatabaseMappingMetadata`, which reads `ComponentSchemaEnricher.SCHEMA_MAPPING == "tcomp::ui::schema::mapping"`). See section 9.

A host MUST treat unknown keys as opaque (custom `ParameterExtensionEnricher` / `ComponentMetadataEnricher` SPIs may add keys). A host SHOULD match keys case-sensitively.

## 2. Configuration type and dependency keys

| Key | Produced by | Value | Meaning / host action |
|---|---|---|---|
| `configurationtype::type` | `@DataStore`, `@DataSet`, `@DatasetDiscovery`, `@DynamicDependenciesConfiguration`, `@Checkpoint` (all meta-annotated with `@ConfigurationType`) | `datastore` \| `dataset` \| `datasetDiscovery` \| `dynamicDependenciesConfiguration` \| `checkpoint` | Marks the object as a reusable configuration type. Designer SHOULD offer save/reuse of the object. Also drives `/configurationtype/*` and the `q` query filter. |
| `configurationtype::name` | same annotations (`value()` attribute, default `default`) | string | Logical name of the config type inside the family. Used to bind a healthcheck (`action::healthcheck`) to a datastore and `@DiscoverSchema` to a dataset. |
| `dependencies::connector` | `DependencyParameterEnricher` | `family` \| `name` \| `mavenReference` | On a `String` field annotated `@ConnectorRef(ConnectorRefValue.X)`: value is `X.getRefValue()` (`mavenReference`, `name`, `family`). On a class (or collection of a class) that has at least one `@ConnectorRef` field: constant `family`. Used by dynamic-dependency resolution (runtime side). |

Legacy note: the test fixture `component-form-core/src/test/resources/jdbc.json` contains a key `dataset` (value `default`); no enricher in the current code emits it (unverified origin, older server). Hosts MUST NOT depend on it.

## 3. Documentation keys

| Key | Value | Meaning |
|---|---|---|
| `documentation::value` | free text | Text of `@Documentation("...")` on the option/class; overridden by the i18n bundle when `ParameterBundle.documentation` returns a different text (exact bundle key format not analysed: unverified). Rendered as description; `component-form` copies it to `UiSchema.description` only when `PropertyContext.Configuration.includeDocumentationMetadata` is true (default false). |
| `documentation::tooltip` | `true` (only present when true) | `@Documentation(tooltip = true)`. `component-form` copies `documentation::value` to `UiSchema.tooltip`. |

## 4. UI keys (`@Ui`-meta-annotated annotations)

Rule (`UiParameterEnricher`): key = `ui::<annotationSimpleName lowercased>` and, for each declared attribute of the annotation, `::<attributeName>`. An annotation without attributes yields `ui::<name>` = `true`. `Class`-typed attributes are lowercased simple names; `String[]` are comma-joined. A `String` attribute value starting with `local_configuration:` is replaced by the value of that key in `LocalConfiguration`.

| Key(s) | Annotation | Value | Notes |
|---|---|---|---|
| `ui::defaultvalue::value` | `@DefaultValue` (`org.talend.sdk.component.api.configuration.ui`) | default as string | Preferred over `SimplePropertyDefinition.defaultValue` by `component-form`. Also set by built-in technical options (section 7). |
| `ui::hidden` | `@Hidden` | `true` | `component-form` renders it as an always-false condition (`{"==":[1,-1]}`). |
| `ui::optionsorder::value` | `@OptionsOrder` | comma-joined property names | Field order for an object; exclusive with `@GridLayout`. |
| `ui::autolayout` | `@AutoLayout` | `true` | Renderer's choice. |
| `ui::horizontallayout` | `@HorizontalLayout` | `true` | |
| `ui::verticallayout` | `@VerticalLayout` | `true` | |
| `ui::gridlayout::<Tab>::value` | `@GridLayout(names = {...}, value = {@Row({...}), ...})`, `@GridLayouts` | rows joined by `\|`, fields in a row joined by `,` (e.g. `first\|second,third`) | One key per tab name in `names` (default `Main`; conventional names `Main`, `Advanced`, `Checkpoint`; `CitizenUser` is deprecated). Tab names are matched case-insensitively by `component-form`. |
| `ui::readonly` | `@ReadOnly` | `true` | |
| `ui::credential` | `@Credential` | `true` | Mask the widget (password). The server uses this key to decide which action parameters to decipher (`vault:` values, see 03 file). |
| `ui::textarea` | `@TextArea` | `true` | Multiline widget. |
| `ui::code::value` | `@Code("lang")` | language id | Code editor; `component-form` sets `options.language`. |
| `ui::datetime` | `@DateTime` (also implicit on `ZonedDateTime`, `LocalDateTime`, `LocalDate`, `LocalTime`) | `date` (LocalDate), `time` (LocalTime), `datetime` (LocalDateTime), `zoneddatetime` (ZonedDateTime or `Object`) | Unsupported field type raises `IllegalArgumentException` at server model build. |
| `ui::datetime::dateFormat` | `@DateTime.dateFormat` | default `YYYY/MM/DD` | Present for date, datetime, zoneddatetime. |
| `ui::datetime::useSeconds` | `@DateTime.useSeconds` | `true`/`false` (default `true`) | Present for time, datetime, zoneddatetime. |
| `ui::datetime::useUTC` | `@DateTime.useUTC` | `true`/`false` (default `true`) | Present for datetime, zoneddatetime. |
| `ui::modulelist` | `@ModuleList` | `true` | Studio-only widget (annotation doc: "only for studio"). |
| `ui::basedonschema` | `@BasedOnSchema` | `true` | Studio-only. |
| `ui::path::value` | `@Path` | `FILE` \| `DIRECTORY` | Studio-only. Enum attribute rendered with `String.valueOf`, so upper case. |
| `ui::structure::value` | `@Structure.value` | connection name, default `__default__` | Marks a `List<String>`/`List<Object>` as the schema/column selector. |
| `ui::structure::discoverSchema` | `@Structure.discoverSchema` | `@DiscoverSchema` id or empty | |
| `ui::structure::type` | `@Structure.type` | `IN` \| `OUT` | `component-form` adds a "Guess Schema" button when an `OUT` structure exists. |

Unrecognised `ui::*` keys MUST be ignored by the host. (Source of the list: annotations carrying `@Ui` in `component-api`: `TextArea`, `Structure`, `ReadOnly`, `Path`, `ModuleList`, `DateTime`, `Credential`, `Code`, `BasedOnSchema`, `VerticalLayout`, `HorizontalLayout`, `GridLayouts`, `GridLayout`, `AutoLayout`, `OptionsOrder`, `Hidden`, `DefaultValue`.)

## 5. Action keys

Rule (`ActionParameterEnricher`): for an annotation meta-annotated with `@ActionRef(X.class)`, where `X` carries `@ActionType("t")`:

- `action::t` = value of the annotation attribute named by `@ActionRef.ref` (default `value`);
- `action::t::parameters` = `parameters()` joined with `,` (only if the annotation has `parameters`);
- `action::t::<attr>` = `String.valueOf` of every other declared attribute (the attributes named `value` and `parameters` are excluded from this loop).

For `@ActionRef(Object.class)` (client-side action) the key is `action::<snake_case(annotationSimpleName)>` with value `name()` if `value()` is `CUSTOM`, else the enum name.

| Key | Annotation | Value | Host semantics |
|---|---|---|---|
| `action::healthcheck` | `@Checkable("name")` (class-level, default `default`) | `@HealthCheck` action name | Show a "validate connection" button; call `type=healthcheck`. |
| `action::suggestions` | `@Suggestable("name")` | `@Suggestions` action name | Suggestions when field focused/changed. |
| `action::suggestions::parameters` | `@Suggestable.parameters` | comma list of relative refs (`.` = this, `../foo` = parent's `foo`, `bar` = sibling, `bar/dummy` = child of sibling) | Resolved by `AbsolutePathResolver`. Default `.`. |
| `action::suggestions::labelDisplayMode` | `@Suggestable.labelDisplayMode` | `LABEL` \| `LABEL_ID` | `LABEL_ID`: show id next to label. |
| `action::dynamic_values` | `@Proposable("name")` | `@DynamicValues` action name | Values loaded once at form build time (no parameters). |
| `action::validation` | `@Validable("name")` | `@AsyncValidation` action name | Async server-side validation of the field. |
| `action::validation::parameters` | `@Validable.parameters` | as above, default `.` | |
| `action::update` | `@Updatable("name")` | `@Update` action name | Button that replaces the whole object with the action result. |
| `action::update::parameters` | `@Updatable.parameters` | default empty (no parameters sent) | |
| `action::update::after` | `@Updatable.after` | sibling property name or empty | Where to insert the button. |
| `action::update::activeIf` | `@Updatable.activeIf` | e.g. `target = setBoolean, value = { true }` | `component-form` only parses the text before the first `,` (`target = X`) and then reads `condition::if::value::X` (default `true`); the `value = {...}` part is not interpreted by that code (inferred from `ObjectWidgetConverter.getCondition`). |
| `action::schema` | `@Structure` (`@ActionRef(value = DiscoverSchema.class, ref = "discoverSchema")`) | `@DiscoverSchema` id (may be empty) | Because `ref = "discoverSchema"`, the value is `Structure.discoverSchema`. `component-form` falls back to the action named `default`. |
| `action::schema::type` | `@Structure.type` | `IN`/`OUT` | Extra attribute of the `@ActionRef` loop. |
| `action::built_in_suggestable` | `@BuiltInSuggestable` | `INCOMING_SCHEMA_ENTRY_NAMES` \| `CURRENT_SCHEMA_ENTRY_NAMES` (Studio only) \| custom `name()` | Client-side action, not on the server (`ActionResource` cannot execute it). `component-form` emits a trigger with `family="builtin_client"`, `remote=false`, `onEvent="focus"`. |
| `action::available_output`, `action::user`, `action::dynamic_dependencies`, ... | (none of the `configuration.action` annotations emit these) | n/a | `component-form`'s `isBuiltInAction` list mentions `action::available_output`, but no `@ActionRef` annotation targets it in `component-api` (unverified use). |

Complete list of `@ActionRef` annotations in `component-api`: `@Checkable` (HealthCheck), `@BuiltInSuggestable` (Object.class = client), `@Proposable` (DynamicValues), `@Validable` (AsyncValidation), `@Updatable` (Update), `@Suggestable` (Suggestions), `@Structure` (DiscoverSchema, `ref = discoverSchema`).

## 6. Condition keys

`@ActiveIf` (`@Condition("if")`): one key per attribute, except `evaluationStrategyOptions` (folded into `evaluationStrategy`).

| Key | Value | Notes |
|---|---|---|
| `condition::if::target` | reference path (relative syntax as in section 5, plus the special target `ui.scope`) | Property whose value is tested. |
| `condition::if::value` | expected values joined by `,` | Compared as string, converted to boolean/number using the *target's* declared `type`. |
| `condition::if::negate` | `true`/`false` (default `false`) | |
| `condition::if::evaluationStrategy` | `DEFAULT` \| `LENGTH` \| `CONTAINS`, optionally suffixed `(name=value,...)` e.g. `CONTAINS(lowercase=true)` | `component-form` lowercases before matching (`length`, `contains`, `contains(lowercase=true)`). |

`@ActiveIfs` (`@Condition("ifs")`): each nested `@ActiveIf` produces the four keys above with suffix `::<n>` (`n` = 0-based; computed as `map.size()/4`), e.g. `condition::if::target::0`, `condition::if::value::1`. Plus `condition::ifs::operator` = `AND` (default) \| `OR`.

`ui.scope` target values (`UIScope`): `studio`, `cloud`, `studio.component`, `studio.component.connection`, `studio.metadata`. `component-form` treats a `value` containing `cloud` as "visible in cloud" and everything else as "not visible" (constant true/false condition).

## 7. Built-in technical options (synthetic properties)

The framework injects extra properties **inside the component's root option** (or into a synthetic root `$configuration` when the component has none). They appear in `ComponentDetail.properties` and MUST be exposed like any other property and serialized like any other key.

| Path (relative to root option `configuration`) | When | `type` | Metadata / validation | Default |
|---|---|---|---|---|
| `configuration.$maxRecords` | `@PartitionMapper(infinite = true, stoppable = true)` | `NUMBER` | `ui::defaultvalue::value`=default; `validation.min = -1` | `-1`, overridable via `LocalConfiguration` key `<ComponentSimpleClassName>$maxRecords` or `$maxRecords` |
| `configuration.$maxDurationMs` | same | `NUMBER` | same as above | `-1`, key `<ComponentSimpleClassName>$maxDurationMs` or `$maxDurationMs` |
| `configuration.$maxBatchSize` | a `@Processor` with an `@AfterGroup` method | `NUMBER` | `ui::defaultvalue::value`=default; `validation.min = 1` | `1000`; disabled (property absent) if `<Class>._maxBatchSize.active` / `_maxBatchSize.active` is `false`; default from `<Class>._maxBatchSize.value` / `_maxBatchSize.value` |

Layout side-effects: the properties are appended to the `ui::gridlayout::Advanced::value` of the root (creating `ui::gridlayout::Main::value` = the other children when the root had no layout); for `verticallayout`/`horizontallayout`/`autolayout`/`optionsorder` roots the key is created/extended accordingly (`ComponentManager.addInfiniteMapperBuiltInParameters` / `addProcessorsBuiltInParameters`).

The CLAUDE.md task text mentions `$maxDurationSeconds`; **no such key exists in code** — the real name is `$maxDurationMs` (milliseconds). See also `Option.MAX_DURATION_PARAMETER = "maxDurationMs"`, `Option.MAX_RECORDS_PARAMETER = "maxRecords"` (constructor-parameter names for streaming inputs).

## 8. Validation (`validation` object, not in `metadata`)

Raw keys `tcomp::validation::<name>` are produced by `ValidationParameterEnricher` (annotation meta-annotated with `@Validation(expectedTypes, name)`, matched against the Java type of the option) and by the implicit enrichers. `PropertyValidationService` copies them into the typed `PropertyValidation`:

| Raw key (after `tcomp::validation::`) | Annotation & Java type | `PropertyValidation` field | JSON |
|---|---|---|---|
| `required` | `@Required` (any object) | `required` (Boolean) | boolean |
| `min` / `max` | `@Min` / `@Max` on `Number`, `int`, `short`, `byte`, `long`, `double`, `float` | `min` / `max` (Integer; value parsed with `Double.valueOf(v).intValue()`) | number |
| `minLength` / `maxLength` | `@Min` / `@Max` on `CharSequence` | `minLength` / `maxLength` | number |
| `minItems` / `maxItems` | `@Min` / `@Max` on `Collection` | `minItems` / `maxItems` | number |
| `uniqueItems` | `@Uniques` on `Collection` | `uniqueItems` | boolean |
| `pattern` | `@Pattern` on `CharSequence` (JavaScript regex) | `pattern` | string |
| (enum) | `ParameterMeta.Type.ENUM` | `enumValues` (`Collection<String>`) | array of strings; filled by `PropertiesService` from the enum constants (comma-joined when parsed from a raw key) |

Implicit constraints: `char` gets `minLength = maxLength = 1`; `Character` gets `maxLength = 1` (`CharConstraintEnricher`); `int`/`Integer` get implicit `min = -2147483648` and `max = 2147483647` unless the option declares its own `@Min`/`@Max` (`IntegerConstraintEnricher`, overridable per `ParameterModelService.getAnnotations`). `validation` is `null` (omitted) when no constraint applies.

The runtime re-checks required/min/max/minLength/maxLength/minItems/maxItems/uniqueItems/pattern at component instantiation (`ReflectionService.PayloadValidator`); violations raise `IllegalArgumentException`. The designer SHOULD validate before saving; the runtime MUST NOT rely on it.

## 9. Component-level metadata (`ComponentIndex.metadata`, `ComponentDetail.metadata`)

Built by `ComponentMetadataService.getMetadata(type)` then `translateMetadata`. **Keys are raw (no prefix stripping).** Every value may be overridden by the component i18n bundle: for each key `a::b::c` the server looks up bundle key `a.b.c` (`bundle.displayName(...)`) and uses it if present (`ComponentResourceImpl.translateMetadata`); `documentation::value` is replaced by the bundle documentation.

| Key | Source | Value |
|---|---|---|
| `documentation::value` | `@Documentation` on the component class | text |
| (any key) | `@Metadatas({@Meta(key, value)})` on the component class | user-defined, verbatim |
| `mapper::infinite` | `@PartitionMapper.infinite()`; `false` for `@Emitter` | `true`/`false` (see note) |
| `mapper::optionalRow` | `@PartitionMapper.optionalRow()` / `@Emitter.optionalRow()` | `true`/`false` |
| `tcomp::ui::schema::mapping` | `@DatabaseMapping(value)` on `@PartitionMapper`/`@Emitter`/`@Processor`/`@DriverRunner` (`ComponentSchemaEnricher.SCHEMA_MAPPING`) | string of `DatabaseMapping.value()` (test value: `custom`) |
| `tcomp::ui::schema::mapper` | `@DatabaseMapping.mapping()`, only when mapping is `CUSTOM` (`SCHEMA_MAPPER`) | name of a `@DatabaseSchemaMapping` action |
| `tcomp::ui::schema::fixed` | `@FixedSchema.value()` (`FIXED_SCHEMA_META_PREFIX`) | name of a `@DiscoverSchema` action |
| `tcomp::ui::schema::flows::fixed` | `@FixedSchema.flows()` joined by `,`, else `__default__` (`Branches.DEFAULT_BRANCH`) | e.g. `__default__`, `reject` |
| `tcomp::ui::schema::fixed::watch` | `@FixedSchema.watch()` joined by `,` (only if non-empty) | property paths that retrigger the discover action |
| `variables::return::value` | `@ReturnVariable(s)` (Studio only) | entries `name:javaType:availability:description` joined by `;` (delimiters `:` and `;` are the regexes `\:` and `\;` in source) |
| `variables::after::value` | `@AfterVariable(s)` (deprecated, Studio only) | entries `name:javaType:description` joined by `;` |
| `conditional_output::value` | `@ConditionalOutput` on a `@Processor` (Studio only) | name of the `@AvailableOutputFlows` action |
| user SPI keys | any `ComponentMetadataEnricher` on the server classpath (test example: `testing::type`, `testing::v2..v5`) | verbatim; `order()` decides conflicts (last wins, a warning is logged) |

Note on `mapper::infinite`: the metadata is emitted for `@PartitionMapper` and `@Emitter` components only; processors/standalone do not carry it (asserted only for the `chain#list` emitter in `ComponentResourceImplTest#getDetails`).

## 10. Coverage cross-check

A grep of `tcomp::` over all `src/main` Java files returned these producers/consumers, all covered above: `ValidationParameterEnricher`, `UiParameterEnricher`, `ActionParameterEnricher`, `ConditionParameterEnricher`, `ConfigurationTypeParameterEnricher`, `DependencyParameterEnricher`, `DocumentationParameterEnricher`, `CharConstraintEnricher`, `ComponentSchemaEnricher`, `MaxBatchSizeParamBuilder`, `StreamingLongParamBuilder`, `ComponentManager` (layout of built-in options, `tcomp::configurationtype::type` lookup), `MigrationHandlerFactory` (`tcomp::configurationtype::` prefix test), `RepositoryModelBuilder` (reads `configurationtype::name/type`), `ReflectionService` (runtime validation reads `tcomp::validation::*`), `VisibilityService` (runtime evaluation of `condition::*`), `PropertiesService`, `SecurityUtils` (`tcomp::ui::credential`). Consumers that also read these keys but were not analysed in depth (unverified): `component-tools` validators (`LayoutValidator`, `DatasetValidator`, `DatasetDiscoveryValidator`, `CheckpointValidator`, `DocBaseGenerator`, `DitaDocumentationGenerator`), `component-studio/component-runtime-di/.../TaCoKitGuessSchema`, `documentation/.../Generator`.
