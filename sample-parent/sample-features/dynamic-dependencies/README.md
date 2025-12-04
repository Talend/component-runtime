# Dynamic Dependencies Module Documentation

## Overview

The `dynamic-dependencies` module is a test TCK connector plugin designed to validate the `@DynamicDependencies` feature in the Talend Component Kit [1](#3-0) . It contains 5 test connectors organized into two categories: dependency checking and SPI checking.

## Dependency Checking Connectors

### 1. DynamicDependenciesWithDataset
- **Family**: `DynamicDependenciesWithDataset`
- **Parameter**: Uses `@Dataset` configuration
- **What it checks**: Validates dynamic dependency loading when triggered from a dataset configuration
- **Record generation**: Extends `AbstractDynamicDependenciesService` to generate diagnostic records [2](#3-1)

### 2. DynamicDependenciesWithDatastore
- **Family**: `DynamicDependenciesWithDatastore`
- **Parameter**: Uses `@Datastore` configuration
- **What it checks**: Tests dynamic dependency resolution from datastore parameters
- **Record generation**: Uses `AbstractDynamicDependenciesService` through its service layer [3](#3-2)

### 3. DynamicDependenciesWithDynamicDependenciesConfiguration
- **Family**: `DynamicDependenciesWithDynamicDependenciesConfiguration`
- **Parameter**: Uses `@DynamicDependenciesConfiguration` annotated sub-config
- **What it checks**: Validates the `@DynamicDependenciesConfiguration` annotation behavior
- **Record generation**: Extends `AbstractDynamicDependenciesService` for record output [4](#3-3)

### 4. DynamicDependenciesWithDataprepRunAnnotation
- **Family**: `DynamicDependenciesWithDataprepRunAnnotation`
- **Parameter**: Uses custom `@DynamicDependencySupported` annotation
- **What it checks**: Tests compatibility with the dataprep run annotation (temporary for testing)
- **Record generation**: Extends `AbstractDynamicDependenciesService` for diagnostic output [5](#3-4)

### Generated Records for Dependency Checkers

All dependency checking connectors generate records through `AbstractDynamicDependenciesService.loadIterator()` [6](#3-5) . Each record contains:

- `maven`: GAV coordinate of the dependency
- `class`: Class name being loaded
- `is_loaded`: Whether the class was successfully loaded
- `connector_classloader`: Classloader ID of the connector
- `clazz_classloader`: Classloader ID of the loaded class
- `from_location`: JAR location where the class was found
- `is_tck_container`: Whether this is a TCK container
- `first_record`: First record from additional connectors (if any)
- `root_repository`: Maven repository path (if environment info enabled)
- `runtime_classpath`: Runtime classpath (if environment info enabled)
- `working_directory`: Current working directory (if environment info enabled) [7](#3-6)

## SPI Checking Connector

### DynamicDependenciesWithSPI
- **Family**: `DynamicDependenciesWithSPI` [8](#3-7)
- **Parameter**: Uses `@Dataset` configuration
- **What it checks**: Validates Java SPI loading from different dependency scopes:
    - Standard dependencies (via `DependencySPIConsumer`)
    - Dynamic dependencies (via `DynamicDependencySPIConsumer`)
    - External/runtime dependencies (via `ExternalDependencySPIConsumer`)
- **Dynamic dependency**: Returns `org.talend.sdk.samplefeature.dynamicdependencies:service-provider-from-dynamic-dependency:{version}` [9](#3-8)

### Resource Files Loaded

The SPI connector loads the following resource files and checks their content [10](#3-9) :

1. **FROM_DEPENDENCY/resource.properties**
    - Loaded from standard dependency
    - Expected property: `ServiceProviderFromDependency.message`

2. **FROM_DYNAMIC_DEPENDENCY/resource.properties**
    - Loaded from dynamic dependency
    - Expected property: `ServiceProviderFromDynamicDependency.message`

3. **FROM_EXTERNAL_DEPENDENCY/resource.properties**
    - Loaded from external/runtime dependency
    - Expected property: `ServiceProviderFromExternalDependency.message`

4. **MULTIPLE_RESOURCE/common.properties**
    - Loaded from all dependencies using `getResources()`
    - Content is combined from all sources (filtered to remove comments and empty lines)

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

### Citations

**File:** sample-parent/sample-features/dynamic-dependencies/README.md (L11-12)
```markdown
This is a test TCK connector plugin to test and validate the DynamicDependency input feature.

```

**File:** sample-parent/sample-features/dynamic-dependencies/README.md (L31-37)
```markdown
### How to use
- Deploy the connector into Studio:
java -jar dynamic-dependencies-with-dataset-x.y.z-SNAPSHOT.car studio-deploy --location c:\Talend-Studio-20251010_0827-V8.0.2SNAPSHOT 

- Use the connector in the job. 
- Click "Guess schema" of the connector. 
```

**File:** sample-parent/sample-features/dynamic-dependencies/dynamic-dependencies-with-dataset/src/main/java/org/talend/sdk/component/sample/feature/dynamicdependencies/withdataset/config/Config.java (L43-66)
```java
public class Config implements DynamicDependencyConfig, Serializable {

    @Option
    @Documentation("The dataset configuration.")
    private Dataset dse = new Dataset();

    @Option
    @Documentation("If enable throw an exception for any error, if not just log the error.")
    private boolean dieOnError = false;

    @Option
    @Documentation("More environment information.")
    private boolean environmentInformation = false;

    @Override
    public List<Dependency> getDependencies() {
        return new ArrayList<>(this.getDse().getDependencies());
    }

    public List<Connector> getConnectors() {
        return new ArrayList<>(this.getDse().getConnectors());
    }

}
```

**File:** sample-parent/sample-features/dynamic-dependencies/dynamic-dependencies-with-datastore/src/main/java/org/talend/sdk/component/sample/feature/dynamicdependencies/withdatastore/input/DynamicDependenciesWithDatastoreInput.java (L50-53)
```java
    @PostConstruct
    public void init() {
        this.recordIterator = this.service.loadIterator(this.config);
    }
```

**File:** sample-parent/sample-features/dynamic-dependencies/dynamic-dependencies-with-dynamicDependenciesConfiguration/src/main/java/org/talend/sdk/component/sample/feature/dynamicdependencies/withDynamicDependenciesConfiguration/config/SubConfig.java (L31-37)
```java
@Data
@DynamicDependenciesConfiguration
@GridLayout({
        @GridLayout.Row({ "dependencies" }),
        @GridLayout.Row({ "connectors" })
})
public class SubConfig implements Serializable {
```

**File:** sample-parent/sample-features/dynamic-dependencies/dynamic-dependencies-with-spi/src/main/java/org/talend/sdk/component/sample/feature/dynamicdependencies/withspi/package-info.java (L16-20)
```java
@Components(
        family = "DynamicDependenciesWithSPI",
        categories = "sample")
@Icon(value = Icon.IconType.CUSTOM, custom = "icon")
package org.talend.sdk.component.sample.feature.dynamicdependencies.withspi;
```

**File:** sample-parent/sample-features/dynamic-dependencies/dynamic-dependencies-with-spi/src/main/java/org/talend/sdk/component/sample/feature/dynamicdependencies/withspi/service/DynamicDependenciesWithSPIService.java (L57-62)
```java
    @DynamicDependencies
    public List<String> getDynamicDependencies(@Option("theDataset") final Dataset dataset) {
        String dep = "org.talend.sdk.samplefeature.dynamicdependencies:service-provider-from-dynamic-dependency:"
                + loadVersion();
        return Collections.singletonList(dep);
    }
```

**File:** sample-parent/sample-features/dynamic-dependencies/dynamic-dependencies-with-spi/src/main/java/org/talend/sdk/component/sample/feature/dynamicdependencies/withspi/service/DynamicDependenciesWithSPIService.java (L75-145)
```java
    public Iterator<Record> getRecordIterator() {
        String contentFromResourceDependency = loadAPropertyFromResource("FROM_DEPENDENCY/resource.properties",
                "ServiceProviderFromDependency.message");

        String contentFromResourceDynamicDependency = loadAPropertyFromResource(
                "FROM_DYNAMIC_DEPENDENCY/resource.properties",
                "ServiceProviderFromDynamicDependency.message");

        String contentFromResourceExternalDependency = loadAPropertyFromResource(
                "FROM_EXTERNAL_DEPENDENCY/resource.properties",
                "ServiceProviderFromExternalDependency.message");

        String contentFromMultipleResources;
        try {
            boolean isFirst = true;
            Enumeration<URL> resources = DynamicDependenciesWithSPIService.class.getClassLoader()
                    .getResources("MULTIPLE_RESOURCE/common.properties");

            StringBuilder stringBuilder = new StringBuilder();
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();

                try (InputStream is = url.openStream()) {
                    String content = filterComments(is);
                    stringBuilder.append(content);
                    if (!isFirst) {
                        stringBuilder.append(System.lineSeparator());
                    }
                    isFirst = false;
                }
            }
            contentFromMultipleResources = stringBuilder.toString();
        } catch (IOException e) {
            throw new ComponentException("Can't retrieve multiple resources at once.", e);
        }

        DependencySPIConsumer<Record> dependencySPIConsumer = new DependencySPIConsumer<>(true);
        List<Record> recordsFromDependencySPI = dependencySPIConsumer
                .transform(s -> recordBuilderFactory.newRecordBuilder()
                        .withString("value", s)
                        .withString("contentFromResourceDependency", contentFromResourceDependency)
                        .withString("contentFromResourceDynamicDependency", contentFromResourceDynamicDependency)
                        .withString("contentFromResourceExternalDependency", contentFromResourceExternalDependency)
                        .withString("contentFromMultipleResources", contentFromMultipleResources)
                        .build());

        DynamicDependencySPIConsumer<Record> dynamicDependencySPIConsumer = new DynamicDependencySPIConsumer<>(true);
        List<Record> recordsFromDynamicDependencySPI = dynamicDependencySPIConsumer
                .transform(s -> recordBuilderFactory.newRecordBuilder()
                        .withString("value", s)
                        .withString("contentFromResourceDependency", contentFromResourceDependency)
                        .withString("contentFromResourceDynamicDependency", contentFromResourceDynamicDependency)
                        .withString("contentFromMultipleResources", contentFromMultipleResources)
                        .build());

        ExternalDependencySPIConsumer<Record> externalDependencySPI = new ExternalDependencySPIConsumer<>(true);
        List<Record> recordsFromExternalSPI = externalDependencySPI
                .transform(s -> recordBuilderFactory.newRecordBuilder()
                        .withString("value", s)
                        .withString("contentFromResourceDependency", contentFromResourceDependency)
                        .withString("contentFromResourceDynamicDependency", contentFromResourceDynamicDependency)
                        .withString("contentFromMultipleResources", contentFromMultipleResources)
                        .build());

        List<Record> values = new ArrayList<>();
        values.addAll(recordsFromDependencySPI);
        values.addAll(recordsFromDynamicDependencySPI);
        values.addAll(recordsFromExternalSPI);

        return values.iterator();
    }
```

**File:** sample-parent/sample-features/dynamic-dependencies/dynamic-dependencies-with-spi/src/test/java/org/talend/sdk/component/sample/feature/dynamicdependencies/withspi/input/DynamicDependenciesWithSPIInputTest.java (L50-68)
```java
        List<Record> collectedData = handler.getCollectedData(Record.class);
        Assertions.assertEquals(9, collectedData.size());

        int i = 0;
        for (; i < 3; i++) {
            Assertions.assertEquals("ServiceProviderFromDependency_" + (i + 1),
                    collectedData.get(i).getString("value"));
        }

        for (; i < 6; i++) {
            Assertions.assertEquals("ServiceProviderFromDynamicDependency_" + (i - 2),
                    collectedData.get(i).getString("value"));
        }

        for (; i < 9; i++) {
            Assertions.assertEquals("ServiceProviderFromExternalDependency_" + (i - 5),
                    collectedData.get(i).getString("value"));
        }

```

**File:** sample-parent/sample-features/dynamic-dependencies/dynamic-dependencies-with-spi/pom.xml (L46-50)
```text
        <dependency>
            <groupId>org.talend.sdk.samplefeature.dynamicdependencies</groupId>
            <artifactId>service-provider-from-dependency</artifactId>
            <version>${project.version}</version>
        </dependency>
```

**File:** sample-parent/sample-features/dynamic-dependencies/dynamic-dependencies-with-spi/pom.xml (L51-56)
```text
        <dependency>
            <groupId>org.talend.sdk.samplefeature.dynamicdependencies</groupId>
            <artifactId>service-provider-from-dynamic-dependency</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
```

**File:** sample-parent/sample-features/dynamic-dependencies/dynamic-dependencies-with-spi/pom.xml (L57-62)
```text
        <dependency>
            <groupId>org.talend.sdk.samplefeature.dynamicdependencies</groupId>
            <artifactId>service-provider-from-external-dependency</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
```