# Dynamic Dependencies Module

## Overview

The `dynamic-dependencies` module provides several TCK connector plugins designed to validate the `@DynamicDependencies` feature integration, at design time and runtime. It contains several connectors organized into two categories:

- Firsts are connectors with a service that inherits `AbstractDynamicDependenciesService` and that check:
  - All supported ways to call `@DynamicDependencies` annotated services, according to expected configuration types
  - Loading of static, dynamic and provided dependencies
  - Load a TCK connectors as dynamic dependencies and retrieve data from them
- The other one is `DynamicDependenciesWithSPIInput`, and it checks:
  - `Service Provider Interface` (_SPI_) loading from different dependency scopes:
    - Standard dependency
    - Dynamic dependency
    - Provided by the runtime
  - Loading of resources from those dependencies
    - Loading a single resource as stream
    - Loading multiple resources at once

## Check dynamic dependencies loading
When TCK connectors are built, a file `TALEND-INF/dependencies.txt` is generated in the `jar`, that contains all its dependencies as a list of maven coordinates (_group:artifactId:version:scope_):

**dependencies.txt**
```
net.datafaker:datafaker:jar:2.4.2:compile
org.yaml:snakeyaml:jar:2.4:compile
com.github.curious-odd-man:rgxgen:jar:2.0:compile
com.googlecode.libphonenumber:libphonenumber:jar:8.13.50:compile
org.talend.components.extension:polling:jar:1.2512.0:compile
```

It happens that some dependencies can only be computed at design time, because, it depends on the connector's configuration. To support this, TCK provides the `@DynamicDependencies` annotation that can be used on a service. The annotated service should return a list of maven coordinates as strings:
```java
    @DynamicDependencies()
    public List<String> getDynamicDependencies(@Option("theDataset") final Dataset dataset) {
        List<String> dynamicDependencies = new ArrayList<>();
        if(dataset.hasASpecialOption()){
           dynamicDependencies.add("org.company:special-dependency:1.0.0");
           dynamicDependencies.add("org.company:another-special-dependency:2.1.0");
        }
        return dynamicDependencies;
    }
```
Then, the final application that integrates TCK, call the service with the configuration the user set, load those dependencies and provide them for the runtime.

### Connectors in this module
As you can see in the previous example, the annotated `@DynamicDependencies` service expect a parameter. This parameter can be of 3 different types:
- A datastore
- A dataset
- Any configuration annotated with `DynamicDependenciesConfiguration`

(_This documentation will not mention the deprecated `DynamicDependencySupported` provided by the `tDataprepRun` connector._)

So, 3 TCK modules have been designed, each one provide a service with one of those parameter types:
- dynamic-dependencies-with-datastore
- dynamic-dependencies-with-dataset
- dynamic-dependencies-with-dynamic-dependencies-configuration
Those 3 modules has the module `dynamic-dependencies-common` as dependency. They all have a service that inherits `AbstractDynamicDependenciesService`, that implements the `@DynamicDependencies` annotated method.

#### How to configure them?
Each of those TCK modules provide an input connector in which you can configure:
- A list of maven coordinate that will be returned as dynamic dependencies.
  - The user has to set the maven coordinate and a class to load coming from this dependency. If the class is successfully loaded, it means that the dependency is well loaded.
  - The coordinate `org.apache.maven.resolver:maven-resolver-api:2.0.14` is also returned by the `@DynamicDependencies`, the class to load is `org.eclipse.aether.artifact.DefaultArtifact`.
- A list of maven coordinates that references a TCK connector.
  - For each of them, the user has to set the connector's family, its name, its version and the configuration to use to retrieve data from it.
    - A boolean option `useDynamicDependencyLoading` is also provided to indicate that we also want to load dependencies coming from the configured connector.

Here is an example of connector's configuration:
```
- groupId: org.talend.components
- artifactId: data-generator
- version: 1.2512.0
- connector's family: DataGenerator
- connnector's name: DataGeneratorInput
- Connector's version: 1
- load transitive dependencies: true
- connector's configuration:
    {
      "configuration.minimumRows": "1000",
      "configuration.maximumRows": "10000",
      "configuration.dataset.seed": "123456",
      "configuration.dataset.customLocale": "false",
      "configuration.dataset.customSeed": "false",
      "configuration.dataset.rows": "10000",
      "configuration.dataset.fields[0].name": "name",
      "configuration.dataset.fields[0].type": "LASTNAME",
      "configuration.dataset.fields[0].$type_name": "Last name",
      "configuration.dataset.fields[0].blank": "0",
      "configuration.randomRows": "false"
    }
```

#### Output of those connectors
All of those connectors generate the same output. Only the configuration type of the `@DynamicDependencies` annotated service is different.

It will generate TCK/records containing those fields:
- `maven`: the maven coordinate of the dependency
- `clazz`: the class that we tried to load from this dependency
- `is_loaded`: a boolean indicating if the class has been successfully loaded
- `connector_classloader`: the classloader name of the dynamic dependency connector
- `clazz_classloader`: the classloader name of the loaded class
- `from_location`: the location from which the class has been loaded
- `is_tck_container`: a boolean indicating if the dependency is a standard dependency or a TCK plugin
- `first_record`: if the dependency is a TCK plugin, this is the first record retrieve from the loaded connector
- `root_repository`: The value of the property `talend.component.manager.m2.repository`
- `runtime_classpath`: the value of the property `java.class.path`
- `Working_directory`: the value of the property `user.dir`
- `comment`: a comment

##### The first record is the result of the class loading from a static dependency, the generated record should be something like:
```
+-----------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| key                   | value                                                                                                                                                                                                                                                                                                                                     |
+-----------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| maven                 | org.talend.sdk.samplefeature.dynamicdependencies:dynamic-dependencies-common:N/A                                                                                                                                                                                                                                                          |
| clazz                 | org.talend.sdk.component.sample.feature.loadinganalysis.config.Dependency                                                                                                                                                                                                                                                             |
| is_loaded             | true                                                                                                                                                                                                                                                                                                                                      |
| connector_classloader | org.talend.sdk.component.classloader.ConfigurableClassLoader@9ebe38b                                                                                                                                                                                                                                                                      |
| clazz_classloader     | org.talend.sdk.component.classloader.ConfigurableClassLoader@9ebe38b                                                                                                                                                                                                                                                                      |
| from_location         | jar:file:/C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository/org/talend/sdk/samplefeature/dynamicdependencies/dynamic-dependencies-common/1.89.0-SNAPSHOT/dynamic-dependencies-common-1.89.0-SNAPSHOT.jar!/org/talend/sdk/component/sample/feature/dynamicdependencies/config/Dependency.class |
| is_tck_container      | false                                                                                                                                                                                                                                                                                                                                     |
| first_record          | null                                                                                                                                                                                                                                                                                                                                      |
| root_repository       | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository                                                                                                                                                                                                                                             |
| runtime_classpath     | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/workspace/AAAA/poms/jobs/process/qtdi2134_dyndeps_0.1/target/classpath.jar;/C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository/org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar;                              |
| Working_directory     | C:\tmp\202601_dyndeps_exports\Talend-Studio-20260121_1719-V8.0.1                                                                                                                                                                                                                                                                          |
| comment               | Check static dependency.                                                                                                                                                                                                                                                                                                                  |
+-----------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
```
In this exemple we can see that we try to load the class `org.talend.sdk.component.sample.feature.loadinganalysis.config.Dependency` from the dependency `org.talend.sdk.samplefeature.dynamicdependencies:dynamic-dependencies-common`.
The version is `N/A` since it is not needed, the dependency is a static one and is loaded at build time.
The `DynamicDependencyWithXxxInput` is well loaded from `org.talend.sdk.component.classloader.ConfigurableClassLoader` as the class to test.

##### The second record is the result when it tries to load a class from the runtime. It tries to load `org.talend.sdk.component.api.service.asyncvalidation.ValidationResult` that is provided by TCK:
```
+-----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| key                   | value                                                                                                                                                                                                                                                                                                        |
+-----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| maven                 | org.talend.sdk.component:component-runtime:N/A                                                                                                                                                                                                                                                               |
| clazz                 | org.talend.sdk.component.api.service.asyncvalidation.ValidationResult                                                                                                                                                                                                                                        |
| is_loaded             | true                                                                                                                                                                                                                                                                                                         |
| connector_classloader | org.talend.sdk.component.classloader.ConfigurableClassLoader@9ebe38b                                                                                                                                                                                                                                         |
| clazz_classloader     | jdk.internal.loader.ClassLoaders$AppClassLoader@73d16e93                                                                                                                                                                                                                                                     |
| from_location         | jar:file:/C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository/org/talend/sdk/component/component-api/1.89.0-QTDI-2134-YPL-SNAPSHOT/component-api-1.89.0-QTDI-2134-YPL-SNAPSHOT.jar!/org/talend/sdk/component/api/service/asyncvalidation/ValidationResult.class    |
| is_tck_container      | false                                                                                                                                                                                                                                                                                                        |
| first_record          | null                                                                                                                                                                                                                                                                                                         |
| root_repository       | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository                                                                                                                                                                                                                |
| runtime_classpath     | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/workspace/AAAA/poms/jobs/process/qtdi2134_dyndeps_0.1/target/classpath.jar;/C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository/org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar; |
| Working_directory     | C:\tmp\202601_dyndeps_exports\Talend-Studio-20260121_1719-V8.0.1                                                                                                                                                                                                                                             |
| comment               | Check provided dependency.                                                                                                                                                                                                                                                                                   |
+-----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
```
In this example, we can see that the class `org.talend.sdk.component.api.service.asyncvalidation.ValidationResult` is loaded from the `AppClassLoader`, meaning that it is provided by the runtime and not loaded from the `ConfigurableClassLoader` of the TCK container.

##### The third record is the result of loading a class from a dynamic dependency:
```
+-----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| key                   | value                                                                                                                                                                                                                                                                                                        |
+-----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| maven                 | org.apache.maven.resolver:maven-resolver-api:2.0.14                                                                                                                                                                                                                                                          |
| clazz                 | org.eclipse.aether.artifact.DefaultArtifact                                                                                                                                                                                                                                                                  |
| is_loaded             | true                                                                                                                                                                                                                                                                                                         |
| connector_classloader | org.talend.sdk.component.classloader.ConfigurableClassLoader@9ebe38b                                                                                                                                                                                                                                         |
| clazz_classloader     | jdk.internal.loader.ClassLoaders$AppClassLoader@73d16e93                                                                                                                                                                                                                                                     |
| from_location         | jar:file:/C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository/org/apache/maven/resolver/maven-resolver-api/2.0.14/maven-resolver-api-2.0.14.jar!/org/eclipse/aether/artifact/DefaultArtifact.class                                                                 |
| is_tck_container      | false                                                                                                                                                                                                                                                                                                        |
| first_record          | null                                                                                                                                                                                                                                                                                                         |
| root_repository       | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository                                                                                                                                                                                                                |
| runtime_classpath     | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/workspace/AAAA/poms/jobs/process/qtdi2134_dyndeps_0.1/target/classpath.jar;/C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository/org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar; |
| Working_directory     | C:\tmp\202601_dyndeps_exports\Talend-Studio-20260121_1719-V8.0.1                                                                                                                                                                                                                                             |
| comment               | An instance has been instantiated and assigned.                                                                                                                                                                                                                                                              |
+-----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
```
This is the result of the default dynamic dependency loading. We can see that the class `org.eclipse.aether.artifact.DefaultArtifact` has been successfully loaded from the dynamic dependency `org.apache.maven.resolver:maven-resolver-api:2.0.14`.
In this environment, it has been loaded from the `AppClassLoader`. It happens when all dependencies are flatten in single folder, and not organized as a maven repository.

##### Example of a record when loading a TCK connector as dynamic dependency
```
+-----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| key                   | value                                                                                                                                                                                                                                                                                                        |
+-----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| maven                 | org.talend.components:data-generator:1.2512.0                                                                                                                                                                                                                                                                |
| clazz                 | N/A                                                                                                                                                                                                                                                                                                          |
| is_loaded             | true                                                                                                                                                                                                                                                                                                         |
| connector_classloader | org.talend.sdk.component.classloader.ConfigurableClassLoader@9ebe38b                                                                                                                                                                                                                                         |
| clazz_classloader     | N/A                                                                                                                                                                                                                                                                                                          |
| from_location         | N/A                                                                                                                                                                                                                                                                                                          |
| is_tck_container      | true                                                                                                                                                                                                                                                                                                         |
| first_record          | {"name":"Rogahn"}                                                                                                                                                                                                                                                                                            |
| root_repository       | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository                                                                                                                                                                                                                |
| runtime_classpath     | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/workspace/AAAA/poms/jobs/process/qtdi2134_dyndeps_0.1/target/classpath.jar;/C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository/org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar; |
| Working_directory     | C:\tmp\202601_dyndeps_exports\Talend-Studio-20260121_1719-V8.0.1                                                                                                                                                                                                                                             |
| comment               |                                                                                                                                                                                                                                                                                                              |
+-----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
```
In that example, we can see that we don't try to load a class, `clazz: N/A`, but TCK plugin, `is_tck_container: true`. It has been well loaded since we retrieve some data from it:
```
| first_record          | {"name":"Rogahn"}   
```

## Check SPI and resource loading
The `dynamic-depdencies-with-spi` provides an input connector that will try to load:
- The SPI implementation from a static dependency
- The SPI implementation from a dynamic dependency
- The SPI implementation provided by the runtime
- A resource as stream from a static dependency
- A resource as stream from a dynamic dependency
- A resource as stream provided by the runtime
- Multiple resources at once from a static, dynamic and provided dependencies

There is no configuration to set in that connector.

It generates TCK records containing those fields:
- `value`: a string value returned by the loaded SPI implementation or values from resources forthe last record
- `SPI_Interface`: the SPI interface class name
- `SPI_Interface_classloader`: the classloader name that loaded the SPI interface
- `SPI_Impl`: the SPI implementation class name
- `SPI_Impl_classloader`: the classloader name that loaded the SPI implementation
- `comment`: a comment
- `rootRepository`: The value of the property `talend.component.manager.m2.repository`
- `classpath`: the value of the property `java.class.path`
- `workingDirectory`: the value of the property `user.dir`

### The first record checks SPI implementation provided by a static dependency
The SPI implementation is provided by the dependency `org.talend.sdk.samplefeature.dynamicdependencies:service-provider-from-dependency`.
```
+---------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| key                       | value                                                                                                                                                                                                                                                                                                                        |
+---------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| value                     | ServiceProviderFromDependency value                                                                                                                                                                                                                                                                                          |
| SPI_Interface             | interface org.talend.sdk.component.sample.feature.loadinganalysis.classloadertestlibrary.serviceInterfaces.StringProviderSPIAsDependency                                                                                                                                                                                 |
| SPI_Interface_classloader | org.talend.sdk.component.classloader.ConfigurableClassLoader@6457a08f                                                                                                                                                                                                                                                        |
| SPI_Impl                  | class org.talend.sdk.component.sample.feature.loadinganalysis.serviceproviderfromdependency.ServiceProviderFromDependency                                                                                                                                                                                                |
| SPI_Impl_classloader      | org.talend.sdk.component.classloader.ConfigurableClassLoader@6457a08f                                                                                                                                                                                                                                                        |
| comment                   | SPI implementation loaded from a dependency.                                                                                                                                                                                                                                                                                 |
| rootRepository            | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository                                                                                                                                                                                                                                |
| classpath                 | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/workspace/AAAA/poms/jobs/process/qtdi2134_dyndeps_withspi_service_0.1/target/classpath.jar;/C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository/org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar; |
| workingDirectory          | C:\tmp\202601_dyndeps_exports\Talend-Studio-20260121_1719-V8.0.1                                                                                                                                                                                                                                                             |
+---------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
```
We can see that the SPI implementation has been well loaded, since the value is retrieved, and has been loaded by the container classloader `ConfigurableClassLoader` as expected.

### The second record checks SPI implementation provided by a dynamic dependency
The spi implementation is provided by the dependency `org.talend.sdk.samplefeature.dynamicdependencies:service-provider-from-dynamic-dependency`. It is returned by the `@DynamicDependency` service.
```
+---------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| key                       | value                                                                                                                                                                                                                                                                                                                        |
+---------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| value                     | ServiceProviderFromDynamicDependency value                                                                                                                                                                                                                                                                                   |
| SPI_Interface             | interface org.talend.sdk.component.sample.feature.loadinganalysis.classloadertestlibrary.serviceInterfaces.StringProviderSPIAsDynamicDependency                                                                                                                                                                          |
| SPI_Interface_classloader | org.talend.sdk.component.classloader.ConfigurableClassLoader@6457a08f                                                                                                                                                                                                                                                        |
| SPI_Impl                  | class org.talend.sdk.component.sample.feature.loadinganalysis.serviceproviderfromdynamicdependency.ServiceProviderFromDynamicDependency                                                                                                                                                                                  |
| SPI_Impl_classloader      | org.talend.sdk.component.classloader.ConfigurableClassLoader@6457a08f                                                                                                                                                                                                                                                        |
| comment                   | SPI implementation loaded from a dynamic dependency.                                                                                                                                                                                                                                                                         |
| rootRepository            | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository                                                                                                                                                                                                                                |
| classpath                 | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/workspace/AAAA/poms/jobs/process/qtdi2134_dyndeps_withspi_service_0.1/target/classpath.jar;/C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository/org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar; |
| workingDirectory          | C:\tmp\202601_dyndeps_exports\Talend-Studio-20260121_1719-V8.0.1                                                                                                                                                                                                                                                             |
+---------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
```
In this second record, we see that the SPI implementation has been loaded from the `ConfigurableClassLoader`.

### The third record checks SPI implementation provided by the runtime
The spi implementation is provided by the dependency `org.talend.sdk.samplefeature.dynamicdependencies:service-provider-from-external-dependency`. In the studio, we can add this library using the `tLibraryLoad` connector.
```
+---------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| key                       | value                                                                                                                                                                                                                                                                                                                        |
+---------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| value                     | [ERROR] StringProviderFromExternalSPI not loaded!                                                                                                                                                                                                                                                                            |
| SPI_Interface             | interface org.talend.sdk.component.sample.feature.loadinganalysis.classloadertestlibrary.serviceInterfaces.StringProviderFromExternalSPI                                                                                                                                                                                 |
| SPI_Interface_classloader | org.talend.sdk.component.classloader.ConfigurableClassLoader@6457a08f                                                                                                                                                                                                                                                        |
| SPI_Impl                  | Not found                                                                                                                                                                                                                                                                                                                    |
| SPI_Impl_classloader      | Not found                                                                                                                                                                                                                                                                                                                    |
| comment                   | SPI implementation loaded from a runtime/provided dependency.                                                                                                                                                                                                                                                                |
| rootRepository            | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository                                                                                                                                                                                                                                |
| classpath                 | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/workspace/AAAA/poms/jobs/process/qtdi2134_dyndeps_withspi_service_0.1/target/classpath.jar;/C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository/org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar; |
| workingDirectory          | C:\tmp\202601_dyndeps_exports\Talend-Studio-20260121_1719-V8.0.1                                                                                                                                                                                                                                                             |
+---------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
```
In this thirds record, we can see that the connector were not able to find implementation for this SPI whereas it should be available in the runtime.

### The fourth record checks resource loading from dependencies
The last record is quite different. Only the `value` field is interesting. It contains a json document with resources loading results:
```
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| key                       | value                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| value                     | {"contentFromResourceDependency":"Message from a dependency resource.","contentFromResourceDynamicDependency":"Message from a dynamic dependency resource.","contentFromResourceExternalDependency":"Message from an external dependency resource.","contentFromMultipleResources":"There should be 3 different values:\nContent from static dependency\nContent from dynamic dependency\nContent from static dependency\nContent from dynamic dependency"} |
| SPI_Interface             | N/A                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| SPI_Interface_classloader | N/A                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| SPI_Impl                  | N/A                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| SPI_Impl_classloader      | N/A                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| comment                   | Resources loading.                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| rootRepository            | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository                                                                                                                                                                                                                                                                                                                                                               |
| classpath                 | C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/workspace/AAAA/poms/jobs/process/qtdi2134_dyndeps_withspi_service_0.1/target/classpath.jar;/C:/tmp/202601_dyndeps_exports/Talend-Studio-20260121_1719-V8.0.1/configuration/.m2/repository/org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar;                                                                                                                                |
| workingDirectory          | C:\tmp\202601_dyndeps_exports\Talend-Studio-20260121_1719-V8.0.1                                                                                                                                                                                                                                                                                                                                                                                            |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
```
Here is the json documentation contained in the `value` field:
```
{
  "contentFromResourceDependency": "Message from a dependency resource.",
  "contentFromResourceDynamicDependency": "Message from a dynamic dependency resource.",
  "contentFromResourceExternalDependency": "Message from an external dependency resource.",
  "contentFromMultipleResources": "There should be 3 different values:\nContent from static dependency\nContent from dynamic dependency\nContent from static dependency\nContent from dynamic dependency"
}
```
The `DynamicDependenciesWithSPIInput` connector tries to load some resources using `DynamicDependenciesWithSPIService.class.getClassLoader().getResourceAsStream(resource)`:
- `FROM_DEPENDENCY/resource.properties`: this resource exists only in `org.talend.sdk.samplefeature.dynamicdependencies:service-provider-from-dependency` module. The resource value is copied in the `contentFromResourceDependency` field of the record.
- `FROM_DYNAMIC_DEPENDENCY/resource.properties`: this resource exists only in `org.talend.sdk.samplefeature.dynamicdependencies:service-provider-from-dynamic-dependency` module. The resource value is copied in the `contentFromResourceDynamicDependency` field of the record.
- `FROM_EXTERNAL_DEPENDENCY/resource.properties`: this resource exists only in `org.talend.sdk.samplefeature.dynamicdependencies:service-provider-from-external-dependency` module. The resource value is copied in the `contentFromResourceExternalDependency` field of the record.

All those three resources are successfully loaded in this example.

The last field, `contentFromMultipleResources` contain the result of loading multiple resources at once using `DynamicDependenciesWithSPIService.class.getClassLoader().getResources(resource)` for the resource path `FROM_MULTIPLE/resource.txt`.
This resource exists in all the three modules `org.talend.sdk.samplefeature.dynamicdependencies:service-provider-from-dependency`, `org.talend.sdk.samplefeature.dynamicdependencies:service-provider-from-dynamic-dependency`, `org.talend.sdk.samplefeature.dynamicdependencies:service-provider-from-external-dependency`.
So, 3 values should be concatenated in this field if everything were loaded. In the example, the content of this resource is successfully loaded from static and dynamic dependencies, but twice for each! And, the `FROM_MULTIPLE/resource.txt`
in `org.talend.sdk.samplefeature.dynamicdependencies:service-provider-from-external-dependency` is not found.