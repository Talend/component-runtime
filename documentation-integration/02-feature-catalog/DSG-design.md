# DSG - Design (component model and metadata)

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`). Category prefix `DSG-`.
> Machine-readable twin: [index.DSG.json](index.DSG.json). Markdown and JSON MUST agree.
> Related: [CFG](CFG-configuration.md), [UI](UI-ui.md), [VAL](VAL-validation.md), [ACT](ACT-actions.md), [05 configuration and UI](../05-configuration-and-ui.md), [appendix meta-annotations](../10-appendix/meta-annotations.md), [appendix icons](../10-appendix/icons.md).

## Scope and cross-category ownership

DSG covers the *identity and descriptive metadata* of a component (family, categories, icon, version value, documentation, i18n, user metadata, Studio-only variable/mapping metadata). Ownership decisions taken to avoid duplicates:

| Type | Owner category | Note |
|---|---|---|
| `@Version` value, `Version.migrationHandler`, `MigrationHandler` | `LCM-001` (declaration/exposure), migration semantics in the LCM catalog | Only the `int` version travels to the host; see [03 component server API](../03-component-server-api.md). |
| `@ReturnVariables`, `@AfterVariables`, `@ConditionalOutput` | RUN catalog (runtime shape) | Their *metadata keys* are enumerated in `DSG-008`. |
| `@FixedSchema`, `Schema` (service.schema), `service.schema.Type` | DAT catalog | Their metadata keys are enumerated in `DSG-008`. |
| `@DatabaseMapping` | `DSG-014` | Studio-only component metadata. |

Metadata key prefix rule: inside the JVM (`ParameterMeta.getMetadata()`) property metadata keys carry the prefix `tcomp::`. The component server strips `tcomp::` from *property* metadata (`PropertiesService`). *Component* metadata (this file) is returned by the server as stored (see `DSG-008`).

### DSG-001 `@Components`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/component/Components.java` (+ `documentation/src/main/antora/modules/ROOT/pages/component-registering.adoc`)
- **Category / Sub-area**: DSG / family and categories
- **Kind**: annotation (`@Target(PACKAGE)`)
- **Applies to**: all component types (input, processor, output, standalone), configuration types (via family)
- **Description**: declared in `package-info.java`; sets the component family and category tree for every component in the package and sub-packages. The pair (`family`, component `name`) MUST be unique across the deployment; one component belongs to exactly one family and one or more categories.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | (required) | Family value for all components of the package and sub-packages. |
| `categories` | `String[]` | `{"Misc"}` | Category paths. `${family}` is replaced by the family; if absent, `/${family}` is appended (`Misc` becomes `Misc/<family>`). The server applies this normalization (`ComponentResourceImpl.normalizeCategory`). |

- **Contract for the Designer**: MUST group the palette by `family` and `categories` from `ComponentIndex`; SHOULD display `familyDisplayName` and translated category names; MUST use (family, name) as the component identity.
- **Contract for the Runtime**: MUST resolve components by plugin id + family + name; family is the key for shared services and actions.
- **Server exposure**: `ComponentIndex.id.family`, `familyDisplayName`, `categories` (and `ComponentDetail.id`); see [03](../03-component-server-api.md). Doc discrepancy: `component-registering.adoc` shows `@Components(name = ...)` but the code attribute is `family` (see appendix known-discrepancies, when present).
- **Maturity level**: 0 - the host cannot list or resolve components without family and categories.
- **Example**:

```java
@Components(family = "myfamily", categories = { "Business", "Cloud" })
package org.example.components;
```

### DSG-002 `@Icon`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/component/Icon.java` (+ `documentation/src/main/antora/modules/ROOT/pages/icons.adoc`)
- **Category / Sub-area**: DSG / icons
- **Kind**: annotation (`@Target({TYPE, PACKAGE, ANNOTATION_TYPE})`)
- **Applies to**: components (`@PartitionMapper`, `@Emitter`, `@Processor`, `@DriverRunner`), configuration classes, packages (family icon), meta-annotations
- **Description**: associates an icon key with a component, a family (on the package) or a configuration type. Icon resolution rules are in `DSG-003`.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `Icon.IconType` | `CUSTOM` | Built-in icon, or `CUSTOM` to use `custom()`. All built-in values except `STAR`, `CUSTOM`, `DEFAULT` are `@Deprecated`. |
| `custom` | `String` | `""` | Custom icon key (file `icons/<key>.svg` or `icons/<key>_icon32.png`). Empty falls back to `default`. |

- **Contract for the Designer**: MUST fall back to a default icon when the icon is missing or unknown; SHOULD fetch the image via the icon endpoints (see [03](../03-component-server-api.md)); MAY use the inline icon content when the index is requested with `includeIconContent`.
- **Contract for the Runtime**: none.
- **Server exposure**: `ComponentIndex.icon` and `ComponentIndex.iconFamily` (both of type `Icon`, i.e. resolved icon objects/content when requested), `ComponentDetail.icon` (icon key `String`); icon binaries via the `/component/icon/...` endpoints (exact paths and payload in [03](../03-component-server-api.md)). Server-side lookup (`IconResolver`): (1) `icons/override/<icon>.svg|_icon32.png` on the server classpath, (2) family classloader, (3) server classloader; with themes enabled the path is prefixed by `icons/<theme>/`; legacy fallback controlled by server options `supportIconTheme` / `supportLegacyIcons` (see `component-server/.../ComponentServerConfiguration`).
- **Maturity level**: 1 - missing icons degrade the palette UX but do not affect correctness.
- **Example**:

```java
@Icon(value = Icon.IconType.CUSTOM, custom = "myicon")
```

### DSG-003 Icon key resolution and `Icon.IconType`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/reflect/IconFinder.java` (+ `component-api/src/main/java/org/talend/sdk/component/api/component/Icon.java`)
- **Category / Sub-area**: DSG / icons
- **Kind**: convention
- **Applies to**: components, families, configuration types
- **Description**: the icon string exposed to hosts is computed by `IconFinder.findIcon`: (1) direct `@Icon` -> `IconType.getKey()` or, for `CUSTOM`, `custom()` (or `default` if empty); (2) else a meta-annotation itself annotated with `@Icon` and having a `value()` (the alphabetically first by annotation class name; if it has a `type()` that is not `custom`, the enum key of `type()` wins, else `value()`; enum values use `getKey`/`getValue`/`name`); (3) else any annotation whose simple name ends with `Icon` and has `value()`; (4) else `default`. The complete `IconType` key list is in [icons appendix](../10-appendix/icons.md) (245 keys plus `custom`, `default`).
- **Attributes**: none (convention).
- **Contract for the Designer**: MUST treat the icon as an opaque key (never assume it is one of `IconType`); MUST support keys that only exist as custom icons on the server.
- **Contract for the Runtime**: none.
- **Server exposure**: `icon` field of the component index/detail.
- **Maturity level**: 1 - affects only rendering.
- **Example**:

```json
{ "icon": "myicon" }
```

### DSG-004 (moved) -> see LCM-001

### DSG-005 `@Metadatas` / `@Metadatas.Metadata`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/component/Metadatas.java` (+ `component-registering.adoc`)
- **Category / Sub-area**: DSG / user metadata
- **Kind**: annotation (`@Target(TYPE)`), container + nested `@Metadata`
- **Applies to**: components
- **Description**: attaches arbitrary key/value pairs to a component; they are copied verbatim in the component `metadata` map (`ComponentMetadataService.getMetadata`). Suggested key style: `user::<name>`.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `Metadatas.value` | `Metadata[]` | (required) | List of pairs. |
| `Metadata.key` | `String` | `""` | Metadata key. |
| `Metadata.value` | `String` | `""` | Metadata value. |

- **Contract for the Designer**: MAY read the `metadata` map for host-specific behavior; MUST ignore unknown keys.
- **Contract for the Runtime**: none.
- **Server exposure**: `ComponentIndex.metadata` / `ComponentDetail.metadata` (map).
- **Maturity level**: 2 - purely optional extension point.
- **Example**:

```java
@Metadatas({ @Metadatas.Metadata(key = "user::value0", value = "myValue0") })
```

### DSG-006 `ComponentMetadataEnricher` SPI
- **Source**: `component-spi/src/main/java/org/talend/sdk/component/spi/component/ComponentMetadataEnricher.java` (+ `component-runtime-manager/src/main/resources/META-INF/services/org.talend.sdk.component.spi.component.ComponentMetadataEnricher`)
- **Category / Sub-area**: DSG / metadata extension
- **Kind**: interface (Java `ServiceLoader` SPI)
- **Applies to**: components
- **Description**: lets a host or plugin add entries to the component metadata map from the component class annotations. Enrichers are applied sorted by `order()`; an enricher overriding an existing key triggers a warning log. Built-in enrichers: `AfterVariableMetadataEnricher`, `ReturnVariableMetadataEnricher`, `ComponentSchemaEnricher`, `ConditionalOutputMetadataEnricher`. Merged from LCM-021: `ComponentMetadataService` builds the component `metadata` map in this order: `documentation::value`, `@Metadatas` entries, `mapper::infinite`, `mapper::optionalRow`, then every enricher sorted by `order()`. Built-in enrichers and keys: `ReturnVariableMetadataEnricher` (`variables::return::value`), `AfterVariableMetadataEnricher` (`variables::after::value`), `ConditionalOutputMetadataEnricher` (`conditional_output::value`), `ComponentSchemaEnricher` (`tcomp::ui::schema::mapping|mapper|fixed|flows::fixed|fixed::watch`). `GenericComponentExtension` is SVC-024.
- **Attributes**: method `Map<String,String> onComponent(Type type, Annotation[] annotations)`; default method `int order()` returning `0` (lower runs first).
- **Contract for the Designer**: none (consumes the resulting keys, see `DSG-008`). MUST treat unknown metadata keys as opaque.
- **Contract for the Runtime**: SHOULD keep the `ServiceLoader` discovery intact when embedding `component-runtime-manager` so the built-in keys exist.
- **Server exposure**: resulting keys appear in the component `metadata` map.
- **Maturity level**: 2 - extension point for host-specific metadata.
- **Example**:

```
META-INF/services/org.talend.sdk.component.spi.component.ComponentMetadataEnricher
org.example.MyEnricher
```

### DSG-007 `@Documentation`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/meta/Documentation.java` (+ `documentation/src/main/antora/modules/ROOT/pages/component-configuration.adoc`)
- **Category / Sub-area**: DSG / documentation
- **Kind**: annotation (`@Retention(RUNTIME)`, no `@Target` restriction)
- **Applies to**: components, `@Option` fields/parameters, configuration classes, services actions (also used by the framework itself to document its annotations)
- **Description**: human documentation of an element. On components it becomes metadata `documentation::value`; on options it becomes property metadata `documentation::value` (and `documentation::tooltip` = `true` when `tooltip = true`). i18n keys `${property_path}._documentation` / `${simple_class_name}.${property_name}._documentation` override the annotation value on the server.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | (required) | Documentation text. |
| `tooltip` | `boolean` | `false` | Render the documentation as a tooltip in the UiSchema (`UiSchema.tooltip`). |

- **Contract for the Designer**: SHOULD show `documentation::value` as help text (or as `tooltip` when `documentation::tooltip` is `true`); MAY also fetch the rendered component documentation (`/documentation/component/{id}`).
- **Contract for the Runtime**: none.
- **Server exposure**: property metadata `documentation::value`, `documentation::tooltip`; component metadata `documentation::value`.
- **Maturity level**: 1 - degraded UX without it.
- **Example**:

```json
{ "documentation::value": "The host name", "documentation::tooltip": "true" }
```

### DSG-008 Component metadata map (keys)
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/reflect/ComponentMetadataService.java` (+ `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/extension/ComponentSchemaEnricher.java`)
- **Category / Sub-area**: DSG / component metadata
- **Kind**: payload field convention (`metadata` map of the component index/detail)
- **Applies to**: components
- **Description**: the exact set of keys produced by the framework for a component. The server returns the map as stored (keys are NOT stripped of `tcomp::`, unlike property metadata); values are translated when the component bundle has `<key with :: replaced by .>._displayName`, and `documentation::value` is overridden by `${component}._documentation`.
- **Attributes**:

| key | produced by | value |
|---|---|---|
| `documentation::value` | `@Documentation` on the class | text |
| user keys | `@Metadatas` / SPI | free |
| `mapper::infinite` | `@PartitionMapper.infinite` (`false` for `@Emitter`) | `true`/`false` |
| `mapper::optionalRow` | `@PartitionMapper.optionalRow` / `@Emitter.optionalRow` | `true`/`false` |
| `variables::after::value` | `@AfterVariable(s)` (deprecated, Studio) | `name:type:description` joined by `;` |
| `variables::return::value` | `@ReturnVariable(s)` (Studio) | `name:type:availability:description` joined by `;` |
| `conditional_output::value` | `@ConditionalOutput` on a `@Processor` | action id of `@AvailableOutputFlows` |
| `tcomp::ui::schema::mapping` | `@DatabaseMapping.value` | `""`, `custom` or DB name |
| `tcomp::ui::schema::mapper` | `@DatabaseMapping.mapping` when value is `custom` | `@DatabaseSchemaMapping` id |
| `tcomp::ui::schema::fixed` | `@FixedSchema.value` | id of a `@DiscoverSchema`/`@DiscoverSchemaExtended` |
| `tcomp::ui::schema::flows::fixed` | `@FixedSchema.flows` (default `__default__`) | comma list |
| `tcomp::ui::schema::fixed::watch` | `@FixedSchema.watch` | comma list of parameter paths |

- **Contract for the Designer**: MUST tolerate unknown keys; MAY use `mapper::infinite`/`mapper::optionalRow` to adapt the canvas (streaming source, optional incoming row); SHOULD refresh a fixed schema when a `watch` path changes (uses the action of the same id, see `ACT-007`/`ACT-008`).
- **Contract for the Runtime**: MAY use `mapper::infinite` to select streaming execution (details in RUN catalog).
- **Server exposure**: `ComponentIndex.metadata`, `ComponentDetail.metadata`.
- **Maturity level**: 1 - fixed-schema and streaming hints degrade UX when ignored.
- **Example**:

```json
{ "documentation::value": "Reads rows", "mapper::infinite": "false", "mapper::optionalRow": "false" }
```

### DSG-009 `@Internal`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/meta/Internal.java`
- **Category / Sub-area**: DSG / API stability markers
- **Kind**: annotation (marker, `@Retention(RUNTIME)`)
- **Applies to**: framework types only
- **Description**: marks a framework feature as internal; "Must not be used". Used on internal APIs (e.g. commented usage on `Validation`, `ConfigurationType`).
- **Attributes**: none.
- **Contract for the Designer**: none.
- **Contract for the Runtime**: none.
- **Server exposure**: none.
- **Maturity level**: 2 - informational marker, ignoring it has no effect.
- **Example**:

```java
@Internal
public @interface SomeFrameworkThing { }
```

### DSG-010 `@Partial`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/meta/Partial.java`
- **Category / Sub-area**: DSG / API stability markers
- **Kind**: annotation (`@Retention(RUNTIME)`)
- **Applies to**: framework types (e.g. `@DiscoverSchema`, `@DiscoverSchemaExtended`, `service.schema.Schema`)
- **Description**: marks a feature as not fully supported; `value()` explains why (e.g. "This API should support nested schema but the Studio is not yet ready").
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | (required) | Reason for partial support. |

- **Contract for the Designer**: SHOULD NOT assume full support (nested schemas) on features marked partial.
- **Contract for the Runtime**: none.
- **Server exposure**: none.
- **Maturity level**: 2 - informational.
- **Example**:

```java
@Partial("See Schema description.")
```

### DSG-011 `@Internationalized`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/internationalization/Internationalized.java` (+ `documentation/src/main/antora/modules/ROOT/pages/services-internationalization.adoc`)
- **Category / Sub-area**: DSG / i18n for services
- **Kind**: annotation (`@Target(TYPE)`), no attributes (keys are normalized, no prefix allowed)
- **Applies to**: service message interfaces (proxied by the framework)
- **Description**: marks an interface whose methods are message keys resolved from `Messages`-style bundles by the framework proxy (`InternationalizationServiceFactory`). Types annotated with it are treated as services (`ParameterModelService.isService`), so they are injected rather than exposed as options.
- **Attributes**: none.
- **Contract for the Designer**: none.
- **Contract for the Runtime**: MUST inject a proxy for `@Internationalized` interfaces (already done by `ComponentManager`); SHOULD supply the current locale (`$lang`).
- **Server exposure**: none.
- **Maturity level**: 1 - needed for localized messages of services/actions.
- **Example**:

```java
@Internationalized
public interface Messages { String hello(String name); }
```

### DSG-012 `@Language`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/internationalization/Language.java` (+ `services-actions.adoc`)
- **Category / Sub-area**: DSG / i18n for services
- **Kind**: annotation (`@Target(PARAMETER)`, marker)
- **Applies to**: parameters of service/action methods
- **Description**: marks a parameter as the explicit locale to use. Combined with the reserved option `$lang` (populated by the server from the `lang` query parameter of `/action/execute`).
- **Attributes**: none.
- **Contract for the Designer**: MUST pass `lang` on action calls when it wants localized results.
- **Contract for the Runtime**: SHOULD supply `$lang` when it executes actions in-process.
- **Server exposure**: action execute request parameter `lang` (default `en`); the server injects `$lang` (language code) into the action parameters.
- **Maturity level**: 2 - localization of action results only.
- **Example**:

```java
public SuggestionValues find(@Option("$lang") final String lang) { ... }
```

### DSG-013 Resource bundle key conventions (`Messages.properties`)
- **Source**: `documentation/src/main/antora/modules/ROOT/pages/component-internationalization.adoc` (+ `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/internationalization/ParameterBundle.java`, `FamilyBundle.java`, `ComponentBundle.java`)
- **Category / Sub-area**: DSG / i18n
- **Kind**: convention
- **Applies to**: families, categories, components, options, enums, datasets/datastores, actions, layout tabs
- **Description**: the server translates display names from `Messages[locale].properties` in the component package (looked up per option package). Keys (see appendix meta-annotations for the summary table):. Merged from ACT-027: `ActionReference.displayName` is the bundle key `${family}.actions.${actionType}.${actionName}._displayName` (optional, falls back to the action name); default labels when missing are `Validate Connection` (health check), `Guess Schema` (schema) and `<name> (<type>)` (update).

| key | meaning |
|---|---|
| `${family}._displayName` | family name |
| `${family}.${category}._category` | category name |
| `${family}.${configurationType}.${name}._displayName` | dataset/datastore name (`configurationType` is `dataset`, `datastore`, ...); read from the family package |
| `${family}.actions.${actionType}.${actionName}._displayName` | action name (optional, default = action name) |
| `${family}.${component_name}._displayName` | component name |
| `${property_path}._displayName`, `._documentation`, `._placeholder` | option label, doc, placeholder |
| `${simple_class_name}.${property_name}._displayName` (`._documentation`, `._placeholder`) | same by class (shared config); the property-path key wins |
| `${enum_simple_class_name}.${enum_name}._displayName` | enum value label (`proposalDisplayNames`) |
| `${property_path or simple_class_name}._gridlayout.${layout_name}._displayName` | tab name; only if server option `talend.component.server.gridlayout.translation.support` is `true`; not supported by the Studio |

The datasetDiscovery key used by the validator is `${family}.datasetdiscovery.${name}._displayName` (`DatasetDiscoveryValidator`).
- **Attributes**: none.
- **Contract for the Designer**: MUST pass the requested `language` on index/detail calls and MUST display server-translated strings as is; SHOULD fall back to `name` when `displayName` is absent.
- **Contract for the Runtime**: none.
- **Server exposure**: `displayName`, `placeholder`, `proposalDisplayNames`, `familyDisplayName`, category strings, `documentation::value`.
- **Maturity level**: 1 - full i18n is level 2 in the checklist but display names are part of normal UX; layout tab translation is optional.
- **Example**:

```properties
memory._displayName = Memory
memory.list._displayName = Memory List
MyConfig.host._displayName = Server Host Name
```

### DSG-014 `@DatabaseMapping`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/component/DatabaseMapping.java`
- **Category / Sub-area**: DSG / Studio schema mapping
- **Kind**: annotation (`@Target(TYPE)`)
- **Applies to**: components (`@PartitionMapper`, `@Emitter`, `@Processor`, `@DriverRunner`)
- **Description**: tells the Studio which database type mapping to use for the component schema (DB column types). `custom` delegates to a `@DatabaseSchemaMapping` action (`ACT-013`). Empty string disables mapping. Studio-only. Merged from DAT-026: applies to input, processor, output and standalone component classes; the action side is `@DatabaseSchemaMapping` (ACT-013).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | `""` (`Mapping.NONE`) | Database type to map, or `custom` (`Mapping.CUSTOM`). |
| `mapping` | `String` | `""` | Action id (`@DatabaseSchemaMapping`) to call when `value` is `custom`. |

- **Contract for the Designer**: MAY (Studio-like hosts only) read `tcomp::ui::schema::mapping` / `tcomp::ui::schema::mapper` (see `DSG-008`) to map column types; other hosts MUST ignore.
- **Contract for the Runtime**: none.
- **Server exposure**: component metadata keys `tcomp::ui::schema::mapping`, `tcomp::ui::schema::mapper`.
- **Maturity level**: 2 - Studio-only optional capability.
- **Example**:

```java
@DatabaseMapping(value = DatabaseMapping.Mapping.CUSTOM, mapping = "my_mapping")
```

### DSG-015 Custom icon file conventions
- **Source**: `documentation/src/main/antora/modules/ROOT/pages/component-defining-custom-icon.adoc` (+ `documentation/src/main/antora/modules/ROOT/pages/ref-server-configuration.adoc`)
- **Category / Sub-area**: DSG / icons
- **Kind**: convention
- **Applies to**: components, families
- **Description**: custom icons live in the plugin at `src/main/resources/icons/`, named `<Name>.svg` (viewBox 16x16) or `<Name>_icon32.png` (32x32, squared). PNG runs in degraded mode in Talend Cloud. SVG is not supported by the Studio; the server property `talend.component.server.icon.paths` controls which icon file types are served.
- **Attributes**: none.
- **Contract for the Designer**: SHOULD request icon content as SVG when it supports it, PNG otherwise; MUST NOT fail when the icon is missing.
- **Contract for the Runtime**: none.
- **Server exposure**: icon endpoints and `includeIconContent` (see [03](../03-component-server-api.md)).
- **Maturity level**: 1 - rendering only.
- **Example**:

```
src/main/resources/icons/myicon.svg
src/main/resources/icons/myicon_icon32.png
```
