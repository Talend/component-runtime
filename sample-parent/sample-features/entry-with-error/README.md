# Component Runtime :: Sample Feature :: Entry with error

## Table of Contents
- [Overview](#overview)
- [Usage](#usage)
  - [How to build the sample connector plugin](#how-to-build-the-sample-connector-plugin)
  - [How to run](#how-to-run)
  - [How to use](#how-to-use)
- [Plugin artifact](#plugin-artifact)
- [Execution examples](#execution-examples)
  - [Run supporterror connector with default behavior](#run-checkpoint-connector-with-default-behavior)
  - [Run with framework feature disabled](#run-with-framework-feature-disabled)


## Overview
This is a simple TCK connector plugin to test and validate the feature about Improve Record API to add error info for invalid entries.

This project contains two things:
 1. A sample connector plugin that implements the supporterror feature.
 2. A simple Cli runner that can be used to run the connector plugin and test the supporterror feature.


## Usage
### How to build the sample connector plugin
Checkout the code from the repository and build the project using `mvn clean install`  
Alternatively build the feature module using `mvn install -am -pl :supporterror`

### How to run
To run the connector, you need exec the generated artifact `org.talend.sdk.component.sample.feature:supporterror`.  
* You can run it directly from `target` folder or repository folder
  * `java -jar target/supporterror-1.81.0-SNAPSHOT.jar`
* or you can run it from the maven repository
  * `java -jar ~/.m2/repository/org/talend/sdk/component/sample/feature/supporterror/1.81.0-SNAPSHOT/supporterror-1.81.0-SNAPSHOT.jar`

For later usage, will use the variable `$RUNCMD` as the way you may choose.  
⚠️ If you're using jdk17, don't forget to add the `--add-opens` option to the command line
(see profile _jdk9_ in master pom at the repository's root) or use instead jdk11.

### How to use
Run supporterror with the option "-s" for execution:

```bash
$ $RUNCMD  supporterror

Usage:  supporterror [-s]

Options:
  --s    use supporterror or not.
 
```

## Plugin artifact
There are two ways to run the supporterror runner with a specific plugin artifact:
- Using the `--gav` option to specify the GAV of the plugin artifact.  
    Syntax:  `groupId:artifactId:version[:packaging[:classifier]]`  

- Using the `--jar` option to specify the path to the plugin artifact.  
    Syntax:  `/path/to/plugin.jar`

⚠️ You cannot use both options at the same time.

## Execution examples
### Run supporterror connector with default behavior
`java -jar target/supporterror-1.81.0-SNAPSHOT.jar supporterror -s`

```bash 
[INFO]  Manager is using plugin: supporterror from GAV org.talend.sdk.component.sample.feature:supporterror:jar:1.81.0-SNAPSHOT.
[INFO]  support true
[INFO]  create input now.
[INFO]  getting the record.
[INFO]  Record isValid = false
[INFO]  Record 'name': example connector
[INFO]  ERROR: date is null
[INFO]  ERROR: wrong int value
[INFO]  finished.
```

### Run with framework feature disabled
In this example we turn off the supporterror capability.  
`% java -jar target/supporterror-1.81.0-SNAPSHOT.jar supporterror`

```bash
[INFO]  Manager is using plugin: supporterror from GAV org.talend.sdk.component.sample.feature:supporterror:jar:1.81.0-SNAPSHOT.
[INFO]  support false
[INFO]  create input now.
[INFO]  getting the record.
[ERROR] date is null
```
We can see that no record is returned because throw errors.
