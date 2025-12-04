# Dynamic Dependencies Module Documentation

## Overview

The `dynamic-dependencies` module provides several TCK connector plugins designed to validate the `@DynamicDependencies` feature in the Talend Component Kit. It contains 5 test connectors organized into two categories: dependencies checking and SPI (_Service Provider Interface_) checking.

## Dependency Checking Connectors
All `dynamic-dependencies-with-*` provide a TCK connector that will get a list of dynamic dependencies (_defined with maven GAV_) and a list of dynamic loaded TCK connectors. They all rely on `dynamic-dependencies-common` to generate same kind of records.

Each `dynamic-dependencies-with-*` module proposes its own implementation of a `@DynamicSchema` service, that serves a list of GAV, each expect a different kind of TCK configuration as parameter:
- `dynamic-dependencies-with-datastore` has a `@DynamicSchema` that expect a `@Datastore`
- `dynamic-dependencies-with-dataset` has a `@DynamicSchema` that expect a `@Dataset`
- `dynamic-dependencies-with-dynamicDependenciesConfiguration` has a `@DynamicSchema` that expect a `@DynamicDependenciesConfiguration`
- `dynamic-dependencies-with-dataPrepRunAnnotation` has a `@DynamicSchema` that expect a `@DynamicDependencySupported`. this annotation is cuurently provided and used in connectors-se/dataprep, and that should be removed afterward

### Generated Records for Dependency Checkers

All dependency checking connectors generate records through `AbstractDynamicDependenciesService.loadIterator()`.
Each record contains:

- `maven`: GAV coordinate of the dependency (_this is set by the user in the connectors' configuration_)
- `class`: Class name being loaded (_this is set by the user in the connectors' configuration_)
- `is_loaded`: Whether the class was successfully loaded
- `connector_classloader`: Classloader ID of the connector
- `clazz_classloader`: Classloader ID of the loaded class
- `from_location`: JAR location where the class was found
- `is_tck_container`: Whether this is a TCK container
- `first_record`: First record from additional connectors (if any). If a record is retrieved, it means the connectors has been well loaded.
- `root_repository`: Maven repository path (if environment info enabled)
- `runtime_classpath`: Runtime classpath (if environment info enabled)
- `working_directory`: Current working directory (if environment info enabled)

## SPI Checking Connector
The `jvm` provides the `Service Provider Interface` (_SPI_) mechanism. It allows to load implementations of interfaces without explicitly specifying them. The classloader search for all resource files in `META-INF/services/` that have the same name as the full qualified name of the interface you want some implementation. See more information in [ServiceLoader javadoc](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ServiceLoader.html). The goal is to check that SPI implementations can be loaded in several case, for instance when the implementation is provided by a dynamic dependency or by te runtime, etc... 

### The dynamic-dependencies-with-spi module
It provides a connector that has `classloader-test-library` as dependency. This library contains several interfaces that have their implementations provided by `SPI` mechanism, loading them from `service-provided-from-*` modules. The connector will also try to load some resources from those modules:

- `service-provider-from-dependency` is a direct dependency of `dynamic-dependencies-with-spi`
  - It proposes those resource files to load:
    - `FROM_DEPENDENCY/resource.properties`
    - `MULTIPLE_RESOURCE/common.properties`
- `service-provider-from-dynamic-dependency` is a dynamic dependency returned by `DynamicDependenciesWithSPIService#getDynamicDependencies` service that is annotated with `@DynamicDependencies`
   - It proposes those resource files to load: 
     - `FROM_DYNAMIC_DEPENDENCY/resource.properties`
     - `MULTIPLE_RESOURCE/common.properties`
- `service-provider-from-external-dependency` is a library that should be loaded and provided by the runtime, for instance in a studio job, it should be loaded by a `tLibraryLoad`
    - It proposes those resource files to load: 
      - `FROM_EXTERNAL_DEPENDENCY/resource.properties`
      - `MULTIPLE_RESOURCE/common.properties`

The connector in `dynamic-dependencies-with-spi` tries to load `FROM_xxxx/resource.properties` resources with `classloadOfTheDependency.getResourceAsStream(resource)`, and, it tries to load `MULTIPLE_RESOURCE/common.properties` with `classloadOfTheDependency.getResources("MULTIPLE_RESOURCE/common.properties")`.

### Generated Records for SPI Checker

The SPI connector generates 9 records total (3 from each SPI type) through `DynamicDependenciesWithSPIService.getRecordIterator()` [11](#3-10) . Each record contains:

- `value`: Value from the SPI implementation
    - Records 0-2: `ServiceProviderFromDependency_1`, `_2`, `_3`
    - Records 3-5: `ServiceProviderFromDynamicDependency_1`, `_2`, `_3`
    - Records 6-8: `ServiceProviderFromExternalDependency_1`, `_2`, `_3`
- `contentFromResourceDependency`: Content from `FROM_DEPENDENCY/resource.properties`
- `contentFromResourceDynamicDependency`: Content from `FROM_DYNAMIC_DEPENDENCY/resource.properties`
- `contentFromResourceExternalDependency`: Content from `FROM_EXTERNAL_DEPENDENCY/resource.properties`
- `contentFromMultipleResources`: Combined content from `MULTIPLE_RESOURCE/common.properties`

## Module Structure

### Core Module
- **dynamic-dependencies-common**: Shared library containing:
    - `AbstractDynamicDependenciesService`: Base service for dependency loading and record generation [12](#3-11)
    - Configuration interfaces (`DynamicDependencyConfig`, `Dependency`, `Connector`)

### SPI Testing Modules
- **classloader-test-library**: Library providing SPI consumers for testing [13](#3-12)
- **service-provider-from-dependency**: SPI provider as standard dependency [14](#3-13)
- **service-provider-from-dynamic-dependency**: SPI provider loaded dynamically [15](#3-14)
- **service-provider-from-external-dependency**: SPI provider from runtime [16](#3-15)

## Usage

### Building
```bash
mvn clean install -am -pl :dynamicdependencies
```

### Testing in Studio
1. Deploy any connector CAR file to Studio:
```bash
java -jar dynamic-dependencies-with-dataset-1.88.0-SNAPSHOT.car studio-deploy --location <studio-path>
```
2. Create a job with the connector
3. Click "Guess schema" to trigger dynamic dependency loading
4. Run the job to see generated diagnostic records [17](#3-16)

## Using Connectors in Talend Studio Jobs

This section provides step-by-step instructions for testing each dynamic dependencies connector in Talend Studio to validate all use cases.

### Prerequisites

1. Build all connectors:
```bash
mvn clean install -am -pl :dynamicdependencies
```

2. Deploy each connector CAR file to Studio:
```bash
java -jar dynamic-dependencies-with-dataset-1.88.0-SNAPSHOT.car studio-deploy --location <studio-path>
java -jar dynamic-dependencies-with-datastore-1.88.0-SNAPSHOT.car studio-deploy --location <studio-path>
java -jar dynamic-dependencies-with-dynamicDependenciesConfiguration-1.88.0-SNAPSHOT.car studio-deploy --location <studio-path>
java -jar dynamic-dependencies-with-dataprepRunAnnotation-1.88.0-SNAPSHOT.car studio-deploy --location <studio-path>
java -jar dynamic-dependencies-with-spi-1.88.0-SNAPSHOT.car studio-deploy --location <studio-path>
```

### Testing Dependency Checking Connectors

#### Step 1: Create Test Job
1. Create a new Standard Job in Studio
2. Add the connector to test (e.g., `DynamicDependenciesWithDataset` > `Input`)
3. Add a `tLogRow` component to view output
4. Connect the connector to `tLogRow`

#### Step 2: Configure Dependencies
For each dependency checking connector, configure the test dependency:

1. Open the component configuration
2. In the **Dependencies** table, add:
    - **Group ID**: `org.apache.commons`
    - **Artifact ID**: `commons-numbers-primes`
    - **Version**: `1.2`
    - **Class**: `org.apache.commons.numbers.primes.SmallPrimes`

3. Enable **Environment Information** to see additional diagnostic data

#### Step 3: Validate Dynamic Loading
1. Click **Guess Schema** on the connector
    - This triggers the `@DynamicDependencies` service
    - Validates that the dependency is loaded dynamically
2. Check Studio logs for dependency loading messages
3. Run the job to generate diagnostic records

#### Step 4: Verify Output Records
Check the `tLogRow` output for these fields:
- `maven`: Should show `org.apache.commons:commons-numbers-primes:1.2`
- `class`: Should show `org.apache.commons.numbers.primes.SmallPrimes`
- `is_loaded`: Should be `true`
- `from_location`: Path to the downloaded JAR
- Additional fields if environment info is enabled

### Testing SPI Connector

#### Step 1: Create SPI Test Job
1. Create a new Standard Job
2. Add `DynamicDependenciesWithSPI` > `Input` component
3. Add `tLogRow` component
4. Connect them

#### Step 2: Validate SPI Loading
1. Click **Guess Schema** to trigger dynamic dependency loading
2. The connector will:
    - Load `service-provider-from-dynamic-dependency` dynamically
    - Discover SPI implementations from all three scopes
3. Run the job

#### Step 3: Verify SPI Output
Check for 9 records with this pattern:

| Record | value Field | Expected Content |
|--------|-------------|------------------|
| 0-2 | ServiceProviderFromDependency_1/2/3 | From standard dependency |
| 3-5 | ServiceProviderFromDynamicDependency_1/2/3 | From dynamic dependency |
| 6-8 | ServiceProviderFromExternalDependency_1/2/3 | From external dependency |

Each record should also contain:
- `contentFromResourceDependency`: Message from standard dependency resource
- `contentFromResourceDynamicDependency`: Message from dynamic dependency resource
- `contentFromResourceExternalDependency`: Message from external dependency resource
- `contentFromMultipleResources`: Combined content from all dependencies

### Validation Checklist

For each connector, verify:

- [ ] **Guess Schema** triggers without errors
- [ ] Dynamic dependencies are downloaded and loaded
- [ ] Generated records contain expected fields
- [ ] Class loading information is correct
- [ ] Resource loading works (for SPI connector)
- [ ] No classloader conflicts occur

### Common Issues and Solutions

1. **Dependency not found**: Check Maven repository configuration in Studio
2. **Class loading fails**: Verify the GAV coordinates and class name
3. **SPI not discovered**: Ensure all service provider modules are deployed
4. **Resource loading fails**: Check that resource files exist in the correct paths

### Advanced Testing

To test multiple dependencies or connectors:
1. Add multiple entries in the Dependencies/Connectors tables
2. Use different Maven coordinates
3. Verify that all dependencies are loaded and reported in the output

## Notes

- The `@DynamicDependencySupported` annotation in the dataprep module is temporary and should be removed after testing [18](#3-17)
- All modules use `org.talend.sdk.samplefeature.dynamicdependencies` as groupId to avoid automatic exclusion
- Test modules use `commons-numbers-primes` as a simulated dynamic dependency [19](#3-18)
- The SPI connector uses a custom classloader customizer to ensure proper SPI loading from different dependency scopes [20](#3-19)