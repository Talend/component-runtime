# Component Runtime :: Sample Feature :: Checkpoint

## Table of Contents
- [Overview](#overview)
- [Usage](#usage)
  - [How to build the sample connector plugin](#how-to-build-the-sample-connector-plugin)
  - [How to run](#how-to-run)
  - [How to use](#how-to-use)
- [Plugin artifact](#plugin-artifact)
- [Execution examples](#execution-examples)
  - [Run checkpoint connector with default behavior](#run-checkpoint-connector-with-default-behavior)
  - [Run checkpoint connector with a checkpointing configuration](#run-checkpoint-connector-with-a-checkpointing-configuration)
  - [Run with framework feature disabled](#run-with-framework-feature-disabled)
  - [Configuration sample](#configuration-sample)

## Overview
This is a simple TCK connector plugin to test and validate the checkpoint input feature.

This project contains two things:
 1. A sample connector plugin that implements the checkpoint feature.
 2. A simple Cli runner that can be used to run the connector plugin and test the checkpoint feature.


## Usage
### How to build the sample connector plugin
Checkout the code from the repository and build the project using `mvn clean install`  
Alternatively build the feature module using `mvn install -am -pl :checkpoint`

### How to run
To run the connector, you need exec the generated artifact `org.talend.sdk.component.sample.feature:checkpointruntime`.  
* You can run it directly from `target` folder or repository folder
  * `java -jar target/checkpointruntime-1.80.0-SNAPSHOT.jar`
* or you can run it from the maven repository
  * `java -jar ~/.m2/repository/org/talend/sdk/component/sample/feature/checkpoint/1.80.0-SNAPSHOT/checkpointruntime-1.80.0-SNAPSHOT.jar`

For later usage, will use the variable `$RUNCMD` as the way you may choose.  
⚠️ If you're using jdk17, don't forget to add the `--add-opens` option to the command line
(see profile _jdk9_ in master pom at the repository's root) or use instead jdk11.

### How to use
Run checkpoint with the help to  get all command options for execution:

```bash
$ $RUNCMD checkpoint help

Usage: checkpoint [options]

Options:
  --checkpoint=<File>        Checkpoint json file to resume execution.
                             (default: ./checkpoint.json)
  --configuration=<File>     Connector configuration in json format.
                             (default: ./configuration.json)
  --disable-feature          Disable checkpointing feature.
  --fail-after=<int>         Throw an exception after n records read to simulate failure.
                             (default: -1)
  --family=<String>          Family of the component to use for the checkpoint.
                             (default: checkpoint)
  --gav=<String>             GAV of the component to use for the checkpoint.
                             (default: org.talend.sdk.component.sample.feature:checkpointruntime:jar:1.80.0-SNAPSHOT)
  --jar=<File>               Full path to jar of the component to use for the checkpoint.
  --log                      Log when a checkpoint is serialized.
  --mapper=<String>          Mapper to use for the checkpoint.
                             (default: incrementalSequenceInput)
  --re-use                   Re-use checkpoint file, it will match latest checkpoint available, otherwise will create numbered checkpoint.
  --work-dir=<File>          Where to create temporary checkpoint files.
                             (default: ./)
```

## Plugin artifact
There are two ways to run the checkpoint runner with a specific plugin artifact:
- Using the `--gav` option to specify the GAV of the plugin artifact.  
    Syntax:  `groupId:artifactId:version[:packaging[:classifier]]`  

- Using the `--jar` option to specify the path to the plugin artifact.  
    Syntax:  `/path/to/plugin.jar`

⚠️ You cannot use both options at the same time.

## Execution examples
### Run checkpoint connector with default behavior
`java -jar target/checkpointruntime-1.80.0-SNAPSHOT.jar checkpoint`

```bash 
[INFO]  Manager is using plugin: checkpoint from GAV org.talend.sdk.component.sample.feature:checkpointruntime:jar:1.80.0-SNAPSHOT.
[WARN]  ./configuration.json (No such file or directory)
[WARN]  ./checkpoint.json (No such file or directory)
[INFO]  configuration: {}
[DATA]  {"data":"0"}
[DATA]  {"data":"1"}
[DATA]  {"data":"2"}
[...]
[DATA]  {"data":"18"}
[DATA]  {"data":"19"}
[INFO]  finished.
```

### Run checkpoint connector with a checkpointing configuration
In this example we turn on the log verbose, giving a checkpointing configuration file.  
`% java -jar target/checkpointruntime-1.80.0-SNAPSHOT.jar checkpoint --configuration=configuration-example.json --log`

```bash
[INFO]  Manager is using plugin: checkpoint from GAV org.talend.sdk.component.sample.feature:checkpointruntime:jar:1.80.0-SNAPSHOT.
[WARN]  ./checkpoint.json (No such file or directory)
[INFO]  configuration: {configuration.dataset.maxRecords=10}
[INFO]  Checkpoint 1 reached with {"$checkpoint":{"sinceId":0,"__version":2}}.
[DATA]  {"data":"0"}
[DATA]  {"data":"1"}
[INFO]  Checkpoint 2 reached with {"$checkpoint":{"sinceId":2,"__version":2}}.
[DATA]  {"data":"2"}
[...]
[DATA]  {"data":"7"}
[INFO]  Checkpoint 5 reached with {"$checkpoint":{"sinceId":8,"__version":2}}.
[DATA]  {"data":"8"}
[DATA]  {"data":"9"}
[INFO]  Checkpoint 6 reached with {"$checkpoint":{"sinceId":9,,"__version":2}}.
[INFO]  finished.
```
This will generate a checkpoint file `checkpoint.json` containing the last checkpoint reached on top of classical
and connector configuration.

```bash
% cat checkpoint.json | jq

{
  "$checkpoint": {
    "sinceId": 9,
    "__version": 2
  }
}
```
Note: `$checkpoint` is a reserved property name, it will be used to store the checkpoint object, as the `__version` property.

### Run with framework feature disabled
In this example we turn off the checkpointing framework capability.  
`% java -jar target/checkpointruntime-1.80.0-SNAPSHOT.jar checkpoint --disable-feature --log`

```bash
[INFO]  Manager is using plugin: checkpoint from GAV org.talend.sdk.component.sample.feature:checkpointruntime:jar:1.80.0-SNAPSHOT.
[WARN]  ./configuration.json (No such file or directory)
[WARN]  ./checkpoint.json (No such file or directory)
[INFO]  configuration: {}
[DATA]  {"data":"0"}
[DATA]  {"data":"1"}
[DATA]  {"data":"2"}
[DATA]  {"data":"3"}
[DATA]  {"data":"4"}
[...]
[DATA]  {"data":"15"}
[DATA]  {"data":"16"}
[DATA]  {"data":"17"}
[DATA]  {"data":"18"}
[DATA]  {"data":"19"}
[INFO]  finished.
```
We can see that no checkpoint file is generated compare to previous example.

### Configuration sample
In order to test and run the feature you will find provided configuration files in the [resources](src/main/resources) folder.

 * Sample configuration [configuration-example.json](src/main/resources/configuration-example.json) file
   * This configuration will limit the number of records to 10.
 * Sample IncrementalSequenceInput connector configuration **VERSION-2** [checkpoint-v2.json](src/main/resources/checkpoint-v2.json)
   * This configuration will start the checkpoint from record Id 1 defined by `sinceId`.
 * Testing migration file, using a **VERSION-1** of IncrementalSequenceInput connector configuration [checkpoint-v1.json](src/main/resources/checkpoint-v1.json)
   * The `lastId` property is used to identify the last record Id processed. 
     This configuration shall be correctly migrated to the new format (`lastId` renamed to `sinceId`)
     by the `CheckpointMigrationHandler`.
   * Execution with this configuration, shall start at 10.

