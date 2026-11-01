## Exhaustive Complete Specification of TCK `/component/details` Endpoint Response

The response of the `component/details?id=<id>` endpoint of the Talend Component Kit (TCK) is a JSON object describing the complete visual, structural, and behavioral model of a requested component.

Below is an exhaustive, ground-up specification of every JSON hierarchy level, object schema, and nested metadata dictionary present in the response.

---

## 0. Root JSON Schema

The top-level JSON object consists of a single root array named `details`.

| Property | Type | Nullable | Description |
| --- | --- | --- | --- |
| **`details`** | `Array<ComponentDetail>` | No | An array containing one or more component definition objects matching the queried `id`.|

---

## 1. `ComponentDetail` Schema (`details[*]`)

Each object inside `details` describes the full identity, interface layout, dynamic actions, and data flow capabilities of a component.

| Property | Type | Nullable | Description                                                                                                                    |
| --- | --- | --- |--------------------------------------------------------------------------------------------------------------------------------|
| **`actions`** | `Array<Action>` | No | A list of server-side service operations (e.g., healthchecks, schema discovery, dynamic updates, ...) bound to this component. |
| **`displayName`** | `String` | No | The human-readable name of the component (e.g., `"Input"`).                                                                    |
| **`icon`** | `String` | No | The identifier key representing the visual icon for this component. this is the icon's name in the connector's family.         |
| **`id`** | `ComponentId` | No | Full coordinates and identifiers of the component plugin and family.                                                           |
| **`inputFlows`** | `Array<String>` | No | List of incoming data flow branch names (empty for source/input components).                                                   |
| **`links`** | `Array<Object>` | No | HATEOAS link objects for component-related REST resources.                                                                     |
| **`metadata`** | `Object<String, String>` | No | Component-level configuration metadata dictionary. *(See Section 1.1)*                                                         |
| **`outputFlows`** | `Array<String>` | No | List of outgoing data flow branch names (e.g., `["__default__"]`).                                                             |
| **`properties`** | `Array<Property>` | No | Exhaustive flat list of configuration property descriptors.                                                                    |
| **`type`** | `String` | No | The component type/paradigm (e.g., `"input"`).                                                                                 |
| **`version`** | `Integer` | No | The contract version number of the component.                                                                                  |

### 1.1 Component `metadata` Keys

This dictionary contains structural hints for runtime mappers, designer, and documentation:

* **`documentation::value`** (`String`): Explanation of the component's purpose.
* **`mapper::infinite`** (`String`): Boolean string (`"true"` / `"false"`) indicating if the mapper outputs an infinite stream.
  * When `false` the mapper has finished to send data when the returned value is `null` value.
  * When `true` the mapper will never finish to send data and the `null` returned values are ignored.
* **`mapper::optionalRow`** (`String`): Boolean string (`"true"` / `"false"`) indicating if output rows are optional. In some cases, input connector doesn't require its outputs to be wired to another connector (processor or output). Setting 'mapper::optionalRow' to 'true' tells the designer that leaving output streams unconnected is valid.

---

## 2. `ComponentId` Schema (`details[*].id`)

Identifies the component's family, package location, and Maven coordinates.

| Property | Type | Nullable | Description |
| --- | --- | --- | --- |
| **`family`** | `String` | No | The component family name (e.g., `"sampleForm"`).|
| **`familyId`** | `String` | No | Base64-encoded unique identifier of the component family.|
| **`id`** | `String` | No | Base64-encoded unique identifier of this specific component.|
| **`name`** | `String` | No | Technical name of the component (e.g., `"Input"`).|
| **`plugin`** | `String` | No | Name of the plugin bundle containing the component.|
| **`pluginLocation`** | `String` | No | Complete Maven coordinates of the plugin artifact (`groupId:artifactId:version`).|

---

## 3. `Action` Schema (`details[*].actions[*]`)

Defines server-side REST callbacks that can be triggered by the UI.

| Property | Type | Nullable | Description                                                                                                                                                                                                                                                                                                                |
| --- | --- | --- |----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`displayName`** | `String` | No | Human-readable label for the action.                                                                                                                                                                                                                                                                                       |
| **`family`** | `String` | No | The component family name associated with the action.                                                                                                                                                                                                                                                                      |
| **`name`** | `String` | No | Unique technical identifier key of the action (e.g., `"HEALTHCHECK"`, `"DISCOVERSCHEMA"`).                                                                                                                                                                                                                                 |
| **`properties`** | `Array<Property>` | No | List of parameter definitions passed to or returned by the action service.                                                                                                                                                                                                                                                 |
| **`type`** | `String` | No | Functional category of the action: `"update"`, `"healthcheck"`, `"suggestions"`, `"validation"`, or `"schema"`. Actions can be called by the component-server `/api/v1/action/execute?family=<family id>&type=<type>&action=<action's name>&lang=<lang>`, the body contains the json configuration expected by the action. |

---

## 4. `Property` Schema (`details[*].properties[*]`, `details[*].actions[*].properties[*]`)

Every item in `properties` defines a field, section, or array element inside the configuration form or action parameter list.

| Property | Type | Nullable | Description |
| --- | --- | --- | --- |
| **`defaultValue`** | `String` | Yes | Default literal value assigned to the property (represented as a string even for numeric/boolean fields).|
| **`displayName`** | `String` | No | Human-readable UI display label for the field.|
| **`metadata`** | `Object<String, String>` | No | An exhaustive dictionary of key-value attributes controlling layout, widgets, conditions, and action bindings. *(See Section 5)*|
| **`name`** | `String` | No | Simple field identifier name.|
| **`path`** | `String` | No | Full dot-notated or bracket-notated hierarchy path (e.g., `"config.dataset.datastore.login"`, `"config.someLists.aListOfStrings[]"`).|
| **`placeholder`** | `String` | Yes | Text placeholder displayed inside UI input controls.|
| **`proposalDisplayNames`** | `Object<String, String>` | Yes | Dictionary mapping raw `ENUM` values to human-readable UI display labels.|
| **`type`** | `String` | No | The data type: `"STRING"`, `"NUMBER"`, `"BOOLEAN"`, `"OBJECT"`, `"ARRAY"`, or `"ENUM"`.|
| **`validation`** | `Validation` | Yes | Validation rules and constraint parameters for this property. *(See Section 6)*|

---

## 5. Exhaustive `properties[*].metadata` Key Reference

The `metadata` map contains string-to-string key-value pairs that dictate how the UI renders, validates, and binds services to the property.

### 5.1 UI Layout & Container Grid Formatting

* **`ui::gridlayout::Main::value`**: A pipe-separated (`|`) and comma-separated (`,`) layout string defining the horizontal and vertical arrangement of child widgets on the `"Main"` form tab (e.g., `"login,password|healthcheckOk"`).
* **`ui::gridlayout::Advanced::value`**: Layout string defining widget positioning on the `"Advanced"` configuration tab.
* **`configurationtype::name`**: Name of the configuration type categorization (usually `"default"`).
* **`configurationtype::type`**: Specifies if the `OBJECT` property acts as a reusable configuration block: `"datastore"` (connection) or `"dataset"` (table/query). The TCK framework allows developers to specify new kind of reusable configuration. So, the type is not limited to `datastore` or `dataset`.
* **`definition::parameter::index`**: Numeric string (`"0"`, `"1"`, etc.) defining the parameter order when invoking actions.
* **`documentation::value`**: Tooltip or help text describing the field.

### 5.2 Specific UI Widgets & Display Modifications

* **`ui::credential`**: `"true"` if the field contains sensitive data (passwords, tokens) and must be masked/encrypted.
* **`ui::hidden`**: `"true"` if the field is hidden from the user interface.
* **`ui::readonly`**: `"true"` if the user cannot modify the field value.
* **`ui::textarea`**: `"true"` to render the string input as a multi-line text area.
* **`ui::code::value`**: Highlights text syntax in a code editor block. Possible values include `"json"`, `"sql"`.
* **`ui::datetime`**: Defines date/time widget behavior: `"datetime"` or `"zoneddatetime"`.
* **`ui::datetime::useUTC`**: `"true"` / `"false"` indicating whether UTC time should be enforced.
* **`ui::datetime::dateFormat`**: Format string for date display (e.g., `"YYYY/MM/DD"`).
* **`ui::datetime::useSeconds`**: `"true"` / `"false"` indicating whether seconds selection is shown.
* **`ui::path::value`**: Renders a file/folder picker widget. Values: `"FILE"` or `"DIRECTORY"`.
* **`ui::defaultvalue::value`**: Metadata documentation string detailing default value behavior.

### 5.3 Conditional Visibility Rules (`condition::if::*`)

Controls dynamic showing/hiding of properties based on other fields' values:

* **`condition::if::target`**: The `name` or `path` of the field being evaluated.
* **`condition::if::value`**: The target value required to trigger visibility (can be comma-separated like `"abc,def"`).
* **`condition::if::evaluationStrategy`**: Strategy used to compare values:
  * `"DEFAULT"`: Exact equality comparison.
  * `"CONTAINS"`: Checks if the target string contains the value.
  * `"LENGTH"`: Checks if the target string length equals the value.
* **`condition::if::negate`**: `"true"` to invert the condition logic.
* **`condition::ifs::operator`**: Logical join operator when multiple conditions apply: `"AND"` or `"OR"`.
  * **Indexed Condition Keys** *(used when combining multiple conditions)*:
  * `condition::if::target::0`, `condition::if::target::1`
  * `condition::if::value::0`, `condition::if::value::1`
  * `condition::if::evaluationStrategy::0`, `condition::if::evaluationStrategy::1`
  * `condition::if::negate::0`, `condition::if::negate::1`

### 5.4 Dynamic Server Action Bindings (`action::*`)

Links form controls to backend actions defined in `details[*].actions`:

* **`action::healthcheck`**: Name of the healthcheck action triggered by a button in this section (`"HEALTHCHECK"`).
* **`action::suggestions`**: Name of the suggestion service action to populate dropdown choices (`"SUGGESTABLE"`).
* **`action::suggestions::parameters`**: Comma-separated list of relative field paths passed as arguments to the suggestion service (`"someComplexConfig,singleString"`).
* **`action::validation`**: Name of the asynchronous validation action to call when the field value changes (`"ASYNC_VALIDATION"`, `"ASYNC_VALIDATION2"`).
* **`action::validation::parameters`**: Relative paths passed to the validation service (`"."`).
* **`action::update`**: Name of the action called to dynamically update/reload form fields (`"UPDATABLE"`).
* **`action::update::parameters`**: Paths of parameters sent to the update service (`"singleString,suggestedElement"`).
* **`action::update::after`**: Specifies that the update action fires after a specific field is modified (`"anInteger"`).
* **`action::update::activeIf`**: Condition controlling when the update callback button is active.

### 5.5 Schema Discovery Attributes

Used on array/structure properties representing data schemas:

* **`action::schema`**: Name of the discover schema action (`"DISCOVERSCHEMA"`).
* **`action::schema::discoverSchema`**: Explicit binding name to the discover action (`"DISCOVERSCHEMA"`).
* **`action::schema::type`**: Flow direction type (`"OUT"`).
* **`ui::structure::type`**: Structure direction indicator (`"OUT"`).
* **`ui::structure::discoverSchema`**: Identifies the schema discovery trigger action.
* **`ui::structure::value`**: Flow target name (`"__default__"`).

---

## 6. `Validation` Schema (`details[*].properties[*].validation`)

Defines rules that values must adhere to before submitting the form.

| Property | Type | Nullable | Description |
| --- | --- | --- | --- |
| **`enumValues`** | `Array<String>` | Yes | Array of valid string options allowed when property `type` is `"ENUM"`.|
| **`max`** | `Number` | Yes | Maximum permissible numeric value.|
| **`maxItems`** | `Integer` | Yes | Maximum number of items allowed in an `ARRAY`.|
| **`maxLength`** | `Integer` | Yes | Maximum allowed character length for a `STRING`.|
| **`min`** | `Number` | Yes | Minimum permissible numeric value (often `-2147483648` for default integers).|
| **`minItems`** | `Integer` | Yes | Minimum number of items required in an `ARRAY`.|
| **`minLength`** | `Integer` | Yes | Minimum required character length for a `STRING`.|
| **`pattern`** | `String` | Yes | A regular expression pattern string that a `STRING` value must match (e.g., `"^start[abcdef]{0,5}END$"`).|
| **`required`** | `Boolean` | Yes | `true` if the field cannot be empty.|
| **`uniqueItems`** | `Boolean` | Yes | `true` if all elements inside an `ARRAY` must be unique.|