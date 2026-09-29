# Appendix - Runtime configuration keys

> Framework version documented: **1.2611.0-SNAPSHOT** (root `pom.xml`).
> Every key below was found by reading `component-api`, `component-runtime-impl`, `component-runtime-manager`, `container-core`, `component-runtime-beam` main sources (calls to `System.getProperty`, `Boolean.getBoolean`, `Integer.getInteger`, `LocalConfiguration.get`, constants). Server-side keys are out of scope (see the server configuration appendix in this folder).
> Narrative: [`../06-runtime-execution.md`](../06-runtime-execution.md). Catalog: [`RUN`](../02-feature-catalog/RUN-runtime.md), [`LCM`](../02-feature-catalog/LCM-lifecycle.md), [`DAT`](../02-feature-catalog/DAT-data-model.md).

Key families:

1. **JVM system properties** (`-Dkey=value`).
2. **Environment variables** (only `MAVEN_HOME`, `M2_HOME` are read directly; `LocalConfiguration` also maps env, see section 5).
3. **LocalConfiguration keys** (read through the plugin `LocalConfiguration` service; sources and lookup order in section 5).
4. **Component configuration map keys** (`Map<String,String>` given to `findMapper`/`findProcessor`).
5. **Job properties** (local runner / Beam executor).
6. **Resource files** inside plugins or the runtime classpath.

## 1. JVM system properties

| Key | Default | Effect | Source | Feature |
|---|---|---|---|---|
| `talend.component.manager.m2.repository` | unset | root Maven repository (highest priority) | `MavenRepositoryResolver` | LCM-008 |
| `talend.component.manager.m2.settings` | unset | path of `settings.xml` to read `<localRepository>` (only with user fallback) | `MavenRepositoryDefaultResolver` | LCM-008 |
| `talend.component.manager.user.m2.fallback` | `false` | enable `settings.xml`, `MAVEN_HOME`/`M2_HOME` and `~/.m2/repository` fallbacks; otherwise an intentionally non-existent path is used | same | LCM-008 |
| `maven.repository` | unset | Studio: `global` = use user Maven settings instead of Studio configuration area | same | LCM-008 |
| `osgi.configuration.area` | `""` | read to locate the Studio repository | same | LCM-008 |
| `talend.component.manager.classloader.container.classesAndPackages` | unset | comma-separated extra classes/packages loaded from the parent (shared) loader | `ComponentManager.PROPERTY_CLASSES_AND_PACKAGES` | LCM-005 |
| `talend.component.manager.classloader.container.parentResources` | unset | comma-separated resource substrings visible from the parent | `PROPERTY_PARENT_RESOURCES` | LCM-005 |
| `talend.component.manager.plugins.parallel` | `false` | register `plugins.properties` plugins in parallel | `ComponentManager` | LCM-004 |
| `talend.component.manager.classpathcontributor.skip` | `false` | ignore `ContainerClasspathContributor` SPI | `ComponentManager` | SVC-026 |
| `talend.component.manager.jmx.skip` | `false` | do not register JMX MBeans for containers | `ComponentManager` | LCM-018 |
| `talend.component.manager.log.info` | `false` | log framework info at INFO (otherwise DEBUG when Studio `routines.TalendString` is present) | `ComponentManager.findLogInfoLevel` | LCM-004 |
| `talend.component.manager.localconfiguration.skip` | `false` | ignore SPI `LocalConfiguration` implementations | `ComponentManager.createRawLocalConfigurations` | SVC-005 |
| `component.manager.callers.skip` | `false` | disable "caller jar as plugin" discovery | `ComponentManager.autoDiscoverPlugins0` | LCM-006 |
| `component.manager.classpath.skip` | `false` | disable classpath (`TALEND-INF/dependencies.txt`) discovery | same | LCM-006 |
| `talend.component.configuration.<containerId>.ignoreLocalConfiguration` | `false` | ignore `TALEND-INF/local-configuration.properties` resources of that plugin | `DefaultServiceProvider` | SVC-027 |
| `talend.component.configuration.validation.skip` | `false` | skip payload validation when instantiating components | `ReflectionService.PayloadValidator` | RUN-041, VAL-012 |
| `talend.component.impl.mode` | `DEFAULT` | `UNSAFE` disables component validation at registration | `impl.Mode` | RUN-047 |
| `talend.checkpoint.enabled` | `false` | enable checkpointing (`InputImpl`, `mergeCheckpointConfiguration`) | `InputImpl`, `ComponentManager` | RUN-033 |
| `talend.component.record.error.support` | `false` | entry-level error support | `Record.RECORD_ERROR_SUPPORT` | DAT-014 |
| `talend.component.record.nullable.check` | `false` | `true` skips null/missing checks in `RecordImpl.BuilderImpl` | `Record.RECORD_NULLABLE_CHECK` | DAT-015 |
| `talend.component.record.skip.sanitize` | `false` | do not sanitize entry names (incompatible with Avro factory) | `Schema.SKIP_SANITIZE_PROPERTY` | DAT-013 |
| `talend.component.runtime.serialization.java.inputstream.whitelist` | unset (deny-list) | comma-separated allowed class-name prefixes for Java deserialization | `EnhancedObjectInputStream` | RUN-039 |
| `talend.tccl.cacheable.classes` | unset | classes for which URL-connection caching is kept | `ConfigurableClassLoader` | LCM-005 |
| `<plugin>.talend.input.streaming.maxRecords` | unset | stream stop condition (per family/plugin) | `Streaming.loadStopStrategy` | RUN-028 |
| `<plugin>.talend.input.streaming.maxDurationMs` | unset | stream stop condition (ms) | same | RUN-028 |
| `talend.component.beam.record.factory.impl` | `auto` | `auto` / `memory` / `default` / `avro` | `AvroRecordBuilderFactoryProvider` | DAT-034 |
| `talend.component.beam.transformers.skip` | `false` | disable Beam IO bytecode enhancement | `BeamComponentExtension` | RUN-044 |
| `talend.component.beam.transformers.io.enhanced` | unset | comma-separated classes enhanced by `BeamIOTransformer` | same | RUN-044 |
| `talend.component.beam.transformers.debug` | `false` | debug the transformer | `BeamIOTransformer` | RUN-044 |
| `talend.beam.job.<option>` | unset | forwarded as Beam `--<option>=value` pipeline options | `BeamExecutor.createPipelineOptions` | RUN-043 |
| `component.runtime.beam.avrocoder.cache.size` | `1024` | max cached Avro coders | `AvroCoderCache` | RUN-045 |

Boolean properties are read with `Boolean.getBoolean` (true only for the string `true`, case-insensitive) or `Boolean.parseBoolean(System.getProperty(...))`; the beam avro cache size uses `Integer.getInteger`.

## 2. Environment variables

| Name | Effect | Feature |
|---|---|---|
| `MAVEN_HOME` | `conf/settings.xml` candidate (needs user fallback flag) | LCM-008 |
| `M2_HOME` | `conf/settings.xml` candidate and `<M2_HOME>/repository` (needs user fallback flag) | LCM-008 |

## 3. LocalConfiguration keys (plugin scope)

Lookup for a key `k` in plugin `p`: `p.k` then `k`, each also with `.` replaced by `_` (section 5).

| Key | Default | Effect | Feature |
|---|---|---|---|
| `talend.input.streaming.retry.maxRetries` | `Integer.MAX_VALUE` | max consecutive empty reads | RUN-029 |
| `talend.input.streaming.retry.strategy` | `constant` | `constant` or `exponential` | RUN-029 |
| `talend.input.streaming.retry.constant.timeout` | `500` | ms pause | RUN-029 |
| `talend.input.streaming.retry.exponential.exponent` | `1.5` | exponent | RUN-029 |
| `talend.input.streaming.retry.exponential.randomizationFactor` | `0.5` | jitter | RUN-029 |
| `talend.input.streaming.retry.exponential.maxDuration` | `300000` | ms cap | RUN-029 |
| `talend.input.streaming.retry.exponential.initialBackOff` | `1000` | ms | RUN-029 |
| `talend.input.streaming.maxRecords` | unset | stream stop condition | RUN-028 |
| `talend.input.streaming.maxDurationMs` | unset | stream stop condition | RUN-028 |
| `<ComponentSimpleClassName>$maxRecords`, `$maxRecords` | `-1` | default of built-in option `$maxRecords` | RUN-028 |
| `<ComponentSimpleClassName>$maxDurationMs`, `$maxDurationMs` | `-1` | default of built-in option `$maxDurationMs` | RUN-028 |
| `<ComponentSimpleClassName>._maxBatchSize.value`, `_maxBatchSize.value` | `1000` | default of `$maxBatchSize` | RUN-026 |
| `<ComponentSimpleClassName>._maxBatchSize.active`, `_maxBatchSize.active` | `true` | `false` removes the `$maxBatchSize` option | RUN-026 |
| `talend.component.manager.services.cache.eviction.defaultEvictionTimeout` | `-1` | default `LocalCache` ttl (ms) (name derived from `@Configuration` prefix + field, *inferred*) | SVC-009 |
| `talend.component.manager.services.cache.eviction.maxDeletionPerEvictionRun` | `-1` | max evictions per run | SVC-009 |
| `talend.component.manager.services.cache.eviction.defaultMaxSize` | `-1` | max cache entries | SVC-009 |

## 4. Component configuration map keys (host -> `findMapper`/`findProcessor`)

| Key | Meaning | Feature |
|---|---|---|
| `<root>.<path>` | ordinary properties (see `03-component-server-api.md` flat properties) | RUN-041 |
| `<path>[i]`, `<path>[length]`, `<path>.key[i]`, `<path>.value[i]` | collection and map encodings | RUN-041 |
| `<path>.__version` | version of a nested configuration type | LCM-003 |
| `$maxBatchSize` / `<root>.$maxBatchSize` | group size upper bound | RUN-026 |
| `$maxRecords`, `$maxDurationMs` (also `<root>.$...`) | stream stop conditions | RUN-028 |
| `$checkpoint.<field>`, `$checkpoint.__version` | restored checkpoint state (mappers only) | RUN-033 |

## 5. LocalConfiguration sources and order

1. Plugin resource `TALEND-INF/local-configuration.properties` (all copies on the plugin classpath, aggregated; key `_ordinal` (int, default 0) sorts the files ascending so higher ordinal overrides), unless the JVM property `talend.component.configuration.<containerId>.ignoreLocalConfiguration=true`.
2. Container-level `LocalConfiguration` implementations found by SPI (unless `talend.component.manager.localconfiguration.skip=true`).
3. JVM system properties.
4. Environment variables: exact name, then non-alphanumerics replaced by `_`, then the upper-cased variant.

For a plugin `p`, `LocalConfigurationService.get(k)` tries `p.k` through all delegates, then `k`; each attempt also tries the key with `.` replaced by `_`.

## 6. Resource files

| Resource | Where | Content | Feature |
|---|---|---|---|
| `TALEND-INF/dependencies.txt` | plugin jar/dir (configurable name via `ComponentManager` constructor) | `mvn dependency:list` output | LCM-007 |
| `TALEND-INF/dynamic-dependencies.properties` | runtime classpath | `<pluginId>=gav1,gav2` | LCM-007, ACT-010 |
| `TALEND-INF/plugins.properties` | runtime jar (nested repo) | `<pluginName>=<GAV or path>` | LCM-006, LCM-009 |
| `TALEND-INF/scanning.properties` | plugin | `classes.list`, `classloader.includes`, `classloader.excludes`, `classloader.filter.strategy` (`include-exclude` or default) | LCM-011 |
| `TALEND-INF/local-configuration.properties` | plugin | plugin LocalConfiguration + `_ordinal` | SVC-027 |
| `TALEND-INF/org.talend.sdk.component.container.ContainerManager.jvmMarkers.txt` | runtime classpath | extra path prefixes considered JVM (one per line, `#` comments); the `ContainerManager` also adds `java.home` and `META-INF/maven/org.talend.sdk.component/` locations | LCM-005 |
| `MAVEN-INF/repository/**` | fat jar / `.car` | nested Maven repository | LCM-009, LCM-010 |
| `TALEND-INF/metadata.properties` | `.car` | `component_coordinates`, `type`, `version`, `date`, `CarBundlerVersion` | LCM-010 |
| `META-INF/services/org.talend.sdk.component.runtime.serialization.ContainerFinder` | manager jar | `StandaloneContainerFinder` | RUN-039 |
| `META-INF/services/org.talend.sdk.component.runtime.manager.service.record.RecordBuilderFactoryProvider` | beam jar | Avro provider | DAT-034 |
| `META-INF/services/org.talend.sdk.component.runtime.manager.chain.Job$ExecutorBuilder` | beam jar | `BeamExecutor` registration | RUN-042 |
| `META-INF/services/org.talend.sdk.component.spi.component.ComponentExtension` | beam jar | `BeamComponentExtension` | SVC-021 |
| `META-INF/services/org.talend.sdk.component.runtime.manager.ComponentManager$Customizer` | beam jar | `BeamCustomizer` | SVC-026 |
| `META-INF/services/org.talend.sdk.component.spi.component.ComponentMetadataEnricher` | manager jar | 4 built-in enrichers | DSG-006 |

## 7. Job properties (local runner / Beam executor)

| Key | Effect | Feature |
|---|---|---|
| `streaming.maxRecords` | stream cap: local default -1; Beam default 1000 | RUN-028, RUN-043 |
| `streaming.maxDurationMs` | Beam default 60000 | RUN-028, RUN-043 |
| `org.talend.sdk.component.runtime.manager.chain.Job$ExecutorBuilder` | executor instance / class / `standalone`, `default`, `local`, `beam` | RUN-042 |
| `org.talend.sdk.component.runtime.manager.chain.GroupKeyProvider` (job or per component) | custom group key provider instance | RUN-050 |
| `__version` (URI query of the component URI) | configuration version | RUN-042 |

## 8. Discrepancies

* Checkpoint property: docs write `talend.checkpoint.enable`, code reads `talend.checkpoint.enabled`.
* No other discrepancy found for the keys above; a key that appears only in Antora pages and not in code is not listed.
