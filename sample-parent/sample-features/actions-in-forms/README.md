This is a simple TCK bundle that helps to validate how users receive error messages when an exception is thrown
by a service called during the configuration of a connector.

The identified services are:
- @AsyncValidation
- @DiscoverSchema
- @DiscoverSchemaExtended
- @Suggestions
- @Update

It contains two connectors:
- An `@Emitter` named `Input`
- A `@Processor` named `Processor`

They work exactly the same, except, that the input calls `@DiscoverSchema` service whereas the processor calls `@DiscoverSchemaExtended` one
when `Guess schema` button is clicked.

## How to build:
```shell
mvn clean install
```

How to deploy in a studio:
```
java -jar .\target\actionsInForms-x.y.z.car studio-deploy  --location "<path to your studio>" -f
```

## How to use
The  connector has few fields:
- Suggestable:
  - Calls a @suggestions service 
  - The user can select one value among the three suggestions
- Input 1:
  - Calls an `@AsyncValidation` service
  - An async validation service check its value:
    - if it starts by `ok` it is valid
    - If it contains `exception`, an exception is thrown at validation time
- `Copy 'Input 1' to 'Input 2'` button:
  - Calls an `@Update` service 
  - Copy the value set in `Input 1` in the following field `Input 2`
- The guess schema button
  - It calls a `@DiscoverSchema` service

All those services work as expected. When the user checks the `All services throw exception`, then all the services will throw an exception (_except `@AsyncValidation` that throws an exception when it contains `exception`_).
So we are able to check how the error message is displayed to the users.

### Expected error messages:
Here are error messages that are thrown by services when `All services throw exception` is checked:
- `@DiscoverSchema`: "Exception thrown in @DiscoverSchema service."
- `@DiscoverSchemaExtended`: "Exception thrown in @DiscoverSchemaExtended service."
- `@Suggestions`: "Exception thrown in @Suggestions service."
- `@Update`: "Exception thrown in @Update service."
- `@AsyncValidation`: "Exception thrown in @AsyncValidation service."