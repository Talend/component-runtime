# VAL - Validation

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`). Category prefix `VAL-`.
> Machine-readable twin: [index.VAL.json](index.VAL.json). Markdown and JSON MUST agree.
> Related: [CFG](CFG-configuration.md), [UI](UI-ui.md), [ACT](ACT-actions.md), [appendix constraints](../10-appendix/constraints.md), [05 configuration and UI](../05-configuration-and-ui.md).

## Overview

Two validation families exist:

1. **Declarative constraints** (`configuration.constraint.*`): annotations mapped by `ValidationParameterEnricher` to in-JVM metadata `tcomp::validation::<name>`. The component server moves them out of `metadata` into the typed `validation` object of each `SimplePropertyDefinition` (`PropertyValidation`, `VAL-007`); the designer enforces them client-side, and the framework re-checks them before instantiating a component (`VAL-012`).
2. **Server validation actions** (`@AsyncValidation`, `VAL-010`), invoked on demand by the designer through the action endpoint, linked to an option with `@Validable` (`ACT-019`).

Type applicability: a constraint annotation carries one or more `@Validation(expectedTypes, name)` meta entries; the first entry whose `expectedTypes` accepts the option's Java class (`isAssignableFrom`, raw type for parameterized types) supplies the metadata name. If no entry matches, no metadata is produced (silently). Java arrays are not `Collection`, so collection constraints do not apply to arrays (inferred from `toClass`).

### VAL-001 `@Required`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/constraint/Required.java`
- **Category / Sub-area**: VAL / constraints
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, `@Validation(expectedTypes = Object.class, name = "required")`)
- **Applies to**: any option
- **Description**: marks the option as mandatory. Metadata `tcomp::validation::required` = `true` -> `validation.required = true`.
- **Attributes**: none.
- **Contract for the Designer**: MUST refuse to save/run a configuration where an active (`UI-016`) required option is empty/null; MUST mark the field visually as required (`UiSchema.required`, JSON schema `required` array of the parent).
- **Contract for the Runtime**: MUST reject a null value for an active required option (the component manager does, see `VAL-012`).
- **Server exposure**: `SimplePropertyDefinition.validation.required`.
- **Maturity level**: 0 - required validations are level 0 per the checklist rules.
- **Example**:

```json
{ "validation": { "required": true } }
```

### VAL-002 `@Min`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/constraint/Min.java`
- **Category / Sub-area**: VAL / constraints
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, three `@Validation` entries)
- **Applies to**: numbers, collections, strings
- **Description**: lower bound. By Java type: `Number`/`int`/`short`/`byte`/`long`/`double`/`float` -> `min`; `Collection` -> `minItems`; `CharSequence` -> `minLength`. Metadata `tcomp::validation::<name>` = the double `value()` rendered by `String.valueOf(double)` (e.g. `1.0`); server `validation.min/minItems/minLength` are `Integer` (`Double.valueOf(v).intValue()`, so fractions are truncated and values above `Integer.MAX_VALUE` are not representable - inferred).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `double` | (required) | Inclusive lower bound (value, size or length). |

- **Contract for the Designer**: MUST enforce `min` on numbers, `minItems` on lists and `minLength` on strings.
- **Contract for the Runtime**: MUST reject values below the bound (component manager does).
- **Server exposure**: `validation.min`, `validation.minItems`, `validation.minLength`.
- **Maturity level**: 1 - standard validations are level 1 (the runtime double check protects correctness).
- **Example**:

```json
{ "validation": { "min": 1 } }
```

### VAL-003 `@Max`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/constraint/Max.java`
- **Category / Sub-area**: VAL / constraints
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, three `@Validation` entries)
- **Applies to**: numbers, collections, strings
- **Description**: upper bound, symmetrical to `@Min`: `max` (numbers), `maxItems` (`Collection`), `maxLength` (`CharSequence`).
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `double` | (required) | Inclusive upper bound. |

- **Contract for the Designer**: MUST enforce `max`, `maxItems`, `maxLength`.
- **Contract for the Runtime**: MUST reject values above the bound.
- **Server exposure**: `validation.max`, `validation.maxItems`, `validation.maxLength`.
- **Maturity level**: 1 - standard validation.
- **Example**:

```json
{ "validation": { "maxLength": 12 } }
```

### VAL-004 `@Pattern`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/constraint/Pattern.java`
- **Category / Sub-area**: VAL / constraints
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, `@Validation(expectedTypes = CharSequence.class, name = "pattern")`)
- **Applies to**: `STRING` options
- **Description**: the string MUST match a *JavaScript* regular expression (also in the Studio). Metadata `tcomp::validation::pattern` -> `validation.pattern`. The framework evaluates it with `JavascriptRegex` (`component-form` and `component-runtime-manager`), so hosts MUST use a JavaScript-compatible engine.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `value` | `String` | (required) | JavaScript regex. |

- **Contract for the Designer**: MUST validate with a JavaScript regex engine (JSON schema `pattern`).
- **Contract for the Runtime**: MUST reject non-matching strings (component manager does).
- **Server exposure**: `validation.pattern`.
- **Maturity level**: 1 - standard validation.
- **Example**:

```json
{ "validation": { "pattern": "^[a-z]+$" } }
```

### VAL-005 `@Uniques`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/constraint/Uniques.java`
- **Category / Sub-area**: VAL / constraints
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`, `@Validation(expectedTypes = Collection.class, name = "uniqueItems")`)
- **Applies to**: `ARRAY` options backed by a `Collection`
- **Description**: elements MUST be distinct. Metadata `tcomp::validation::uniqueItems` = `true` -> `validation.uniqueItems`.
- **Attributes**: none.
- **Contract for the Designer**: MUST prevent duplicate entries (JSON schema `uniqueItems`).
- **Contract for the Runtime**: MUST reject duplicates (component manager compares JSON values).
- **Server exposure**: `validation.uniqueItems`.
- **Maturity level**: 1 - standard validation.
- **Example**:

```json
{ "validation": { "uniqueItems": true } }
```

### VAL-006 `@Validation` and `@Validations` (meta-annotations)
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/configuration/constraint/meta/Validation.java` (+ `.../meta/Validations.java`)
- **Category / Sub-area**: VAL / constraint definition
- **Kind**: annotation (`@Target(ANNOTATION_TYPE)`, `@Repeatable(Validations.class)`)
- **Applies to**: constraint annotations
- **Description**: declares which Java types a constraint supports and the metadata name. The name is prefixed by `tcomp::validation::` by convention. `@Validations` is the repeatable container. Extension point: a custom constraint annotation carrying `@Validation` is picked up automatically.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `expectedTypes` | `Class<?>[]` | (required) | Java types accepted (assignable). |
| `name` | `String` | (required) | Metadata name (`min`, `maxItems`, ...). |

- **Contract for the Designer**: none (only the resulting names matter; unknown `validation` fields SHOULD be ignored).
- **Contract for the Runtime**: none.
- **Server exposure**: none directly.
- **Maturity level**: 2 - only relevant to constraint authors.
- **Example**:

```java
@Validation(expectedTypes = CharSequence.class, name = "pattern")
```

### VAL-007 `PropertyValidation` payload and JSON-schema mapping
- **Source**: `component-server-parent/component-server-model/src/main/java/org/talend/sdk/component/server/front/model/PropertyValidation.java` (+ `component-server-parent/component-server/src/main/java/org/talend/sdk/component/server/service/PropertyValidationService.java`)
- **Category / Sub-area**: VAL / payload
- **Kind**: payload field
- **Applies to**: `SimplePropertyDefinition.validation` (`null` when the option has no constraint)
- **Description**: typed constraints of one option. Built by reflection: each field named `X` is filled from metadata `tcomp::validation::X`; `Integer` fields via `Double.valueOf(v).intValue()`, `Boolean` via `parseBoolean`, `Collection` (`enumValues`) split on `,`. Fields:

| field | type | applies to | JSON schema keyword in `component-form` |
|---|---|---|---|
| `required` | `Boolean` | any | parent `required` array |
| `min` / `max` | `Integer` | numbers | `minimum` / `maximum` |
| `minLength` / `maxLength` | `Integer` | strings | `minLength` / `maxLength` |
| `minItems` / `maxItems` | `Integer` | arrays | `minItems` / `maxItems` |
| `uniqueItems` | `Boolean` | arrays | `uniqueItems` |
| `pattern` | `String` | strings | `pattern` |
| `enumValues` | `Collection<String>` | enums (set by `PropertiesService` from the proposals, not by an annotation) | `enum` |

- **Attributes**: fields above.
- **Contract for the Designer**: MUST read constraints from `validation` (not from `metadata`); MUST treat absent fields as unconstrained.
- **Contract for the Runtime**: none.
- **Server exposure**: `ComponentDetail.properties[*].validation`, same in `ConfigTypeNode` and `ActionItem`.
- **Maturity level**: 0 - the designer cannot parse the property payload correctly without it.
- **Example**:

```json
{ "required": true, "minLength": 1, "maxLength": 64, "pattern": "^[A-Za-z]+$" }
```

### VAL-008 Implicit constraints from Java types
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/reflect/parameterenricher/IntegerConstraintEnricher.java` (+ `CharConstraintEnricher.java`)
- **Category / Sub-area**: VAL / constraints
- **Kind**: convention
- **Applies to**: `int`/`Integer`, `char`/`Character` options
- **Description**: implicit constraints are added without annotation: `int` and `Integer` get `min = -2147483648`, `max = 2147483647` (explicit `@Min`/`@Max` override them, no ambiguity error); `char` gets `minLength = 1` and `maxLength = 1`; `Character` gets `maxLength = 1`. `long`, `short`, `double` etc. get none.
- **Attributes**: none.
- **Contract for the Designer**: MUST enforce them like explicit constraints (they arrive in `validation`).
- **Contract for the Runtime**: none beyond `VAL-012`.
- **Server exposure**: `validation.min`, `validation.max`, `validation.minLength`, `validation.maxLength`.
- **Maturity level**: 1 - standard validation.
- **Example**:

```json
{ "type": "NUMBER", "validation": { "min": -2147483648, "max": 2147483647 } }
```

### VAL-009 Enum value restriction
- **Source**: `component-server-parent/component-server/src/main/java/org/talend/sdk/component/server/service/PropertiesService.java`
- **Category / Sub-area**: VAL / payload
- **Kind**: payload field convention
- **Applies to**: `ENUM` options
- **Description**: for an `ENUM` option the server always creates a `validation` object and sets `enumValues` to the constant names in declaration order; `proposalDisplayNames` maps constant -> label. The value stored in configuration MUST be one of the constants.
- **Attributes**: none.
- **Contract for the Designer**: MUST restrict the input to `enumValues` (restricted list) and MUST store the constant name, not the label.
- **Contract for the Runtime**: MUST reject unknown constants (conversion fails: `Enum.valueOf`).
- **Server exposure**: `validation.enumValues`, `proposalDisplayNames`.
- **Maturity level**: 0 - an invalid enum value breaks instantiation.
- **Example**:

```json
{ "type": "ENUM", "validation": { "enumValues": ["Insert", "Update", "Delete"] } }
```

### VAL-010 `@AsyncValidation`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/asyncvalidation/AsyncValidation.java`
- **Category / Sub-area**: VAL / server validation
- **Kind**: annotation (`@Target(METHOD)`, `@ActionType(value = "validation", expectedReturnedType = ValidationResult.class)`)
- **Applies to**: methods of `@Service` classes
- **Description**: "Mark a method as being used to validate a configuration. IMPORTANT: this is a server validation so only use it if you can't use other client side validation." Linked to an option through `@Validable(value)` (`ACT-019`) whose `parameters` select the values sent to the method. Invoked via `POST /action/execute?family=<f>&type=validation&action=<value>`; returns `ValidationResult`.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `family` | `String` | `""` | Component family of the action (defaults to the service family). |
| `value` | `String` | (required) | Identifier matched with `@Validable.value`. |

- **Contract for the Designer**: SHOULD call the action when the validable field changes/loses focus and display `comment` on failure (`status = KO`); MUST NOT block editing while it runs (inferred).
- **Contract for the Runtime**: none (design-time only).
- **Server exposure**: action type `validation`, metadata `action::validation` (+ `action::validation::parameters`) on the option; `ActionReference` in the component detail.
- **Maturity level**: 2 - async validation is level 2 in the checklist.
- **Example**:

```java
@AsyncValidation("checkUrl")
public ValidationResult check(@Option("value") final String value) { return new ValidationResult(Status.OK, null); }
```

### VAL-011 `ValidationResult`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/asyncvalidation/ValidationResult.java`
- **Category / Sub-area**: VAL / server validation
- **Kind**: payload (class `@Data`)
- **Applies to**: return type of `@AsyncValidation` methods
- **Description**: result of an async validation.
- **Attributes**:

| name | type | default | meaning |
|---|---|---|---|
| `status` | `ValidationResult.Status` (`OK`, `KO`) | - | Outcome. |
| `comment` | `String` | - | Message shown to the user when `KO`. |

- **Contract for the Designer**: MUST treat `KO` as a validation error on the field and show `comment`.
- **Contract for the Runtime**: none.
- **Server exposure**: body of the `/action/execute` response for type `validation`.
- **Maturity level**: 2 - with `VAL-010`.
- **Example**:

```json
{ "status": "KO", "comment": "Something went wrong" }
```

### VAL-012 Runtime configuration validation
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/reflect/ReflectionService.java` (inner class `PayloadValidator`) (+ `documentation/src/main/antora/modules/ROOT/pages/component-configuration.adoc`)
- **Category / Sub-area**: VAL / runtime enforcement
- **Kind**: convention (behavior of the component manager)
- **Applies to**: component instantiation (mapper, processor, standalone)
- **Description**: before creating a component instance the manager validates the configuration payload against the metadata: `required` (null value), `min`, `max`, `minLength`, `maxLength`, `minItems`, `maxItems`, `uniqueItems`, `pattern`. Only *visible* parameters are validated (`VisibilityService` evaluates `@ActiveIf` on the payload). Failures raise `IllegalArgumentException` with one `- message` line per violation and abort instantiation. The check can be disabled with JVM system property `talend.component.configuration.validation.skip=true`. Merged from INT-016: applies to `findMapper`/`findProcessor`/`findDriverRunner`; messages come from the built-in `Messages` bundle; enum conversion failures (`Enum.valueOf`) and missing services surface as `IllegalArgumentException` from the constructor factory. Design-time equivalent: validation metadata `tcomp::validation::*` on properties.
- **Attributes**: system property `talend.component.configuration.validation.skip` (boolean, default `false`).
- **Contract for the Designer**: SHOULD pre-validate so users see errors before execution. MUST validate inputs at design time (see 05-configuration-and-ui.md) so this failure does not reach run time.
- **Contract for the Runtime**: MUST surface the validation error to the operator (job fails at start); MAY set the skip property only to bypass legacy configurations. MUST report the message to the user as a configuration (USER) error.
- **Server exposure**: none.
- **Maturity level**: 0 - error propagation and required validations are level 0.
- **Example**:

```
-Dtalend.component.configuration.validation.skip=true
```

### VAL-013 Build-time model validators
- **Source**: `component-tools/src/main/java/org/talend/sdk/component/tools/validator/Validators.java` (+ `LayoutValidator.java`, `ActionValidator.java`, `OptionNameValidator.java`, `NoFinalOptionValidator.java`, `FixedSchemaValidator.java`, `DocumentationValidator.java`, `MetadataValidator.java`, `FamilyValidator.java`, `DatasetValidator.java`)
- **Category / Sub-area**: VAL / component model checks
- **Kind**: convention (executed by `talend-component-maven-plugin validate` / `component-tools`)
- **Applies to**: component projects before packaging
- **Description**: rules a component module must satisfy; hosts that accept third-party plugins MAY run them at deployment. Design-time relevant rules: option names contain no `.` and do not start with `$`; `@Option` fields are not `final`; every option of a class appears in its `@GridLayout` (or `@OptionsOrder`) and every layout item exists as an option; a component has a single root option and exactly one component annotation; `@Structure` fields are `List<String>`/`List<Object>`; each component has `@Icon` and `@Version`, family has an icon; `@Documentation` on components and options; `@HealthCheck`/`@DiscoverDataset` take one `@DataStore` parameter; `@DiscoverSchema` takes one `@DataSet` parameter; `@DynamicValues` takes no option parameter; `@DiscoverSchemaExtended` has one `@Option` parameter, an `incomingSchema` parameter named `incomingSchema` and optionally a `String branch`; `@Update` returns an object and `@Updatable.after` references a direct child; `@Proposable` not on enums and needs a matching `@DynamicValues`; `@FixedSchema` value matches a `@DiscoverSchema`/`@DiscoverSchemaExtended`.
- **Attributes**: none.
- **Contract for the Designer**: none.
- **Contract for the Runtime**: none.
- **Server exposure**: none.
- **Maturity level**: 2 - informational for hosts.
- **Example**:

```
mvn talend-component:validate
```
