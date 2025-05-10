# Overview of TCK Configuration Form Features
TCK connectivity (_emitters, processors, sinks_) are configured by standard java classes. Configurations classes support
composition and inheritance.
Those configuration classes can be decorated using some annotation to add some metadata that will be used to accurate
the configuration forms generation.

All java attributes that should be rendered in forms must be annotated with `@Option`. Those attributes can be set
in some layouts, each one could be rendered as a panel, tab, etc... Final applications that integrate TCK should
at least support two layout: `Main` and `Advanced`.

```Java
@GridLayout(value = {
        @GridLayout.Row({ "login", "password" }),
        @GridLayout.Row({ "healthcheckOk" })
})
@GridLayout(names = FormType.ADVANCED, value = { @GridLayout.Row({ "timeout" }) })
public class ADatastore implements Serializable {

    @Option
    @Documentation("A login.")
    private String login;

    @Option
    @Credential
    @Documentation("A password.")
    private String password;
    
    ...

}
```
Many other annotations can be set on attribute to configure how forms should be rendered or validated.

## How to build and test
To build the sample, clone the repository and build the module and its dependencies:
```bash
git clone git@github.com:Talend/component-runtime.git
mvn clean install -pl sample-parent/sample-features/configuration-form/ -am -DskipTests 
```

### Test in the Web UI
The web tester is an autonomous web application that can render TCK form locally in a Web UI (the same used in Talend
cloud applications). To execute it, go to the sample module and execute the maven plugin:
```bash
cd sample-parent/sample-features/configuration-form/
mvn talend-component:web -Dtalend.web.port=8080
```
With a web browser, open the url `http://localhost:8080` if it is not automatically open.

### Test in the studio
The `configuration-form` module provide a TCK plugin a single input connector. At build time, a `.car`
(_Component ARchive_) file is generated and can be deployed in Talend studio with:
```bash
java -jar sample-parent/sample-features/configuration-form/target/configurationFormSample-1.81.0-SNAPSHOT.car \
           studio-deploy --location <path to your studio> -f
```
A new connector will be available in the palette: `tSampleFormInput`.

### Usage
Each section of the configuration ha sits own checkbox to display or not the section. There is one input connector
that will generate just one record containing those attributes:
- `configuration`: the configuration set by the user and received by the runtime.
- `fixedString`: Always contains `"A fixed string"`
- `fixedInt`: always contains `100`
- `fixedBoolean`: always contains `true`

For instance, there is the output in the studio:
```text[statistics] connecting to socket on port 3797
[statistics] connecting to socket on port 4062
[statistics] connected
.------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------+--------+------------.
|                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   tLogRow_1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|=-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------+--------+-----------=|
|configuration                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |fixedString   |fixedInt|fixedBoolean|
|=-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------+--------+-----------=|
|ComplexConfig(dataset=ADataset(datastore=ADatastore(login=, password=null, healthcheckOk=false, timeout=0), entity=ORDER, schema=[StudioSchema(label=configuration, originalDbColumnName=configurationxx, key=false, type=, talendType=id_String, nullable=true, pattern=, length=50, precision=-1, defaultValue=null, comment=aa), StudioSchema(label=fixedString, originalDbColumnName=fixedStringxx, key=false, type=, talendType=id_String, nullable=true, pattern=, length=-1, precision=-1, defaultValue=null, comment=zzz), StudioSchema(label=fixedInt, originalDbColumnName=fixedIntxx, key=true, type=, talendType=id_Integer, nullable=false, pattern=, length=-1, precision=5, defaultValue=, comment=eeerr), StudioSchema(label=fixedBoolean, originalDbColumnName=fixedBooleanxx, key=false, type=, talendType=id_Boolean, nullable=false, pattern=, length=-1, precision=-1, defaultValue=, comment=ttt)]), displayAllSupportedTypes=false, allSupportedTypes=null, displayConfiguredWidget=false, configuredWidgets=null, displaySomeLists=false, someLists=null, displayElementsWithConstraints=false, elementsWithConstraints=null, displayConditionalDisplay=false, conditionalDisplay=null, displayDynamicElements=false, dynamicElements=DynamicElements(singleString=null, someComplexConfig=DynamicElements.SomeComplexConfig(aString=null, aBoolean=false, anInteger=0), asyncValidation=, suggestedElement=null))|A fixed string|100     |true        |
'------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------+--------+------------'

[statistics] disconnected
```

## Supported types
The class [AllSupportedTypes.java](src/main/java/org/talend/sdk/component/feature/form/config/AllSupportedTypes.java)
lists all main supported types:
- `BigDecimal`
- `BigInteger`
- `Boolean`
- `File`
- `float`
- `InetAddress`
- `int`
- `LocalDateTime`
- `String`
- `URI`
- `URL`
- `ZonedDateTime`

The [documentation](https://talend.github.io/component-runtime/main/latest/component-configuration.html#_primitives)
specifies that it should support all types with a `org.apache.xbean.propertyeditor.Converter`.

## Configuration of widgets
Some anotations are only dedicated to give more information about how the UI widget should be rendered. For instance,
if the java attribute is a `String`, we should be able to specify if it should be a single line input, or, a multiline
one.

The class [ConfiguredWidgets.java](src/main/java/org/talend/sdk/component/feature/form/config/ConfiguredWidgets.java)
exposes the usage of all of them:
- `@Code`
- `@Credential`
- `@DefaultValue`
- `@Hidden`
- `@Path`
- `@ReadOnly`
- `@TextArea`

## Supported lists
Configuration could contain list. It can be list of primitive attributes or of complex objects. This is presented in the
[SomeLists.java](src/main/java/org/talend/sdk/component/feature/form/config/SomeLists.java) class.

## Constraint on elements
Some annotations help to explicit constraints on widgets. They are listed in 
[ElementsWithConstraints.java](src/main/java/org/talend/sdk/component/feature/form/config/ElementsWithConstraints.java)
class:
- `@Max`
- `@Min`
- `@Pattern`
- `@Required`
- `@Uniques`

They can exhibit different behaviors depending on the element they are associated with.

## Conditional Element Display
The class [ConditionalDisplay.java](src/main/java/org/talend/sdk/component/feature/form/config/ConditionalDisplay.java)
shows how some element can be hidden or display according to the value set in some others. Rules are defined with
`@ActiveIf` annotation. It takes the name of another attribute, an expected value, an evaluation strategy, and, a flag
if we want to inverse the logic.

Several `@ActiveIf` can be evaluated with an operator `AND` or `OR` using the `@ActiveIfs` annotation.

## Dynamic elements binded to backend services
TCK form can call services to retrieve some value, update a sub-section of the configuration, or to validate some
fields. It is exposed in [DynamicElements.java](src/main/java/org/talend/sdk/component/feature/form/config/DynamicElements.java)
:
- `@Updatable`
- `@Validable`
- `@Suggestable`

## Schema
The `@Structure` annotation let the use define a list elements that define a schema. The type of element in the list 
should contain some of all attributes listed in the
[documentation](https://talend.github.io/component-runtime/main/latest/studio-schema.html#_accessing_columns_metadata):
- `comment`
- `defaultValue`
- `key`
- `label`
- `length`
- `nullable`
- `originalDbColumnName`
- `pattern`
- `precision`
- `talentType`
- `type`

In a studio job, at runtime it will be populated with the information in the studio schema. In the web UI, the user 
will have to set each field manually. 

# Form descriptions
All metadata are aggregated by the `component-manager` and served by the `component-server`. An extension transforms
TCK raw metadata to uispec for Web UI.

## Component-server metadata
The studio starts a `component-server` in a thread. It can be queried to retrieve the full description of the
`Input` connector. In a Windows powershell it can be done with:
```bash
# Send the web request
$response = Invoke-WebRequest -Uri http://localhost:9999/api/v1/component/details?identifiers=Y29uZmlndXJhdGlvbkZvcm1TYW1wbGUjc2FtcGxlRm9ybSNJbnB1dA
# Extract the content from the response
$jsonContent = $response.Content

# Convert the JSON string to a PowerShell object
$jsonObject = $jsonContent | ConvertFrom-Json

# Convert the object back to a JSON string with indentation for pretty printing
$formattedJson = $jsonObject | ConvertTo-Json -Depth 100 -Compress:$false

# Specify the path where you want to save the file
$filePath = "C:\tmp\metadata.json"

# Write the formatted JSON content to the file
$formattedJson | Out-File -FilePath $filePath -Encoding utf8
```
and in a linux system:
```bash
curl http://localhost:9999/api/v1/component/details?identifiers=Y29uZmlndXJhdGlvbkZvcm1TYW1wbGUjc2FtcGxlRm9ybSNJbnB1dA | jq . > /tmp/metadata.json
```
Here is the full response payload: [tck metadata](src/main/resources/tck-metadatas.json). The form description is found
the `properties` attribute.

## Generated UISpec
Using the web tester, it is possible to get the generated [uispec](src/main/resources/uispec.json). Press `F12` in the
browser and select network. then after select the connector in the web ui. A rest call is done and you can retrieve 
the response payload.


