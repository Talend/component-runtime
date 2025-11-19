# Component Runtime :: Sample Feature :: DynamicDependency

## Table of Contents
- [Overview](#overview)
- [Usage](#usage)
    - [How to build the connector plugin](#how-to-build-the-sample-connector-plugin)
    - [How to use](#how-to-use)


## Overview
This is a test TCK connector plugin to test and validate the DynamicDependency input feature.

This project contains 4 test connectors:
### DynamicDependency with Dataset 
The service of this connector use a dataset as parameter. 

### DynamicDependency with Datastore
The service of this connector use a datastore as parameter.

### DynamicDependency with Dataprep run annotation 
The service of this connector use a new annotation @DynamicDependencySupported.

### DynamicDependency with @DynamicDependenciesConfiguration
The service of this connector use a config which using @DynamicDependenciesConfiguration.

## Usage
### How to build the sample connector plugin
Checkout the code from the repository and build the project using `mvn clean install`  
Alternatively build the feature module using `mvn install -am -pl :dynamicdependencies`

### How to use
- Deploy the connector into Studio:
java -jar dynamic-dependencies-with-dataset-1.86.0-SNAPSHOT.car studio-deploy --location c:\Talend-Studio-20251010_0827-V8.0.2SNAPSHOT 

- Use the connector in the job. 
- Click "Guess schema" of the connector. 
- Add others you want to use in the job, then run the job. 

