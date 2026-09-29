# Feature Catalog - RUN: Runtime execution

> Framework version documented: **1.2611.0-SNAPSHOT** (root `pom.xml`). Category prefix: `RUN-`. Generated from the same data as [`index.RUN.json`](index.RUN.json); both agree exactly.

Scope: component types as seen by a runtime (input mapper/emitter/producer, processor/output, standalone), groups, named branches, streaming, checkpoint, Studio-only variables, serialization, configuration delivery, and the reference execution engines (local Job runner, Beam). Narrative: [`../06-runtime-execution.md`](../06-runtime-execution.md); lifecycle hook table: [`../10-appendix/lifecycle-hooks.md`](../10-appendix/lifecycle-hooks.md); keys: [`../10-appendix/runtime-configuration-keys.md`](../10-appendix/runtime-configuration-keys.md). Plugin loading is in [`LCM-lifecycle.md`](LCM-lifecycle.md).

Levels (see `../09-integration-checklist.md`): 0 = mandatory, 1 = normal user experience, 2 = full TCK.

## Index

| ID | Name | Level | Designer | Runtime |
|---|---|---|---|---|
| [RUN-001](#run-001-emitter) | `@Emitter` | 0 | MUST show it as an input component (no input flow, one `__default__` output flow). | MUST obtain it through `ComponentManager.findMapper(plugin, name, version, config)`, call `create()` on the mapper and drive the `Input` (RUN-024). |
| [RUN-002](#run-002-partitionmapper) | `@PartitionMapper` | 0 | MUST show it as an input component; if `stoppable`, MUST render the extra options `$maxRecords` and `$maxDurationMs` returned in the component properties (default -1 = unlimited). | MUST call `assess()`, `split(desiredSize)` on the coordinator side and `create()` per split on the worker side; MUST treat `isStream()` as streaming mode. |
| [RUN-003](#run-003-assessor) | `@Assessor` | 0 | none | MUST call `Mapper.assess()` on a started mapper and use the value to compute the split size; the unit is component-defined (bytes by convention). |
| [RUN-004](#run-004-split) | `@Split` | 0 | none | MUST call `Mapper.split(desiredSize)` and create one `Input` per returned Mapper (sequentially or in parallel); returned mappers are Serializable and MAY be shipped to workers. |
| [RUN-005](#run-005-partitionsize) | `@PartitionSize` | 0 | none | MUST pass the value it wants to `split(long)`; for `int` parameters the runtime narrows with `intValue()`. |
| [RUN-006](#run-006-producer) | `@Producer` | 0 | none | MUST loop `Input.next()` until it returns null (batch) or until the stop strategy / external stop (streaming); MUST call `Input.start()` before and `Input.stop()` after. |
| [RUN-007](#run-007-bufferizedproducersupport) | `BufferizedProducerSupport` | 2 | none | none |
| [RUN-008](#run-008-processor) | `@Processor` | 0 | MUST render one input connection per input flow and one output connection per output flow returned by the server. | MUST create it with `ComponentManager.findProcessor(...)`, call `start()`, then per group `beforeGroup()`, `onNext(...)` per record, `afterGroup(...)`, finally `stop()`. |
| [RUN-009](#run-009-elementlistener) | `@ElementListener` | 0 | none | MUST feed each record through `Processor.onNext(InputFactory, OutputFactory)`; InputFactory.read(name) returns the current record of that input branch (null if none). |
| [RUN-010](#run-010-beforegroup) | `@BeforeGroup` | 0 | none | MUST call `Processor.beforeGroup()` before the first `onNext` of each group. |
| [RUN-011](#run-011-aftergroup) | `@AfterGroup` | 0 | none | MUST call `Processor.afterGroup(OutputFactory)` (or `afterGroup(OutputFactory, boolean last)` when `isLastGroupUsed()`) at group end and at end of data (flush); MUST route emitted records like `onNext` outputs. |
| [RUN-012](#run-012-lastgroup) | `@LastGroup` | 1 | none | MUST call `afterGroup(output, last)` when `Processor.isLastGroupUsed()` is true, with `last=true` exactly on the final flush. |
| [RUN-013](#run-013-input) | `@Input` | 1 | MUST create one input connection per distinct name (`ComponentDetail.inputFlows`) and label them. | MUST return, in `InputFactory.read(name)`, the record for that branch. |
| [RUN-014](#run-014-output) | `@Output` | 0 | MUST create one output connection per branch (`ComponentDetail.outputFlows`); connection named `REJECT` is the reject connection. | MUST provide an `OutputFactory` whose `create(name)` returns an emitter for every name; unknown/unconnected branches MUST NOT fail (discard). |
| [RUN-015](#run-015-outputemitter) | `OutputEmitter` | 0 | none | MUST implement it in `OutputFactory.create(name)`; MUST convert non-Record values. |
| [RUN-016](#run-016-multioutputiterator) | `MultiOutputIterator` | 2 | SHOULD display all branches listed in `@Output(branches)`. | MAY implement `createMultiOutputIterator()`; if not implemented, components using it fail at runtime (Beam reference does not). |
| [RUN-017](#run-017-taggedoutput) | `TaggedOutput` | 2 | none | MAY interpret `FLOW` as `__default__` if MultiOutputIterator is implemented. |
| [RUN-018](#run-018-named-branches-__default__-reject) | `Named branches (__default__, REJECT)` | 1 | MUST expose one connection per name in `inputFlows`/`outputFlows`; SHOULD style `REJECT` differently. | MUST route by exact branch name; job edges default to `__default__` (`Job.from(id)` / `to(id)`). |
| [RUN-019](#run-019-output-component-sink) | `Output component (sink)` | 0 | MUST treat a processor with empty `outputFlows` as a terminal component (no outgoing connection allowed). | MUST run it as the last stage; it MUST still get `beforeGroup/afterGroup` calls and a final flush. |
| [RUN-020](#run-020-combiner-not-supported) | `Combiner (not supported)` | 2 | none | none |
| [RUN-021](#run-021-driverrunner) | `@DriverRunner` | 0 | MUST show it without connections; SHOULD allow it in a job as an independent step/trigger target. | MUST obtain it with `ComponentManager.findDriverRunner(plugin, name, version, config)`, call `start()`, `runAtDriver()`, `stop()` on the coordinator; it MUST NOT handle records. |
| [RUN-022](#run-022-runatdriver) | `@RunAtDriver` | 0 | none | MUST invoke `DriverRunner.runAtDriver()` exactly once per execution, inside the plugin classloader (TCCL) - `DriverRunnerImpl` handles it. |
| [RUN-023](#run-023-component-lifecycle-hooks-postconstruct--predestroy) | `Component lifecycle hooks (@PostConstruct / @PreDestroy)` | 0 | none | MUST call `start()` before use and `stop()` in a finally block, on every component instance including every split Input; MUST NOT reuse a stopped instance. |
| [RUN-024](#run-024-runtime-wrapper-interfaces-lifecycle-mapper-input-processor-driverrunner) | `Runtime wrapper interfaces (Lifecycle, Mapper, Input, Processor, DriverRunner)` | 0 | none | MUST code the host engine against these interfaces (they are the only stable surface with the manager). |
| [RUN-025](#run-025-component-instantiation-componentmanagerfind) | `Component instantiation (ComponentManager.find*)` | 0 | MUST persist the component `version` together with each saved configuration (ComponentDetail.version at design time). | MUST pass the persisted version so migration runs; MUST handle empty Optional as 'component missing'. |
| [RUN-026](#run-026-batch-grouping-and-maxbatchsize) | `Batch grouping and maxBatchSize` | 1 | MUST render `$maxBatchSize` (it is delivered as a normal property) and send its value back in the flat configuration. | MUST apply `$maxBatchSize` as an upper bound of the group size (default 1 in the local runner when absent) and MUST always flush at end of data. |
| [RUN-027](#run-027-streaming-input-infinite-mapper) | `Streaming input (infinite mapper)` | 2 | SHOULD flag `mapper::infinite=true` components as streaming (job never ends unless stop conditions are set). | MUST treat `Mapper.isStream()` as unbounded; MUST provide a way to stop (call `Input.stop()` or set stop conditions). |
| [RUN-028](#run-028-streaming-stop-conditions-maxrecords-maxdurationms) | `Streaming stop conditions ($maxRecords, $maxDurationMs)` | 2 | MUST render the two options for stoppable inputs (they are ordinary properties). | MUST honour them by passing the flat properties to `findMapper`; the framework enforces them in `StreamingInputImpl`. |
| [RUN-029](#run-029-streaming-retry-strategy) | `Streaming retry strategy` | 2 | none | SHOULD expose these keys through its LocalConfiguration implementation (family-prefixed keys `<family>.<key>` are tried first). |
| [RUN-030](#run-030-checkpoint) | `@Checkpoint` | 2 | SHOULD hide checkpoint properties from the normal form (they are state), or show them in the `CHECKPOINT` form type. | MUST persist the state emitted by RUN-031/RUN-033 and MUST re-inject it as `$checkpoint.*` at restart; only when `talend.checkpoint.enabled=true`. |
| [RUN-031](#run-031-checkpointdata) | `@CheckpointData` | 2 | none | MUST call `Input.getCheckpoint()` only when `isCheckpointReady()` is true (or via the callback) and MUST serialize the state. |
| [RUN-032](#run-032-checkpointavailable) | `@CheckpointAvailable` | 2 | none | MUST poll `Input.isCheckpointReady()` after `next()` (explicit mode) or supply a callback to `Input.start(Consumer<CheckpointState>)` (automatic mode). |
| [RUN-033](#run-033-checkpoint-runtime-protocol-checkpoint-checkpointstate) | `Checkpoint runtime protocol ($checkpoint, CheckpointState)` | 2 | none | MUST implement persistence/restore of the JSON above; MUST migrate the state when `__version` is older (the framework's nested migration applies to the checkpoint type). |
| [RUN-034](#run-034-conditionaloutput) | `@ConditionalOutput` | 2 | MAY call `type=available_output` (ACT-014) with the current configuration to filter `outputFlows`. | none |
| [RUN-036](#run-036-returnvariables--returnvariable) | `@ReturnVariables / @ReturnVariable` | 2 | MAY expose the variables to downstream expressions (Studio-like host). | none |
| [RUN-037](#run-037-aftervariables--aftervariable--aftervariablecontainer) | `@AfterVariables / @AfterVariable / @AfterVariableContainer` | 2 | MAY expose the variables. | MAY call the container method after the component finished. |
| [RUN-038](#run-038-optionalrow) | `optionalRow` | 2 | MAY relax the requirement of an outgoing connection for such inputs. | none |
| [RUN-039](#run-039-serialization-requirements) | `Serialization requirements` | 0 | none | MUST ship the plugin to every worker and register it before deserializing; MUST set the whitelist in secured deployments; MUST use `ContainerFinder.Instance.set(...)` when the default finder cannot see plugins. |
| [RUN-040](#run-040-internal-configuration-keys--prefix) | `Internal configuration keys ($-prefix)` | 1 | MUST send them under the same prefix as the other properties of the component configuration root. | MUST forward them unchanged to `findMapper`/`findProcessor`. |
| [RUN-041](#run-041-flat-configuration-to-constructor-arguments) | `Flat configuration to constructor arguments` | 0 | MUST serialize form values into these exact key shapes using each property's `path` from ComponentDetail. | MUST pass the full map (including defaults) to the manager; MUST surface the validation IllegalArgumentException as user error. |
| [RUN-042](#run-042-job-dsl-and-local-runner) | `Job DSL and local runner` | 1 | MAY use `family://name?...` URIs as a portable job serialization for tests. | MAY reuse the Job API as a reference or embedded engine; a custom engine MUST reproduce the semantics (levels, branch wiring, flush). SHOULD be compared against for acceptance tests: the same chain must give the same output in the host. |
| [RUN-043](#run-043-beam-translation-of-a-job) | `Beam translation of a job` | 2 | none | Reference for an engine adapter: MUST reproduce branch filtering/mapping, key-based multi-input join and group semantics on the target engine. |
| [RUN-044](#run-044-beam-io-adapters-talendio-talendfn) | `Beam I/O adapters (TalendIO, TalendFn)` | 2 | none | MAY reuse these classes; if embedded, MUST have `component-runtime-beam` and Beam on the container parent classloader (see `BeamCustomizer` class index). |
| [RUN-045](#run-045-beam-coders-and-schema-registry) | `Beam coders and schema registry` | 2 | none | A distributed host MUST guarantee that the schema id resolves on the decoding side. |
| [RUN-046](#run-046-component-validation-at-registration-modelvisitor) | `Component validation at registration (ModelVisitor)` | 1 | none | A host registering plugins through ComponentManager gets this for free; a host with its own scanner MUST replicate these rules. |
| [RUN-047](#run-047-runtime-mode-unsafe) | `Runtime Mode (UNSAFE)` | 2 | none | MAY set UNSAFE in production once plugins have been validated at build time. |
| [RUN-048](#run-048-input-and-output-flows-flowsfactory) | `Input and output flows (FlowsFactory)` | 0 | MUST use `inputFlows` / `outputFlows` of ComponentDetail to build connectors. | MUST honour the same names when wiring branches. |
| [RUN-049](#run-049-processor-input-conversion-and-group-buffer) | `Processor input conversion and group buffer` | 0 | none | MUST call `beforeGroup()` before the first `onNext` (it initializes reflection state and the buffer) - `onNext` before `beforeGroup` fails with NullPointerException on `parameterBuilderProcess`. |
| [RUN-050](#run-050-multi-input-record-grouping-groupkeyprovider) | `Multi-input record grouping (GroupKeyProvider)` | 2 | MAY let users configure the join key per input. | MUST provide a join strategy for multi-input processors. |

## Entries

### RUN-001 `@Emitter`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/input/Emitter.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-define-input.adoc`)
- **Category / Sub-area**: RUN / Input
- **Kind**: annotation
- **Applies to**: input (class = simple input without mapper; method = producer factory inside a @PartitionMapper)
- **Description**: On a class, declares an input connector that is not partitioned: the runtime wraps it in `LocalPartitionMapper` (assess=1, split=itself, batch). On a method of a `@PartitionMapper`, marks the parameterless factory returning the `@Producer` object (the actual reader). The emitter class MUST be public, Serializable, have exactly one public constructor and exactly one parameterless `@Producer` method.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | family | String | "" | component family (falls back to `@Components(family)` of the package) |
  | name | String | "" | component name (falls back to the class name) |
  | optionalRow | boolean | false | Studio only: the input may ignore its mandatory output row; exposed as metadata `mapper::optionalRow` |

- **Contract for the Designer**: MUST show it as an input component (no input flow, one `__default__` output flow).
- **Contract for the Runtime**: MUST obtain it through `ComponentManager.findMapper(plugin, name, version, config)`, call `create()` on the mapper and drive the `Input` (RUN-024).
- **Server exposure**: `ComponentIndex.type` / `ComponentDetail.type` = `"input"` for mappers/emitters, `"processor"` for processors, `"standalone"` for driver runners (`ComponentResourceImpl`); `inputFlows`/`outputFlows` (RUN-048). Metadata `mapper::infinite=false`, `mapper::optionalRow`.
- **Maturity level**: 0 - no data can enter the job without it.
- **Example**:

```java
@Emitter(family = "demo", name = "numbers")
public class Numbers implements Serializable {
    public Numbers(@Option("configuration") final Config c) { }
    @Producer public Record next() { return null; }
}
```

### RUN-002 `@PartitionMapper`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/input/PartitionMapper.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-partition-mapper.adoc`)
- **Category / Sub-area**: RUN / Input
- **Kind**: annotation
- **Applies to**: input
- **Description**: Marks a splittable input. It MUST expose exactly one `@Split` and one `@Emitter` method, plus one `@Assessor` unless `infinite=true` (validated by `ModelVisitor`). `infinite=true` declares a streaming source (RUN-027); `stoppable=true` (only with infinite, otherwise IllegalArgumentException) adds the built-in stop-condition options `$maxRecords`/`$maxDurationMs` (RUN-028). Must be Serializable; in distributed engines the constructor runs on the coordinator, `@PostConstruct` on workers.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | family | String | "" | component family |
  | name | String | "" | component name (else class name) |
  | infinite | boolean | false | true = streaming input (null from producer does NOT end the flow) |
  | stoppable | boolean | false | only valid with infinite; enables UI/config stop conditions |
  | optionalRow | boolean | false | Studio only; metadata `mapper::optionalRow` |

- **Contract for the Designer**: MUST show it as an input component; if `stoppable`, MUST render the extra options `$maxRecords` and `$maxDurationMs` returned in the component properties (default -1 = unlimited).
- **Contract for the Runtime**: MUST call `assess()`, `split(desiredSize)` on the coordinator side and `create()` per split on the worker side; MUST treat `isStream()` as streaming mode.
- **Server exposure**: `ComponentIndex.type` / `ComponentDetail.type` = `"input"` for mappers/emitters, `"processor"` for processors, `"standalone"` for driver runners (`ComponentResourceImpl`); `inputFlows`/`outputFlows` (RUN-048). Metadata `mapper::infinite`, `mapper::optionalRow`; extra properties named `$maxRecords`, `$maxDurationMs` under the configuration root.
- **Maturity level**: 0 - the standard batch input contract.
- **Example**:

```java
@PartitionMapper(family = "demo", name = "reader", infinite = true, stoppable = true)
public class Reader implements Serializable {
    @Assessor public long estimate() { return 1; }   // optional when infinite
    @Split public List<Reader> split(@PartitionSize final long size) { return Collections.singletonList(this); }
    @Emitter public ReaderSource create() { return new ReaderSource(); }
}
```

### RUN-003 `@Assessor`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/input/Assessor.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-partition-mapper.adoc`)
- **Category / Sub-area**: RUN / Input
- **Kind**: annotation
- **Applies to**: input (method of a @PartitionMapper)
- **Description**: Method returning a `Number` estimating the size of the whole dataset for the current configuration; MUST have no parameter. Absent (allowed for infinite mappers) the runtime returns 1. Used by `Mapper.assess()`.
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST call `Mapper.assess()` on a started mapper and use the value to compute the split size; the unit is component-defined (bytes by convention).
- **Server exposure**: none
- **Maturity level**: 0 - needed to compute a partition plan; default 1 keeps single partition.
- **Example**:

```java
@Assessor public long estimateDataSetByteSize() { return 1024L; }
```

### RUN-004 `@Split`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/input/Split.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-partition-mapper.adoc`)
- **Category / Sub-area**: RUN / Input
- **Kind**: annotation
- **Applies to**: input (method of a @PartitionMapper)
- **Description**: Computes the distribution plan. Return type MUST be a parameterized `Collection<M>` where M is (a subtype of) the mapper class; the only allowed parameters are `int`/`long` annotated `@PartitionSize` (validation error otherwise). Each returned instance is re-wrapped by `PartitionMapperImpl` (same family/name/plugin/internal configuration).
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST call `Mapper.split(desiredSize)` and create one `Input` per returned Mapper (sequentially or in parallel); returned mappers are Serializable and MAY be shipped to workers.
- **Server exposure**: none
- **Maturity level**: 0 - core of the mapper contract.
- **Example**:

```java
@Split public List<Reader> split(@PartitionSize final long desiredSize) { ... }
```

### RUN-005 `@PartitionSize`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/input/PartitionSize.java`
- **Category / Sub-area**: RUN / Input
- **Kind**: annotation
- **Applies to**: input (parameter of @Split, type long or int)
- **Description**: Injects the `desiredSize` argument of `Mapper.split(long)`. Javadoc: number of desired partitions for infinite connectors, estimated size of each partition for finite ones (Beam module passes `desiredBundleSizeBytes` for bounded and `desiredNumSplits` for unbounded sources). Any other parameter type -> IllegalArgumentException `@PartitionSize only supports int and long`.
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST pass the value it wants to `split(long)`; for `int` parameters the runtime narrows with `intValue()`.
- **Server exposure**: none
- **Maturity level**: 0 - argument semantics of the mandatory split call.
- **Example**: none

### RUN-006 `@Producer`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/input/Producer.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-producer.adoc`)
- **Category / Sub-area**: RUN / Input
- **Kind**: annotation
- **Applies to**: input (method of the emitter object)
- **Description**: Parameterless method returning the next element or `null`. Batch: null = end of data. Streaming (`infinite`): null = no data now, the runtime applies the retry strategy (RUN-029) and calls again. The returned object is converted to `Record` (DAT-029). Called by `InputImpl.next()`.
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST loop `Input.next()` until it returns null (batch) or until the stop strategy / external stop (streaming); MUST call `Input.start()` before and `Input.stop()` after.
- **Server exposure**: none
- **Maturity level**: 0 - the data source call itself.
- **Example**:

```java
@Producer public Record next() { return it.hasNext() ? convert(it.next()) : null; }
```

### RUN-007 `BufferizedProducerSupport`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/base/BufferizedProducerSupport.java`
- **Category / Sub-area**: RUN / Input
- **Kind**: class
- **Applies to**: input (helper for component authors)
- **Description**: Helper to implement `@Producer` over paged sources: `next()` refills from `Supplier<Iterator<T>>` when the current iterator is exhausted and returns null when the supplier yields nothing.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | BufferizedProducerSupport(Supplier<Iterator<T>>) | constructor | - | page supplier |
  | next() | T | - | next item or null |

- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none
- **Maturity level**: 2 - author-side helper, no host contract.
- **Example**: none

### RUN-008 `@Processor`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/processor/Processor.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-processor.adoc`)
- **Category / Sub-area**: RUN / Processor
- **Kind**: annotation
- **Applies to**: processor, output
- **Description**: Marks a class transforming, filtering or consuming records. It MUST be Serializable, have a single `@ElementListener` method (or an `@AfterGroup` taking a `Collection<Record|JsonObject>`), optional `@BeforeGroup`/`@AfterGroup`. A processor with no output branch is an output component (RUN-019).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | family | String | "" | component family |
  | name | String | "" | component name (else class name) |

- **Contract for the Designer**: MUST render one input connection per input flow and one output connection per output flow returned by the server.
- **Contract for the Runtime**: MUST create it with `ComponentManager.findProcessor(...)`, call `start()`, then per group `beforeGroup()`, `onNext(...)` per record, `afterGroup(...)`, finally `stop()`.
- **Server exposure**: `ComponentIndex.type` / `ComponentDetail.type` = `"input"` for mappers/emitters, `"processor"` for processors, `"standalone"` for driver runners (`ComponentResourceImpl`); `inputFlows`/`outputFlows` (RUN-048).
- **Maturity level**: 0 - processing/writing data is the second half of any job.
- **Example**:

```java
@Processor(family = "demo", name = "upper")
public class Upper implements Serializable {
    @ElementListener public Record map(final Record in) { ... }
}
```

### RUN-009 `@ElementListener`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/processor/ElementListener.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-processor.adoc`)
- **Category / Sub-area**: RUN / Processor
- **Kind**: annotation
- **Applies to**: processor, output
- **Description**: The per-record method. Parameters: unannotated or `@Input(name)` parameters receive input records (converted to the parameter type: Record, JsonObject or POJO); `@Output(name) OutputEmitter<T>`/`MultiOutputIterator<T>` parameters receive emitters. A non-void return value is emitted on `__default__`. Validation: at most one such method; at least one non-`@Output` parameter. Javadoc text is a copy-paste error ('returning an input connector') - actual semantics as above.
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST feed each record through `Processor.onNext(InputFactory, OutputFactory)`; InputFactory.read(name) returns the current record of that input branch (null if none).
- **Server exposure**: none
- **Maturity level**: 0 - core processor contract.
- **Example**:

```java
@ElementListener
public Record process(@Input final Record in, @Output("REJECT") final OutputEmitter<Record> rejected) { ... }
```

### RUN-010 `@BeforeGroup`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/processor/BeforeGroup.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/concept-processor-and-batch-processing.adoc`)
- **Category / Sub-area**: RUN / Processor
- **Kind**: annotation
- **Applies to**: processor, output
- **Description**: Parameterless void method called before the first element of a group (bundle). Without group handling by the host it is called around every element. `ProcessorImpl.beforeGroup()` also resets the record buffer used by collection-style `@AfterGroup`.
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST call `Processor.beforeGroup()` before the first `onNext` of each group.
- **Server exposure**: none
- **Maturity level**: 0 - components reset buffers there; skipping it corrupts batch state.
- **Example**:

```java
@BeforeGroup public void begin() { buffer = new ArrayList<>(); }
```

### RUN-011 `@AfterGroup`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/processor/AfterGroup.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/concept-processor-and-batch-processing.adoc`)
- **Category / Sub-area**: RUN / Processor
- **Kind**: annotation
- **Applies to**: processor, output
- **Description**: Method called after a group (may run several times, one method or many). Allowed parameters: `@Output` emitters/iterators, `Collection<Record|JsonObject>` (the group buffer - only when there is no `@ElementListener`; records are collected by `onNext`), `@LastGroup boolean` (RUN-012). Any other parameter -> IllegalArgumentException `Parameter of AfterGroup method need to be annotated with Output`. The host MUST call it after the last element even if the group is not full (flush), otherwise buffered writes are lost.
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST call `Processor.afterGroup(OutputFactory)` (or `afterGroup(OutputFactory, boolean last)` when `isLastGroupUsed()`) at group end and at end of data (flush); MUST route emitted records like `onNext` outputs.
- **Server exposure**: none
- **Maturity level**: 0 - final flush is required for correctness of bulk outputs.
- **Example**:

```java
@AfterGroup public void commit(final Collection<Record> records) { db.bulkInsert(records); }
```

### RUN-012 `@LastGroup`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/processor/LastGroup.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/concept-processor-and-batch-processing.adoc`)
- **Category / Sub-area**: RUN / Processor
- **Kind**: annotation
- **Applies to**: processor, output (Boolean parameter of @AfterGroup)
- **Description**: Receives `true` on the final `@AfterGroup` call (no more groups), `false` otherwise. Only one `@AfterGroup` method may declare it. The runtime appends the value at the END of the argument list, so the parameter MUST be the last one (inferred from `ProcessorImpl.afterGroup(output, last)`).
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST call `afterGroup(output, last)` when `Processor.isLastGroupUsed()` is true, with `last=true` exactly on the final flush.
- **Server exposure**: none
- **Maturity level**: 1 - needed by connectors doing a final commit; others work without it.
- **Example**:

```java
@AfterGroup public void after(@Output("REJECT") OutputEmitter<Record> rejected, @LastGroup Boolean last) { if (last) { ... } }
```

### RUN-013 `@Input`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/processor/Input.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-processor.adoc`)
- **Category / Sub-area**: RUN / Processor
- **Kind**: annotation
- **Applies to**: processor, output (parameter)
- **Description**: Names the input branch feeding a parameter (default `__default__`). Several `@Input` parameters model multi-input processors (e.g. lookup/join); records of different branches are then delivered together (grouped by key, RUN-050).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | value | String | "__default__" | input branch name |

- **Contract for the Designer**: MUST create one input connection per distinct name (`ComponentDetail.inputFlows`) and label them.
- **Contract for the Runtime**: MUST return, in `InputFactory.read(name)`, the record for that branch.
- **Server exposure**: `inputFlows` in ComponentDetail.
- **Maturity level**: 1 - single-input processors (default) work without it.
- **Example**:

```java
@ElementListener public Record join(@Input final Record main, @Input("lookup") final Record lookup) { ... }
```

### RUN-014 `@Output`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/processor/Output.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-processor.adoc`)
- **Category / Sub-area**: RUN / Processor
- **Kind**: annotation
- **Applies to**: processor, output (parameter of @ElementListener/@AfterGroup)
- **Description**: Marks an `OutputEmitter<T>` or `MultiOutputIterator<T>` parameter as feeding an output branch. `value` names the branch; `branches` (MultiOutputIterator only, else IllegalArgumentException `@Output#branches is only supported on MultiOutputIterator parameters`) declares several branches for the design layer; when empty, `value` is used (`OutputBranches.of`). A non-generic-parameterized parameter is invalid (`@Output parameter must be of type OutputEmitter or MultiOutputIterator`).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | value | String | "__default__" | branch name |
  | branches | String[] | {} | branch names for MultiOutputIterator |

- **Contract for the Designer**: MUST create one output connection per branch (`ComponentDetail.outputFlows`); connection named `REJECT` is the reject connection.
- **Contract for the Runtime**: MUST provide an `OutputFactory` whose `create(name)` returns an emitter for every name; unknown/unconnected branches MUST NOT fail (discard).
- **Server exposure**: `outputFlows` in ComponentDetail.
- **Maturity level**: 0 - any processor emitting through parameters needs it; single default branch is the minimum.
- **Example**:

```java
@ElementListener public void map(final Record in, @Output final OutputEmitter<Record> main,
    @Output("REJECT") final OutputEmitter<Record> reject) { ... }
```

### RUN-015 `OutputEmitter`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/processor/OutputEmitter.java`
- **Category / Sub-area**: RUN / Processor
- **Kind**: interface
- **Applies to**: processor, output
- **Description**: `void emit(T value)` pushes one value to the branch. The value may be Record, JsonObject or POJO (converted to Record, DAT-029); `null` is ignored by the Beam implementation.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | emit(T) | void | - | push a value |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST implement it in `OutputFactory.create(name)`; MUST convert non-Record values.
- **Server exposure**: none
- **Maturity level**: 0 - the way processors emit data.
- **Example**: none

### RUN-016 `MultiOutputIterator`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/processor/MultiOutputIterator.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-processor.adoc`)
- **Category / Sub-area**: RUN / Processor
- **Kind**: interface
- **Applies to**: processor (Studio DI runtime only)
- **Description**: Lazy, unbuffered alternative to `OutputEmitter`. Two mutually exclusive modes per invocation: split mode `setIterator(Iterator<TaggedOutput<T>>)` (one source routed per record) and independent mode `setIterator(String outputName, Iterator<T>)` (one iterator per output, consumed in parallel). `OutputFactory.createMultiOutputIterator()` throws UnsupportedOperationException `MultiOutputIterator is only supported in the Studio DI runtime` by default.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | setIterator(Iterator<TaggedOutput<T>>) | void | - | split mode |
  | setIterator(String, Iterator<T>) | void | - | independent mode |

- **Contract for the Designer**: SHOULD display all branches listed in `@Output(branches)`.
- **Contract for the Runtime**: MAY implement `createMultiOutputIterator()`; if not implemented, components using it fail at runtime (Beam reference does not).
- **Server exposure**: `outputFlows` contains the declared branches.
- **Maturity level**: 2 - Studio-DI-only optimization.
- **Example**:

```java
@ElementListener public void map(final Record in, @Output(branches = {"MAIN","REJECT"}) MultiOutputIterator<Record> out) {
    out.setIterator(source.stream().map(r -> ok(r) ? TaggedOutput.of("MAIN", r) : TaggedOutput.of("REJECT", r)).iterator());
}
```

### RUN-017 `TaggedOutput`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/processor/TaggedOutput.java`
- **Category / Sub-area**: RUN / Processor
- **Kind**: class
- **Applies to**: processor (Studio DI)
- **Description**: Value object pairing a record with the target branch (`outputName`, `record`); factory `TaggedOutput.of(name, record)`. `__default__` or `FLOW` denote the default output.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | outputName | String | - | target branch |
  | record | T | - | value |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY interpret `FLOW` as `__default__` if MultiOutputIterator is implemented.
- **Server exposure**: none
- **Maturity level**: 2 - part of the Studio-only MultiOutputIterator.
- **Example**: none

### RUN-018 `Named branches (__default__, REJECT)`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/output/Branches.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-processor.adoc`)
- **Category / Sub-area**: RUN / Processor
- **Kind**: convention
- **Applies to**: processor, output
- **Description**: `Branches.DEFAULT_BRANCH = "__default__"` is the implicit branch of unnamed `@Input`/`@Output` and of the return value. `REJECT` is the reserved name of the (single) reject connection recognised by Talend applications. Studio calls the main flow `FLOW`/`MAIN` in javadoc; the framework keys remain `__default__`. The Beam module sanitizes branch names (`sanitizeName`) when wrapping them in records.
- **Attributes**: none
- **Contract for the Designer**: MUST expose one connection per name in `inputFlows`/`outputFlows`; SHOULD style `REJECT` differently.
- **Contract for the Runtime**: MUST route by exact branch name; job edges default to `__default__` (`Job.from(id)` / `to(id)`).
- **Server exposure**: `inputFlows`, `outputFlows`.
- **Maturity level**: 1 - single-output pipelines work at level 0; multi-output/reject is the normal-UX feature.
- **Example**:

```json
{"inputFlows": ["__default__"], "outputFlows": ["__default__", "REJECT"]}
```

### RUN-019 `Output component (sink)`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/processor/Processor.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-output.adoc`)
- **Category / Sub-area**: RUN / Processor
- **Kind**: convention
- **Applies to**: output
- **Description**: An output is a `@Processor` whose `@ElementListener` returns void and declares no `@Output` parameter (no output flow). The server exposes it as `type="processor"` with empty `outputFlows` (there is no separate `output` type). Beam translation applies `TalendIO.write`.
- **Attributes**: none
- **Contract for the Designer**: MUST treat a processor with empty `outputFlows` as a terminal component (no outgoing connection allowed).
- **Contract for the Runtime**: MUST run it as the last stage; it MUST still get `beforeGroup/afterGroup` calls and a final flush.
- **Server exposure**: `type=processor`, `outputFlows=[]`.
- **Maturity level**: 0 - writing data is a core use case.
- **Example**:

```java
@Processor(family = "demo", name = "sink")
public class Sink implements Serializable { @ElementListener public void write(final Record r) { } }
```

### RUN-020 `Combiner (not supported)`
- **Source**: `documentation/src/main/antora/modules/ROOT/pages/component-combiner.adoc` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-combiner.adoc`)
- **Category / Sub-area**: RUN / Processor
- **Kind**: convention
- **Applies to**: n/a
- **Description**: The documentation states the framework does not (yet) allow defining a combiner (symmetric to the partition mapper, aggregating results in one partition). No annotation exists in `component-api`. Aggregation MUST be implemented in a processor with groups.
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: none
- **Server exposure**: none
- **Maturity level**: 2 - feature absent; documented for completeness.
- **Example**: none

### RUN-021 `@DriverRunner`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/standalone/DriverRunner.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-define-standalone.adoc`)
- **Category / Sub-area**: RUN / Standalone
- **Kind**: annotation
- **Applies to**: standalone
- **Description**: Marks a component with neither input nor output flows, run once on the driver node (e.g. create an index). MUST have exactly one parameterless `@RunAtDriver` method. Wrapped by `DriverRunnerImpl`. Exposed as component `type="standalone"` with empty flows.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | family | String | "" | component family |
  | name | String | "" | component name |

- **Contract for the Designer**: MUST show it without connections; SHOULD allow it in a job as an independent step/trigger target.
- **Contract for the Runtime**: MUST obtain it with `ComponentManager.findDriverRunner(plugin, name, version, config)`, call `start()`, `runAtDriver()`, `stop()` on the coordinator; it MUST NOT handle records.
- **Server exposure**: `ComponentIndex.type` / `ComponentDetail.type` = `"input"` for mappers/emitters, `"processor"` for processors, `"standalone"` for driver runners (`ComponentResourceImpl`); `inputFlows`/`outputFlows` (RUN-048).
- **Maturity level**: 0 - standalone lifecycle is listed as mandatory for Level 0.
- **Example**:

```java
@DriverRunner(family = "demo", name = "createIndex")
public class CreateIndex implements Serializable { @RunAtDriver public void run() { } }
```

### RUN-022 `@RunAtDriver`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/standalone/RunAtDriver.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-driver-runner.adoc`)
- **Category / Sub-area**: RUN / Standalone
- **Kind**: annotation
- **Applies to**: standalone (method)
- **Description**: The execution method of a `@DriverRunner`. Public, parameterless (validation: exactly one, no parameter, otherwise IllegalArgumentException).
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST invoke `DriverRunner.runAtDriver()` exactly once per execution, inside the plugin classloader (TCCL) - `DriverRunnerImpl` handles it.
- **Server exposure**: none
- **Maturity level**: 0 - sole entry point of standalone components.
- **Example**:

```java
@RunAtDriver public void run() { service.createIndex(cfg); }
```

### RUN-023 `Component lifecycle hooks (@PostConstruct / @PreDestroy)`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/base/LifecycleImpl.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-execution.adoc`)
- **Category / Sub-area**: RUN / Lifecycle
- **Kind**: convention
- **Applies to**: input, processor, output, standalone, service
- **Description**: `javax.annotation.PostConstruct` / `PreDestroy` methods (public) are invoked by `Lifecycle.start()` / `stop()` with the plugin classloader as TCCL (`doInvoke`), on the worker (after deserialization). Constructors run on the coordinator. Streaming inputs may receive `@Option("maxDurationMs")`/`@Option("maxRecords")` parameters on `@PostConstruct` (RUN-028). Private methods are ignored (`getMethods()`). Detailed hook table: `../10-appendix/lifecycle-hooks.md`.
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST call `start()` before use and `stop()` in a finally block, on every component instance including every split Input; MUST NOT reuse a stopped instance.
- **Server exposure**: none
- **Maturity level**: 0 - resource acquisition/release depends on it.
- **Example**:

```java
@PostConstruct public void init() { client = open(cfg); }
@PreDestroy public void close() { client.close(); }
```

### RUN-024 `Runtime wrapper interfaces (Lifecycle, Mapper, Input, Processor, DriverRunner)`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/input/Mapper.java`
- **Category / Sub-area**: RUN / Lifecycle
- **Kind**: interface
- **Applies to**: host runtime
- **Description**: Host-facing interfaces of `component-runtime-impl`: `Lifecycle{plugin(),rootName(),name(),start(),stop()}`; `Mapper extends Lifecycle{long assess(); List<Mapper> split(long); Input create(); boolean isStream()}`; `Input extends Lifecycle{Object next(); start(Consumer<CheckpointState>); getCheckpoint(); isCheckpointReady()}`; `Processor extends Lifecycle{beforeGroup(); afterGroup(OutputFactory[, boolean last]); isLastGroupUsed(); onNext(InputFactory, OutputFactory)}`; `DriverRunner extends Lifecycle{runAtDriver()}`; `InputFactory{Object read(String)}`, `OutputFactory{OutputEmitter create(String); createMultiOutputIterator()}`. Implementations `PartitionMapperImpl`, `LocalPartitionMapper`, `InputImpl`, `StreamingInputImpl`, `ProcessorImpl`, `DriverRunnerImpl`. All wrappers are Serializable via `writeReplace` (RUN-039). The checkpoint defaults throw UnsupportedOperationException.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | rootName() | String | - | family name |
  | name() | String | - | component name |
  | plugin() | String | - | plugin (container) id |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST code the host engine against these interfaces (they are the only stable surface with the manager).
- **Server exposure**: none
- **Maturity level**: 0 - the whole runtime integration is written against them.
- **Example**: none

### RUN-025 `Component instantiation (ComponentManager.find*)`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/ComponentManager.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-execution.adoc`)
- **Category / Sub-area**: RUN / Lifecycle
- **Kind**: convention
- **Applies to**: host runtime
- **Description**: `ComponentManager.instance()` (contextual singleton) exposes `findMapper`, `findProcessor`, `findDriverRunner` (`plugin`=family name or plugin id, `name`, `version` = configuration version stored with the saved config, `configuration` = flat `Map<String,String>`) returning `Optional`. Steps: auto-discover plugins if none (`autoDiscoverPluginsIfEmpty`), merge checkpoint config (RUN-033), resolve family+name in each container (`ComponentInstantiator`), apply migration (LCM-003), evaluate constructor parameters (RUN-041), wrap in the runtime implementation. `createComponent(plugin,name,ComponentType,version,config)` returns the raw delegate. `ComponentType` = MAPPER | PROCESSOR | DRIVER_RUNNER.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | plugin | String | - | family name (`buildAutoIdFromName` applied) - the lookup is by family |
  | name | String | - | component name |
  | version | int | - | version of the persisted configuration |
  | configuration | Map<String,String> | - | flat properties, prefix and format see RUN-041 |

- **Contract for the Designer**: MUST persist the component `version` together with each saved configuration (ComponentDetail.version at design time).
- **Contract for the Runtime**: MUST pass the persisted version so migration runs; MUST handle empty Optional as 'component missing'.
- **Server exposure**: ComponentId `family`/`name`/`plugin`; ComponentDetail `version`.
- **Maturity level**: 0 - the runtime entry point.
- **Example**:

```java
Mapper mapper = ComponentManager.instance().findMapper("demo", "numbers", 1, Map.of("configuration.limit", "10")).orElseThrow();
```

### RUN-026 `Batch grouping and maxBatchSize`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/chain/AutoChunkProcessor.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/concept-processor-and-batch-processing.adoc`)
- **Category / Sub-area**: RUN / Processor
- **Kind**: convention
- **Applies to**: processor, output with @AfterGroup
- **Description**: Group size is decided by the host. When a processor class has an `@AfterGroup` method, the manager appends a built-in option `$maxBatchSize` (Integer, metadata `tcomp::ui::defaultvalue::value` = default 1000, `tcomp::validation::min` = 1, Advanced tab/last position in the layout; i18n `Max batch size`). Defaults come from LocalConfiguration: `<ComponentSimpleClassName>._maxBatchSize.value` / `_maxBatchSize.active` then `_maxBatchSize.value` / `_maxBatchSize.active` (active default true; inactive -> option not added). The runtime reads the configured value from the `$`-prefixed internal configuration (key ending `$maxBatchSize`). Reference algorithms: `AutoChunkProcessor` (beforeGroup at count 0, afterGroup when count == chunkSize, `flush` at end) and Beam `BaseProcessorFn` (bundle = group; `maxBatchSize>0` forces afterGroup every N elements).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | $maxBatchSize | Integer option | 1000 (LocalConfiguration `_maxBatchSize.value`) | max group size; min 1 |
  | _maxBatchSize.active | LocalConfiguration key | true | false hides the option |

- **Contract for the Designer**: MUST render `$maxBatchSize` (it is delivered as a normal property) and send its value back in the flat configuration.
- **Contract for the Runtime**: MUST apply `$maxBatchSize` as an upper bound of the group size (default 1 in the local runner when absent) and MUST always flush at end of data.
- **Server exposure**: Property `$maxBatchSize` (type NUMBER) added to the configuration root by `ComponentManager` (`MaxBatchSizeParamBuilder`).
- **Maturity level**: 1 - without it groups are engine-sized; correct but not user-controllable.
- **Example**:

```json
{"configuration.$maxBatchSize": "500"}
```

### RUN-027 `Streaming input (infinite mapper)`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/input/StreamingInputImpl.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-implementing-streaming.adoc`)
- **Category / Sub-area**: RUN / Streaming
- **Kind**: convention
- **Applies to**: input with `@PartitionMapper(infinite = true)`
- **Description**: `PartitionMapperImpl.create()` returns a `StreamingInputImpl` when `stream=true`. `readNext()`: returns null when `!running` or the stop strategy fires; otherwise loops (under a 1-permit semaphore) calling the producer; a non-null value resets the retry strategy and increments `readRecords`; null triggers `retries--` and a pause (`RetryStrategy.nextPauseDuration()`; <0 = give up and stop; pauses >=1s are sliced in 250 ms steps so that stop is responsive). With `maxDurationMs` set, each read runs in a single-thread executor with timeout = maxDuration + 3000 ms grace minus elapsed. `start()` registers a JVM shutdown hook flipping `running`; `stop()` waits for the semaphore. A `null` from `next()` therefore means 'the stream is over' (stop condition, give-up or stop()), not 'no data yet'.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | talend.input.streaming.retry.maxRetries | LocalConfiguration | Integer.MAX_VALUE | max consecutive empty reads |

- **Contract for the Designer**: SHOULD flag `mapper::infinite=true` components as streaming (job never ends unless stop conditions are set).
- **Contract for the Runtime**: MUST treat `Mapper.isStream()` as unbounded; MUST provide a way to stop (call `Input.stop()` or set stop conditions).
- **Server exposure**: Metadata `mapper::infinite`.
- **Maturity level**: 2 - streaming is a Level 2 capability in the checklist rules.
- **Example**: none

### RUN-028 `Streaming stop conditions ($maxRecords, $maxDurationMs)`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/input/Streaming.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-implementing-streaming.adoc`)
- **Category / Sub-area**: RUN / Streaming
- **Kind**: convention
- **Applies to**: input with infinite + stoppable
- **Description**: Two stop conditions (combinable, -1 = unlimited): `maxRecords` and `maxDurationMs`. Resolution order in `Streaming.loadStopStrategy`: internal configuration key starting with or containing `.` + `$maxRecords` / `$maxDurationMs` (i.e. property passed in the flat config), then JVM property `<plugin>.talend.input.streaming.maxRecords|maxDurationMs`, then LocalConfiguration `talend.input.streaming.maxRecords|maxDurationMs`. The manager adds options `$maxRecords` and `$maxDurationMs` (Long, default from LocalConfiguration `<Class>$maxRecords` else `$maxRecords` else -1; min -1) for `stoppable` mappers. Component code reads them via `@PostConstruct` parameters `@Option(Option.MAX_RECORDS_PARAMETER="maxRecords")` and `@Option(Option.MAX_DURATION_PARAMETER="maxDurationMs")` (int/long; other types get null + warning). Beam job properties: `streaming.maxRecords` (local runner and BeamExecutor default 1000) and `streaming.maxDurationMs` (BeamExecutor default 60000).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | $maxRecords | Long option | -1 | stop after N records |
  | $maxDurationMs | Long option | -1 | stop after N milliseconds |
  | streaming.maxRecords / streaming.maxDurationMs | job properties | -1 local / 1000 & 60000 Beam | engine-level defaults |

- **Contract for the Designer**: MUST render the two options for stoppable inputs (they are ordinary properties).
- **Contract for the Runtime**: MUST honour them by passing the flat properties to `findMapper`; the framework enforces them in `StreamingInputImpl`.
- **Server exposure**: Properties `$maxRecords`, `$maxDurationMs` under the configuration root; layout key `tcomp::ui::gridlayout::Advanced::value` (or matching layout type).
- **Maturity level**: 2 - same as RUN-027.
- **Example**:

```json
{"configuration.$maxRecords": "1000", "configuration.$maxDurationMs": "60000"}
```

### RUN-029 `Streaming retry strategy`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/input/Streaming.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-implementing-streaming.adoc`)
- **Category / Sub-area**: RUN / Streaming
- **Kind**: convention
- **Applies to**: input with infinite = true
- **Description**: Read from the plugin's LocalConfiguration by `Streaming.loadRetryConfiguration`.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | talend.input.streaming.retry.maxRetries | int | Integer.MAX_VALUE | max retries |
  | talend.input.streaming.retry.strategy | constant\|exponential | constant | pause strategy |
  | talend.input.streaming.retry.constant.timeout | long ms | 500 | constant pause |
  | talend.input.streaming.retry.exponential.exponent | double | 1.5 | exponent |
  | talend.input.streaming.retry.exponential.randomizationFactor | double | 0.5 | jitter |
  | talend.input.streaming.retry.exponential.maxDuration | long ms | 300000 | cap |
  | talend.input.streaming.retry.exponential.initialBackOff | long ms | 1000 | initial pause |

- **Contract for the Designer**: none
- **Contract for the Runtime**: SHOULD expose these keys through its LocalConfiguration implementation (family-prefixed keys `<family>.<key>` are tried first).
- **Server exposure**: none
- **Maturity level**: 2 - tuning for streaming.
- **Example**: none

### RUN-030 `@Checkpoint`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/input/checkpoint/Checkpoint.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-checkpoint.adoc`)
- **Category / Sub-area**: RUN / Checkpoint
- **Kind**: annotation
- **Applies to**: input (configuration type)
- **Description**: Marks a model class as the checkpoint configuration/state (meta `@ConfigurationType("checkpoint")`, so property metadata `tcomp::configurationtype::type=checkpoint`, `...::name=<value>`). Nested inside the input configuration; the runtime feeds the saved state back when restarting via the `$checkpoint` prefix (RUN-033). Layout form type `CHECKPOINT` exists in `@GridLayout`.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | value | String | "default" | checkpoint configuration name |

- **Contract for the Designer**: SHOULD hide checkpoint properties from the normal form (they are state), or show them in the `CHECKPOINT` form type.
- **Contract for the Runtime**: MUST persist the state emitted by RUN-031/RUN-033 and MUST re-inject it as `$checkpoint.*` at restart; only when `talend.checkpoint.enabled=true`.
- **Server exposure**: Metadata `tcomp::configurationtype::type=checkpoint` on the checkpoint option.
- **Maturity level**: 2 - Level 2 by checklist rules; feature is off by default.
- **Example**:

```java
@Checkpoint @Version(2) public class State implements Serializable { @Option private long sinceId; }
```

### RUN-031 `@CheckpointData`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/input/checkpoint/CheckpointData.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-checkpoint.adoc`)
- **Category / Sub-area**: RUN / Checkpoint
- **Kind**: annotation
- **Applies to**: input (method returning the @Checkpoint object)
- **Description**: Method returning the latest checkpoint object. `InputImpl.getCheckpoint()` wraps it in `CheckpointState(version, state)` where version = `@Version` value of the returned class (default 1).
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST call `Input.getCheckpoint()` only when `isCheckpointReady()` is true (or via the callback) and MUST serialize the state.
- **Server exposure**: none
- **Maturity level**: 2 - part of checkpoint.
- **Example**:

```java
@CheckpointData public Object checkpoint() { newBookmark = false; return config.checkpoint; }
```

### RUN-032 `@CheckpointAvailable`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/input/checkpoint/CheckpointAvailable.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-checkpoint.adoc`)
- **Category / Sub-area**: RUN / Checkpoint
- **Kind**: annotation
- **Applies to**: input (boolean method)
- **Description**: Method answering whether a newer checkpoint is available. `InputImpl.isCheckpointReady()` returns its result, or the value of the `talend.checkpoint.enabled` property when the method is absent. Checked after each non-null `next()` when a callback is registered.
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST poll `Input.isCheckpointReady()` after `next()` (explicit mode) or supply a callback to `Input.start(Consumer<CheckpointState>)` (automatic mode).
- **Server exposure**: none
- **Maturity level**: 2 - part of checkpoint.
- **Example**:

```java
@CheckpointAvailable public Boolean ready() { return newBookmark; }
```

### RUN-033 `Checkpoint runtime protocol ($checkpoint, CheckpointState)`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/input/CheckpointState.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-checkpoint.adoc`)
- **Category / Sub-area**: RUN / Checkpoint
- **Kind**: convention
- **Applies to**: input
- **Description**: Enabled by JVM property `talend.checkpoint.enabled=true` (default false; the doc page says `talend.checkpoint.enable` - code wins, see `../10-appendix/runtime-configuration-keys.md`). `CheckpointState{version, state}`; `toJson()` = `{"$checkpoint": {<state fields>, "__version": v}}`. Constants: `CHECKPOINT_KEY="$checkpoint"`, `VERSION_KEY="__version"`. On restart the host passes flat properties `$checkpoint.<field>=value` (and `$checkpoint.__version`); `ComponentManager.mergeCheckpointConfiguration` (mappers only) rewrites the `$checkpoint` prefix to the real path of the `@Checkpoint` parameter. Helpers: `ComponentManager.jsonToMap(JsonValue[, path])` (objects `a.b`, arrays `a[0]`), `replaceKeys`. Modes: explicit (`start()`, poll `isCheckpointReady()`, `getCheckpoint()`) and automatic (`start(callback)`; callback also invoked at `stop()`). `ChainedInput` forwards the callback to each split input.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | $checkpoint.<field> | flat property | - | restore state |
  | $checkpoint.__version | flat property | - | version of persisted state for migration (LCM-003) |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST implement persistence/restore of the JSON above; MUST migrate the state when `__version` is older (the framework's nested migration applies to the checkpoint type).
- **Server exposure**: none
- **Maturity level**: 2 - Level 2 by checklist rules.
- **Example**:

```json
{"$checkpoint": {"sinceId": 95, "strategy": "BY_ID", "__version": 2}}
```

### RUN-034 `@ConditionalOutput`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/meta/ConditionalOutput.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-processor.adoc`)
- **Category / Sub-area**: RUN / Processor
- **Kind**: annotation
- **Applies to**: processor (class)
- **Description**: Declares that the processor's output flow list depends on its configuration; `value` names an `@AvailableOutputFlows` action. Exposed as component metadata `conditional_output::value`. (Docs call the annotation `@ConditionalOutputFlows`; code name is `ConditionalOutput`.) Only Studio uses it.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | value | String | "default" | name of the @AvailableOutputFlows action |

- **Contract for the Designer**: MAY call `type=available_output` (ACT-014) with the current configuration to filter `outputFlows`.
- **Contract for the Runtime**: none
- **Server exposure**: Component metadata `conditional_output::value`.
- **Maturity level**: 2 - Studio-only dynamic flows.
- **Example**: none

### RUN-035 (moved) -> see ACT-014

### RUN-036 `@ReturnVariables / @ReturnVariable`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/component/ReturnVariables.java`
- **Category / Sub-area**: RUN / Studio variables
- **Kind**: annotation
- **Applies to**: input, processor, standalone (class); Studio only
- **Description**: Declares Studio return variables (e.g. `QUERY`) of a component; repeatable `@ReturnVariable(value,description,type,availability)` with `availability` AFTER (default) or FLOW; grouped by `@ReturnVariables`. Serialized into component metadata `variables::return::value` as `name\:javaType\:availability\:description` entries joined by `\;` (the separators are the two-character sequences backslash-colon and backslash-semicolon, written verbatim in the value).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | value | String | - | variable name |
  | description | String | "" | documentation |
  | type | Class<?> | String.class | value type |
  | availability | AVAILABILITY | AFTER | AFTER or FLOW (key strings `AFTER`, `FLOW`) |

- **Contract for the Designer**: MAY expose the variables to downstream expressions (Studio-like host).
- **Contract for the Runtime**: none
- **Server exposure**: Component metadata `variables::return::value`.
- **Maturity level**: 2 - Studio-only.
- **Example**: none

### RUN-037 `@AfterVariables / @AfterVariable / @AfterVariableContainer`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/component/AfterVariables.java`
- **Category / Sub-area**: RUN / Studio variables
- **Kind**: annotation (deprecated)
- **Applies to**: input, processor (class, method); Studio only
- **Description**: `@Deprecated` Studio after-variables. `@AfterVariable(value, description, type)` on the class (repeatable, grouped by `@AfterVariables`); `@AfterVariableContainer` on ONE parameterless method returning `Map<String,Object>` with the runtime values. Allowed types: Boolean, Byte, byte[], Character, Date, Double, Float, BigDecimal, Integer, Long, Object, Short, String, List (else registration fails). Metadata key `variables::after::value` = `name\:type\:description` joined by `\;` (literal backslash sequences, as for return variables). Supported on `@PartitionMapper`, `@Emitter`, `@Processor` (note: the validator also runs for driver runners).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | value | String | - | variable name |
  | description | String | "" | documentation |
  | type | Class<?> | String.class | value type |

- **Contract for the Designer**: MAY expose the variables.
- **Contract for the Runtime**: MAY call the container method after the component finished.
- **Server exposure**: Component metadata `variables::after::value`.
- **Maturity level**: 2 - Studio-only, deprecated.
- **Example**: none

### RUN-038 `optionalRow`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/input/PartitionMapper.java`
- **Category / Sub-area**: RUN / Input
- **Kind**: convention
- **Applies to**: input; Studio only
- **Description**: Boolean attribute on `@Emitter` and `@PartitionMapper` telling Studio the input may ignore its mandatory output row. Published as component metadata `mapper::optionalRow` ("true"/"false").
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | optionalRow | boolean | false | see above |

- **Contract for the Designer**: MAY relax the requirement of an outgoing connection for such inputs.
- **Contract for the Runtime**: none
- **Server exposure**: Component `metadata.mapper::optionalRow`.
- **Maturity level**: 2 - Studio-only.
- **Example**: none

### RUN-039 `Serialization requirements`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/base/LifecycleImpl.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-execution.adoc`)
- **Category / Sub-area**: RUN / Lifecycle
- **Kind**: convention
- **Applies to**: input, processor, output, standalone, services
- **Description**: Component classes (and their configuration objects) MUST be `java.io.Serializable`; constructors return `Serializable` (`ClassCastException` -> IllegalArgumentException `should return a Serializable`). All runtime wrappers replace themselves on serialization with a `SerializationReplacer` carrying `plugin`, `rootName`, `name`, the delegate bytes (`Serializer.toBytes`, TCCL = delegate classloader) and extra state (inputName/stream/internalConfiguration; retry and stop strategy for streaming); on read, `readResolve` rebuilds the wrapper by deserializing the delegate with `EnhancedObjectInputStream` using `ContainerFinder.Instance.get().find(plugin).classloader()`. Therefore the target JVM MUST have the plugin registered under the same id and MUST configure `ContainerFinder` (default: SPI `StandaloneContainerFinder`, fallback TCCL). Services are serialized as `SerializableService(plugin, className)` and re-looked-up (`LightContainer.findService`). `EnhancedObjectInputStream` applies a class filter: with `-Dtalend.component.runtime.serialization.java.inputstream.whitelist=<prefixes>` only those prefixes are allowed; otherwise a built-in blacklist (commons-collections functors, xalan, groovy runtime, `java.lang.Process`, ...) applies with a warning. `Serial` (service.serialization) lets a service define `readResolve`.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | talend.component.runtime.serialization.java.inputstream.whitelist | JVM property | unset (blacklist) | comma-separated allowed class-name prefixes |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST ship the plugin to every worker and register it before deserializing; MUST set the whitelist in secured deployments; MUST use `ContainerFinder.Instance.set(...)` when the default finder cannot see plugins.
- **Server exposure**: none
- **Maturity level**: 0 - distributed execution is impossible otherwise.
- **Example**:

```java
ContainerFinder.Instance.set(() -> plugin -> lightContainerFor(plugin)); // optional custom finder
```

### RUN-040 `Internal configuration keys ($-prefix)`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/ComponentManager.java`
- **Category / Sub-area**: RUN / Configuration
- **Kind**: convention
- **Applies to**: processor, input
- **Description**: Configuration keys starting with `$` or containing `.$` are considered internal: `ComponentManager` copies them into the `internalConfiguration` map of `PartitionMapperImpl` and `ProcessorImpl` (exposed via `getInternalConfiguration()`), and they are still available to the constructor mapping. Known keys: `$maxBatchSize` (RUN-026), `$maxRecords`, `$maxDurationMs` (RUN-028), `$checkpoint.*` (RUN-033), `__version` (LCM-003).
- **Attributes**: none
- **Contract for the Designer**: MUST send them under the same prefix as the other properties of the component configuration root.
- **Contract for the Runtime**: MUST forward them unchanged to `findMapper`/`findProcessor`.
- **Server exposure**: Properties named `$maxBatchSize`, `$maxRecords`, `$maxDurationMs` in ComponentDetail.properties.
- **Maturity level**: 1 - needed for batch/stop controls (Level 1/2 features).
- **Example**: none

### RUN-041 `Flat configuration to constructor arguments`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/reflect/ReflectionService.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-configuration.adoc`)
- **Category / Sub-area**: RUN / Configuration
- **Kind**: convention
- **Applies to**: input, processor, output, standalone
- **Description**: `ReflectionService.parameterFactory` builds constructor arguments from the flat `Map<String,String>`: a class parameter annotated `@Option("name")` is built from keys `name.<field>` (objects by `ConstructorProperties` or field injection), enums by `Enum.valueOf(trim)`, primitives/String by conversion (Xbean converters), `Schema` by JSON (DAT-032), `JsonObject` by JSON string. Collections read `name[0]`, `name[1]`, ... (stops at first gap, or `name[length]` if present), object items `name[i].field`. Maps read `name.key[i]` / `name.value[i]` pairs. Services and `@Configuration`-annotated LocalConfiguration objects are injected from the container. Before building, `PayloadValidator` validates visible parameters (`tcomp::validation::required|min|max|minLength|maxLength|minItems|maxItems|pattern|uniqueItems` and `@ActiveIf` visibility) and throws with all errors, unless `-Dtalend.component.configuration.validation.skip=true`. Serialization side helper: `ConfigurationMapper.map(nestedParameters, instance)`. Exact prefixes: `../03-component-server-api.md` (flat properties section).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | talend.component.configuration.validation.skip | JVM property | false | true disables payload validation |

- **Contract for the Designer**: MUST serialize form values into these exact key shapes using each property's `path` from ComponentDetail.
- **Contract for the Runtime**: MUST pass the full map (including defaults) to the manager; MUST surface the validation IllegalArgumentException as user error.
- **Server exposure**: ComponentDetail `properties[].path`, `type`, `metadata`.
- **Maturity level**: 0 - configuration correctness is a Level 0 requirement.
- **Example**:

```json
{"configuration.url": "http://x", "configuration.headers[0].key": "a", "configuration.headers[0].value": "b", "configuration.tags[0]": "t"}
```

### RUN-042 `Job DSL and local runner`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/chain/Job.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-execution.adoc`)
- **Category / Sub-area**: RUN / Execution engines
- **Kind**: interface
- **Applies to**: host runtime (reference implementation)
- **Description**: `Job.components().component(id, "family://name?prop=value&__version=N")....connections().from(id[,branch]).to(id[,branch]).build().property(k,v).run()` (`DSLParser`: scheme = family, authority = component name, query = flat configuration, `__version` = configuration version). Rules (`JobImpl`): a node with no incoming edge is a source (mapper); orphan nodes are dropped with a warning; a (node, branch) may be connected once (IllegalStateException); cycles -> `the job pipeline has cyclic connection`. Local runner: builds `InputRunner` per source and `AutoChunkProcessor` per processor (chunk = `$maxBatchSize` else 1), iterates levels, joins several incoming branches by group key with a fusion sort (`GroupKeyProvider`, default key from `LocalSequenceHolder`), calls `flush` then `stop` in `finally`. Executor selection: job property `ExecutorBuilder` class name, `standalone|default|local` (local), `beam` (BeamExecutor), or SPI `META-INF/services/...Job$ExecutorBuilder`. `checkpoint(Consumer<CheckpointState>)` per component; `streaming.maxRecords` property. Merged from TST-018 (Job DSL for end-to-end chains): URI form `[family]://[component][?version][&configuration]`, version key `__version`, configuration keys are flat property paths with URI-encoded values; `build()` validates at least one starter (a producer), no cycles, all components declared, each branch used at most once; execution is linear (no parallelism); runner selection order: `Job.ExecutorBuilder` job property (instance, `Class` or `String`), then the `ExecutionBuilder` SPI (present with `component-runtime-beam`; Beam options via `-Dtalend.beam.job.<option>`), else local; job properties: `GroupKeyProvider` (per component or global, join key for multi-input processors), `streaming.maxRecords` (default -1) and `streaming.maxDurationMs`. Also the practical end-to-end check of an integration (see TST catalog).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | __version | uri query | -1 | configuration version for migration |
  | streaming.maxRecords | job property | -1 | local runner cap for streams |
  | org.talend.sdk.component.runtime.manager.chain.Job$ExecutorBuilder | job property key | - | executor instance/class/name |

- **Contract for the Designer**: MAY use `family://name?...` URIs as a portable job serialization for tests.
- **Contract for the Runtime**: MAY reuse the Job API as a reference or embedded engine; a custom engine MUST reproduce the semantics (levels, branch wiring, flush). SHOULD be compared against for acceptance tests: the same chain must give the same output in the host.
- **Server exposure**: none
- **Maturity level**: 1 - reference engine, used by tests/tools.
- **Example**:

```java
Job.components()
   .component("in", "demo://numbers?configuration.limit=10")
   .component("out", "demo://sink")
   .connections().from("in").to("out").build().run();
```

### RUN-043 `Beam translation of a job`
- **Source**: `component-runtime-beam/src/main/java/org/talend/sdk/component/runtime/beam/chain/impl/BeamExecutor.java`
- **Category / Sub-area**: RUN / Execution engines
- **Kind**: convention
- **Applies to**: host runtime (reference engine)
- **Description**: `BeamExecutor` translates the Job graph: each source -> `TalendIO.read(mapper, {maxRecords,maxDurationMs})` (bounded `Read` with split via `Mapper.split(desiredBundleSizeBytes)`, or `InfiniteRead` with `UnboundedSource`) + `RecordNormalizer`; each processor input edge -> `RecordBranchFilter` (select upstream branch) -> optional `RecordBranchMapper` (rename branch) -> `RecordBranchUnwrapper` -> `AutoKVWrapper` (key by `GroupKeyProvider`); one input -> `RecordKVUnwrapper` + `RecordNormalizer`, several inputs -> `CoGroupByKey` + `CoGroupByKeyResultMappingTransform`; processor with outgoing edges -> `TalendFn.asFn(processor)` (ParDo), terminal -> `TalendIO.write(processor)`. Pipeline options come from JVM properties `talend.beam.job.<option>=value` (-> `--option=value`). Multi-branch output is transported as ONE record whose entries are arrays named after the branches (`BeamSingleOutputFactory` / `BeamMultiOutputFactory`); `BeamInputFactory` reads them back (names starting `__talend_internal` are skipped). `BaseProcessorFn`: `@Setup`->`start()`, `@ProcessElement` -> beforeGroup at count 0 + onNext, `@FinishBundle` -> afterGroup, `@Teardown` -> `stop()`. Bounded source: `Read.from(BoundedSourceImpl)`; unbounded uses NoCheckpointCoder (no checkpoint).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | talend.beam.job.* | JVM properties | - | forwarded as Beam PipelineOptions |
  | streaming.maxRecords / streaming.maxDurationMs | job properties | 1000 / 60000 | stream caps in Beam |

- **Contract for the Designer**: none
- **Contract for the Runtime**: Reference for an engine adapter: MUST reproduce branch filtering/mapping, key-based multi-input join and group semantics on the target engine.
- **Server exposure**: none
- **Maturity level**: 2 - reference implementation only; a host may pick another engine.
- **Example**: none

### RUN-044 `Beam I/O adapters (TalendIO, TalendFn)`
- **Source**: `component-runtime-beam/src/main/java/org/talend/sdk/component/runtime/beam/TalendIO.java`
- **Category / Sub-area**: RUN / Execution engines
- **Kind**: class
- **Applies to**: host runtime (reference engine)
- **Description**: `TalendIO.read(Mapper[, config])` builds a Read/InfiniteRead (config keys allowed for streams: `maxRecords`, `maxDurationMs`; anything else -> IllegalArgumentException `Unsupported configuration`); `getEstimatedSizeBytes` = `mapper.assess()`; `split` = `mapper.split(size)` (each call brackets the mapper with start/stop); readers call `Input.start()/next()/stop()` and convert non-Record values through `RecordConverters` using the plugin's services. `TalendIO.write(Processor)` = ParDo `WriteFn` with a no-op output. `TalendFn.asFn(Processor)` = ParDo emitting Records. All coders are `SchemaRegistryCoder` (DAT-031/RUN-045).
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY reuse these classes; if embedded, MUST have `component-runtime-beam` and Beam on the container parent classloader (see `BeamCustomizer` class index).
- **Server exposure**: none
- **Maturity level**: 2 - reference engine only.
- **Example**: none

### RUN-045 `Beam coders and schema registry`
- **Source**: `component-runtime-beam/src/main/java/org/talend/sdk/component/runtime/beam/coder/registry/SchemaRegistryCoder.java`
- **Category / Sub-area**: RUN / Execution engines
- **Kind**: class
- **Applies to**: host runtime (reference engine)
- **Description**: `SchemaRegistryCoder` encodes a Record as `<schema id>\n<Avro binary>` where the id is the generated Avro record name (`SchemaIdGenerator`) and schemas are kept in a per-JVM `SchemaRegistry` (`InMemorySchemaRegistry`); decode fails with `Invalid schema id` if the schema is unknown on the receiver. Avro coders are cached (`component.runtime.beam.avrocoder.cache.size`, default 1024). Other coders: `FullSerializationRecordCoder`, `JsonbCoder`, `JsonpJsonObjectCoder`, `ContextualSerializableCoder`, `NoCheckpointCoder`. Implication (inferred): in a distributed run the schema registry must be shared with, or replicated to, every decoding JVM.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | component.runtime.beam.avrocoder.cache.size | JVM property | 1024 | max cached Avro coders |

- **Contract for the Designer**: none
- **Contract for the Runtime**: A distributed host MUST guarantee that the schema id resolves on the decoding side.
- **Server exposure**: none
- **Maturity level**: 2 - reference engine only.
- **Example**: none

### RUN-046 `Component validation at registration (ModelVisitor)`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/visitor/ModelVisitor.java`
- **Category / Sub-area**: RUN / Registration
- **Kind**: convention
- **Applies to**: input, processor, output, standalone
- **Description**: When a plugin is scanned, each public class annotated with exactly one of `@Emitter`, `@PartitionMapper`, `@Processor`, `@DriverRunner` is validated (mixing annotations -> IllegalArgumentException `You can't mix @Emitter, @PartitionMapper and @Processor`). Rules: PartitionMapper = exactly one `@Split`+`@Emitter` (+`@Assessor` if finite), Assessor/Emitter parameterless, Split params only `@PartitionSize` int/long, Split returns `Collection<MapperType>`, `stoppable` only with `infinite`; Emitter = single parameterless `@Producer`; DriverRunner = single parameterless `@RunAtDriver`; Processor = at most one `@ElementListener` (or a collection param in `@AfterGroup`), `@BeforeGroup` parameterless, `@AfterGroup` params only `@Output`/`@LastGroup`/group collection, single `@AfterGroup` with `@LastGroup`; `@Output` param types OutputEmitter or MultiOutputIterator; after-variable rules (RUN-037). Component name defaults to the class name; conflicting names within a family -> `Conflicting processors|mappers|driver runners`. Validation skipped when `Mode.UNSAFE` (RUN-047) or when an extension calls `skipValidation()` (SVC-026).
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: A host registering plugins through ComponentManager gets this for free; a host with its own scanner MUST replicate these rules.
- **Server exposure**: none
- **Maturity level**: 1 - invalid components are rejected up-front; without it errors show at run time.
- **Example**: none

### RUN-047 `Runtime Mode (UNSAFE)`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/impl/Mode.java`
- **Category / Sub-area**: RUN / Registration
- **Kind**: convention
- **Applies to**: host runtime
- **Description**: System property `talend.component.impl.mode` = `DEFAULT` | `UNSAFE`. UNSAFE deactivates component validation (RUN-046) 'for an unsafe runtime (speed gain when environment is already validated)'.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | talend.component.impl.mode | JVM property | DEFAULT | DEFAULT or UNSAFE (Enum.valueOf, other values fail at class init) |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY set UNSAFE in production once plugins have been validated at build time.
- **Server exposure**: none
- **Maturity level**: 2 - optimization.
- **Example**: none

### RUN-048 `Input and output flows (FlowsFactory)`
- **Source**: `component-runtime-design-extension/src/main/java/org/talend/sdk/component/design/extension/flows/FlowsFactory.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-processor.adoc`)
- **Category / Sub-area**: RUN / Design projection
- **Kind**: interface
- **Applies to**: input, processor, output, standalone
- **Description**: `component-runtime-design-extension` computes the flow names shown by the server: PartitionMapper/Emitter -> in `[]`, out `[__default__]`; DriverRunner -> both empty; Processor -> input flows = names of non-`@Output` parameters of `@ElementListener` (or of the first `@AfterGroup` with parameters) using `@Input` value or `__default__`; output flows = `__default__` if the listener returns non-void, plus every `@Output` branch (`OutputBranches.of`) of the listener and of the first `@AfterGroup` with parameters (distinct). Native Beam `PTransform` components default to the BeamFlowFactory.
- **Attributes**: none
- **Contract for the Designer**: MUST use `inputFlows` / `outputFlows` of ComponentDetail to build connectors.
- **Contract for the Runtime**: MUST honour the same names when wiring branches.
- **Server exposure**: ComponentDetail `inputFlows`, `outputFlows`.
- **Maturity level**: 0 - wrong connectors make jobs unbuildable.
- **Example**:

```json
{"type": "processor", "inputFlows": ["__default__"], "outputFlows": ["__default__", "REJECT"]}
```

### RUN-049 `Processor input conversion and group buffer`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/output/ProcessorImpl.java`
- **Category / Sub-area**: RUN / Processor
- **Kind**: convention
- **Applies to**: processor, output
- **Description**: `ProcessorImpl` converts each incoming value to the declared parameter type (`RecordConverters.toType`): same instance if compatible; JsonObject from Record via `toJson`; POJO via JSON-B; `Record` from any object. Without `@ElementListener`, each incoming record is converted to the element type of the `Collection<T>` parameter of `@AfterGroup` and appended to the group buffer, reset by `beforeGroup()` and released after `afterGroup`. A non-void listener return is emitted on `__default__`. Lookup of `Jsonb`, `JsonBuilderFactory`, `JsonProvider`, `RecordBuilderFactory` is lazy from the plugin container with fallbacks (`RecordBuilderFactoryImpl("$volatile")`, `Json.createBuilderFactory`).
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST call `beforeGroup()` before the first `onNext` (it initializes reflection state and the buffer) - `onNext` before `beforeGroup` fails with NullPointerException on `parameterBuilderProcess`.
- **Server exposure**: none
- **Maturity level**: 0 - protocol order is mandatory.
- **Example**: none

### RUN-050 `Multi-input record grouping (GroupKeyProvider)`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/chain/GroupKeyProvider.java`
- **Category / Sub-area**: RUN / Execution engines
- **Kind**: interface
- **Applies to**: processor with several @Input branches
- **Description**: `GroupKeyProvider extends Function<GroupContext,String>, Serializable` computes the join key of a record given `(componentId, branchName, record)`. The local runner groups by key (sorted, fusion join) and the Beam executor uses it in `AutoKVWrapper` before `CoGroupByKey`; when several branches share a key the processor receives one record per branch in the same `onNext`. Reference key resolution (`JobExecutor.getKeyProvider`): component property, then job property (both keyed by the class name `GroupKeyProvider`), then ServiceLoader, then a per-component sequence counter.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | getComponentId() | String | - | producing component |
  | getBranchName() | String | - | producing branch |
  | getData() | Record | - | record |

- **Contract for the Designer**: MAY let users configure the join key per input.
- **Contract for the Runtime**: MUST provide a join strategy for multi-input processors.
- **Server exposure**: none
- **Maturity level**: 2 - multi-input processors are a niche.
- **Example**: none

### RUN-051 (moved) -> see SVC-003

