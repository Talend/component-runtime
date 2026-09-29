# Appendix - Built-in services

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`; last release tag `1.2610.0`).
> Normative reference for what a Runtime host MUST make injectable in a plugin container. Feature entries: [SVC](../02-feature-catalog/SVC-services.md), [HTTP](../02-feature-catalog/HTTP-http-client.md). Overview: [../01-overview-and-architecture.md](../01-overview-and-architecture.md).
> Sources: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/service/DefaultServiceProvider.java` (the lookup implementation, code beats prose), `.../service/DefaultServices.java`, `.../ComponentManager.java`, doc `documentation/src/main/antora/modules/ROOT/pages/services-built-in.adoc`.

## 1. Injection rules

1. Injection is **by exact declared type**. Components receive services as constructor parameters; services receive them as constructor parameters or `@Service`-annotated fields (SVC-001, SVC-027). A field typed `Collection<X>` receives all services assignable to `X` when no exact match exists (`InjectorImpl`).
2. Each plugin container has its **own** service map (a lazy map keyed by `Class`, built on first access from `DefaultServiceProvider.lookup(pluginId, ...)`). Services are per plugin, not shared across plugins.
3. Built-in services returned by the manager are **serializable** (either `Serializable` implementations returning a `SerializableService(plugin, className)` from `writeReplace()`, or serializable proxies built by `JavaProxyEnricherFactory`). Serialized references re-resolve in the target JVM through `ContainerFinder`. Hosts MUST preserve this property when running on distributed engines (SVC-003).
4. Unknown types return `null` from the built-in lookup (no exception); user `@Service` classes are then searched, so a component requesting an unknown type fails at injection.
5. Services are singletons: they MUST be thread-safe and stateless (state is held by components). `@PostConstruct` runs after injection; `@PreDestroy` on plugin undeploy.
6. A global (plugin-less) lookup exists for a few types (`DefaultServices.lookup`, section 3).

## 2. Built-in services table

| # | Type (exact injection type) | Provider class in `component-runtime-manager` | Serializable form | Feature IDs | Level |
|---|---|---|---|---|---|
| 1 | `org.talend.sdk.component.api.service.configuration.LocalConfiguration` | `service.LocalConfigurationService` (delegates: SPI providers, system properties, environment, `TALEND-INF/local-configuration.properties`) | `SerializableService` | SVC-005 | 0 |
| 2 | `org.talend.sdk.component.api.service.cache.LocalCache` | `service.LocalCacheService` (4-thread scheduler shared by the manager) | `SerializableService` | SVC-008, SVC-009 | 0 |
| 3 | `org.talend.sdk.component.api.service.injector.Injector` | `service.InjectorImpl` | `SerializableService` | SVC-007 | 1 |
| 4 | `org.talend.sdk.component.api.service.dependency.Resolver` | `service.ResolverImpl` | `SerializableService` | SVC-013, SVC-014, ACT-010 | 1 |
| 5 | `org.talend.sdk.component.api.service.factory.ObjectFactory` | `service.ObjectFactoryImpl` | `SerializableService` | SVC-015 | 2 |
| 6 | `org.talend.sdk.component.api.service.source.ProducerFinder` | SPI `ProducerFinder` if present (first wins, warning if several; `BeamProducerFinder` in `component-runtime-beam`) else `service.ProducerFinderImpl` | `SerializableService` | SVC-012 | 2 |
| 7 | `org.talend.sdk.component.api.service.record.RecordBuilderFactory` | `recordBuilderFactoryProvider.apply(pluginId)` (see DAT catalog) | serializable | (DAT) | 0 |
| 8 | `org.talend.sdk.component.api.service.record.RecordService` | `service.RecordServiceImpl` | serializable | DAT-017 | 1 |
| 9 | `org.talend.sdk.component.api.record.RecordPointerFactory` | `service.RecordPointerFactoryImpl` | serializable | DAT-020 | 1 |
| 10 | `javax.json.spi.JsonProvider` | `json.PreComputedJsonpProvider` | serializable | SVC-016 | 0 |
| 11 | `javax.json.JsonBuilderFactory` | serializable proxy over the shared factory | serializable proxy | SVC-016 | 0 |
| 12 | `javax.json.JsonReaderFactory` | same | serializable proxy | SVC-016 | 0 |
| 13 | `javax.json.JsonWriterFactory` | same | serializable proxy | SVC-016 | 0 |
| 14 | `javax.json.stream.JsonParserFactory` | same | serializable proxy | SVC-016 | 0 |
| 15 | `javax.json.stream.JsonGeneratorFactory` | same | serializable proxy | SVC-016 | 0 |
| 16 | `javax.json.bind.Jsonb` | `service.GenericOrPojoJsonb` (generic JSON-P/Record plus POJO JSON-B) | serializable | SVC-017 | 0 |
| 17 | `org.talend.sdk.component.api.service.http.HttpClientFactory` | `service.http.HttpClientFactoryImpl` | `SerializableService` | HTTP-001 | 0 |
| 18 | Every interface extending `org.talend.sdk.component.api.service.http.HttpClient` that has `@Request` methods | proxy created by `HttpClientFactory.create(proxy, null)` at plugin load and registered in the service map | serializable proxy (`SerializationHandlerReplacer`) | HTTP-002 | 0 |
| 19 | Every `@Internationalized` interface found in the plugin | proxy from `internationalizationServiceFactory.create` wrapped by `JavaProxyEnricherFactory.asSerializable` | serializable proxy | (DSG/INT) | 1 |
| 20 | Every `@Service` class of the plugin (incl. interceptor-proxied ones) | `ServiceHelper.createServiceInstance` (generated `$$TalendServiceProxy` if interceptors or not serializable) | `BaseService` helper or generated proxy | SVC-001, SVC-002 | 0 |
| 21 | `org.talend.sdk.component.runtime.manager.service.ContainerInfo` (internal) | `ContainerInfo(containerId)` | `Serializable` | SVC-018 | 2 |
| 22 | `org.talend.sdk.component.runtime.manager.asm.ProxyGenerator` (internal) | the manager's generator | not serializable | SVC-018 | 2 |
| 23 | Extension-provided types | `ComponentExtension.getExtensionServices(pluginId)` merged into the plugin map when the extension owns a component | extension defined | SVC-021 | 2 |

Levels follow the rule "lowest level at which omission causes wrong behaviour": services used by virtually every connector are level 0; helper services used by specific connectors are level 1 or 2.

Note on the reference page: `services-built-in.adoc` lists `Resolver` twice and describes `RecordPointerFactory`; the code registers both once (rows 4 and 9). The page states that `LocalConfiguration` "is not recommended for the runtime because the local configuration is usually different"; the manager nevertheless injects it in every container.

## 3. Global (plugin-less) services

`DefaultServices.lookup(String type)` is used by code that runs outside a plugin container (for example when deserializing a record). It returns the manager-wide instance for exactly these class names: `javax.json.JsonBuilderFactory`, `JsonReaderFactory`, `stream.JsonGeneratorFactory`, `stream.JsonParserFactory`, `JsonWriterFactory`, and `org.talend.sdk.component.api.service.record.RecordBuilderFactory` (built with plugin id `null`). Any other type raises `IllegalArgumentException("<type> can't be a global service, didn't you pass a null plugin?")`.

## 4. LocalConfiguration resolution reference

| Step | Source | Note |
|---|---|---|
| 1 | `LocalConfiguration` SPI implementations (`ServiceLoader`) | Skipped when `-Dtalend.component.manager.localconfiguration.skip=true`. The Component Server registers `VirtualDependenciesService$LocalConfigurationImpl` this way |
| 2 | System properties | `System.getProperty(key)` |
| 3 | Environment variables | `System.getenv(key)`, else key with non-alphanumerics replaced by `_`, else the upper-cased variant |
| 4 | `TALEND-INF/local-configuration.properties` in the plugin (all copies aggregated, sorted by integer property `_ordinal`, default 0, later overrides earlier) | Skipped when `-Dtalend.component.configuration.<pluginId>.ignoreLocalConfiguration=true` |

Key lookup: each delegate is asked for `<pluginId>.<key>` first; only if no delegate answers is the plain `<key>` tried; each read also retries with `.` replaced by `_`. `keys()` returns the union, additionally exposing keys prefixed by `<pluginId>.` without the prefix.

`local_configuration:<key>` inside annotation string attributes (for example `@DefaultValue`, `@Ui*` family) is resolved once when parameter metadata is built (`UiParameterEnricher`), against this same `LocalConfiguration` view of the plugin; unresolved keys stay as the literal string.

## 5. Manager system properties that influence services

Only properties verified in `ComponentManager`/service code are listed here; the complete list belongs to the runtime docs.

| Property | Default | Effect |
|---|---|---|
| `talend.component.manager.localconfiguration.skip` | `false` | Do not load `LocalConfiguration` SPI providers |
| `talend.component.configuration.<pluginId>.ignoreLocalConfiguration` | `false` | Ignore the plugin `local-configuration.properties` |
| `talend.component.manager.services.cache.eviction.defaultEvictionTimeout` | unset (no expiry) | `LocalCache` default TTL in ms (read with `@Configuration("talend.component.manager.services.cache.eviction")`) |
| `talend.component.manager.services.cache.eviction.maxDeletionPerEvictionRun` | unset | Max entries removed per `clean()` |
| `talend.component.manager.services.cache.eviction.defaultMaxSize` | unset | Max cache entries before forced eviction |
| `talend.component.manager.classpathcontributor.skip` | `false` | Do not load `ContainerClasspathContributor` SPI |
| `talend.component.manager.jmx.skip` | `false` | Do not register JMX |
| `talend.component.manager.plugins.parallel` | `false` | Load plugins in parallel |
| `talend.component.manager.classloader.container.classesAndPackages` | (unverified default) | Classes/packages the container loader takes from the parent |
| `talend.component.manager.classloader.container.parentResources` | (unverified default) | Resources taken from the parent |
| `talend.component.manager.log.info` | `false` | Verbose `info(...)` logging |
| `talend.checkpoint.enabled` | `false` | Checkpoint support flag read by the manager (see RUN catalog) |

## 6. Host acceptance test for services (informative)

Given a plugin exposing a component that injects `LocalCache`, `Jsonb`, `JsonBuilderFactory`, `RecordBuilderFactory`, `LocalConfiguration` and an `HttpClient` interface, when the host loads the plugin and instantiates the component, then every constructor parameter is non-null, `Serializable` where required, `LocalConfiguration.get("x")` follows section 4, and `@PostConstruct` of user services observes injected fields. See [RUN-042](../02-feature-catalog/RUN-runtime.md#run-042-job-dsl-and-local-runner) for the Job DSL used to run such a chain.
