# Appendix - Meta-annotations, markers and i18n keys

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`). Catalog: [DSG](../02-feature-catalog/DSG-design.md), [CFG](../02-feature-catalog/CFG-configuration.md), [UI](../02-feature-catalog/UI-ui.md), [VAL](../02-feature-catalog/VAL-validation.md), [ACT](../02-feature-catalog/ACT-actions.md).

## 1. Meta-annotations (annotations used to define other annotations)

| Meta-annotation | Package (`org.talend.sdk.component.api...`) | Attribute(s) | Effect | Enricher / consumer | Catalog |
|---|---|---|---|---|---|
| `@Ui` | `configuration.ui.meta` | none | annotation produces `tcomp::ui::<simplename lower>[::<method>]` metadata | `UiParameterEnricher` | `UI-001` |
| `@Condition` | `configuration.condition.meta` | `String value` | condition type id -> `tcomp::condition::<type>::<attr>` | `ConditionParameterEnricher`, `VisibilityService` | `UI-018` |
| `@Validation` (repeatable) / `@Validations` | `configuration.constraint.meta` | `Class<?>[] expectedTypes`, `String name` | constraint name/type applicability -> `tcomp::validation::<name>` | `ValidationParameterEnricher` | `VAL-006` |
| `@ConfigurationType` | `configuration.type.meta` | `String value` | type id -> `configurationtype::type` / `::name` | `ConfigurationTypeParameterEnricher`, `RepositoryModelBuilder` | `CFG-007` |
| `@ActionRef` | `configuration.action.meta` | `Class<?> value`, `String ref = "value"` | binds an option annotation to an action type (or client action) | `ActionParameterEnricher` | `ACT-015` |
| `@ActionType` | `service` | `String value`, `Class<?> expectedReturnedType = Object.class` | defines an action annotation (type id, return contract) | `ActionParameterEnricher`, `ActionValidator`, server action registry | `ACT-002` |

## 2. Marker annotations

| Annotation | Package | Attributes | Meaning | Catalog |
|---|---|---|---|---|
| `@Documentation` | `meta` | `String value`, `boolean tooltip = false` | documentation; metadata `documentation::value` / `::tooltip` | `DSG-007` |
| `@Internal` | `meta` | none | internal API, must not be used | `DSG-009` |
| `@Partial` | `meta` | `String value` | not fully supported, with reason | `DSG-010` |
| `@Internationalized` | `internationalization` | none | i18n message interface (injected proxy) | `DSG-011` |
| `@Language` | `internationalization` | none (`@Target(PARAMETER)`) | explicit locale parameter | `DSG-012` |
| `@Metadatas` / `@Metadatas.Metadata` | `component` | `Metadata[] value` / `key`, `value` | user component metadata | `DSG-005` |
| `@Components` | `component` | `family`, `categories = {"Misc"}` | package-level family and categories | `DSG-001` |
| `@Icon` | `component` | `IconType value = CUSTOM`, `String custom = ""` | icon | `DSG-002` |
| `@Version` | `component` | `int value = 1`, `Class<? extends MigrationHandler> migrationHandler` | version + migration | `LCM-001` |
| `@DatabaseMapping` | `component` | `String value = ""`, `String mapping = ""` | Studio DB mapping | `DSG-014` |

## 3. Component metadata key conventions

Component-level keys are listed in `DSG-008`; property-level keys in [05 section 4](../05-configuration-and-ui.md). Prefixes: `tcomp::` (in-JVM, stripped by the server for properties), `ui::`, `condition::`, `action::`, `configurationtype::`, `dependencies::`, `documentation::`, `definition::`, `variables::`, `mapper::`, `conditional_output::`, `user::` (free).

## 4. Extension points (SPI)

| SPI | Package | Method(s) | Catalog |
|---|---|---|---|
| `ComponentMetadataEnricher` | `org.talend.sdk.component.spi.component` | `Map<String,String> onComponent(Type, Annotation[])`, `int order()` (default `0`) | `DSG-006` |
| `ParameterExtensionEnricher` | `org.talend.sdk.component.spi.parameter` | `Map<String,String> onParameterAnnotation(String, Type, Annotation)`, `Map<Type, Collection<Annotation>> getImplicitAnnotationForTypes()` (default empty) | `CFG-015` |

Registered built-ins: component enrichers `AfterVariableMetadataEnricher`, `ReturnVariableMetadataEnricher`, `ComponentSchemaEnricher`, `ConditionalOutputMetadataEnricher`; parameter enrichers `CharConstraintEnricher`, `IntegerConstraintEnricher`, `ConfigurationTypeParameterEnricher`, `ValidationParameterEnricher`, `ConditionParameterEnricher`, `ActionParameterEnricher`, `UiParameterEnricher`, `DocumentationParameterEnricher`, `DependencyParameterEnricher`.

## 5. Internationalization keys (`Messages[_locale].properties`)

| Key pattern | Meaning |
|---|---|
| `${family}._displayName` | family display name |
| `${family}.${category}._category` | category display name |
| `${family}.${configurationType}.${name}._displayName` | dataset/datastore/... display name (read from the family package) |
| `${family}.actions.${actionType}.${actionName}._displayName` | action display name (optional) |
| `${family}.${component_name}._displayName` | component display name |
| `${property_path}._displayName` / `._documentation` / `._placeholder` | option label / documentation / placeholder |
| `${simple_class_name}.${property_name}._displayName` / `._documentation` / `._placeholder` | same, by class (property-path key wins) |
| `${enum_simple_class_name}.${enum_name}._displayName` | enum value label |
| `${property_path or simple_class_name}._gridlayout.${layout_name}._displayName` | layout tab label (needs server option `talend.component.server.gridlayout.translation.support=true`; not in Studio) |
| `${family}.datasetdiscovery.${name}._displayName` | dataset discovery display name (validator convention) |
| `<ComponentSimpleClassName>.$maxBatchSize._displayName` etc. | built-in option labels (`StreamingLongParamBuilder.$maxRecords`, `$maxDurationMs`, `MaxBatchSizeParamBuilder.$maxBatchSize`) in `builtinparams/Messages.properties` |

Lookup order for options: full path key, then `${simple_class_name}.${property_name}` for each i18n package of the property (`ParameterBundle`); the enum label lookup strips the last segment of the class name.
