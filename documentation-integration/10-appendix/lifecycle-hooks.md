# Appendix - Lifecycle hooks

> Framework version documented: **1.2611.0-SNAPSHOT** (root `pom.xml`).
> Sources: `component-runtime-impl` (`base/LifecycleImpl`, `input/*`, `output/ProcessorImpl`, `standalone/DriverRunnerImpl`), `component-runtime-manager` (`ComponentManager.Updater`), `container-core` (`ContainerManager`, `ContainerListener`), `component-runtime-beam` (`BaseProcessorFn`, `TalendIO`).
> Narrative: [`../06-runtime-execution.md`](../06-runtime-execution.md). Catalog: [`RUN`](../02-feature-catalog/RUN-runtime.md), [`LCM`](../02-feature-catalog/LCM-lifecycle.md).

All hooks discovered by reflection MUST be `public` (`Class.getMethods()`); private methods are ignored. Every invocation runs with the plugin classloader as thread context classloader and unwraps `InvocationTargetException` (INT-009).

## 1. Author-side hooks (methods on component classes)

| Hook | Applies to | Called by | When | Parameters | Node (distributed) | Notes / feature |
|---|---|---|---|---|---|---|
| constructor | all components, config classes | `ComponentManager` instantiator | at `findMapper/findProcessor/findDriverRunner` | `@Option` configuration objects, services | coordinator | single public constructor; result MUST be Serializable (RUN-039, RUN-041) |
| `@PostConstruct` | mapper, emitter/producer object, processor, driver runner, `@Service` | `LifecycleImpl.start()` (components); `Updater.onCreate` (services) | `start()` before use | none; for streaming inputs `@Option("maxDurationMs"/"maxRecords")` int/long (RUN-028) | worker | all public annotated methods run (`findMethods().forEach`); services: once per container (RUN-023, SVC-027) |
| `@PreDestroy` | same | `LifecycleImpl.stop()`; services: `Updater.onClose` | `stop()` / container close | none | worker | services skipped when they are generated proxies (`Proxy.isProxyClass`) |
| `@Assessor` | mapper | `PartitionMapperImpl.assess()` | after `start()` | none | coordinator | result `Number.longValue()`; default 1 (RUN-003) |
| `@Split` | mapper | `PartitionMapperImpl.split(long)` | after `start()` | `@PartitionSize int/long` only | coordinator | returns `Collection<M>` (RUN-004) |
| `@Emitter` (method) | mapper | `PartitionMapperImpl.create()` | when creating an Input | none | worker | returns the producer object (RUN-001) |
| `@Producer` | emitter/producer object | `InputImpl.next()` | every read | none | worker | `null` ends batch input (RUN-006) |
| `@CheckpointAvailable` | producer object | `InputImpl.isCheckpointReady()` | after each non-null `next()` when callback set | none | worker | RUN-032 |
| `@CheckpointData` | producer object | `InputImpl.getCheckpoint()` | when ready, and at `stop()` if callback set | none | worker | RUN-031 |
| `@BeforeGroup` | processor | `ProcessorImpl.beforeGroup()` | before first element of a group | none | worker | RUN-010 |
| `@ElementListener` | processor | `ProcessorImpl.onNext()` | per element | inputs, `@Output` emitters | worker | RUN-009 |
| `@AfterGroup` | processor | `ProcessorImpl.afterGroup(out[, last])` | end of group and final flush | `@Output` emitters, group `Collection`, `@LastGroup` boolean | worker | RUN-011, RUN-012 |
| `@RunAtDriver` | driver runner | `DriverRunnerImpl.runAtDriver()` | once | none | driver | RUN-022 |
| `MigrationHandler.migrate` | component / config class | `BaseMeta.instantiate`, server migrate endpoints | before construction | `(int incomingVersion, Map<String,String>)` | wherever `findXxx` runs | LCM-002, LCM-003 |
| `@Action` methods (`@HealthCheck`, `@Suggestions`, `@DiscoverSchema`, ...) | services | server `ActionResource` / `ServiceMeta.ActionMeta.getInvoker()` | design time on demand | configuration parameters | component server | see ACT catalog |
| `@AvailableOutputFlows`, `@DynamicDependencies`, `@CreateConnection`, `@CloseConnection`, `@DatabaseSchemaMapping` | services | design-time action | on demand | configuration | component server / Studio | ACT-014, ACT-010, ACT-011, ACT-012, ACT-013 |
| `@AfterVariableContainer` | mapper, processor, standalone | Studio DI | after execution | none | worker | RUN-037 |

## 2. Framework lifecycle interfaces (host-side)

| Interface / method | Implementation | Effect | Feature |
|---|---|---|---|
| `Lifecycle.start()` | `LifecycleImpl` | invoke `@PostConstruct` methods | RUN-023 |
| `Lifecycle.stop()` | `LifecycleImpl` | invoke `@PreDestroy` methods | RUN-023 |
| `Input.start()` | `InputImpl` | `super.start()` + resolve checkpoint methods (only if `talend.checkpoint.enabled`) | RUN-033 |
| `Input.start(Consumer<CheckpointState>)` | `InputImpl` | `start()`; keeps the callback only if checkpointing enabled | RUN-033 |
| `StreamingInputImpl.start()` | | `running=true`, JVM shutdown hook | RUN-027 |
| `StreamingInputImpl.stop()` | | `running=false`, hook removed, semaphore taken, then `@PreDestroy` | RUN-027 |
| `InputImpl.stop()` | | final checkpoint callback (if set), then `@PreDestroy` | RUN-033 |
| `Mapper.start()/stop()` | `PartitionMapperImpl` | `@PostConstruct` / `@PreDestroy` of the mapper object | RUN-002 |
| `LocalPartitionMapper.start()/stop()` | | no-ops | RUN-001 |
| `Processor.beforeGroup()/afterGroup()/onNext()` | `ProcessorImpl` | see above | RUN-010..RUN-012 |
| `AutoChunkProcessor.onElement()/flush()` | local runner | group cutting | RUN-026 |

## 3. Container and manager hooks

| Hook | Owner | When | Effect |
|---|---|---|---|
| `ContainerListener.onCreate(Container)` | `ContainerManager` (all listeners, in registration order) | after classloader creation, during `create()` | manager's `Updater` scans and registers components/services; any exception aborts deployment (`<id> can't be deployed`) and triggers `onClose` on already-called listeners |
| `ContainerListener.onClose(Container)` | `ContainerManager` | `Container.close()` | registry cleared, service `@PreDestroy`, Jsonb closed, JMX unregistered |
| `ContainerListenerExtension` (SPI, `order()`) | `ComponentManager` | at manager construction | registered after the built-in `Updater` and JMX listener (LCM-018) |
| `ComponentExtension.onComponent(ComponentContext)` | `ComponentManager.Updater.onComponent` | for each public component class during scan | may `skipValidation()` or take ownership (SVC-021) |
| `ComponentMetadataEnricher.onComponent` | `ComponentMetadataService` | when a component's metadata is built | adds metadata keys (DSG-006) |
| `Customizer`, `ContainerClasspathContributor` | `ComponentManager` constructor | manager construction / container creation | class filters, classpath contributions (SVC-026) |
| JVM shutdown hook | `ComponentManager.SingletonHolder` | JVM exit | closes the contextual manager once |
| `Container.State` | `Container` | transitions | CREATED -> DEPLOYED (or ON_ERROR) -> UNDEPLOYING -> UNDEPLOYED |

## 4. Beam mapping of hooks (reference engine)

| Beam callback | TCK call |
|---|---|
| `DoFn.@Setup` | `Processor.start()` |
| `DoFn.@ProcessElement` | `beforeGroup()` (count 0) then `onNext()` [then `afterGroup()` at `maxBatchSize`] |
| `DoFn.@FinishBundle` | `afterGroup()` if `count>0` |
| `DoFn.@Teardown` | `Processor.stop()` |
| `BoundedSource.split/getEstimatedSizeBytes/createReader` | `Mapper.start(); split/assess/create; Mapper.stop()` |
| `Reader.start()` | `Input.start()` then first `next()` |
| `Reader.advance()` | `Input.next()` |
| `Reader.close()` | `Input.stop()` |
