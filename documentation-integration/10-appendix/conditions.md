# Appendix - Conditions

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`). Sources: `component-api/.../configuration/condition/*`, `documentation/src/main/antora/modules/ROOT/pages/_partials/generated_conditions.adoc`, `ConditionParameterEnricher`, `VisibilityService`, `AbstractWidgetConverter.createCondition`. Catalog: `UI-016`..`UI-019` in [UI](../02-feature-catalog/UI-ui.md). Algorithms: [05 section 7](../05-configuration-and-ui.md).

## 1. Condition annotations

| Annotation | `@Condition` type | Target | Attributes (default) |
|---|---|---|---|
| `@ActiveIf` (`@Repeatable(ActiveIfs.class)`) | `if` | field, parameter | `target` (required), `value` (`String[]`, required), `negate` (`false`), `evaluationStrategy` (`DEFAULT`), `evaluationStrategyOptions` (`{}`) |
| `@ActiveIfs` | `ifs` | field, parameter | `operator` (`AND`), `value` (`ActiveIf[]`) |

`@Condition(String value)` (meta, `@Target(ANNOTATION_TYPE)`) names the type; `ConditionParameterEnricher` builds `tcomp::condition::<type>::<attribute>`.

### Enumerations

| Enum | Constants | Meaning |
|---|---|---|
| `ActiveIf.EvaluationStrategy` | `DEFAULT` | compare raw value |
| | `LENGTH` | compare size of array/collection/string |
| | `CONTAINS` | string contains value / collection has an element containing value |
| `ActiveIfs.Operator` | `AND`, `OR` | combine conditions |
| `ActiveIf.EvaluationStrategyOption(name, value="true")` | `lowercase` (for `CONTAINS`) | lower-case the actual value before comparing |

### `UIScope` constants (`target = "ui.scope"`)

`TARGET = "ui.scope"`; values: `STUDIO_SCOPE = "studio"`, `CLOUD_SCOPE = "cloud"`, `STUDIO_COMPONENT_SCOPE = "studio.component"`, `STUDIO_CONNECTION_COMPONENT_SCOPE = "studio.component.connection"`, `STUDIO_METADATA_SCOPE = "studio.metadata"`.

## 2. Metadata produced (web keys, `tcomp::` stripped)

`@ActiveIf`:

```json
{
  "condition::if::evaluationStrategy": "DEFAULT",
  "condition::if::negate": "false",
  "condition::if::target": "test",
  "condition::if::value": "value1,value2"
}
```

`@ActiveIfs` (indexed, sample from `generated_conditions.adoc`):

```json
{
  "condition::if::evaluationStrategy::0": "DEFAULT",
  "condition::if::evaluationStrategy::1": "LENGTH",
  "condition::if::negate::0": "false",
  "condition::if::negate::1": "true",
  "condition::if::target::0": "sibling1",
  "condition::if::target::1": "../../other",
  "condition::if::value::0": "value1,value2",
  "condition::if::value::1": "SELECTED",
  "condition::ifs::operator": "AND"
}
```

Details: `String[]` values are joined by `,`; `evaluationStrategyOptions` are appended to the strategy as `CONTAINS(lowercase=true)` (`name=value` pairs joined by `,` inside parentheses).

## 3. Target path syntax

`.` self; name without `.` = sibling; `..` = parent (repeatable, `/`-separated); `./x` child; other = absolute. Full algorithm: `ACT-025` / [05 section 7.1](../05-configuration-and-ui.md).

## 4. Evaluation summary

| Strategy | True when |
|---|---|
| `DEFAULT` | stringified actual value equals any listed value |
| `LENGTH` | size of actual (collection/array/string; missing = 0) equals any listed integer |
| `CONTAINS` | actual (string) contains a listed value, or any element of an actual collection/array contains it (optionally lower-cased) |

Final result of one condition = `negate != anyMatch`. Several conditions are combined with `condition::ifs::operator` (default `AND`). Absent condition = always active. Inactive properties are not validated.

## 5. JSON-logic output of `component-form`

| Case | Output |
|---|---|
| `DEFAULT` | `{"===":[{"var":"<path>"}, <value>]}` (value converted to boolean/number by target type) |
| `LENGTH` | `{"===":[{"var":"<path>.length"}, <int>]}` |
| `CONTAINS` (target not array) | `{"in":[<value>, {"var":"<path>"}]}` |
| `CONTAINS` (target array) | `{"in":[{"var":"<path>"}, <value>]}` |
| `CONTAINS(lowercase=true)` | as above with lower-cased value |
| several values | `{"or":[ ... ]}` |
| `negate=true` | `{"==":[ <cond>, false ]}` |
| several conditions | `{"and"\|"or":[ ... ]}` per `condition::ifs::operator` |
| `ui::hidden` | `{"==":[1,-1]}` (always false) |
| `ui.scope` | constant `{"==":[1,1]}` (shown) or `{"==":[1,-1]}` (hidden); web acts as `cloud` |
