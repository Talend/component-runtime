# Component Runtime :: Sample Feature :: Checkpoint

## Overview

This is a simple TCK connector to test and validate the checkpoint input feature.

## How to build

Checkout the code from the repository and build the project using the following command:

```bash
mvn clean install
```
or simply build the feature module using the following command:

```bash 
mvn install -am -pl :checkpoint
```

## How to run

To run the connector, you need exec the generated artifact `org.talend.sdk.component.sample.feature:checkpoint`.

You can run it directly from `target` folder or repository folder:

```bash
java -jar target/checkpoint-1.80.0-SNAPSHOT-exec.jar

```

or you can run it from the maven repository:

```bash
java -jar ~/.m2/repository/org/talend/sdk/component/sample/feature/checkpoint/1.80.0-SNAPSHOT/checkpoint-1.80.0-SNAPSHOT.jar
```

For later usage, will use the variable `$RUNCMD` as the way you may choose.

**IMPORTANT**: If you're using jdk17, please don't forget to add the `--add-opens` option to the command line (see profile _jdk9_ in master pom at the repository's root) or use instead jdk11.

## How to use

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
                             (default: org.talend.sdk.component.sample.feature:checkpoint:jar:1.80.0-SNAPSHOT)
  --jar=<File>               Full path to jar of the component to use for the checkpoint.
  --log                      Log when a checkpoint is serialized.
  --mapper=<String>          Mapper to use for the checkpoint.
                             (default: input)
  --no-re-use                Re-use checkpoint file, it will match latest checkpoint available, otherwise will create numbered checkpoint.
  --work-dir=<File>          Where to create temporary checkpoint files.
                             (default: ./)
```

### Plugin artifact

There are two ways to run the checkpoint runner with a specific plugin artifact:

- Using the `--gav` option to specify the GAV of the plugin artifact.

    Syntax:  `groupId:artifactId:version[:packaging[:classifier]]`  

- Using the `--jar` option to specify the path to the plugin artifact.

    Syntax:  `/path/to/plugin.jar`

IMPORTANT: You cannot use both options at the same time.

### Run checkpoint with default behavior

`java -jar target/checkpoint-1.80.0-SNAPSHOT.jar checkpoint`

```bash 
[INFO]  Manager is using plugin: checkpoint from GAV org.talend.sdk.component.sample.feature:checkpoint:jar:1.80.0-SNAPSHOT.
[WARN]  ./configuration.json (No such file or directory)
[WARN]  ./checkpoint.json (No such file or directory)
[INFO]  configuration: {}
[DATA]  {"data":"0"}
[DATA]  {"data":"1"}
[DATA]  {"data":"2"}
[DATA]  {"data":"3"}
[DATA]  {"data":"4"}
[DATA]  {"data":"5"}
[DATA]  {"data":"6"}
[DATA]  {"data":"7"}
[DATA]  {"data":"8"}
[DATA]  {"data":"9"}
[DATA]  {"data":"10"}
[DATA]  {"data":"11"}
[DATA]  {"data":"12"}
[DATA]  {"data":"13"}
[DATA]  {"data":"14"}
[DATA]  {"data":"15"}
[DATA]  {"data":"16"}
[DATA]  {"data":"17"}
[DATA]  {"data":"18"}
[DATA]  {"data":"19"}
[INFO]  finished.
```

### Run checkpoint with a configuraton

and turn log verbose on:

`% java -jar target/checkpoint-1.80.0-SNAPSHOT.jar checkpoint --configuration=configuration-default.json --log`

```bash
[INFO]  Manager is using plugin: checkpoint from GAV org.talend.sdk.component.sample.feature:checkpoint:jar:1.80.0-SNAPSHOT.
[WARN]  ./checkpoint.json (No such file or directory)
[INFO]  configuration: {configuration.dataset.maxRecords=10}
[INFO]  Checkpoint 1 reached with {"$checkpoint":{"sinceId":0,"status":"running","__version":2}}.
[DATA]  {"data":"0"}
[DATA]  {"data":"1"}
[INFO]  Checkpoint 2 reached with {"$checkpoint":{"sinceId":2,"status":"running","__version":2}}.
[DATA]  {"data":"2"}
[DATA]  {"data":"3"}
[INFO]  Checkpoint 3 reached with {"$checkpoint":{"sinceId":4,"status":"running","__version":2}}.
[DATA]  {"data":"4"}
[DATA]  {"data":"5"}
[INFO]  Checkpoint 4 reached with {"$checkpoint":{"sinceId":6,"status":"running","__version":2}}.
[DATA]  {"data":"6"}
[DATA]  {"data":"7"}
[INFO]  Checkpoint 5 reached with {"$checkpoint":{"sinceId":8,"status":"running","__version":2}}.
[DATA]  {"data":"8"}
[DATA]  {"data":"9"}
[INFO]  Checkpoint 6 reached with {"$checkpoint":{"sinceId":9,"status":"finished","__version":2}}.
[INFO]  finished.
```
Check generated checkpoint file `checkpoint.json`:

```bash
% cat checkpoint.json | jq

{
  "$checkpoint": {
    "sinceId": 9,
    "status": "finished",
    "__version": 2
  }
}
```
Note: `$checkpoint` is a reserved property name, it will be used to store the checkpoint object, as the `__version` property.

### Run with framework feature disabled

and turn log verbose on:

`% java -jar target/checkpoint-1.80.0-SNAPSHOT.jar checkpoint --disable-feature --log`

```bash
[INFO]  Manager is using plugin: checkpoint from GAV org.talend.sdk.component.sample.feature:checkpoint:jar:1.80.0-SNAPSHOT.
[WARN]  ./configuration.json (No such file or directory)
[WARN]  ./checkpoint.json (No such file or directory)
[INFO]  configuration: {}
[DATA]  {"data":"0"}
[DATA]  {"data":"1"}
[DATA]  {"data":"2"}
[DATA]  {"data":"3"}
[DATA]  {"data":"4"}
[DATA]  {"data":"5"}
[DATA]  {"data":"6"}
[DATA]  {"data":"7"}
[DATA]  {"data":"8"}
[DATA]  {"data":"9"}
[DATA]  {"data":"10"}
[DATA]  {"data":"11"}
[DATA]  {"data":"12"}
[DATA]  {"data":"13"}
[DATA]  {"data":"14"}
[DATA]  {"data":"15"}
[DATA]  {"data":"16"}
[DATA]  {"data":"17"}
[DATA]  {"data":"18"}
[DATA]  {"data":"19"}
[INFO]  finished.
```
No checkpoint file is generated.

## Configuration sample

You'll find those files in `resources` folder or in artifact archive.

### Sample configuration `configuration-default.json`
```json
{
  "configuration": {
    "dataset": {
      "maxRecords": 10
    }
  }
}
```
This configuration will limit the number of records to 10.

### Sample checkpoint configuration `checkpoint-default.json`
```json
{
  "$checkpoint": {
    "sinceId": 1,
    "status": "running",
    "__version": 2
  }
}
```
This checkpoint configuration will start the checkpoint from record Id 1.
When the runner is finished, it will be updated with the last record Id processed and its status will be set as _"finished"_.
The `__version` property is used to identify the version of the checkpoint configuration object. 

## Testing migration

Provide a `checkpoint-v1.json` file with the following content:

```json
{
  "$checkpoint": {
    "lastId": 10,
    "status": "running",
    "__version": 1
  }
}
```
Notice the `lastId` field instead of `sinceId`.
