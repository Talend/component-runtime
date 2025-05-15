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
This is a simple TCK connector plugin to test and validate the feature about improve record API to add error info for
invalid entries.

This project contains two things:
 1. A sample connector plugin that implements the entry-with-error feature.
 2. A simple Cli runner that can be used to run the connector plugin and test the entry-with-error feature.


## Usage
### How to build the sample connector plugin
Checkout the code from the repository and build the project using `mvn clean install`  
Alternatively build the feature module using `mvn install -am -pl :entrywitherror`

### How to run
To run the connector, you need exec the generated artifact `org.talend.sdk.component.sample.feature:entrywitherror`.  
* You can run it directly from `target` folder or repository folder
  * `java -jar target/entrywitherror-1.81.0-SNAPSHOT.jar `
* or you can run it from the maven repository
  * `java -jar  ~/.m2/repository/org/talend/sdk/component/sample/feature/entrywitherror/1.81.0-SNAPSHOT/entrywitherror-1.81.0-SNAPSHOT.jar`

For later usage, will use the variable `$RUNCMD` as the way you may choose.  
⚠️ If you're using jdk17, don't forget to add the `--add-opens` option to the command line
(see profile _jdk9_ in master pom at the repository's root) or use instead jdk11.

### How to use
Run `entry-with-error` command:

```bash
$ $RUNCMD  entry-with-error

Usage: entry-with-error [options]

Options: 
  --family=<String>              default: sampleRecordWithEntriesInError
  --fields-in-error=<String>     default: age,date
  --gav=<String>                 default: org.talend.sdk.component.sample.feature:entrywitherror:jar:1.81.0-SNAPSHOT
  --gen-nb-records=<int>         default: 10
  --how-many-errors=<int>        default: 0
  --jar=<File>
  --mapper=<String>              default: RecordWithEntriesInErrorEmitter
  --support-entry-with-error
  --use-avro-impl
```

## Plugin artifact
There are two ways to run the entry-with-error runner with a specific plugin artifact:
- Using the `--gav` option to specify the GAV of the plugin artifact.  
    Syntax:  `groupId:artifactId:version[:packaging[:classifier]]`  

- Using the `--jar` option to specify the path to the plugin artifact.  
    Syntax:  `/path/to/plugin.jar`

⚠️ You cannot use both options at the same time.

## Execution examples
### Run entry-with-error sample with default behavior
To execute with legacy behavior, just don't add the `--support-entry-with-error` option. The command 
`$RUNCMD entry-with-error --gen-nb-records=4` will use default parameters values. It will generate 10 records with 0 errors.

```bash 
Parameters:
	gav: org.talend.sdk.component.sample.feature:entrywitherror:jar:1.81.0-SNAPSHOT
	support-entry-with-error: false
	use-avro-impl: false
	how-many-errors: 0
	gen-nb-records: 4
	fields-in-error: age,date
	jar: null
	family: sampleRecordWithEntriesInError
	mapper: RecordWithEntriesInErrorEmitter
[INFO]  Manager is using plugin: entrywitherror from GAV org.talend.sdk.component.sample.feature:entrywitherror:jar:1.81.0-SNAPSHOT.
[INFO]  configuration: {configuration.fieldsInError[0]=age, configuration.fieldsInError[1]=date, configuration.nbRecords=4, configuration.howManyErrors=0}
-----------------------------------------------------
Record (RecordImpl) no 1 is valid ? yes
	Name: name 1
	Date: 2025-04-02T15:30Z[UTC]
	Age: 51
-----------------------------------------------------
Record (RecordImpl) no 2 is valid ? yes
	Name: name 2
	Date: 2025-04-03T15:30Z[UTC]
	Age: 52
-----------------------------------------------------
Record (RecordImpl) no 3 is valid ? yes
	Name: name 3
	Date: 2025-04-04T15:30Z[UTC]
	Age: 53
-----------------------------------------------------
Record (RecordImpl) no 4 is valid ? yes
	Name: name 4
	Date: 2025-04-05T15:30Z[UTC]
	Age: 54
-----------------------------------------------------
[INFO]  finished.

```
The following command do the same, but an error is generated on the `age` attribute of the first record. So, an exception is raised:
```bash
$RUNCMD entry-with-error --gen-nb-records=4 --how-many-errors=1 --fields-in-error=age

Parameters:
        gav: org.talend.sdk.component.sample.feature:entrywitherror:jar:1.81.0-SNAPSHOT
        support-entry-with-error: false                                                                                                                                                                                               use-avro-impl: false
        how-many-errors: 1
        gen-nb-records: 4                                                                                                                                                                                                             gfields-in-error: age
        jar: null
        family: sampleRecordWithEntriesInError
        mapper: RecordWithEntriesInErrorEmitter
[INFO]  Manager is using plugin: entrywitherror from GAV org.talend.sdk.component.sample.feature:entrywitherror:jar:1.81.0-SNAPSHOT.
[INFO]  configuration: {configuration.fieldsInError[0]=age, configuration.nbRecords=4, configuration.howManyErrors=1}
[EXCEPTION] Entry 'age' of type INT is not compatible with given value of type 'java.lang.String': '-78'.
java.lang.IllegalArgumentException: Entry 'age' of type INT is not compatible with given value of type 'java.lang.String': '-78'.
```

### Run with entry-wit-error enable
In this example we turn on the `entry-with-error` capability with the option `--support-entry-with-error`.

```bash
$RUNCMD entry-with-error --gen-nb-records=4 --how-many-errors=1 --fields-in-error=age --support-entry-with-error

Parameters:
        gav: org.talend.sdk.component.sample.feature:entrywitherror:jar:1.81.0-SNAPSHOT                                                                                                                                               support-entry-with-error: true
        use-avro-impl: false
        how-many-errors: 1                                                                                                                                                                                                            gen-nb-records: 4
        gfields-in-error: age
        jar: null
        family: sampleRecordWithEntriesInError
        mapper: RecordWithEntriesInErrorEmitter
[INFO]  Manager is using plugin: entrywitherror from GAV org.talend.sdk.component.sample.feature:entrywitherror:jar:1.81.0-SNAPSHOT.
[INFO]  configuration: {configuration.fieldsInError[0]=age, configuration.nbRecords=4, configuration.howManyErrors=1}
-----------------------------------------------------
Record (RecordImpl) no 1 is valid ? no
        Name: name 1
        Date: 2025-04-02T15:30Z[UTC]
        Age is on error:
                Value (should be null): null
                Message:Entry 'age' of type INT is not compatible with given value of type 'java.lang.String': '-78'.
                Fallback value: -78
-----------------------------------------------------
Record (RecordImpl) no 2 is valid ? yes
        Name: name 2
        Date: 2025-04-03T15:30Z[UTC]
        Age: 52
-----------------------------------------------------
Record (RecordImpl) no 3 is valid ? yes
        Name: name 3
        Date: 2025-04-04T15:30Z[UTC]
        Age: 53
-----------------------------------------------------
Record (RecordImpl) no 4 is valid ? yes
        Name: name 4
        Date: 2025-04-05T15:30Z[UTC]
        Age: 54
-----------------------------------------------------
[INFO]  finished.
```
The first record is on error, bu, bo exception is raise and following records are well generated.
