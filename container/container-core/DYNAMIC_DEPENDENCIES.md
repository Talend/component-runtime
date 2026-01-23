# Dynamic Dependency Management for TCK Containers

## Overview

This document describes the dynamic dependency management capabilities added to the Container infrastructure to support TCK (Test Compatibility Kit) containers with runtime dependency resolution.

## Features

### 1. Dynamic Dependency Addition

Containers can now have dependencies added at runtime after initial creation:

```java
Container container = containerManager.builder("my-container", "my-module.jar").create();

// Add a single dynamic dependency
Artifact dynamicDep = new Artifact("org.apache.commons", "commons-lang3", "jar", null, "3.12.0", "compile");
container.addDynamicDependency(dynamicDep);

// Or add multiple dependencies at once
List<Artifact> dependencies = Arrays.asList(
    new Artifact("org.apache.commons", "commons-lang3", "jar", null, "3.12.0", "compile"),
    new Artifact("com.google.guava", "guava", "jar", null, "31.1-jre", "compile")
);
container.addDynamicDependencies(dependencies);
```

### 2. ClassLoader Isolation

Each container maintains its own isolated `ConfigurableClassLoader`, ensuring that:
- Different containers can use different versions of the same dependency
- Dynamic dependencies are added only to the specific container that requires them
- No conflicts occur between containers with overlapping dependencies

```java
Container container1 = manager.builder("test1", "module1.jar").create();
Container container2 = manager.builder("test2", "module2.jar").create();

// Each container can have a different version of the same library
container1.addDynamicDependency(new Artifact("org.apache.commons", "commons-lang3", "jar", null, "3.11.0", "compile"));
container2.addDynamicDependency(new Artifact("org.apache.commons", "commons-lang3", "jar", null, "3.12.0", "compile"));
// No conflicts - each container has its own classloader with its own version
```

### 3. Loading from Maven Coordinates

Containers can be loaded using Maven GAV (GroupId:ArtifactId:Version) coordinates:

```java
// Load a container from Maven coordinates
String gav = "org.apache.tomee:ziplock:8.0.14";
Container container = manager.builder("my-container", gav).create();
```

The ContainerManager will resolve the GAV to the local repository location automatically.

### 4. Loading from Flat lib/ Folder

Containers can also be loaded from a flat library folder where all JARs are placed at the same directory level:

```java
// Load from a jar file name (searches in the root repository location)
Container container = manager.builder("my-container", "mycomponent.jar").create();
```

The ContainerManager's resolve() method checks multiple locations:
1. Direct file path
2. Maven repository structure (groupId/artifactId/version/)
3. Flat lib/ folder (just the jar name)

### 5. SPI and Resource Discovery

The `ConfigurableClassLoader` supports full Java SPI (Service Provider Interface) discovery:

#### Static Dependencies
- SPI implementations in static dependencies (from pom.xml) are automatically discovered
- `META-INF/services/*` files are correctly loaded from the classpath

#### Dynamic Dependencies  
- When dynamic dependencies are added, the classloader is reloaded
- After reloading, SPI implementations from dynamic dependencies become available
- Resources in dynamic dependencies (e.g., `META-INF/`, configuration files) are discoverable via `getResource()` and `getResources()`

Example:
```java
container.addDynamicDependency(dynamicDep);

// After adding dependency, use ServiceLoader to discover services
container.execute(() -> {
    ServiceLoader<MyService> loader = ServiceLoader.load(MyService.class);
    for (MyService service : loader) {
        // service instances from both static and dynamic dependencies
    }
    return null;
});
```

## Implementation Details

### Container Class Changes

1. **New Field**: `dynamicDependencies` - A mutable collection tracking runtime-added dependencies

2. **New Methods**:
   - `addDynamicDependency(Artifact)` - Adds a single dependency
   - `addDynamicDependencies(Collection<Artifact>)` - Adds multiple dependencies
   - `getDynamicDependencies()` - Returns all dynamic dependencies

3. **Modified Methods**:
   - `findDependencies()` - Now returns both static and dynamic dependencies
   - `findExistingClasspathFiles()` - Includes both static and dynamic dependency files

### ClassLoader Reloading

When dynamic dependencies are added:
1. Dependencies are added to the `dynamicDependencies` collection
2. The container's `reload()` method is called
3. The classloader is closed and recreated with all dependencies (static + dynamic)
4. All resources and classes from dynamic dependencies become available

## Integration with @DynamicDependencies

The `@DynamicDependencies` annotation is part of the component API and allows components to declare dependencies based on runtime configuration.

### Usage Pattern

1. Component declares a service method with `@DynamicDependencies`:
```java
@Service
public class MyComponentService {
    @DynamicDependencies
    public List<String> getDependencies(@Option("config") MyConfig config) {
        // Return list of Maven GAV coordinates based on configuration
        return Arrays.asList(
            "org.apache.derby:derbyclient:jar:10.12.1.1",
            "org.postgresql:postgresql:jar:42.5.0"
        );
    }
}
```

2. Container manager or test framework invokes the service:
```java
// Get the container's loader
ClassLoader loader = container.getLoader();

// Load and instantiate the service class
Class<?> serviceClass = loader.loadClass("com.example.MyComponentService");
Object serviceInstance = serviceClass.getDeclaredConstructor().newInstance();

// Invoke the @DynamicDependencies method
Method method = serviceClass.getMethod("getDependencies", MyConfig.class);
List<String> gavCoordinates = (List<String>) method.invoke(serviceInstance, config);

// Convert to Artifacts and add to container
List<Artifact> artifacts = gavCoordinates.stream()
    .map(Artifact::from)
    .collect(Collectors.toList());
container.addDynamicDependencies(artifacts);
```

## Testing

The implementation includes comprehensive tests in `DynamicDependencyTest`:

1. **addDynamicDependency** - Tests adding a single dependency
2. **addMultipleDynamicDependencies** - Tests adding multiple dependencies at once
3. **multipleContainersWithDifferentVersions** - Verifies isolation between containers
4. **findDependenciesIncludesDynamicDeps** - Verifies dependency discovery
5. **loadContainerFromMavenGAV** - Tests loading from Maven coordinates
6. **loadContainerFromFlatLibFolder** - Tests loading from flat lib/ folder

## Best Practices

1. **Add dependencies before using the container**: Dynamic dependencies should be added immediately after container creation and before executing any code that depends on them.

2. **Use isolation**: Create separate containers when you need different versions of the same dependency.

3. **Batch additions**: When adding multiple dependencies, use `addDynamicDependencies()` to add them all at once rather than multiple calls to `addDynamicDependency()`. This reduces the number of classloader reloads.

4. **Resource cleanup**: Always close containers when done to properly release classloader resources.

## Limitations

1. **Performance**: Adding dynamic dependencies triggers a classloader reload, which has some overhead. Batch additions when possible.

2. **State loss**: Classloader reloading means any static state in classes loaded by the container will be reset.

3. **Dependency resolution**: Currently, only Maven repository resolution is supported. Dependencies must exist in the configured repository location.

## Future Enhancements

Potential improvements for future versions:

1. **Automatic @DynamicDependencies invocation**: Automatically discover and invoke @DynamicDependencies methods during container initialization.

2. **Remote repository support**: Add support for downloading dependencies from remote Maven repositories.

3. **Dependency conflict resolution**: Implement automatic conflict resolution when multiple versions of the same dependency are requested.

4. **Lazy loading**: Load dynamic dependencies only when first needed rather than eagerly.
