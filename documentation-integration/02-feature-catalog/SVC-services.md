# SVC - Services (host-provided injection) and SPI

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`; last release tag `1.2610.0`).
> Category prefix: `SVC`. Machine-readable twin: [index.SVC.json](index.SVC.json) (generated from this file, both MUST agree).
> Related: [01-overview-and-architecture.md](../01-overview-and-architecture.md), [built-in services appendix](../10-appendix/built-in-services.md), [HTTP client](HTTP-http-client.md), [server](SRV-server.md), [testing](TST-testing.md).

Conventions used in this file:

- API base path (abbreviated `API/` below): `component-api/src/main/java/org/talend/sdk/component/api/`.
- Doc pages (Antora): `documentation/src/main/antora/modules/ROOT/pages/*.adoc`.
- "Designer" = the design-time host (calls the Component Server over HTTP, renders forms). "Runtime" = the run-time host (loads plugins with `ComponentManager`, executes mapper/processor/output). In practice the Designer never instantiates services itself: it reaches them through `POST /api/v1/action/execute` on the Component Server (see [SRV-013](SRV-server.md#srv-013-post-apiv1actionexecute)). A "service" is a plugin-scoped singleton instantiated by `ComponentManager` inside the plugin classloader.
- Sub-areas: Core, Built-in, Studio-only, SPI.
- Not repeated here (primary entry elsewhere): `@HealthCheck`/`HealthCheckStatus` (ACT-003), `@DynamicDependencies` (ACT-010), `@CreateConnection`/`@CloseConnection`/`@Connection` (ACT-011, ACT-012), `ComponentMetadataEnricher` SPI (DSG-006), `ParameterExtensionEnricher` SPI (CFG-015), `RecordBuilderFactory`/`Record`/`Schema` (DAT category), interceptor mechanism `@Intercepts` (INT category).

---

### SVC-001 @Service
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/Service.java` (+ doc `documentation/src/main/antora/modules/ROOT/pages/index-defining-services.adoc`, `services-actions.adoc`)
- **Category / Sub-area**: SVC / Core
- **Kind**: annotation (`@Target({TYPE, FIELD})`, `@Retention(RUNTIME)`)
- **Applies to**: service classes; fields of services or of components (injection points)
- **Description**: Marks a public class as a plugin-scoped singleton service that can be injected by type into components (constructor parameters) and into other services (fields annotated `@Service`, or constructor parameters). The class MUST be `public`. Services MUST be stateless and thread-safe because they are singletons and may be serialized at any time; state lives in the component.
- **Attributes**: none.
- **Contract for the Designer**: none (services are reached only through actions, see ACT-002).
- **Contract for the Runtime**: MUST scan each plugin for `@Service` classes, instantiate one instance per plugin, inject by type into components and services, and honor the lifecycle in SVC-027.
- **Server exposure**: none directly; `@Service` methods carrying an action-type annotation are exposed via `/action/index` and `/action/execute`.
- **Maturity level**: 0 - components cannot be constructed if their service constructor parameters are not injected.
- **Example**:

```java
@Service
public class MyDbTester {
    @Action(family = "mycomp", value = "test")
    public Status doTest(final IncomingData data) { return null; }
}
```

### SVC-002 BaseService
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/BaseService.java` (+ `component-runtime-manager/.../service/ServiceHelper.java`)
- **Category / Sub-area**: SVC / Core
- **Kind**: interface-like base class (`public class BaseService implements Serializable`)
- **Applies to**: service
- **Description**: Optional base class for services that must be `Serializable` and participate in the Component Manager serialization system. It holds a `Serial serializationHelper` and implements a final `writeReplace()` returning it; it throws `IllegalArgumentException("Serialization not found for <class>")` if the helper is unset. `ServiceHelper.createServiceInstance` sets the helper (a `SerializableService(pluginId, serviceClassName)`) when the instance is a `BaseService`.
- **Attributes**: `serializationHelper` (`Serial`, set by the framework, getter/setter).
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST set the `serializationHelper` on every `BaseService` instance it creates so distributed engines can serialize and re-resolve the service on workers.
- **Server exposure**: none.
- **Maturity level**: 1 - only needed when services are serialized (distributed engines); local execution works without it.
- **Example**:

```java
@Service
public class MyService extends BaseService { /* stateless logic */ }
```

### SVC-003 Serial
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/serialization/Serial.java` (+ `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/serialization/SerializableService.java`)
- **Category / Sub-area**: SVC / Core
- **Kind**: interface (`Serial extends Serializable`, method `Object readResolve() throws ObjectStreamException`)
- **Applies to**: service
- **Description**: Serialization helper contract used by `BaseService`. The runtime implementation (`SerializableService`) re-resolves the live service instance from the plugin container (`plugin` + service key) on deserialization, which is why injected built-in services (LocalConfiguration, LocalCache, Injector, HttpClientFactory, Resolver, ObjectFactory, ProducerFinder) return `SerializableService` from `writeReplace()`. Merged from RUN-051: `ServiceHelper` sets `serializationHelper` automatically on `BaseService` subclasses (SVC-002) created by the manager, so a service reference deserialized on a worker resolves to the worker's own container instance.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST make deserialized service references resolve to the target worker's plugin instance (via `ContainerFinder`, SPI `org.talend.sdk.component.runtime.serialization.ContainerFinder`); MUST NOT ship service state across the wire. A custom container implementation MUST supply an equivalent replacement for non-Serializable services (or make services extend `BaseService`, SVC-002).
- **Server exposure**: none.
- **Maturity level**: 1 - required by distributed execution (Beam), not by a single-JVM runtime.
- **Example**:

```java
// Java serialization of an injected service yields a SerializableService(plugin, className);
// readResolve() looks the service up again in the plugin container on the worker.
```

### SVC-004 (moved) -> see ACT-002

### SVC-005 LocalConfiguration
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/configuration/LocalConfiguration.java` (+ doc `services-built-in.adoc`; `component-runtime-manager/.../service/LocalConfigurationService.java`, `DefaultServiceProvider.java`, `ComponentManager.createRawLocalConfigurations`)
- **Category / Sub-area**: SVC / Built-in
- **Kind**: interface (`String get(String key)`, `Set<String> keys()`); injectable service; also a `ServiceLoader` SPI for additional providers
- **Applies to**: component, service
- **Description**: Read-only view on the host environment configuration, scoped per plugin. Lookup order for `get(key)`: for each delegate in order, `<pluginId>.<key>` then (on miss for all delegates) the raw `key`; each read retries with `.` replaced by `_`. Delegates in order: `LocalConfiguration` providers found by `ServiceLoader` (unless system property `talend.component.manager.localconfiguration.skip=true`), JVM system properties, environment variables (raw key, then non-alphanumerics replaced by `_`, then upper-cased), and the aggregated `TALEND-INF/local-configuration.properties` files of the plugin (added last; sorted by their `_ordinal` property; skipped when system property `talend.component.configuration.<pluginId>.ignoreLocalConfiguration=true`). The syntax `local_configuration:<key>` in a `@DefaultValue`/`@Ui`-family annotation string attribute is resolved at metadata build time (`UiParameterEnricher`), so the Designer receives already-resolved values.
- **Attributes**: none (interface methods above).
- **Contract for the Designer**: MUST NOT resolve `local_configuration:` itself if it talks to a Component Server (values arrive resolved); MAY expose server-side configuration through the server's own `LocalConfiguration` SPI (`VirtualDependenciesService$LocalConfigurationImpl`).
- **Contract for the Runtime**: MUST inject a `LocalConfiguration` scoped to the plugin id, honoring the lookup order above; SHOULD NOT rely on it for runtime-critical values because runtime and design-time environments differ. SHOULD provide plugin settings (retry strategy, `_maxBatchSize`, streaming caps, cache eviction) through system properties or environment variables using the documented keys (merged from LCM-020).
- **Server exposure**: resolved values in property `metadata` / `defaultValue` in `GET /component/details`.
- **Maturity level**: 0 - `@Service`/component code injecting it fails without it, and defaults with `local_configuration:` need it.
- **Example**:

```java
@Option
@DefaultValue("local_configuration:myfamily.model.key")
private String value;
```

### SVC-006 @Configuration
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/configuration/Configuration.java` (+ `component-runtime-manager/.../service/InjectorImpl.java`)
- **Category / Sub-area**: SVC / Built-in
- **Kind**: annotation (`@Target({FIELD, PARAMETER})`)
- **Applies to**: service field, component constructor parameter
- **Description**: Injects a POJO built from `LocalConfiguration` keys sharing a prefix, using the same rules as any `@Option` configuration object. In a service the field type MUST be `Supplier<X>` (so the value stays current); otherwise `IllegalArgumentException("Field ... is not a Supplier<X> ...")` is thrown.
- **Attributes**:

| Name | Type | Default | Meaning |
|---|---|---|---|
| `value` | `String` | required | Prefix of the `LocalConfiguration` keys to map into the object |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST support `@Configuration("prefix") Supplier<X>` on service fields (re-evaluated at each `get()`) and `@Configuration("prefix") X` on constructor parameters.
- **Server exposure**: none.
- **Maturity level**: 1 - used by services for tuning (e.g. the built-in cache config `talend.component.manager.services.cache.eviction`); not needed by all components.
- **Example**:

```java
@Service
public class ConfiguredService {
    @Configuration("myprefix")
    private Supplier<MyConfig> config;
}
```

### SVC-007 Injector
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/injector/Injector.java` (+ `component-runtime-manager/.../service/InjectorImpl.java`)
- **Category / Sub-area**: SVC / Built-in
- **Kind**: interface (`<T> T inject(T instance)`)
- **Applies to**: component, service
- **Description**: Injects services into an unmanaged instance: every non-static field annotated `@Service` (searched up the class hierarchy) is set by declared field type; a field typed `Collection<X>` receives the matching service entries when no exact type matches; fields annotated `@Configuration` are set to a `Supplier`. Proxied services (`$$TalendServiceProxy`) are unwrapped before injection.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST provide an injector that resolves against the plugin's service map.
- **Server exposure**: none.
- **Maturity level**: 1 - used by components that create helper objects; core injection is done by SVC-027.
- **Example**:

```java
final MyHelper helper = injector.inject(new MyHelper()); // MyHelper has @Service fields
```

### SVC-008 LocalCache
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/cache/LocalCache.java` (+ `component-runtime-manager/.../service/LocalCacheService.java`)
- **Category / Sub-area**: SVC / Built-in
- **Kind**: interface + nested `LocalCache.Element`
- **Applies to**: component, service
- **Description**: In-memory, per-plugin key/value cache (`ConcurrentHashMap`, keys internally prefixed `<plugin>@<key>`). Methods: `computeIfAbsent(Class, key, Predicate<Element> toRemove, long timeoutMs, Supplier)`, `computeIfAbsent(Class, key, Predicate<Element>, Supplier)`, `computeIfAbsent(Class, key, long timeoutMs, Supplier)`, `computeIfAbsent(Class, key, Supplier)`, `evict(key)`, `evictIfValue(key, expected)`. `Element` exposes `getValue(Class)`, default `getValue()`, `getLastValidityTimestamp()`. Timeouts `<= 0` mean no expiry; expiry is scheduled on a 4-thread scheduler and `computeIfAbsent` re-creates elements whose validity elapsed and whose `toRemove` predicate allows removal. A wrong `expectedClass` throws `ClassCastException`.
- **Attributes**: tuning is read from `LocalConfiguration` prefix `talend.component.manager.services.cache.eviction` (`CacheConfiguration`: `defaultEvictionTimeout` `long` ms, `maxDeletionPerEvictionRun` `int`, `defaultMaxSize` `int`; unset means unlimited, `-1`).
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST inject a cache per plugin and release it on plugin undeploy (`@PreDestroy release()`); SHOULD keep default unlimited behaviour unless configured.
- **Server exposure**: none (distinct from the server's HTTP-level JCache, see [SRV-017](SRV-server.md#srv-017-response-caching-and-invalidation)).
- **Maturity level**: 0 - built-in service listed in the reference table; components/actions inject it directly and `@Cached` depends on it.
- **Example**:

```java
final Foo foo = cache.computeIfAbsent(Foo.class, "foo-key", 60_000L, () -> loadFoo());
```

### SVC-009 @Cached
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/cache/Cached.java` (+ `component-runtime-manager/.../interceptor/CacheHandler.java`)
- **Category / Sub-area**: SVC / Built-in
- **Kind**: annotation (`@Target({TYPE, METHOD})`, meta-annotated `@Intercepts(InterceptorHandler.class)`)
- **Applies to**: service methods (or whole service)
- **Description**: Interceptor that caches the result of a service method in `LocalCache`. The cache key is `declaringClass#method(arg/hashCode,...)` (assumes `toString()`/`hashCode()` of arguments are representative); the timeout is `Cached.timeout()`. The interceptor mechanism itself belongs to category INT. Merged from INT-004: `Cached` is itself meta-annotated `@Intercepts(InterceptorHandler.class)` (built-in, INT-001); the handler `CacheHandler` stores results in the `LocalCache` service (SVC-008) under `<declaringClass>#<method>(<arg>/<hash>,...)` with the method's `timeout`. The reference `LocalCacheService` reads the config prefix `talend.component.manager.services.cache.eviction` (`defaultEvictionTimeout`, `maxDeletionPerEvictionRun`, `defaultMaxSize`). Interceptor mechanism: INT-003.
- **Attributes**:

| Name | Type | Default | Meaning |
|---|---|---|---|
| `timeout` | `long` | `Integer.MAX_VALUE` | Cache TTL in milliseconds |

- **Contract for the Designer**: none
- **Contract for the Runtime**: SHOULD wrap services with interceptors (proxy) so `@Cached` is honored; if unsupported, the method MUST still execute correctly (uncached). MUST provide `LocalCache` (reference `LocalCacheService`).
- **Server exposure**: none.
- **Maturity level**: 1 - degraded performance only if omitted, not wrong results.
- **Example**:

```java
@Cached(timeout = 30_000)
public Values loadValues(final String id) { return doLoad(id); }
```

### SVC-010 (moved) -> see DAT-017

### SVC-011 (moved) -> see DAT-020

### SVC-012 ProducerFinder
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/source/ProducerFinder.java` (+ `component-runtime-manager/.../service/ProducerFinderImpl.java`; SPI override `component-runtime-beam/.../BeamProducerFinder`)
- **Category / Sub-area**: SVC / Built-in
- **Kind**: interface (`Serializable`); injectable service; `ServiceLoader` SPI
- **Applies to**: component, service
- **Description**: Retrieves an `Iterator<Record>` from a configured dataset of any deployed input connector. `init(String plugin, Object builder, Function<Object, Record> converter)` is called by the host; `find(String familyName, String inputName, int version, Map<String,String> configuration)` locates the `@PartitionMapper`/`@Emitter` by family + name, instantiates it with the flat configuration map and version, then iterates its `Input` (start on first `hasNext`, stop at exhaustion). Assumes a finite producer with no extra dataset configuration. If several `ProducerFinder` SPI implementations exist, the first is used and a warning is logged; otherwise `ProducerFinderImpl` is used. Unknown family/input raises `IllegalArgumentException("Can't find <input> for family <family>.")`.
- **Attributes**: `find` parameters above.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY provide it; if provided MUST resolve mappers across all deployed plugins and MUST convert emitted objects to `Record`.
- **Server exposure**: none.
- **Maturity level**: 2 - optional cross-connector capability.
- **Example**:

```java
final Iterator<Record> it = producerFinder.find("myfamily", "reader", 1, configurationMap);
```

### SVC-013 Resolver
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/dependency/Resolver.java` (+ `component-runtime-manager/.../service/ResolverImpl.java`)
- **Category / Sub-area**: SVC / Built-in
- **Kind**: interface + nested `Resolver.ClassLoaderDescriptor extends AutoCloseable`
- **Applies to**: component, service
- **Description**: Resolves Maven coordinates (from a `dependencies.txt`-style descriptor or a `List<String>` of `groupId:artifactId:version[...]`) to local files or to a fresh classloader. Methods: `ClassLoaderDescriptor mapDescriptorToClassLoader(InputStream)`, `... (List<String> gavs)`, `... (InputStream, ClassLoaderDefinition)` (default throws `UnsupportedOperationException`), `... (List<String>, ClassLoaderDefinition)`, `Collection<File> resolveFromDescriptor(InputStream)`, `resolveFromDescriptor(List<String>)`. `ClassLoaderDescriptor.asClassLoader()` and `resolvedDependencies()`; callers MUST `close()` the descriptor to avoid leaks. Files are resolved against the plugin's local Maven repository (`container.getLocalDependencyRelativeResolver()`); artifacts present nested under `MAVEN-INF/repository/` in the parent are also supported. Default classloader: parent = parent of the context loader, classes filter accepts all, parent-classes filter rejects all, resource dependencies enabled. The `@DynamicDependencies` action that computes the GAV list to resolve is ACT-010 in [ACT-actions.md](ACT-actions.md). Merged from LCM-014: the default `ClassLoaderDefinition` uses `classesFilter` = all, `parentClassesFilter` = none (child-first) and `parentResourcesFilter` = all; instances serialize as `SerializableService` (SVC-003); the InputStream or descriptor MUST be closed to avoid leaks; the GAV list typically comes from a `@DynamicDependencies` action (ACT-010).
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST inject a `Resolver` bound to the plugin repository and MUST create an isolated `ConfigurableClassLoader` for `mapDescriptorToClassLoader`.
- **Server exposure**: none (dynamic dependencies: see ACT-010 in [ACT-actions.md](ACT-actions.md)).
- **Maturity level**: 1 - required by connectors loading drivers at runtime; absent otherwise.
- **Example**:

```java
try (Resolver.ClassLoaderDescriptor d = resolver.mapDescriptorToClassLoader(asList("org.apache.derby:derbyclient:jar:10.12.1.1"))) {
    final ClassLoader loader = d.asClassLoader();
}
```

### SVC-014 ClassLoaderDefinition
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/dependency/ClassLoaderDefinition.java`
- **Category / Sub-area**: SVC / Built-in
- **Kind**: interface
- **Applies to**: service
- **Description**: Configuration passed to `Resolver.mapDescriptorToClassLoader(..., ClassLoaderDefinition)`. Merged from LCM-015: `ContainerManager.ClassLoaderConfiguration` is the reference implementation; the `getParentResourcesFilter()` predicate receives the resource URL file/path, not the logical resource name.
- **Attributes**:

| Method | Type | Meaning |
|---|---|---|
| `getParent()` | `ClassLoader` | Parent of the created loader |
| `getClassesFilter()` | `Predicate<String>` | If false for a class, load from parent |
| `getParentClassesFilter()` | `Predicate<String>` | If false for a class, the new loader loads it itself instead of delegating |
| `getParentResourcesFilter()` | `Predicate<String>` | Same for resources; argument is the `URL#getFile()` path |
| `isSupportsResourceDependencies()` | `boolean` | Dependencies list may contain resources |
| `getNestedPluginMappingResource()` | `String` | Resource path of the plugins mapping (`TALEND-INF/plugins.properties`) |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY support it; an implementation that does not MUST throw `UnsupportedOperationException` (the API default).
- **Server exposure**: none.
- **Maturity level**: 2 - advanced classloader control.
- **Example**:

```java
resolver.mapDescriptorToClassLoader(gavs, definition);
```

### SVC-015 ObjectFactory
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/factory/ObjectFactory.java` (+ `component-runtime-manager/.../service/ObjectFactoryImpl.java`)
- **Category / Sub-area**: SVC / Built-in
- **Kind**: interface + nested `ObjectFactory.ObjectFactoryInstance`
- **Applies to**: service
- **Description**: Creates an instance from a class name and a property map with type coercion (XBean `ObjectRecipe`). `ObjectFactoryInstance createInstance(String className)`; builder methods `withFieldInjection()` (enables field injection including private), `withoutFieldInjection()`, `ignoreUnknownProperties()`, `withProperties(Map<String,?>)`, `withProperties(Stream<T>, keyExtractor, valueExtractor)` (last key wins), `<T> T create(Class<T> parentType)` (wraps failures in `IllegalArgumentException`; property names are matched case-insensitively when unambiguous). It does not use the `@Option` configuration format. Merged from LCM-016: by default only non-private fields are injected (`withFieldInjection()` extends this to private ones); `create` fails on unknown properties unless `ignoreUnknownProperties()` is called; it is not the `@Option` configuration format and can be used to convert UI properties to an instance.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST provide it when hosting components that inject it.
- **Server exposure**: none.
- **Maturity level**: 2 - optional convenience service.
- **Example**:

```java
final MyBean bean = factory.createInstance(MyBean.class.getName()).withProperties(props).create(MyBean.class);
```

### SVC-016 JSON-P services
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/service/DefaultServiceProvider.java` (+ doc `services-built-in.adoc`)
- **Category / Sub-area**: SVC / Built-in
- **Kind**: injectable services (types from `javax.json`)
- **Applies to**: component, service
- **Description**: The host MUST inject serializable, memory-optimized JSON-P instances for the types `javax.json.spi.JsonProvider` (a `PreComputedJsonpProvider`), `javax.json.JsonBuilderFactory`, `javax.json.JsonWriterFactory`, `javax.json.JsonReaderFactory`, `javax.json.stream.JsonParserFactory`, `javax.json.stream.JsonGeneratorFactory`. They are wrapped as serializable proxies scoped to the plugin id. Components should prefer these over creating their own.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST inject these six types by exact type; injected instances MUST be `Serializable`.
- **Server exposure**: none.
- **Maturity level**: 0 - ubiquitous in components (JSON handling) and used by the HTTP client codecs.
- **Example**:

```java
public MyService(final JsonBuilderFactory json) { this.json = json; }
```

### SVC-017 Jsonb
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/service/DefaultServiceProvider.java`, `GenericOrPojoJsonb.java`
- **Category / Sub-area**: SVC / Built-in
- **Kind**: injectable service (`javax.json.bind.Jsonb`)
- **Applies to**: component, service
- **Description**: A JSON-B instance that serializes plain POJOs and also handles generic JSON-P/`Record` values (`GenericOrPojoJsonb` delegates to a JSON-B configured with the shared `JsonProvider` and a second POJO-oriented JSON-B whose generator factory is `RecordJsonGenerator.Factory`).
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST inject a `Jsonb` per plugin sharing the JSON-P buffers.
- **Server exposure**: none.
- **Maturity level**: 0 - used by `HttpClientFactory` and by most connectors.
- **Example**:

```java
final String json = jsonb.toJson(myPojo);
```

### SVC-018 ContainerInfo and ProxyGenerator (internal services)
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/service/ContainerInfo.java`, `.../asm/ProxyGenerator.java`, `DefaultServiceProvider.java`
- **Category / Sub-area**: SVC / Built-in
- **Kind**: injectable internal services (not in `component-api`)
- **Applies to**: service
- **Description**: `ContainerInfo` (`@Data`, `Serializable`, field `containerId`) exposes the plugin id; `ProxyGenerator` returns the manager's ASM proxy generator. Both are resolvable from the services map but are not part of the public API contract (unverified stability).
- **Attributes**: `ContainerInfo.containerId` (`String`).
- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY expose them; hosts not built on `component-runtime-manager` can omit them.
- **Server exposure**: none.
- **Maturity level**: 2 - internal convenience.
- **Example**:

```java
final String plugin = containerInfo.getContainerId();
```

### SVC-019 @RuntimeContext
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/context/RuntimeContext.java` (+ `component-studio/component-runtime-di/.../studio/RuntimeContextInjector.java`)
- **Category / Sub-area**: SVC / Studio-only
- **Kind**: annotation (`@Target(FIELD)`, `@Documentation("Auto inject the runtime to field, The functionality is for the Studio only.")`)
- **Applies to**: component field (input, processor, standalone) and service field
- **Description**: A field of type `RuntimeContextHolder` annotated `@RuntimeContext` is set by the Studio DI runtime (`RuntimeContextInjector.injectLifecycle` for runners, `injectService` for services/connection/close helpers, scanning superclasses). Other hosts do not provide it. The related Studio-only `@Connection` field injection and the `@CreateConnection`/`@CloseConnection` actions are ACT-011 and ACT-012 in [ACT-actions.md](ACT-actions.md).
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: A Studio-compatible runtime MUST inject the holder into annotated fields before `start`; other runtimes MAY leave the field null and components MUST tolerate that.
- **Server exposure**: none.
- **Maturity level**: 2 - applies to Studio only; lost optional capability elsewhere.
- **Example**:

```java
@RuntimeContext
private transient RuntimeContextHolder context;
```

### SVC-020 RuntimeContextHolder
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/context/RuntimeContextHolder.java`
- **Category / Sub-area**: SVC / Studio-only
- **Kind**: class (`Serializable`, `@AllArgsConstructor`, `@Getter`)
- **Applies to**: Studio runtime
- **Description**: Key/value holder shared between Studio components. `getGlobal/setGlobal(key)` use the raw key; `get/set(key)` prefix the key with `<connectorId>_`.
- **Attributes**:

| Name | Type | Meaning |
|---|---|---|
| `connectorId` | `String` | Prefix for scoped keys |
| `map` | `Map<String,Object>` | Backing store (globals and scoped entries) |

- **Contract for the Designer**: none
- **Contract for the Runtime**: Studio runtime MUST create one per connector instance sharing the global map.
- **Server exposure**: none.
- **Maturity level**: 2 - Studio only.
- **Example**:

```java
context.set("conn", connection); // stored under "<connectorId>_conn"
```

### SVC-021 ComponentExtension
- **Source**: `component-spi/src/main/java/org/talend/sdk/component/spi/component/ComponentExtension.java`
- **Category / Sub-area**: SVC / SPI
- **Kind**: interface, `ServiceLoader` SPI (requires `ComponentManager` to be active)
- **Applies to**: host / framework extension
- **Description**: Hook into component scanning and conversion, used e.g. by `BeamComponentExtension` and `BeamDiExtension`. Methods: `default boolean isActive()` (true), `void onComponent(ComponentContext)`, `boolean supports(Class<?> componentType)`, `<T> T convert(ComponentInstance, Class<T> component)`, `default Map<Class<?>,Object> getExtensionServices(String plugin)` (empty), `default int priority()` (`Integer.MAX_VALUE`, smaller wins, first supporting extension wins), `default <T> T unwrap(Class<T>, Object...)`, `default Collection<ClassFileTransformer> getTransformers()`, `default Collection<String> getAdditionalDependencies()`. Merged from LCM-017: extensions are loaded by `ServiceLoader` on the thread context classloader and only active ones (`isActive()`) are sorted by `priority()`; `GenericComponentExtension` (SVC-024) replaces scanning; `BeamComponentExtension` is the reference implementation (it skips validation for PTransforms and unwraps `FlowsFactory`). Nested types: SVC-022, SVC-023.
- **Attributes**: see methods above.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY support; if supported MUST load via `ServiceLoader` with the manager classloader, sort by `priority()`, call `onComponent` for every scanned component class, and merge `getExtensionServices` into the plugin's services when the extension owns the component. MUST NOT expect extensions to run without `ComponentManager`.
- **Server exposure**: none (the server uses `component-server-extension-api`, see [SRV-023](SRV-server.md#srv-023-server-configuration-keys-summary)).
- **Maturity level**: 2 - needed only for non-native programming models.
- **Example**:

```java
public class MyExtension implements ComponentExtension {
    public void onComponent(ComponentContext c) { if (c.getType().isAnnotationPresent(Foo.class)) c.skipValidation(); }
    public boolean supports(Class<?> t) { return Mapper.class == t; }
    public <T> T convert(ComponentInstance i, Class<T> t) { return t.cast(wrap(i.instance())); }
}
```

### SVC-022 ComponentExtension.ComponentContext
- **Source**: `component-spi/src/main/java/org/talend/sdk/component/spi/component/ComponentExtension.java` (nested interface)
- **Category / Sub-area**: SVC / SPI
- **Kind**: interface
- **Applies to**: extension callback
- **Description**: Handle given to `onComponent`: `Class<?> getType()`, `void skipValidation()` (component is not usable through `findMapper()/findProcessor()` and validation is disabled, so `@PartitionMapper`/`@Processor` can target another runtime), `ComponentExtension owningExtension()` (null if none).
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST provide a per-component context to each extension.
- **Server exposure**: none.
- **Maturity level**: 2 - extension authors only.
- **Example**:

```java
context.skipValidation();
```

### SVC-023 ComponentExtension.ComponentInstance
- **Source**: `component-spi/src/main/java/org/talend/sdk/component/spi/component/ComponentExtension.java` (nested interface)
- **Category / Sub-area**: SVC / SPI
- **Kind**: interface
- **Applies to**: extension callback
- **Description**: Describes the instantiated component passed to `convert`: `Object instance()`, `String plugin()`, `String family()`, `String name()`.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST pass the native instance and its coordinates to the extension.
- **Server exposure**: none.
- **Maturity level**: 2 - extension authors only.
- **Example**:

```java
final Object nativeInstance = instance.instance();
```

### SVC-024 GenericComponentExtension
- **Source**: `component-spi/src/main/java/org/talend/sdk/component/spi/component/GenericComponentExtension.java`
- **Category / Sub-area**: SVC / SPI
- **Kind**: interface (marked `// @Internal`), `ServiceLoader` SPI
- **Applies to**: virtual component family
- **Description**: Creates all components of a family the same way without scanning annotated classes. `boolean canHandle(Class<?> expectedType, String plugin, String name)`; `<T> T createInstance(Class<T> type, String plugin, String name, int version, Map<String,String> configuration, Map<Class<?>,Object> services)`. At most one per plugin: two implementations raise `IllegalArgumentException("A component can't have two generic component extensions")`; when present the manager does NOT scan `@PartitionMapper`/`@Processor`/`@Emitter`/`@DriverRunner`.
- **Attributes**: see methods.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY support; if so MUST consult it before standard lookup and enforce the single-implementation rule.
- **Server exposure**: none.
- **Maturity level**: 2 - internal/virtual components only.
- **Example**:

```java
public boolean canHandle(Class<?> t, String plugin, String name) { return "virtual".equals(plugin); }
```

### SVC-025 ContainerListenerExtension
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/spi/ContainerListenerExtension.java`
- **Category / Sub-area**: SVC / SPI
- **Kind**: interface (extends `ContainerListener`), `ServiceLoader` SPI
- **Applies to**: host
- **Description**: Listens to plugin container creation/closing: `default void setComponentManager(ComponentManager)` and `default int order()` (0; sorted ascending) in addition to `ContainerListener` callbacks. Used e.g. by `component-runtime-design-extension` (`DesignContainerListener`).
- **Attributes**: see methods.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY support (only relevant if reusing `component-runtime-manager`).
- **Server exposure**: none.
- **Maturity level**: 2 - extension point.
- **Example**:

```java
public int order() { return 10; }
```

### SVC-026 ContainerClasspathContributor and ComponentManager.Customizer
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/ComponentManager.java` (nested interface, marked internal extension point)
- **Category / Sub-area**: SVC / SPI
- **Kind**: interface, `ServiceLoader` SPI (skipped when system property `talend.component.manager.classpathcontributor.skip=true`)
- **Applies to**: host
- **Description**: Contributes extra artifacts to a plugin classpath: `Collection<Artifact> findContributions(String pluginId)`, `boolean canResolve(String path)`, `Path resolve(String path)`. The Component Server registers one (`VirtualDependenciesService`) to serve user extension jars. Merged from LCM-019: `ComponentManager.Customizer` (`ServiceLoader` SPI, WARNING: advanced) provides `containerClassesAndPackages()`, `parentResources()`, `ignoreBeamClassLoaderExclusions()` and `ignoreDefaultDependenciesDescriptor()` (use an EmptyResolver), plus the deprecated `setCustomizers`; Beam registers `BeamCustomizer` (class index of Beam packages).
- **Attributes**: see methods.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY support; WARNING internal. MAY implement a `Customizer` to integrate with a host-specific class/dependency layout.
- **Server exposure**: contributed artifacts appear in `GET /component/dependencies`.
- **Maturity level**: 2 - internal extension point.
- **Example**:

```java
public boolean canResolve(String path) { return path.startsWith("org/myco/"); }
```

### SVC-027 Service instantiation, injection and lifecycle order
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/ComponentManager.java` (`ContainerModelBuilder`), `service/ServiceHelper.java`
- **Category / Sub-area**: SVC / Core
- **Kind**: convention
- **Applies to**: plugin container
- **Description**: Per plugin the manager creates a lazy services map (`LazyMap`) resolving built-in types on first access (see [built-in services appendix](../10-appendix/built-in-services.md)), then in order: (1) creates `@Internationalized` proxies, (2) creates one HTTP client per `HttpClient` interface that has `@Request` methods (`HttpClientFactory.create(proxy, null)`, HTTP-002), (3) instantiates every `@Service` class (wrapping in a generated proxy when it has interceptors or is not serializable), (4) injects `@Service` fields via `Injector`, invokes `@PostConstruct`, then registers action metadata for annotated methods, (5) scans components. On plugin close, `@PreDestroy` is invoked on every registered service. Merged from LCM-012: the lazy service map (24 initial slots) is resolved by `DefaultServiceProvider` (JSON-P/JSON-B factories, `Jsonb`, `LocalConfiguration`, `RecordBuilderFactory`, `LocalCache`, `Injector`, `HttpClientFactory`, `Resolver`, `ObjectFactory`, `ProducerFinder`, `RecordService`, `RecordPointerFactory`, `ContainerInfo`, `ProxyGenerator`); action methods (methods carrying an `@ActionType`-meta annotation) are registered as `ServiceMeta.ActionMeta` (return type checked against `ActionType.expectedReturnedType`; family from the annotation or the package `@Components`, else the error `No component for <method>, maybe add a @Components on your package`; action name from `name`/`value`, else `default`); on close `@PreDestroy` runs on non-proxy services and `Jsonb.close()` is called; JVM property `talend.component.configuration.<containerId>.ignoreLocalConfiguration=true` disables the local-configuration resources of that container.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST follow this order so `@PostConstruct` sees injected services; MUST call `@PreDestroy` on undeploy. MUST expose the built-in service set when replacing `DefaultServiceProvider`; MUST run `@PostConstruct` of services once per container start.
- **Server exposure**: none.
- **Maturity level**: 0 - wrong behaviour if services are not initialized before components.
- **Example**:

```java
@Service
public class MyService {
    @Service private LocalCache cache;
    @PostConstruct void init() { /* cache is injected */ }
}
```

