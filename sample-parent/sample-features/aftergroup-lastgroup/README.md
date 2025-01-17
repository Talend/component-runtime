This is a TCK connector to validate thethe new `@LastGroup` annotation that is used in conjonctino with `@AfterGroup` within a processor.

# How to build & deploy in a studio
From the root of `component-runtime` workspace, run:
```shell
mvn clean sample -pl sample-parent/sample-features/aftergroup-lastgroup/ -am
```
Then deploy it into a studio:
```shell
java -jar sample-parent/sample-features/aftergroup-lastgroup/target/lastGroup-<version>.car studio-deploy --location <path to a studio> -f
```

# What is `@AfterGroup` / `@LastGroup` parameter for ?
Now, methods with annotated with `@AfterGroup` in a `@Processor` connector, can have a boolean parameter annotated `@LastGroup`.  If this parameter is defined in the `@AfterGroup` method, it will be set to `true` for the last group, and only for it.

# LastGroupProcessor Sample explanation
This sample is a simple `@Processor` that uses the `@AfterGroup` annotation with a `@LastGroup` parameter, [as you can see here](./src/main/java/org/talend/sdk/component/feature/lastgroup/processor/LastGroupProcessor.java#L41).

This processor just buffers records, and, in the last call of `@AfterGroup` [method](./src/main/java/org/talend/sdk/component/feature/lastgroup/processor/LastGroupProcessor.java#L62), it will send all the records to the output. It also checks that the number of bufferized records is the same as the expected number set in its configuration [Config.java](./src/main/java/org/talend/sdk/component/feature/lastgroup/config/Config.java#L36). 