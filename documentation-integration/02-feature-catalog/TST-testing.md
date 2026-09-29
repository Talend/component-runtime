# TST - Testing support (informative)

> Framework version documented: `1.2611.0-SNAPSHOT` (root `pom.xml`; last release tag `1.2610.0`).
> Category prefix: `TST`. Machine-readable twin: [index.TST.json](index.TST.json) (generated from this file, both MUST agree).
> Related: [01-overview-and-architecture.md](../01-overview-and-architecture.md), [SVC-services.md](SVC-services.md), [HTTP-http-client.md](HTTP-http-client.md), [SRV-server.md](SRV-server.md).

These entries describe test tooling an integrator can reuse to **verify** a Designer or Runtime integration. None is required for a host to function, so they are level 2, except the Job DSL (RUN-042) which is the fastest way to run a component chain end to end and is therefore level 1. All test modules live under `component-runtime-testing/` (Maven modules `component-runtime-junit`, `component-runtime-junit-base`, `component-runtime-http-junit`, `component-runtime-beam-junit`, `component-runtime-testing-spark`); Antora pages: `testing-junit.adoc`, `testing-multiple-envs.adoc`, `testing-http.adoc`, `testing-maven-passwords.adoc`, `testing-beam.adoc`, `testing-spark.adoc`, `testing-best-practices.adoc`, `testing-generating-data.adoc`, `ref-junit-environments.adoc` (generated `_partials/generated_junit-environments.adoc`).

Integration-verification guidance (informative): a Runtime host is validated by (1) deploying a reference plugin, (2) running the Job DSL chain `test://emitter` -> component -> `test://collector`, (3) asserting collected records. A Designer host is validated against recorded server payloads (component-server tests under `component-server-parent/component-server/src/test/java/org/talend/sdk/component/server/front/*Test.java` are executable examples of every endpoint, TST-019).

---

### TST-001 SimpleComponentRule (JUnit 4)
- **Source**: `component-runtime-testing/component-runtime-junit/src/main/java/org/talend/sdk/component/junit/SimpleComponentRule.java` (+ doc `testing-junit.adoc`)
- **Category / Sub-area**: TST / JUnit 4
- **Kind**: JUnit 4 `TestRule` (usable as `@Rule` or `@ClassRule`)
- **Applies to**: component tests
- **Description**: Creates an embedded `ComponentManager` (via `BaseComponentsHandler.EmbeddedComponentManager`) for the components found under a root package and registers the mock components of TST-004. Constructor `SimpleComponentRule(String packageName)`; `withIsolatedPackage(String pck, String... packages)` loads listed packages in their own classloader.
- **Attributes**: constructor package; `withIsolatedPackage`.
- **Contract for the Designer**: none
- **Contract for the Runtime**: none (test-only); a runtime host MAY use it as a reference behaviour oracle.
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
@Rule public final SimpleComponentRule components = new SimpleComponentRule("org.talend.sdk.component.mycomponent");
```

### TST-002 @WithComponents, ComponentExtension and @Injected (JUnit 5)
- **Source**: `component-runtime-testing/component-runtime-junit/src/main/java/org/talend/sdk/component/junit5/WithComponents.java`, `component-runtime-testing/component-runtime-junit/src/main/java/org/talend/sdk/component/junit5/ComponentExtension.java`, `component-runtime-testing/component-runtime-junit/src/main/java/org/talend/sdk/component/junit5/Injected.java`
- **Category / Sub-area**: TST / JUnit 5
- **Kind**: annotations (`@WithComponents` = `@ExtendWith(ComponentExtension.class)`; `@Injected` on a field)
- **Applies to**: test classes
- **Description**: JUnit 5 equivalent of TST-001. `@WithComponents(value = "<package>", isolatedPackages = {})` starts the embedded manager; `@Injected` injects a `ComponentsHandler` field (TST-003); fields annotated `@Service` are injected with plugin services.
- **Attributes**:

| Name | Type | Default | Meaning |
|---|---|---|---|
| `value` | `String` | required | Package containing the components |
| `isolatedPackages` | `String[]` | `{}` | Packages loaded from their own classloader |

- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
@WithComponents("org.talend.sdk.component.junit.component")
class ComponentExtensionTest { @Injected private ComponentsHandler handler; }
```

### TST-003 ComponentsHandler and BaseComponentsHandler
- **Source**: `component-runtime-testing/component-runtime-junit/src/main/java/org/talend/sdk/component/junit/BaseComponentsHandler.java`, `ComponentsHandler.java`
- **Category / Sub-area**: TST / Handler API
- **Kind**: class / interface (shared by JUnit 4 and 5)
- **Applies to**: test code driving components
- **Description**: Public methods: `withIsolatedPackage(String, String...)`, `start()` (returns `EmbeddedComponentManager`), `close()`, `createMapper(Class<?> componentType, Object configuration)`, `createProcessor(Class<?> componentType, Object configuration)`, `collect(Processor, ControllableInputFactory)` and `collect(Processor, ControllableInputFactory, int bundleSize)` (return `Outputs`), `collect(Class<T>, Mapper, int maxRecords)` (`Stream<T>`), `collectAsList(Class<T>, Mapper)` / `collectAsList(Class<T>, Mapper, int maxRecords)`, `collect(Class<T>, String family, String component, int version, ...)`, `process(Iterable<T>, String family, String component, int version, ...)`, `asManager()` (the `ComponentManager`), `findService(Class<T>)` / `findService(String plugin, Class<T>)`, `injectServices(T)`, `getTestPlugins()`, `setInputData(Iterable<T>)`, `getCollectedData(Class<T>)`, `resetState()`. `Outputs` exposes `size()`, `keys()`, `get(Class<T>, String name)`. Timeouts/state: system property `talend.component.junit.handler.state` (`thread` default, or `static` for JVM-wide collector state) and `talend.component.junit.timeout` (default 5).
- **Attributes**: see methods.
- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
final Mapper mapper = handler.createMapper(Source.class, config);
final List<String> out = handler.collectAsList(String.class, mapper);
```

### TST-004 Mock components test://emitter and test://collector
- **Source**: `component-runtime-testing/component-runtime-junit/src/main/java/org/talend/sdk/component/junit/SimpleEmitter.java`, `SimpleCollector.java`
- **Category / Sub-area**: TST / Mock components
- **Kind**: components (family `test`)
- **Applies to**: Job DSL chains in tests
- **Description**: `@Emitter(family = "test", name = "emitter")` produces the data given to `setInputData(...)`; the `collector` processor stores every received record in memory, retrievable with `getCollectedData(type)`. Collector state is per thread by default (`talend.component.junit.handler.state=thread`); Beam or other concurrent runs need `static`.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
components.setInputData(asList(a, b));
Job.components().component("emitter", "test://emitter").component("out", "fam://out?...")
   .connections().from("emitter").to("out").build().run();
```

### TST-005 ServiceInjectionRule
- **Source**: `component-runtime-testing/component-runtime-junit/src/main/java/org/talend/sdk/component/junit/ServiceInjectionRule.java`
- **Category / Sub-area**: TST / JUnit 4
- **Kind**: JUnit 4 rule
- **Applies to**: test classes
- **Description**: Injects the plugin's services into `@Service` fields of the test instance. Constructor `ServiceInjectionRule(<components controller>, Object testInstance)`; MUST be a `@Rule` (needs the instance), not a `@ClassRule`. In JUnit 5 no extra extension is needed.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
@Rule public final ServiceInjectionRule injections = new ServiceInjectionRule(COMPONENT_FACTORY, this);
@Service private LocalConfiguration configuration;
```

### TST-006 Input factories and Outputs
- **Source**: `component-runtime-testing/component-runtime-junit/src/main/java/org/talend/sdk/component/junit/ControllableInputFactory.java`, `MainInputFactory.java`, `JoinInputFactory.java`, `InputFactoryIterable.java`
- **Category / Sub-area**: TST / Processor testing
- **Kind**: classes / interface
- **Applies to**: processor unit tests
- **Description**: `MainInputFactory` feeds the default branch; `JoinInputFactory.withInput(branch, data)` feeds several named input branches (default branch name `__default__`); `ControllableInputFactory` is the extension interface; `collect(processor, inputFactory)` returns `Outputs` keyed by output branch name.
- **Attributes**: `withInput(String branch, Iterable data)`.
- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
final Outputs o = components.collect(processor, new JoinInputFactory().withInput("__default__", list1).withInput("second", list2));
```

### TST-007 SimpleFactory.configurationByExample
- **Source**: `component-runtime-testing/component-runtime-junit/src/main/java/org/talend/sdk/component/junit/SimpleFactory.java`
- **Category / Sub-area**: TST / Configuration
- **Kind**: utility
- **Applies to**: tests building runtime configuration
- **Description**: Converts a configuration POJO (`@Option` fields) to the flat `Map<String,String>` a runtime expects: `configurationByExample(T instance)`, `configurationByExample(T instance, String prefix)`, or the fluent `configurationByExample().withPrefix(p).forInstance(i).configured().toMap()` / `.toQueryString()` (URI-encoded for the Job DSL). It shows the flat-key serialization a Designer must reproduce (see [../03-component-server-api.md](../03-component-server-api.md)). Tests can force `maxBatchSize` with `$configuration.$maxBatchSize=10`.
- **Attributes**: `prefix`.
- **Contract for the Designer**: MAY use it to cross-check its own form-to-map serialization.
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
final String uri = "family://component?" + configurationByExample().forInstance(config).configured().toQueryString();
```

### TST-008 RecordAsserts and ExceptionVerifier
- **Source**: `component-runtime-testing/component-runtime-junit/src/main/java/org/talend/sdk/component/junit/RecordAsserts.java`, `ExceptionVerifier.java`
- **Category / Sub-area**: TST / Assertions
- **Kind**: classes
- **Applies to**: tests
- **Description**: `RecordAsserts` is a serializable `Function<Iterable<Map<String,List<Serializable>>>, Void>` validating outputs per branch name (`withAsserts(name, consumer)`; missing expected outputs throw `IllegalArgumentException("Missing outputs: ...")`). `ExceptionVerifier<T extends RuntimeException>` is a JUnit 4 rule: `assertWith(Consumer<T>)` asserts on a thrown exception (for example an `HttpException`).
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
httpExceptionRule.assertWith(e -> assertEquals(401, e.getResponse().status()));
```

### TST-009 Multi-environment test framework
- **Source**: `component-runtime-testing/component-runtime-junit/src/main/java/org/talend/sdk/component/junit/environment/Environment.java` (same package: `Environments.java`, `EnvironmentProvider.java`, `BaseEnvironmentProvider.java`, `DecoratingEnvironmentProvider.java`, `ClassLoaderEnvironment.java`, `Dependencies.java`, `EnvironmentConfiguration.java`, `EnvironmentConfigurations.java`, `EnvironmentsConfigurationParser.java`, `MultiEnvironmentsRunner.java`; `junit5/environment/EnvironmentalTest.java`, `EnvironmentsExtension.java`, `EnvironmentalContext.java`; `junit/delegate/DelegateRunWith.java`, `DelegatingRunner.java`; doc `testing-multiple-envs.adoc`)
- **Category / Sub-area**: TST / Multi-environment
- **Kind**: annotations, runner, extension, SPI
- **Applies to**: tests that must run against several runtimes (standalone, Beam runners)
- **Description**: `@Environment(Class<? extends EnvironmentProvider>)` (repeatable, inherited) declares environments; JUnit 4: `@RunWith(MultiEnvironmentsRunner.class)` (delegate runner via `@DelegateRunWith`); JUnit 5: replace `@Test` by `@EnvironmentalTest` (tests execute one after another for all environments, so `@BeforeAll`/`@AfterAll` run once). `EnvironmentProvider.start(Class<?> clazz, Annotation[] annotations)` returns an `AutoCloseable`. `@EnvironmentConfiguration(environment = "<name>", systemProperties = @Property(key, value))` (repeatable) customizes system properties per environment. System property `<environment name>.skip=true` skips an environment.
- **Attributes**: `EnvironmentConfiguration.environment` (`String`), `EnvironmentConfiguration.systemProperties` (`Property[]`, each `key`,`value`).
- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
@Environment(ContextualEnvironment.class)
@Environment(DirectRunnerEnvironment.class)
class TheComponentTest { @EnvironmentalTest void run() { } }
```

### TST-010 Built-in environments
- **Source**: `component-runtime-testing/component-runtime-junit/src/main/java/org/talend/sdk/component/junit/environment/builtin/ContextualEnvironment.java` (same tree: `builtin/beam/DirectRunnerEnvironment.java`, `FlinkRunnerEnvironment.java`, `SparkRunnerEnvironment.java`, `BeamEnvironment.java`; generated list `documentation/src/main/antora/modules/ROOT/pages/_partials/generated_junit-environments.adoc`)
- **Category / Sub-area**: TST / Multi-environment
- **Kind**: `EnvironmentProvider` implementations
- **Applies to**: multi-environment tests
- **Description**: Names `Contextual` (`ContextualEnvironment`, standalone simulation of Studio), `Direct` (`DirectRunnerEnvironment`), `Flink` (`FlinkRunnerEnvironment`), `Spark` (`SparkRunnerEnvironment`). Beam environments set the contextual classloader to the corresponding Beam runner dependencies; configuration is read from system properties (for example `beamTestPipelineOptions`) and environment variables.
- **Attributes**: environment names above.
- **Contract for the Designer**: none
- **Contract for the Runtime**: A Beam-based runtime SHOULD be validated with at least `Direct`.
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
@Environment(DirectRunnerEnvironment.class)
@EnvironmentConfiguration(environment = "Direct", systemProperties = @EnvironmentConfiguration.Property(key = "beamTestPipelineOptions", value = "..."))
```

### TST-011 HTTP mocking with JUnit 4
- **Source**: `component-runtime-testing/component-runtime-http-junit/src/main/java/org/talend/sdk/component/junit/http/junit4/JUnit4HttpApi.java`, `JUnit4HttpApiPerMethodConfigurator.java` (+ doc `testing-http.adoc`)
- **Category / Sub-area**: TST / HTTP mocking
- **Kind**: JUnit 4 rules
- **Applies to**: tests of components calling REST APIs
- **Description**: `JUnit4HttpApi` (`@ClassRule`) starts a Netty-based proxy server and auto-configures `http[s].proxyHost/proxyPort`, `HttpsURLConnection#defaultSSLSocketFactory` and `SSLContext#default`; `JUnit4HttpApiPerMethodConfigurator(API)` (`@Rule`) configures per test and enables capture mode (without it capture and per-test mocking are disabled). `activeSsl()` enables SSL and `getSslContext()` gives the client socket factory. Both use the shared `HttpApiHandler` setters (`setHeaderFilter`, `setExecutor`, `setGlobalProxyConfiguration`, `setPort`, `setSslContext`, `setResponseLocator`, `setLogLevel`, `setSkipProxyHeaders`).
- **Attributes**: see methods.
- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
@ClassRule public static final JUnit4HttpApi API = new JUnit4HttpApi();
@Rule public final JUnit4HttpApiPerMethodConfigurator configurator = new JUnit4HttpApiPerMethodConfigurator(API);
```

### TST-012 HTTP mocking with JUnit 5
- **Source**: `component-runtime-testing/component-runtime-http-junit/src/main/java/org/talend/sdk/component/junit/http/junit5/HttpApi.java`, `HttpApiInject.java`, `HttpApiName.java`
- **Category / Sub-area**: TST / HTTP mocking
- **Kind**: annotations
- **Applies to**: test classes and methods
- **Description**: `@HttpApi` on the class starts the proxy; `@HttpApiInject` injects the `HttpApiHandler<?>`; `@HttpApiName("${class}_${method}")` overrides the capture file name (placeholders `${class}`, `${method}`, `${displayName}`) so parameterized/repeated tests can share or split mock files.
- **Attributes** (`@HttpApi`):

| Name | Type | Default | Meaning |
|---|---|---|---|
| `port` | `int` | `0` | Proxy port (0 = random) |
| `globalProxyConfiguration` | `boolean` | `true` | Configure JVM-wide proxy settings |
| `logLevel` | `String` | `"DEBUG"` | Handler log level |
| `useSsl` | `boolean` | `false` | Serve HTTPS |
| `skipProxyHeaders` | `boolean` | `false` | Do not add proxy headers |

- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
@HttpApi(useSsl = true)
class MyHttpsApiTest { @HttpApiInject private HttpApiHandler<?> handler; }
```

### TST-013 HTTP capture, passthrough and response location
- **Source**: `component-runtime-testing/component-runtime-http-junit/src/main/java/org/talend/sdk/component/junit/http/api/ResponseLocator.java`, `internal/impl/DefaultResponseLocator.java`, `Handlers.java`, `PassthroughHandler.java`, `DefaultResponseLocatorCapturingHandler.java`
- **Category / Sub-area**: TST / HTTP mocking
- **Kind**: convention + SPI (`ResponseLocator`)
- **Applies to**: HTTP JUnit users
- **Description**: The default locator reads mocked responses from `talend/testing/http/<class name>_<method name>.json` (or `talend/testing/http/<request path>.json`). System property `talend.junit.http.capture` (folder, usually `src/test/resources`, or `true`) records real exchanges as JSON; `talend.junit.http.passthrough=true` proxies to the real server; `talend.junit.http.starting.timeout` (default 60 s) bounds startup. Sensitive query parameters can be stripped from captured files and the mock matches ignoring them.
- **Attributes**: system properties above.
- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```
mvn test -Dtalend.junit.http.capture=true
```

### TST-014 MavenDecrypter, Server and JUnit helpers
- **Source**: `component-runtime-testing/component-runtime-junit/src/main/java/org/talend/sdk/component/maven/MavenDecrypter.java`, `maven/Server.java`, `junit/MavenDecrypterRule.java`, `junit5/WithMavenServers.java`, `junit5/MavenDecrypterExtension.java`, `api/DecryptedServer.java`
- **Category / Sub-area**: TST / Secrets
- **Kind**: utility, JUnit 4 rule, JUnit 5 extension, parameter annotation
- **Applies to**: tests using real credentials on CI
- **Description**: `new MavenDecrypter().find("<server-id>")` returns a `Server` (`getUsername()`, `getPassword()`) read from `settings.xml` (locations: `${talend.maven.decrypter.m2.location}` or `~/.m2`, `M2_HOME/conf`, `MAVEN_HOME/conf`) and decrypted with `settings-security.xml` (AES/CBC, Maven master password); `${env.X}` / `${prop}` placeholders are also resolved. Throws `IllegalArgumentException` if no settings file or server id is found. JUnit 5: `@WithMavenServers` + `@DecryptedServer` injection.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
final Server s = new MavenDecrypter().find("my-test-server");
```

### TST-015 Beam JUnit helpers
- **Source**: `component-runtime-testing/component-runtime-beam-junit/src/main/java/org/talend/sdk/component/junit/beam/Data.java` (+ doc `testing-beam.adoc`)
- **Category / Sub-area**: TST / Beam
- **Kind**: utility class
- **Applies to**: Beam-based runtime tests
- **Description**: Helper to build Beam test data/assertions; for general Beam testing the docs recommend the Beam `DirectRunner`. (Only `Data` exists in the module; unverified beyond its presence.)
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
// see component-runtime-beam and testing-beam.adoc
```

### TST-016 Spark cluster testing
- **Source**: `component-runtime-testing/component-runtime-testing-spark/src/main/java/org/talend/sdk/component/runtime/testing/spark/SparkClusterRule.java`, `junit5/WithSpark.java`, `junit5/SparkInject.java`, `junit5/internal/SparkExtension.java` (+ doc `testing-spark.adoc`)
- **Category / Sub-area**: TST / Spark
- **Kind**: JUnit 4 rule, JUnit 5 extension and annotations
- **Applies to**: runtime tests against a Spark cluster
- **Description**: `SparkClusterRule(scalaVersion, sparkVersion, slaves)` forks a Spark master and N slaves (use as `@ClassRule`), with `submit*` methods to send jobs from the test classpath or a shade; JUnit 5 uses `@WithSpark` and `@SparkInject`.
- **Attributes**: constructor arguments (Scala version, Spark version, number of slaves).
- **Contract for the Designer**: none
- **Contract for the Runtime**: A Spark-based runtime MAY be verified with it.
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
@ClassRule public static final SparkClusterRule SPARK = new SparkClusterRule("2.10", "1.6.3", 1);
```

### TST-017 Temporary folder helpers
- **Source**: `component-runtime-testing/component-runtime-junit-base/src/main/java/org/talend/sdk/component/junit/base/junit5/WithTemporaryFolder.java`, `TemporaryFolder.java`, `TempFolder.java`, `internal/TemporaryFolderExtension.java`, `JUnit5InjectionSupport.java`
- **Category / Sub-area**: TST / Utilities
- **Kind**: annotation and extension (`@WithTemporaryFolder` is `@Deprecated`, "part of jupiter 5.4 now")
- **Applies to**: JUnit 5 tests
- **Description**: Backport of the JUnit 4 temporary folder rule for JUnit 5 with `@TempFolder` parameter/field injection; `JUnit5InjectionSupport` is the shared injection base of the framework extensions.
- **Attributes**: none.
- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none.
- **Maturity level**: 2 - optional test tooling.
- **Example**:

```java
@WithTemporaryFolder class MyTest { }
```

### TST-018 (moved) -> see RUN-042

### TST-019 Executable payload fixtures (server and manager tests)
- **Source**: `component-server-parent/component-server/src/test/java/org/talend/sdk/component/server/front/ActionResourceImplTest.java` (and sibling `*ResourceImplTest.java`), `component-runtime-manager/src/test/java/org/talend/sdk/component/runtime/manager/`
- **Category / Sub-area**: TST / Fixtures
- **Kind**: convention
- **Applies to**: integrators writing contract tests
- **Description**: The server tests (Meecrowave + JAX-RS client) call every endpoint against sample plugins and assert real payloads (for example `executeDynamicDependencies` posts `configuration.driver=derby` to `/action/execute?type=dynamic_dependencies&family=jdbc&action=jdbc-deps` and expects a JSON list of GAVs). They are the recommended source of real request/response samples for a Designer's mock server; manager tests under `.../runtime/manager/` do the same for runtime lifecycle.
- **Attributes**: none.
- **Contract for the Designer**: MAY replay these interactions to test its client.
- **Contract for the Runtime**: MAY reuse manager tests as conformance examples.
- **Server exposure**: none.
- **Maturity level**: 2 - informative.
- **Example**:

```
component-server-parent/component-server/src/test/java/org/talend/sdk/component/server/front/ActionResourceImplTest.java
```
