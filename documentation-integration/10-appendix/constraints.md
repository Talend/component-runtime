# Appendix - Constraints (validations)

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`). Sources: `component-api/.../configuration/constraint/*`, `documentation/src/main/antora/modules/ROOT/pages/_partials/generated_constraints.adoc`, `ValidationParameterEnricher`, `PropertyValidationService`. Catalog: [VAL](../02-feature-catalog/VAL-validation.md).

## 1. Constraint table (9 metadata names from 5 annotations)

In-JVM key = `tcomp::validation::<name>`. On the server the value is exposed in `SimplePropertyDefinition.validation.<field>` (NOT in `metadata`; the generated documentation still shows `validation::<name>` keys).

| Annotation | Metadata `<name>` | Supported Java types | Parameter | `validation` field (type) | JSON-schema keyword | Sample value |
|---|---|---|---|---|---|---|
| `@Required` | `required` | `java.lang.Object` (any) | none | `required` (`Boolean`) | parent `required[]` | `"true"` |
| `@Min` | `min` | `Number`, `int`, `short`, `byte`, `long`, `double`, `float` | `double value` | `min` (`Integer`) | `minimum` | `"12.34"` |
| `@Min` | `minItems` | `java.util.Collection` | `double value` | `minItems` (`Integer`) | `minItems` | `"12.34"` |
| `@Min` | `minLength` | `java.lang.CharSequence` | `double value` | `minLength` (`Integer`) | `minLength` | `"12.34"` |
| `@Max` | `max` | `Number`, `int`, `short`, `byte`, `long`, `double`, `float` | `double value` | `max` (`Integer`) | `maximum` | `"12.34"` |
| `@Max` | `maxItems` | `java.util.Collection` | `double value` | `maxItems` (`Integer`) | `maxItems` | `"12.34"` |
| `@Max` | `maxLength` | `java.lang.CharSequence` | `double value` | `maxLength` (`Integer`) | `maxLength` | `"12.34"` |
| `@Pattern` | `pattern` | `java.lang.CharSequence` | `String value` (JavaScript regex) | `pattern` (`String`) | `pattern` | `"test"` |
| `@Uniques` | `uniqueItems` | `java.util.Collection` | none | `uniqueItems` (`Boolean`) | `uniqueItems` | `"true"` |

Resolution: for repeated `@Validation` entries on one annotation, the first whose `expectedTypes` accepts the option's raw Java class wins; none = no metadata. Server conversion of numbers: `Double.valueOf(text).intValue()` (fractions truncated). `enumValues` (`Collection<String>`) is added by the server for `ENUM` options and is not an annotation.

## 2. Implicit constraints

| Java type | Implicit metadata |
|---|---|
| `int`, `Integer` | `min = -2147483648`, `max = 2147483647` (overridable by explicit `@Min`/`@Max`) |
| `char` | `minLength = 1`, `maxLength = 1` |
| `Character` | `maxLength = 1` |
| `java.time.*` (`ZonedDateTime`, `LocalDateTime`, `LocalDate`, `LocalTime`) | implicit `@DateTime` (not a constraint, see UI-010) |

## 3. Built-in options constraints

`$maxBatchSize`: `min = 1`; `$maxRecords`, `$maxDurationMs`: `min = -1` (`CFG-016`).

## 4. Runtime enforcement

`ReflectionService.PayloadValidator` checks `required` (null), `min`, `max`, `minLength`, `maxLength`, `minItems`, `maxItems`, `uniqueItems`, `pattern`, only for visible parameters; disabled by `-Dtalend.component.configuration.validation.skip=true`; failure message `- <msg>` per violation in an `IllegalArgumentException` (`VAL-012`).

## 5. Meta-annotations

`@Validation(expectedTypes, name)` (repeatable) and `@Validations` (container), both `@Target(ANNOTATION_TYPE)`, package `configuration.constraint.meta` (`VAL-006`).

## 6. Server-side validation action

`@AsyncValidation` -> `ValidationResult{status: OK|KO, comment}` (`VAL-010`, `VAL-011`).
