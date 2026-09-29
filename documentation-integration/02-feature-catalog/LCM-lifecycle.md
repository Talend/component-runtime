# Feature Catalog - LCM: Lifecycle and evolution

> Framework version documented: **1.2611.0-SNAPSHOT** (root `pom.xml`). Category prefix: `LCM-`. Generated from the same data as [`index.LCM.json`](index.LCM.json); both agree exactly.

Scope: component/configuration versioning and migration, plugin (container) model, classloader isolation, discovery and dependency resolution, packaging (nested repository, `.car`), dynamic dependencies, and SPI extension points. Narrative: [`../06-runtime-execution.md`](../06-runtime-execution.md) (sections on loading and migration). Note: `Resolver`, `DynamicDependencies`, `ObjectFactory` are catalogued here (dependency/factory packages); their injection as built-in services is also described in the SVC catalog.

Levels (see `../09-integration-checklist.md`): 0 = mandatory, 1 = normal user experience, 2 = full TCK.

## Index

| ID | Name | Level | Designer | Runtime |
|---|---|---|---|---|
| [LCM-001](#lcm-001-version) | `@Version` | 0 | MUST store the component `version` (and each configuration type `version`) with every saved configuration and MUST send it back when calling migrate. | MUST pass the persisted version as the `version` argument of `findMapper`/`findProcessor`/`findDriverRunner`. MUST NOT rewrite the stored version silently. |
| [LCM-002](#lcm-002-migrationhandler) | `MigrationHandler` | 0 | none | MUST NOT migrate itself; MUST rely on the manager (`BaseMeta.instantiate`) or the server migrate endpoint. |
| [LCM-003](#lcm-003-configuration-migration-protocol) | `Configuration migration protocol` | 1 | MUST record `__version` for each nested configuration type it persists (datastore, dataset) and SHOULD call the migrate endpoints on load when `saved version < ComponentDetail.version`. | MUST call `findMapper/findProcessor` with the saved component version (migration is applied inside) and MUST keep `<path>.__version` entries in the map. |
| [LCM-004](#lcm-004-plugin-container-model) | `Plugin (container) model` | 0 | none | MUST register every plugin before instantiating components; MUST call `removePlugin`/`close()` to release classloaders; SHOULD hold one ComponentManager per JVM. |
| [LCM-005](#lcm-005-classloader-isolation-configurableclassloader) | `Classloader isolation (ConfigurableClassLoader)` | 0 | none | MUST give each plugin its own classloader whose parent exposes at least the API/SPI/runtime packages above; MUST set the thread context classloader to the plugin loader around every component call (LifecycleImpl does). |
| [LCM-006](#lcm-006-plugin-discovery-and-registration-sources) | `Plugin discovery and registration sources` | 0 | none | MUST decide how plugins are supplied (list, `.car` deployment, classpath) and register them at startup; SHOULD use stable ids (artifactId) so upgrades replace instead of duplicate. |
| [LCM-007](#lcm-007-dependency-resolution-dependenciestxt) | `Dependency resolution (dependencies.txt)` | 0 | none | MUST provide a Maven-layout repository (or nested repository) containing all `compile`/`runtime` dependencies; MAY replace resolution with a `ContainerClasspathContributor` or `Customizer.ignoreDefaultDependenciesDescriptor()`. |
| [LCM-008](#lcm-008-maven-repository-discovery-m2) | `Maven repository discovery (m2)` | 0 | none | MUST set `talend.component.manager.m2.repository` (or pass the path to the ComponentManager constructor) in production; the default is intentionally not the user's ~/.m2. |
| [LCM-009](#lcm-009-nested-maven-repository-and-shade-transformers) | `Nested Maven repository and shade transformers` | 2 | none | MAY package the host runtime as an uber jar with these transformers; then the m2 path becomes irrelevant. |
| [LCM-010](#lcm-010-component-archive-car) | `Component archive (.car)` | 2 | none | SHOULD support deploying `.car` files (unpack repository, register the main GAV) for remote/engine deployment. |
| [LCM-011](#lcm-011-component-scanning-rules) | `Component scanning rules` | 1 | none | MUST NOT scan itself; SHOULD provide `scanning.properties` in fat-jar deployments for startup speed. |
| [LCM-018](#lcm-018-containerlistener-and-jmx-exposure) | `ContainerListener and JMX exposure` | 2 | none | MAY register listeners for monitoring hooks (checklist level 2). |

## Entries

### LCM-001 `@Version`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/component/Version.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-versions-and-migration.adoc`)
- **Category / Sub-area**: LCM / Versioning
- **Kind**: annotation
- **Applies to**: input, processor, output, standalone, configuration types (@DataStore, @DataSet, @Checkpoint, nested option classes)
- **Description**: Declares the current version of a component or of a nested configuration class and an optional migration handler class. Component version is read as `type.getAnnotation(Version).value()` (default 1 when the annotation is absent) and exposed as `version`. Configuration classes carry their own independent version (nested migration, LCM-003). Merged from DSG-004: `@Version` on a `@DataStore`/`@DataSet` class versions that configuration type; `ConfigTypeNode.version` is `-1` when the configuration class has no `@Version`; migrate endpoints are SRV-004 (`/component/migrate/{id}/{configurationVersion}`) and SRV-011 (`/configurationtype/migrate/{id}/{configurationVersion}`). Design-side declaration (component metadata) is documented here only.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | value | int | 1 | current version |
  | migrationHandler | Class<? extends MigrationHandler> | `MigrationHandler.class` (= none) | handler instantiated with the constructor having the most parameters; parameters are injected services |

- **Contract for the Designer**: MUST store the component `version` (and each configuration type `version`) with every saved configuration and MUST send it back when calling migrate.
- **Contract for the Runtime**: MUST pass the persisted version as the `version` argument of `findMapper`/`findProcessor`/`findDriverRunner`. MUST NOT rewrite the stored version silently.
- **Server exposure**: `ComponentDetail.version`, `ComponentIndex.version`, `ConfigTypeNode.version`.
- **Maturity level**: 0 - wrong or dropped versions skip migrations and yield wrong configuration (checklist: `@Version` value passed through).
- **Example**:

```java
@Version(value = 3, migrationHandler = MyMigration.class)
@Processor(family = "demo", name = "p")
public class P implements Serializable { }
```

### LCM-002 `MigrationHandler`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/component/MigrationHandler.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-versions-and-migration.adoc`)
- **Category / Sub-area**: LCM / Versioning
- **Kind**: interface
- **Applies to**: component/configuration `@Version.migrationHandler`
- **Description**: `Map<String,String> migrate(int incomingVersion, Map<String,String> incomingData)` converts a flat configuration of an older version to the current one. `incomingData` keys use the property paths of the component metadata; no typing, no class instantiation. The result replaces the input map. Handlers are created lazily and cached in the plugin services map (`MigrationHandlerFactory`); for a component-level handler the full path (`configuration.datastore.url`) is seen, for a nested configuration class handler the path is relative (`url`).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | migrate(int, Map<String,String>) | Map<String,String> | - | returns the migrated properties (may add/remove/rename) |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST NOT migrate itself; MUST rely on the manager (`BaseMeta.instantiate`) or the server migrate endpoint.
- **Server exposure**: Server endpoint `POST /component/migrate/{id}/{configurationVersion}` and configuration-type migrate (see `../03-component-server-api.md`).
- **Maturity level**: 0 - old saved configurations break without it.
- **Example**:

```java
public class MyMigration implements MigrationHandler {
    public Map<String, String> migrate(int v, Map<String, String> in) {
        if (v < 2) { in.put("location", in.remove("filepath")); }
        return in;
    }
}
```

### LCM-003 `Configuration migration protocol`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/reflect/MigrationHandlerFactory.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-versions-and-migration.adoc`)
- **Category / Sub-area**: LCM / Versioning
- **Kind**: convention
- **Applies to**: all components and configuration types
- **Description**: 1) Component level: `ComponentFamilyMeta.BaseMeta.instantiate(configuration, configVersion)` ALWAYS calls `migrationHandler.migrate(configVersion, configuration)` before building the component (the handler decides; no equality check) and passes null configuration through untouched. 2) Nested configuration types (any nested parameter whose metadata has `tcomp::configurationtype::*` and whose class has `@Version`): the flat map may contain `<path>.__version=<n>`; if present and `n < class version`, the sub-map under `<path>.` (excluding `__version`) is stripped of its prefix, migrated with that class's handler, re-prefixed, and `<path>.__version` is set to the current version. Absent `__version` -> no nested migration ('No version for X so skipping'). Nested handlers run before the component handler (composition `implicit -> explicit`). 3) Designer: the server endpoint `POST /component/migrate/{id}/{version}` does the same for components (skips with a warning and returns the config unchanged if `incoming > registry`; values prefixed `base64://` are URL-safe-Base64-decoded first) and `POST /configurationtype/migrate/{id}/{version}` for configuration types (adds `<meta.path>.__version` when absent, removes it after). ComponentException from a handler maps to HTTP 400 (USER), 456 (BACKEND), 520 (other).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | <path>.__version | flat property | - | version of the nested configuration stored with its values |
  | version argument | int | - | component version at save time |

- **Contract for the Designer**: MUST record `__version` for each nested configuration type it persists (datastore, dataset) and SHOULD call the migrate endpoints on load when `saved version < ComponentDetail.version`.
- **Contract for the Runtime**: MUST call `findMapper/findProcessor` with the saved component version (migration is applied inside) and MUST keep `<path>.__version` entries in the map.
- **Server exposure**: `ConfigTypeNode.version`, `ComponentDetail.version`; endpoints `/component/migrate`, `/configurationtype/migrate`.
- **Maturity level**: 1 - level 1 item 'configuration migration' in the checklist rules; level 0 hosts only pass `@Version` through.
- **Example**:

```json
{"configuration.datastore.url": "http://x", "configuration.datastore.__version": "1", "configuration.dataset.table": "t", "configuration.dataset.__version": "2"}
```

### LCM-004 `Plugin (container) model`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/ComponentManager.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-loading.adoc`)
- **Category / Sub-area**: LCM / Loading
- **Kind**: class
- **Applies to**: host runtime
- **Description**: `ComponentManager` (AutoCloseable; contextual singleton `instance()`, created with a shutdown hook and closed once) owns a `ContainerManager`. Each plugin = one `Container` with id, root module, dependency artifacts, its own `ConfigurableClassLoader` and a typed data bag (`ContainerComponentRegistry`, `AllServices`, `LightContainer`, `ComponentContexts`, `OriginalId`). Container states: CREATED, DEPLOYED, ON_ERROR, UNDEPLOYING, UNDEPLOYED. Registration (`addPlugin(pathOrGav)`): `ContainerManager.builder(...).create()` builds the classloader, then every `ContainerListener.onCreate` runs (the manager's `Updater` scans annotations, creates services, registers components; any listener failure -> `IllegalArgumentException <id> can't be deployed` with suppressed causes and `onClose` rollback; a duplicate id -> `Container '<id>' already exists`). `removePlugin(id)` closes the container (`onClose`: registry cleared, non-proxy services get `@PreDestroy`, Jsonb closed). Reads/writes are guarded by a `ReentrantReadWriteLock`. `ComponentManager.instance()` also auto-registers plugins declared in `TALEND-INF/plugins.properties` (`talend.component.manager.plugins.parallel`).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | addPlugin(String) | String id | - | path or GAV; returns existing id if already registered |
  | removePlugin(String) | void | - | close and unregister |
  | availablePlugins() | List<String> | - | sorted plugin ids |
  | hasPlugin / findPlugin | - | - | lookup by id or by any name converted by `buildAutoIdFromName` |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST register every plugin before instantiating components; MUST call `removePlugin`/`close()` to release classloaders; SHOULD hold one ComponentManager per JVM.
- **Server exposure**: none
- **Maturity level**: 0 - no component can run before its plugin is loaded.
- **Example**:

```java
ComponentManager manager = ComponentManager.instance();
String id = manager.addPlugin("/repo/org/acme/acme-components/1.0/acme-components-1.0.jar");
```

### LCM-005 `Classloader isolation (ConfigurableClassLoader)`
- **Source**: `container/container-core/src/main/java/org/talend/sdk/component/classloader/ConfigurableClassLoader.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-loading.adoc`)
- **Category / Sub-area**: LCM / Loading
- **Kind**: class
- **Applies to**: host runtime
- **Description**: Tree classloading: a shared parent (the host/manager loader) and one child `ConfigurableClassLoader` per plugin. Two predicates decide delegation: `parentClassesFilter` (may load from parent) and `classesFilter` (child-first). `ComponentManager` defaults: parent-first prefixes `org.talend.sdk.component.api.`, `.spi.`, `.classloader.`, `.runtime.`, `.container.`, `.dependencies.`, `javax.annotation.`, `javax.json.`, `org.slf4j.`, `org.apache.johnzon.` plus values from `Customizer.containerClassesAndPackages()` and system property `talend.component.manager.classloader.container.classesAndPackages` (comma-separated); everything else is child-first. `loadClass` order: JVM/platform classes (`jvmMarkers`) -> already loaded -> child if child-first -> parent if `parentFilter` -> child otherwise -> JVM fallback -> java classpath (`classLoaderFromClasspath`). Resources: own first, then parent when `parentResourcesFilter` accepts the URL (default parent resources: `/xmlMappings/`; property `talend.component.manager.classloader.container.parentResources`); `TALEND-INF/*` is never read from the parent; `META-INF/services/org.xml.sax.driver` is blacklisted. Supports one level of nested jars (`MAVEN-INF/repository/`, LCM-008) and class transformers (`ClassFileTransformer`). Optional `talend.tccl.cacheable.classes` tunes URL-connection caching.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | talend.component.manager.classloader.container.classesAndPackages | JVM property | unset | extra parent-loaded classes/packages |
  | talend.component.manager.classloader.container.parentResources | JVM property | unset | extra parent-visible resources |
  | talend.tccl.cacheable.classes | JVM property | unset | classes whose URL connection caching stays enabled |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST give each plugin its own classloader whose parent exposes at least the API/SPI/runtime packages above; MUST set the thread context classloader to the plugin loader around every component call (LifecycleImpl does).
- **Server exposure**: none
- **Maturity level**: 0 - isolation is a Level 0 requirement (plugin loading & classloader isolation).
- **Example**: none

### LCM-006 `Plugin discovery and registration sources`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/ComponentManager.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-loading.adoc`)
- **Category / Sub-area**: LCM / Loading
- **Kind**: convention
- **Applies to**: host runtime
- **Description**: Plugins enter through: explicit `addPlugin`/`addWithLocationPlugin`; the nested plugin index `TALEND-INF/plugins.properties` (key = plugin name, value = GAV/path; read by `ContainerManager` when `supportsResourceDependencies`); `autoDiscoverPluginsIfEmpty(callers, classpath)` (runs when no plugin exists: `callers` adds the jar of the first non-framework stack frame - skipped by `-Dcomponent.manager.callers.skip=true`; `classpath` scans `TALEND-INF/dependencies.txt` resources on the TCCL - skipped by `-Dcomponent.manager.classpath.skip=true`). Plugin id = `ContainerManager.buildAutoIdFromName(path)`: for a GAV the artifactId, else the file name without `.jar`, `-SNAPSHOT`, Jira suffix `-ABC-123`, milestone `M1` and trailing numeric version (up to 3 dot/dash separated numbers). Consequently two versions of the same artifact share one id; the second registration returns the existing id or fails `already exists` (single version per id). A plugin without components is removed after `addJarContaining`.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | component.manager.callers.skip | JVM property | false | skip caller-jar discovery |
  | component.manager.classpath.skip | JVM property | false | skip classpath discovery |
  | talend.component.manager.plugins.parallel | JVM property | false | load `plugins.properties` plugins in parallel |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST decide how plugins are supplied (list, `.car` deployment, classpath) and register them at startup; SHOULD use stable ids (artifactId) so upgrades replace instead of duplicate.
- **Server exposure**: none
- **Maturity level**: 0 - components are unusable until discovered.
- **Example**:

```java
manager.autoDiscoverPluginsIfEmpty(true, true);
```

### LCM-007 `Dependency resolution (dependencies.txt)`
- **Source**: `container/container-core/src/main/java/org/talend/sdk/component/dependencies/maven/MvnDependencyListLocalRepositoryResolver.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-loading.adoc`)
- **Category / Sub-area**: LCM / Loading
- **Kind**: convention
- **Applies to**: host runtime
- **Description**: For a plugin jar/dir the resolver reads `TALEND-INF/dependencies.txt` (Maven `dependency:list` output, optional header `The following files have been resolved:`), keeps entries of scope `compile` or `runtime` and type `jar`, `bundle` or `zip` (`provided`, `test` dropped; API artifacts are `provided` because the runtime supplies them) and maps `groupId:artifactId:type[:classifier]:version:scope` to `<m2>/<g/as/path>/<a>/<v>/<a>-<v>[-classifier].<type>` (`MvnCoordinateToFileConverter`). Additional dynamic dependencies are appended from `TALEND-INF/dynamic-dependencies.properties` (key = plugin id, value = comma-separated GAVs, ACT-010). `ContainerManager.resolve(path)` order: existing path -> GAV -> `<root>/<path>` -> file name in the root (job lib folder) -> runtime classpath entry -> (missing, filtered later). `classpath.jar` manifests (job server) are expanded. Framework jars matching `FRAMEWORK_JAR_PATTERN` (component-api, -impl, -manager, -spi, container-core, geronimo specs, johnzon, xbean, slf4j...) are removed from the runtime classpath list.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | TALEND-INF/dependencies.txt | resource | - | `mvn dependency:list -DoutputFile` output; configurable name via `ComponentManager(m2, dependenciesResource, jmxPattern)` |
  | TALEND-INF/dynamic-dependencies.properties | resource | - | plugin id -> extra GAV list |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST provide a Maven-layout repository (or nested repository) containing all `compile`/`runtime` dependencies; MAY replace resolution with a `ContainerClasspathContributor` or `Customizer.ignoreDefaultDependenciesDescriptor()`.
- **Server exposure**: none
- **Maturity level**: 0 - missing dependencies cause ClassNotFoundException.
- **Example**:

```java
// TALEND-INF/dependencies.txt
// org.talend.sdk.component:component-api:jar:1.2611.0:provided
// org.acme:acme-client:jar:2.3.4:compile
```

### LCM-008 `Maven repository discovery (m2)`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/service/MavenRepositoryDefaultResolver.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-loading.adoc`)
- **Category / Sub-area**: LCM / Loading
- **Kind**: convention
- **Applies to**: host runtime
- **Description**: `ComponentManager.findM2()` resolves the root repository in order: JVM property `talend.component.manager.m2.repository`; Studio configuration (`osgi.configuration.area` unless `maven.repository=global`); [only if `talend.component.manager.user.m2.fallback=true`] `settings.xml` `<localRepository>` (path from `talend.component.manager.m2.settings`, `~/.m2/settings.xml`, `$MAVEN_HOME/conf/settings.xml`, `$M2_HOME/conf/settings.xml`), `$M2_HOME/repository`; fallback `~/.m2/repository` if the user fallback flag is true, otherwise a non-existent random path (so nothing resolves).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | talend.component.manager.m2.repository | JVM property | unset | root repository path |
  | talend.component.manager.m2.settings | JVM property | unset | settings.xml path (needs user fallback flag) |
  | talend.component.manager.user.m2.fallback | JVM property | false | enable settings.xml / M2_HOME / ~/.m2 fallbacks |
  | maven.repository | JVM property | unset | Studio: `global` = user settings |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST set `talend.component.manager.m2.repository` (or pass the path to the ComponentManager constructor) in production; the default is intentionally not the user's ~/.m2.
- **Server exposure**: none
- **Maturity level**: 0 - no repository, no dependency resolution.
- **Example**: none

### LCM-009 `Nested Maven repository and shade transformers`
- **Source**: `container/nested-maven-repository/src/main/java/org/talend/sdk/component/container/maven/shade/ContainerDependenciesTransformer.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-loading.adoc`)
- **Category / Sub-area**: LCM / Packaging
- **Kind**: convention
- **Applies to**: host packaging
- **Description**: Fat-jar alternative to an external m2: dependencies are embedded under `MAVEN-INF/repository/` (`ConfigurableClassLoader.NESTED_MAVEN_REPOSITORY`). `ContainerDependenciesTransformer` (maven-shade) embeds the repository, `PluginTransformer` writes `TALEND-INF/plugins.properties`. Options: `session` (`${session}`), `scope` (default `provided`), `include`, `exclude`, `userArtifacts`, `includeTransitiveDependencies` (true), `includeProjectComponentDependencies` (false), `repositoryBase` (`MAVEN-INF/repository`), `ignoredPaths`, `pluginListResource` (`TALEND-INF/plugins.properties`). The class loader reads nested jars one level deep (`nested:` URL protocol).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | MAVEN-INF/repository/ | jar folder | - | embedded repo root |
  | TALEND-INF/plugins.properties | resource | - | plugin name -> GAV index |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY package the host runtime as an uber jar with these transformers; then the m2 path becomes irrelevant.
- **Server exposure**: none
- **Maturity level**: 2 - packaging option.
- **Example**: none

### LCM-010 `Component archive (.car)`
- **Source**: `component-tools/src/main/java/org/talend/sdk/component/tools/CarBundler.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-loading.adoc`)
- **Category / Sub-area**: LCM / Packaging
- **Kind**: convention
- **Applies to**: host packaging / deployment
- **Description**: A `.car` is an executable jar produced by `talend-component-maven-plugin:car` (`CarMojo`, properties `talend.car.output`, `talend.car.attach`, `talend.car.classifier`=component, `talend.car.type`=connector|extension, `talend.car.skip`). Layout: folders `TALEND-INF/`, `META-INF/`, `MAVEN-INF/repository/` (all resolved artifacts in Maven layout), `TALEND-INF/metadata.properties` (`date`, `version`, `CarBundlerVersion`, `component_coordinates` (required), `type`, custom metadata) and the class `CarMain` (manifest Main-Class). `CarMain` commands: `studio-deploy <studio> | --location`, `maven-deploy <repo> | --location`, `deploy-to-nexus`, flag `-f` overwrite; `component.java.coordinates` and `component.server.extensions` are read from the metadata. A runtime can unpack `MAVEN-INF/repository/` into its m2 and register `component_coordinates`.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | component_coordinates | metadata.properties key | - | GAV of the main component jar |
  | type | metadata.properties key | connector | connector or extension |

- **Contract for the Designer**: none
- **Contract for the Runtime**: SHOULD support deploying `.car` files (unpack repository, register the main GAV) for remote/engine deployment.
- **Server exposure**: none
- **Maturity level**: 2 - packaging/deployment convenience (remote engine / .car deployment is Level 2).
- **Example**: none

### LCM-011 `Component scanning rules`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/xbean/KnownClassesFilter.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-loading.adoc`)
- **Category / Sub-area**: LCM / Loading
- **Kind**: convention
- **Applies to**: host runtime
- **Description**: For each plugin the manager scans annotations (`@Internationalized`, `@Service`, `@Request`, `@PartitionMapper`, `@Processor`, `@Emitter`, `@DriverRunner`) with xbean-finder on the root module and the jars that carry `TALEND-INF/dependencies.txt` (nested repository supported). Default exclusion list (`KnownClassesFilter`): avro.shaded, com.fasterxml.jackson, com.google.common, io.netty, javax, junit, org.apache, org.slf4j, org.eclipse, org.junit, org.yaml, ... (full list in source). Overrides: resource `TALEND-INF/scanning.properties` with `classes.list` (pre-scanned class names, avoids scanning), `classloader.includes`, `classloader.excludes` (comma patterns) and `classloader.filter.strategy` (`include-exclude` or default exclude-include). Components MUST be public classes. `@Components` (package-info of the class package or a parent package) provides family/categories; without it the annotation `family` attribute is required, otherwise `Missing component`.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | TALEND-INF/scanning.properties | resource | - | keys: classes.list, classloader.includes, classloader.excludes, classloader.filter.strategy |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST NOT scan itself; SHOULD provide `scanning.properties` in fat-jar deployments for startup speed.
- **Server exposure**: none
- **Maturity level**: 1 - correctness is unaffected, startup/registration cost is.
- **Example**: none

### LCM-012 (moved) -> see SVC-027

### LCM-013 (moved) -> see ACT-010

### LCM-014 (moved) -> see SVC-013

### LCM-015 (moved) -> see SVC-014

### LCM-016 (moved) -> see SVC-015

### LCM-017 (moved) -> see SVC-021

### LCM-018 `ContainerListener and JMX exposure`
- **Source**: `container/container-core/src/main/java/org/talend/sdk/component/container/ContainerListener.java`
- **Category / Sub-area**: LCM / Extension points
- **Kind**: interface (SPI)
- **Applies to**: host runtime
- **Description**: `ContainerListener{onCreate(Container), onClose(Container)}` is called by `ContainerManager` for every container (create failure of any listener aborts the deployment). `ContainerListenerExtension` (SVC-025) is loaded by ServiceLoader, sorted by order and registered after the manager's own listener. The design extension registers `DesignContainerListener` to compute the repository model (`RepositoryModel`, `Family`/`Config` tree used by config-type endpoints). `JmxManager` (unless `-Dtalend.component.manager.jmx.skip=true`, pattern `org.talend.sdk.component:type=component,value=%s`) exposes containers as MBeans.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | talend.component.manager.jmx.skip | JVM property | false | do not register JMX beans |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY register listeners for monitoring hooks (checklist level 2).
- **Server exposure**: none
- **Maturity level**: 2 - monitoring/extension hook.
- **Example**: none

### LCM-019 (moved) -> see SVC-026

### LCM-020 (moved) -> see SVC-005

### LCM-021 (moved) -> see DSG-006

