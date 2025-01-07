This is a TCK connector to validate the integration of `@FixedSchema` annotation.

# How to build & deploy in a studio
From the root of `component-runtime` workspace, run:
```shell
mvn clean install -pl sample-parent/sample-features/fixed-schema/ -am
```
Then deploy it into a studio:
```shell
java -jar sample-parent/sample-features/fixed-schema/target/fixedschema-<version>.car studio-deploy --location <path to a studio> -f
```

# @FixedSchema feature
The `@FixedSchema` is used to inform final application (_application where TCK is integrated_), that the output flow
designed by this annotation should:
- Have its `Guess schema` button hidden
- Have the `@DiscoverSChema` or ``@DiscoverSChemaExtended` automatically called and the schema set silently

## In an input connector
This sample connector has the `@FixedSchema` annotation set on its `@Emitter` [here](./src/main/java/org/talend/sdk/component/sample/feature/fixedschema/input/FixedSchemaInput.java#L32).
