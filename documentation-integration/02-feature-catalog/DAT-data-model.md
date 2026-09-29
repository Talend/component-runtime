# Feature Catalog - DAT: Data model

> Framework version documented: **1.2611.0-SNAPSHOT** (root `pom.xml`). Category prefix: `DAT-`. Generated from the same data as [`index.DAT.json`](index.DAT.json); both agree exactly.

Scope: `Record`, `Schema`, `Schema.Type`, builders, visitors, pointers, schema properties, schema/dataset discovery, and the JSON / JSON-B / Avro mappings and coercion rules. Narrative and rules: [`../04-data-model.md`](../04-data-model.md). Runtime consumers: [`RUN-runtime.md`](RUN-runtime.md). Design-time action plumbing (`/action/execute`) belongs to the ACT catalog; the schema/dataset discovery annotations are catalogued here because they produce data-model payloads.

Levels (see `../09-integration-checklist.md`): 0 = mandatory, 1 = normal user experience, 2 = full TCK.

## Index

| ID | Name | Level | Designer | Runtime |
|---|---|---|---|---|
| [DAT-001](#dat-001-record) | `Record` | 0 | MUST NOT implement or instantiate; only handle schemas (DAT-004) exchanged as JSON. | MUST create records via RecordBuilderFactory (DAT-016) and pass them unchanged between components. |
| [DAT-002](#dat-002-recordbuilder) | `Record.Builder` | 0 | none | MUST call `build()` once per record; SHOULD provide a schema for predictable output; name collisions are auto-renamed (DAT-013). |
| [DAT-003](#dat-003-record-value-access-and-coercion) | `Record value access and coercion` | 0 | none | MUST use typed accessors matching `Entry.getType()`; SHOULD use `getOptionalXxx` for nullable entries. |
| [DAT-004](#dat-004-schema) | `Schema` | 0 | MUST be able to read the Schema JSON returned by schema-discovery actions and MAY store it as design-time schema of a connection. | MUST treat schemas as immutable value objects; MUST create them via RecordBuilderFactory (DAT-016). |
| [DAT-005](#dat-005-schematype) | `Schema.Type` | 0 | MUST map each constant to a UI/column type and to the host's internal type system. | MUST support all 11 constants in the host record model (or convert losslessly, see DAT-031 for Avro). |
| [DAT-006](#dat-006-schemaentry) | `Schema.Entry` | 0 | MUST display name, type, nullable and comment; SHOULD show rawName as label when present. | MUST keep name/rawName pairs intact across components. |
| [DAT-007](#dat-007-schemaentrybuilder) | `Schema.Entry.Builder` | 0 | none | SHOULD set `nullable` explicitly (builder default is false). |
| [DAT-008](#dat-008-schemabuilder) | `Schema.Builder` | 0 | none | MUST build RECORD schemas through the factory (DAT-016) so the host-selected implementation (memory or Avro, DAT-034) is used. |
| [DAT-009](#dat-009-schemaentriesorder) | `Schema.EntriesOrder` | 1 | SHOULD display columns using the order given by `getEntriesOrdered()` / prop `talend.fields.order`. | SHOULD preserve the order property when copying/transforming schemas. |
| [DAT-010](#dat-010-orderedmap) | `OrderedMap` | 2 | none | MAY reuse when implementing a custom Record/Schema; otherwise none. |
| [DAT-011](#dat-011-schemaproperty) | `SchemaProperty` | 1 | SHOULD honor `field.key`, `field.size`, `field.scale`, `field.pattern`, `field.origin.type` when showing or mapping schemas. | MUST preserve props when copying entries; SHOULD NOT invent semantics for unknown keys. |
| [DAT-012](#dat-012-schemapropertylogicaltype) | `SchemaProperty.LogicalType` | 1 | SHOULD render DATE/TIME/TIMESTAMP with the matching widget/format. | MUST keep `field.logical.type` when converting to/from Avro. |
| [DAT-013](#dat-013-schemacompanionutil-name-sanitization-and-collisions) | `SchemaCompanionUtil (name sanitization and collisions)` | 0 | MUST use `name` (sanitized) as the technical identifier and MAY show `rawName`. | MUST NOT rely on original field names for lookups; use `Entry.getName()`. |
| [DAT-014](#dat-014-entry-level-error-support) | `Entry-level error support` | 2 | MAY display invalid cells using the entry props. | MAY set the system property; SHOULD check `entry.isValid()` before reading and MUST NOT treat invalid entries as data. |
| [DAT-015](#dat-015-nullable-check-switch) | `Nullable check switch` | 2 | none | SHOULD leave the default; MAY set true for performance in a trusted environment. |
| [DAT-016](#dat-016-recordbuilderfactory) | `RecordBuilderFactory` | 0 | none | MUST provide it as an injectable service to every plugin container (ComponentManager does by default). |
| [DAT-017](#dat-017-recordservice) | `RecordService` | 1 | none | MUST provide it as a built-in service when hosting TCK plugins with a custom container; SHOULD reuse `RecordServiceImpl`. The injected instance MUST be backed by the plugin's `RecordBuilderFactory` and JSON-B. |
| [DAT-018](#dat-018-recordvisitor) | `RecordVisitor` | 2 | none | SHOULD implement RecordService.visit to support it; not needed otherwise. |
| [DAT-019](#dat-019-recordpointer) | `RecordPointer` | 2 | none | MUST implement with the semantics of DAT-020 if RecordPointerFactory is provided. |
| [DAT-020](#dat-020-recordpointerfactory) | `RecordPointerFactory` | 1 | none | SHOULD provide the reference semantics (RFC 6901 adapted to records). MUST inject a factory scoped to the plugin. |
| [DAT-021](#dat-021-deprecated-schema-api-serviceschemaschema-and-type) | `Deprecated schema API (service.schema.Schema and Type)` | 2 | MUST still accept schemas returned by legacy components (same JSON shape). | none |
| [DAT-024](#dat-024-fixedschema) | `@FixedSchema` | 1 | MUST NOT let the user edit the schema of the listed flows; MUST re-call the action when a `watch` path changes. | none |
| [DAT-029](#dat-029-component-data-type-conversion-record--jsonobject--pojo) | `Component data type conversion (Record / JsonObject / POJO)` | 0 | none | MUST convert non-Record values to Record before handing data to the engine (as `InputImpl.next()` and `BeamOutputEmitter` do); SHOULD standardize on `Record`. |
| [DAT-030](#dat-030-json-p--json-b-mapping-of-record) | `JSON-P / JSON-B mapping of Record` | 1 | none | MUST use a Johnzon-compatible JSON-B/JSON-P provider with the settings above when it produces JSON from records; MUST decode BYTES as Base64. |
| [DAT-031](#dat-031-avro-mapping-beam-runtime) | `Avro mapping (Beam runtime)` | 2 | none | MAY use the Avro implementation; if used, MUST run with `talend.component.record.skip.sanitize=false`; MUST NOT mix implementations inside one plugin. |
| [DAT-032](#dat-032-schema-json-serialization-schemaconverter) | `Schema JSON serialization (SchemaConverter)` | 1 | MUST serialize a chosen schema with these keys when a component option is of type `Schema`. | MUST accept this JSON in the option value. |
| [DAT-033](#dat-033-type-conversion-rules-mappingutilscoerce) | `Type conversion rules (MappingUtils.coerce)` | 1 | none | MUST reproduce these rules if the host supplies its own Record implementation; SHOULD reuse `RecordImpl`. |
| [DAT-034](#dat-034-recordbuilderfactoryprovider-spi) | `RecordBuilderFactoryProvider SPI` | 2 | none | MAY provide one implementation per JVM; MUST make it return Serializable factories (they serialize as SerializableService). |
| [DAT-035](#dat-035-metadata-entries) | `Metadata entries` | 2 | SHOULD hide metadata entries from the default column list and expose them separately. | MUST NOT drop metadata entries when copying records. |
| [DAT-036](#dat-036-arrays-and-nested-records) | `Arrays and nested records` | 1 | MUST handle ARRAY/RECORD entries (render nested, or flatten) when propagating schemas. | MUST handle nested types in transport and conversion. |

## Entries

### DAT-001 `Record`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/Record.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/record-types.adoc`)
- **Category / Sub-area**: DAT / Record
- **Kind**: interface
- **Applies to**: input, processor, output (data exchanged between components); service
- **Description**: The unit of data flowing between components: an immutable, schema-carrying row. It exposes `getSchema()` and typed accessors; it MUST be created through `RecordBuilderFactory`, never by implementing the interface in host code.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | getSchema() | Schema | - | schema of this record |
  | get(Class<T>, String) | T | - | value of an entry, coerced to the expected type (see DAT-033); null if absent |
  | getString/getInt/getLong/getFloat/getDouble/getBoolean/getBytes/getDateTime/getInstant/getDecimal/getRecord/getArray | typed | - | typed shortcuts on `get(...)`; primitive getters throw NullPointerException on null values |
  | getOptionalXxx(String) | Optional* | - | null-safe flavour (String, Int, Long, Double, Float, Boolean, Bytes, Record, DateTime, Instant, Decimal, Array) |
  | withNewSchema(Schema) | Record.Builder | throws UnsupportedOperationException | builder pre-filled with entries of this record present (and equal) in the new schema |
  | isValid() | boolean | true | false if any entry is flagged on error (DAT-014) |
  | RECORD_ERROR_SUPPORT | String constant | `talend.component.record.error.support` | system property name enabling entry-level errors |
  | RECORD_NULLABLE_CHECK | String constant | `talend.component.record.nullable.check` | system property name; see DAT-015 |

- **Contract for the Designer**: MUST NOT implement or instantiate; only handle schemas (DAT-004) exchanged as JSON.
- **Contract for the Runtime**: MUST create records via RecordBuilderFactory (DAT-016) and pass them unchanged between components.
- **Server exposure**: Not a REST payload; only its `Schema` travels (DiscoverSchema results).
- **Maturity level**: 0 - every input/processor/output exchanges Record; wrong handling breaks data flow.
- **Example**:

```java
final Record r = factory.newRecordBuilder().withString("name", "Gary").withInt("age", 33).build();
String name = r.getString("name");
OptionalInt age = r.getOptionalInt("age");
```

### DAT-002 `Record.Builder`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/Record.java`
- **Category / Sub-area**: DAT / Record
- **Kind**: interface
- **Applies to**: input, processor, output, service
- **Description**: Fluent builder returned by `RecordBuilderFactory.newRecordBuilder(...)`. Without a provided schema the schema is inferred from the added entries (in insertion order unless `before/after` is used); with a provided schema every value is validated against it. Reference implementation: `RecordImpl.BuilderImpl`.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | build() | Record | - | fails with IllegalArgumentException `Missing entries: a, b` when a non-nullable entry of the provided schema has no value (unless DAT-015) |
  | withString/withInt/withLong/withFloat/withDouble/withBoolean/withBytes(name\|Entry, value) | Builder | - | typed setters; the by-name variant creates an entry when no schema is provided (nullable=true for String, Bytes, DateTime(Date\|ZonedDateTime), Decimal, Record; nullable=false for int/long/float/double/boolean, withTimestamp and withInstant) |
  | withDateTime(name\|Entry, Date\|ZonedDateTime) | Builder | - | stored internally as epoch millis (Long) |
  | withTimestamp(name\|Entry, long) | Builder | - | DATETIME from epoch millis |
  | withInstant / withDecimal | Builder | throws UnsupportedOperationException in the default interface | implemented by RecordImpl; Instant stored as Instant, decimal as BigDecimal |
  | withRecord(name\|Entry, Record) | Builder | - | entry needs an element schema; by-name variant rejects null |
  | withArray(Entry, Collection) | Builder | - | entry needs `elementSchema`; item types are not checked (`todo` in code) |
  | with(Entry, Object) | Builder | - | generic setter; checks `Type.isCompatible(value)` else IllegalArgumentException (or DAT-014 error entry) |
  | before(String) / after(String) | Builder | throws UnsupportedOperationException in the interface | position of the NEXT added entry relative to an existing entry (which must exist) |
  | removeEntry(Entry) / updateEntryByName(String, Entry[, Function]) | Builder | - | schema-less builder only mutates in place; with provided schema a new builder is created |
  | getValue(String) / getEntry(String) / getCurrentEntries() | - | - | introspection of the builder state |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST call `build()` once per record; SHOULD provide a schema for predictable output; name collisions are auto-renamed (DAT-013). Behaviour verified in `component-runtime-impl/.../record/RecordImpl.java`.
- **Server exposure**: none
- **Maturity level**: 0 - record creation is required by every emitting component.
- **Example**:

```java
final Schema schema = factory.newSchemaBuilder(Schema.Type.RECORD)
    .withEntry(factory.newEntryBuilder().withName("id").withType(Schema.Type.LONG).withNullable(false).build())
    .build();
final Record r = factory.newRecordBuilder(schema).withLong("id", 1L).build();
```

### DAT-003 `Record value access and coercion`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/record/RecordConverters.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/record-types.adoc`)
- **Category / Sub-area**: DAT / Record
- **Kind**: convention
- **Applies to**: input, processor, output
- **Description**: `Record.get(Class, name)` returns the stored value if `expectedType.isInstance(value)`, otherwise coerces it via `RecordConverters.coerce` -> `MappingUtils.coerce` (rules in DAT-033). DATETIME entries are stored as `Long` epoch millis (or `Instant`); `get(Object.class, name)` on a DATETIME entry therefore returns the raw stored value, not a ZonedDateTime. Unsupported conversions throw IllegalArgumentException `<name> can't be converted to <type> as its value is '<v>' of type <cls>.`
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST use typed accessors matching `Entry.getType()`; SHOULD use `getOptionalXxx` for nullable entries.
- **Server exposure**: none
- **Maturity level**: 0 - incorrect coercion changes data values.
- **Example**:

```java
ZonedDateTime dt = record.getDateTime("birth");   // from stored epoch millis, UTC zone
Long raw = record.get(Long.class, "birth");     // same entry, raw millis
```

### DAT-004 `Schema`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/Schema.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/record-types.adoc`)
- **Category / Sub-area**: DAT / Schema
- **Kind**: interface
- **Applies to**: input, processor, output, configuration (Schema-typed @Option), service (DiscoverSchema)
- **Description**: Describes a `RECORD` (list of `Entry`), an `ARRAY` (`elementSchema`) or a primitive type. Records expose data entries (`getEntries()`), metadata entries (`getMetadata()`), or both (`getAllEntries()`); `getEntriesOrdered()` returns the designed order (DAT-009). `getProps()` is a free `Map<String,String>` (metadata such as `talend.fields.order`). Schema JSON produced by JSON-B from `SchemaImpl` has keys `type`, `elementSchema`, `entries`, `props` (asserted by `component-server` `SchemaTest`) and `metadata` (inferred from `getMetadata()`).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | getType() | Schema.Type | - | RECORD, ARRAY or a primitive |
  | getElementSchema() | Schema | null | element schema for ARRAY |
  | getEntries() | List<Entry> | - | data entries (excludes metadata entries) |
  | getMetadata() | List<Entry> | - | metadata entries |
  | getAllEntries() | Stream<Entry> | - | metadata entries first, then data entries (SchemaImpl) |
  | getEntry(String) / getEntryMap() | Entry / Map | - | lookup by sanitized name |
  | getEntriesOrdered() / getEntriesOrdered(Comparator) / naturalOrder() | List<Entry> / EntriesOrder | - | ordered view (DAT-009) |
  | getProps() / getProp(String) / getJsonProp(String) | Map / String / JsonValue | - | schema properties; `getJsonProp` parses a JSON value or falls back to a JSON string |
  | toBuilder() | Schema.Builder | throws UnsupportedOperationException in the interface | copy-builder |
  | SKIP_SANITIZE_PROPERTY / SKIP_SANITIZE | String / boolean | `talend.component.record.skip.sanitize` / false | disables entry-name sanitization (DAT-013) |
  | sanitizeConnectionName(String), avoidCollision(...) | static | @Deprecated | delegates to SchemaCompanionUtil |

- **Contract for the Designer**: MUST be able to read the Schema JSON returned by schema-discovery actions and MAY store it as design-time schema of a connection.
- **Contract for the Runtime**: MUST treat schemas as immutable value objects; MUST create them via RecordBuilderFactory (DAT-016).
- **Server exposure**: Returned by actions of type `schema` / `schema_extended` (see `../03-component-server-api.md`); also accepted as an `@Option Schema` value (DAT-032).
- **Maturity level**: 0 - schema propagation and record building depend on it.
- **Example**:

```json
{
  "type": "RECORD",
  "entries": [
    {"name": "id", "type": "LONG", "nullable": false},
    {"name": "name", "type": "STRING", "nullable": true, "comment": "customer name"}
  ],
  "props": {"talend.fields.order": "id,name"}
}
```

### DAT-005 `Schema.Type`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/Schema.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/record-types.adoc`)
- **Category / Sub-area**: DAT / Schema
- **Kind**: enum
- **Applies to**: input, processor, output
- **Description**: Entry/schema data type. Each constant lists the Java classes accepted by `isCompatible(Object)` (null is always compatible). A `Type` used with the wrong value fails record building.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | RECORD | enum | - | nested record; Java class `Record`; needs elementSchema on entries |
  | ARRAY | enum | - | `Collection`; homogeneous; needs elementSchema |
  | STRING | enum | - | `String` or any `Object` |
  | BYTES | enum | - | `byte[]` or `Byte[]` |
  | INT | enum | - | `Integer` |
  | LONG | enum | - | `Long` |
  | FLOAT | enum | - | `Float` |
  | DOUBLE | enum | - | `Double` |
  | BOOLEAN | enum | - | `Boolean` |
  | DATETIME | enum | - | `Long`, `java.util.Date`, `java.time.temporal.Temporal` (ZonedDateTime, Instant, ...); logical types DATE/TIME/TIMESTAMP refine it (DAT-012) |
  | DECIMAL | enum | - | `BigDecimal` |

- **Contract for the Designer**: MUST map each constant to a UI/column type and to the host's internal type system.
- **Contract for the Runtime**: MUST support all 11 constants in the host record model (or convert losslessly, see DAT-031 for Avro).
- **Server exposure**: JSON string in schema `type`/entry `type`.
- **Maturity level**: 0 - unknown types make records unreadable.
- **Example**:

```json
{"name": "amount", "type": "DECIMAL", "nullable": true}
```

### DAT-006 `Schema.Entry`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/Schema.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/record-types.adoc`)
- **Category / Sub-area**: DAT / Schema
- **Kind**: interface
- **Applies to**: input, processor, output
- **Description**: A column (or metadata column) of a RECORD schema. `name` is the sanitized name; `rawName` keeps the original when sanitization changed it (`getOriginalFieldName()` returns rawName or name). Reference implementation `SchemaImpl.EntryImpl` is serialized with JSON-B (getOriginalFieldName is `@JsonbTransient`).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | getName() | String | - | sanitized name (DAT-013) |
  | getRawName() | String | null | original name if different |
  | getOriginalFieldName() | String | - | rawName else name |
  | getType() | Schema.Type | - | entry type |
  | isNullable() | boolean | false for the interface; builder default false | whether null is allowed (server JSON key `nullable`) |
  | isMetadata() | boolean | false | metadata vs data entry |
  | isErrorCapable() | boolean | false | entry may hold an invalid value (DAT-014) |
  | isValid() | boolean | true | false when prop `record.value.on.error` is true |
  | getDefaultValue() | T | null | default value |
  | getElementSchema() | Schema | null | for RECORD/ARRAY entries |
  | getComment() | String | null | documentation only, no runtime use |
  | getProps() / getProp / getJsonProp | Map<String,String> | empty | entry properties, keys in DAT-011 |
  | getLogicalType() | String | prop `field.logical.type` | date/time/timestamp/uuid (DAT-012) |
  | getErrorMessage() / getErrorFallbackValue() | String | props | only when the entry is on error |
  | toBuilder() | Entry.Builder | throws UnsupportedOperationException in the interface | copy-builder |

- **Contract for the Designer**: MUST display name, type, nullable and comment; SHOULD show rawName as label when present.
- **Contract for the Runtime**: MUST keep name/rawName pairs intact across components.
- **Server exposure**: Entry object inside the Schema JSON (`name`, `rawName`, `type`, `nullable`, `metadata`, `errorCapable`, `defaultValue`, `elementSchema`, `comment`, `props`).
- **Maturity level**: 0 - entries are the schema; no schema, no propagation.
- **Example**:

```json
{"name": "first_name", "rawName": "first name", "type": "STRING", "nullable": true, "props": {"field.size": "50"}}
```

### DAT-007 `Schema.Entry.Builder`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/Schema.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/record-types.adoc`)
- **Category / Sub-area**: DAT / Schema
- **Kind**: interface
- **Applies to**: input, processor, output, service
- **Description**: Builder for entries obtained from `RecordBuilderFactory.newEntryBuilder()` or `newEntryBuilder(Entry model)` (copies type, nullable, name, elementSchema, defaultValue, comment, props). `withName` sanitizes the name and stores the original in rawName.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | withName(String) | Builder | - | sanitizes (DAT-013); sets rawName when changed |
  | withRawName / withType / withNullable / withErrorCapable / withMetadata | Builder | - | plain setters |
  | withLogicalType(SchemaProperty.LogicalType \| String) | Builder | throws UnsupportedOperationException in the interface | sets `field.logical.type`; if no type is given, type = logical type storage type |
  | withDefaultValue / withElementSchema / withComment | Builder | - | plain setters |
  | withProp(String,String) / withProps(Map) | Builder | - | entry properties |
  | build() | Entry | - | immutable entry |

- **Contract for the Designer**: none
- **Contract for the Runtime**: SHOULD set `nullable` explicitly (builder default is false).
- **Server exposure**: none
- **Maturity level**: 0 - needed to create schemas.
- **Example**:

```java
Schema.Entry e = factory.newEntryBuilder().withName("created").withType(Schema.Type.DATETIME)
    .withLogicalType(SchemaProperty.LogicalType.TIMESTAMP).withNullable(true).build();
```

### DAT-008 `Schema.Builder`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/Schema.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/record-types.adoc`)
- **Category / Sub-area**: DAT / Schema
- **Kind**: interface
- **Applies to**: input, processor, output, service
- **Description**: Builder for schemas: `withType` (RECORD/ARRAY/primitive), `withEntry`, positional insert/move, `withElementSchema` (ARRAY only), `withProp(s)`, `build()` and `build(Comparator)`. Primitive schemas are the singletons `Schemas.STRING`, ... which reject any mutation (UnsupportedOperationException `Not allowed for a primitive`).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | withType(Type) | Builder | - | schema type |
  | withEntry(Entry) | Builder | - | RECORD only, else IllegalArgumentException `entry is only valid for RECORD type of schema`; name collision -> DAT-013; duplicate raw -> `Entry with name X already exist in schema` |
  | withEntryAfter/withEntryBefore(String, Entry) | Builder | throws UnsupportedOperationException in the interface | insert relative to another entry |
  | remove(String\|Entry) / moveAfter / moveBefore / swap | Builder | throws UnsupportedOperationException in the interface | editing; unknown names -> IllegalArgumentException `X not in schema` |
  | withElementSchema(Schema) | Builder | - | ARRAY only, else IllegalArgumentException |
  | withProps / withProp | Builder | - | schema props |
  | build() / build(Comparator<Entry>) | Schema | - | second form fixes the order and writes prop `talend.fields.order` |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST build RECORD schemas through the factory (DAT-016) so the host-selected implementation (memory or Avro, DAT-034) is used.
- **Server exposure**: none
- **Maturity level**: 0 - needed to create schemas.
- **Example**:

```java
Schema s = factory.newSchemaBuilder(Schema.Type.RECORD)
    .withEntry(factory.newEntryBuilder().withName("a").withType(Schema.Type.STRING).build())
    .withEntry(factory.newEntryBuilder().withName("b").withType(Schema.Type.INT).build())
    .build();
```

### DAT-009 `Schema.EntriesOrder`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/Schema.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/record-types.adoc`)
- **Category / Sub-area**: DAT / Schema
- **Kind**: class
- **Applies to**: input, processor, output
- **Description**: Comparator that orders entries following a comma-separated list of names (`talend.fields.order` schema prop, `SchemaImpl.ENTRIES_ORDER_PROP`). Entries missing from the list sort last. `SchemaImpl` recomputes the prop from the entries when it is absent.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | of(String) / of(Iterable<String>) | EntriesOrder | - | factory; string form splits on `,` |
  | moveAfter / moveBefore / swap | EntriesOrder | - | in-place reordering (throws IllegalArgumentException if a name is not present) |
  | getFieldsOrder() / toFields() | Stream<String> / String | - | current order |
  | getComparator() | Comparator<Entry> | - | lazily built, reset on any change |

- **Contract for the Designer**: SHOULD display columns using the order given by `getEntriesOrdered()` / prop `talend.fields.order`.
- **Contract for the Runtime**: SHOULD preserve the order property when copying/transforming schemas.
- **Server exposure**: Schema JSON prop `talend.fields.order`; SchemaConverter JSON key `order` (DAT-032).
- **Maturity level**: 1 - wrong order degrades UX but data stays correct (values are accessed by name).
- **Example**:

```json
{"type": "RECORD", "entries": [{"name":"b","type":"INT"},{"name":"a","type":"STRING"}], "props": {"talend.fields.order": "a,b"}}
```

### DAT-010 `OrderedMap`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/OrderedMap.java`
- **Category / Sub-area**: DAT / Schema
- **Kind**: class
- **Applies to**: framework internal (used by schema/record builders)
- **Description**: Insertion-ordered map with O(1) access and controllable order (`addValue`, `moveAfter`, `moveBefore`, `swap`, `replace`, `removeValue`, `getValue`, `streams`). Keyed by an identifier function; duplicate ids are silently ignored on `addValue`. Public API type but only relevant to implementers of Record/Schema.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | OrderedMap(Function<T,String> id[, Iterable<T>]) | constructor | - | identifier extractor and initial values |
  | addValue / removeValue / replace / getValue | - | - | removeValue on unknown id -> IllegalArgumentException |
  | moveAfter / moveBefore / swap | - | - | unknown pivot -> IllegalArgumentException `X not in schema` |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY reuse when implementing a custom Record/Schema; otherwise none.
- **Server exposure**: none
- **Maturity level**: 2 - only needed by alternative Record implementations.
- **Example**: none

### DAT-011 `SchemaProperty`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/SchemaProperty.java`
- **Category / Sub-area**: DAT / Schema
- **Kind**: constants
- **Applies to**: input, processor, output
- **Description**: Well-known keys of `Entry.getProps()` / `Schema.getProps()`. All values are strings.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | ORIGIN_TYPE | String | `field.origin.type` | type in the origin system (e.g. DB column type) |
  | LOGICAL_TYPE | String | `field.logical.type` | date \| time \| timestamp \| uuid |
  | SIZE | String | `field.size` | length/precision |
  | SCALE | String | `field.scale` | decimal scale |
  | PATTERN | String | `field.pattern` | date/format pattern |
  | STUDIO_TYPE | String | `talend.studio.type` | Talend Studio type id |
  | IS_KEY | String | `field.key` | primary key flag |
  | IS_FOREIGN_KEY | String | `field.foreign.key` | foreign key flag |
  | IS_UNIQUE | String | `field.unique` | unique flag |
  | ALLOW_SPECIAL_NAME | String | `field.special.name` | allow special characters in name |
  | ENTRY_IS_ON_ERROR | String | `record.value.on.error` | true when the entry value is invalid (DAT-014) |
  | ENTRY_ERROR_MESSAGE | String | `record.value.on.error.message` | error message |
  | ENTRY_ERROR_FALLBACK_VALUE | String | `record.value.on.error.fallback_value` | raw value that failed |

- **Contract for the Designer**: SHOULD honor `field.key`, `field.size`, `field.scale`, `field.pattern`, `field.origin.type` when showing or mapping schemas.
- **Contract for the Runtime**: MUST preserve props when copying entries; SHOULD NOT invent semantics for unknown keys.
- **Server exposure**: `props` map of Schema/Entry JSON.
- **Maturity level**: 1 - metadata is optional; core flow works without it.
- **Example**:

```json
{"name":"id","type":"LONG","nullable":false,"props":{"field.key":"true","field.origin.type":"BIGINT"}}
```

### DAT-012 `SchemaProperty.LogicalType`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/SchemaProperty.java`
- **Category / Sub-area**: DAT / Schema
- **Kind**: enum
- **Applies to**: input, processor, output
- **Description**: Refines DATETIME (and STRING for UUID). `storageType()` is `DATETIME` for DATE, TIME, TIMESTAMP and `STRING` for UUID. In Avro (DAT-031) DATE maps to int `date`, TIME to int `time-millis`, TIMESTAMP (or none) to long `timestamp-millis`.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | DATE | enum | `date` | day precision |
  | TIME | enum | `time` | time of day (millis) |
  | TIMESTAMP | enum | `timestamp` | instant |
  | UUID | enum | `uuid` | stored as STRING |

- **Contract for the Designer**: SHOULD render DATE/TIME/TIMESTAMP with the matching widget/format.
- **Contract for the Runtime**: MUST keep `field.logical.type` when converting to/from Avro.
- **Server exposure**: Entry prop `field.logical.type`.
- **Maturity level**: 1 - without it DATETIME is treated as a full timestamp; still correct but lossy for date-only data.
- **Example**:

```java
factory.newEntryBuilder().withName("d").withType(Schema.Type.DATETIME).withLogicalType(SchemaProperty.LogicalType.DATE).build();
```

### DAT-013 `SchemaCompanionUtil (name sanitization and collisions)`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/SchemaCompanionUtil.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/record-types.adoc`)
- **Category / Sub-area**: DAT / Schema
- **Kind**: convention
- **Applies to**: input, processor, output
- **Description**: Entry names are sanitized to be Avro-compatible: first char must be a letter or `_` (otherwise skipped when the next char is valid, else replaced by `_`), other chars must be letter/digit/`_` (else `_`); non-ASCII letters become `_`, non-ASCII non-letters are Base64-encoded then sanitized. Examples from the docs: `1foo`->`foo`, `f@o`->`f_o`, `1234f5@o`->`___f5_o`. `avoidCollision` renames a colliding entry to `<sanitized raw>_<n>` (n from 1). Disabled by system property `talend.component.record.skip.sanitize=true` (which is INCOMPATIBLE with the Avro record factory).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | sanitizeName(String) | String | - | sanitized name, null/empty returned as is |
  | avoidCollision(Entry, Function<String,Entry>, BiConsumer<String,Entry>) | Entry | - | returns the entry to add (maybe renamed), or null when the same raw entry already exists |

- **Contract for the Designer**: MUST use `name` (sanitized) as the technical identifier and MAY show `rawName`.
- **Contract for the Runtime**: MUST NOT rely on original field names for lookups; use `Entry.getName()`.
- **Server exposure**: Entry `name` vs `rawName` in Schema JSON.
- **Maturity level**: 0 - names differ from source names; lookup by raw name fails.
- **Example**:

```java
SchemaCompanionUtil.sanitizeName("f@o"); // "f_o" 
```

### DAT-014 `Entry-level error support`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/Record.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-record.adoc`)
- **Category / Sub-area**: DAT / Record
- **Kind**: convention
- **Applies to**: input (quality-oriented connectors)
- **Description**: When JVM property `talend.component.record.error.support=true` and the entry is `errorCapable`, a value that fails validation (wrong type, provided-schema mismatch) does not throw: the entry is rewritten as nullable with props `record.value.on.error=true`, `record.value.on.error.message`, `record.value.on.error.fallback_value`, the value stays null and `Record.isValid()` returns false. Otherwise IllegalArgumentException is thrown with the message. Default: disabled.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | talend.component.record.error.support | system property | false | enable the feature (RECORD_ERROR_SUPPORT) |
  | Entry.Builder.withErrorCapable(boolean) | builder | false | opt-in per entry |

- **Contract for the Designer**: MAY display invalid cells using the entry props.
- **Contract for the Runtime**: MAY set the system property; SHOULD check `entry.isValid()` before reading and MUST NOT treat invalid entries as data.
- **Server exposure**: none (runtime-only).
- **Maturity level**: 2 - optional data-quality capability, off by default.
- **Example**:

```java
if (!record.getSchema().getEntry("age").isValid()) {
    String msg = record.getSchema().getEntry("age").getErrorMessage();
}
```

### DAT-015 `Nullable check switch`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/Record.java`
- **Category / Sub-area**: DAT / Record
- **Kind**: convention
- **Applies to**: input, processor, output
- **Description**: JVM property `talend.component.record.nullable.check` (default `false`; constant `Record.RECORD_NULLABLE_CHECK`) is read by `RecordImpl.BuilderImpl` as `skipNullCheck`. Despite its name, `true` SKIPS the null/missing-entry validation: with the default `false`, a null value for a non-nullable entry throws IllegalArgumentException `<name> is not nullable but got a null value` and missing non-nullable entries throw `Missing entries: ...` at `build()`. (Naming inferred from code: variable `skipNullCheck`.)
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | talend.component.record.nullable.check | system property | false | true = skip nullability validation |

- **Contract for the Designer**: none
- **Contract for the Runtime**: SHOULD leave the default; MAY set true for performance in a trusted environment.
- **Server exposure**: none
- **Maturity level**: 2 - tuning switch; default behaviour is correct.
- **Example**: none

### DAT-016 `RecordBuilderFactory`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/record/RecordBuilderFactory.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/services-built-in.adoc`)
- **Category / Sub-area**: DAT / Factory service
- **Kind**: interface
- **Applies to**: service (injectable into components and services), host
- **Description**: Entry point to create records, schemas and entries. Serializable service provided per plugin by the runtime (`ComponentManager.getRecordBuilderFactoryProvider()`); serialized as a `SerializableService` reference. Implementation chosen by SPI `RecordBuilderFactoryProvider` (DAT-034).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | newRecordBuilder() | Record.Builder | - | schema inferred |
  | newRecordBuilder(Schema) | Record.Builder | - | enforces the schema |
  | newRecordBuilder(Schema, Record) | Record.Builder | - | copies matching entries (by name) of the record |
  | newSchemaBuilder(Schema.Type) | Schema.Builder | - | RECORD/ARRAY -> mutable builder; other types -> shared primitive schema |
  | newSchemaBuilder(Schema) | Schema.Builder | - | copy: entries (RECORD) or element schema (ARRAY) |
  | newEntryBuilder() | Entry.Builder | - | new entry |
  | newEntryBuilder(Entry) | Entry.Builder | default method | copy of type, nullable, name, elementSchema, defaultValue, comment, props |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST provide it as an injectable service to every plugin container (ComponentManager does by default).
- **Server exposure**: none
- **Maturity level**: 0 - components cannot create records without it.
- **Example**:

```java
@Service class MyService { RecordBuilderFactory factory; MyService(RecordBuilderFactory f) { this.factory = f; } }
```

### DAT-017 `RecordService`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/record/RecordService.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/services-built-in.adoc`)
- **Category / Sub-area**: DAT / Factory service
- **Kind**: interface
- **Applies to**: service
- **Description**: Helper service (implementation `RecordServiceImpl`) for POJO <-> Record conversion, forwarding an entry between records, and custom record rebuilding with a `Collector`. Merged from SVC-010: injectable built-in service (applies to processor, output, input and service); `RecordVisitor<T>` is DAT-018 and `RecordBuilderFactory` is DAT-016.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | toObject(Record, Class<T>) | T | - | record to POJO |
  | toRecord(T) | Record | - | POJO to record |
  | forwardEntry(Record source, Record.Builder, String sourceColumn, Entry entry) | boolean | - | copies the value if present; true when forwarded |
  | toRecord(Schema, Record fallback, BiFunction customHandler, BiConsumer beforeFinish) | Collector<Entry,Builder,Record> | - | custom record assembly |
  | create(Schema, Record, BiFunction, BiConsumer) | Record | - | shortcut for the collector |
  | visit(RecordVisitor<T>, Record) | T | - | visitor traversal (DAT-018) |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST provide it as a built-in service when hosting TCK plugins with a custom container; SHOULD reuse `RecordServiceImpl`. The injected instance MUST be backed by the plugin's `RecordBuilderFactory` and JSON-B.
- **Server exposure**: none
- **Maturity level**: 1 - used by some components; core flow works without it.
- **Example**: none

### DAT-018 `RecordVisitor`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/record/RecordVisitor.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/services-built-in.adoc`)
- **Category / Sub-area**: DAT / Factory service
- **Kind**: interface
- **Applies to**: service
- **Description**: Visitor over record entries with no-op default callbacks; also a `Supplier<T>` (result) and `BinaryOperator<T>` (merge). `onRecord`/`onRecordArray` return the visitor to use for nested content (default: this).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | onInt/onLong/onFloat/onDouble/onBoolean/onString/onObject/onDatetime/onInstant/onDecimal/onBytes | void | no-op | scalar entries with Optional values (OptionalInt/Long/Double for numeric) |
  | onRecord(Entry, Optional<Record>) | RecordVisitor<T> | this | nested record |
  | onIntArray/onLongArray/onFloatArray/onDoubleArray/onBooleanArray/onStringArray/onDatetimeArray/onDecimalArray/onBytesArray | void | no-op | arrays of scalars |
  | onRecordArray(Entry, Optional<Collection<Record>>) | RecordVisitor<T> | this | array of records |
  | get() / apply(T,T) | T | null / t1 | result and merge |

- **Contract for the Designer**: none
- **Contract for the Runtime**: SHOULD implement RecordService.visit to support it; not needed otherwise.
- **Server exposure**: none
- **Maturity level**: 2 - convenience API for components.
- **Example**:

```java
service.visit(new RecordVisitor<String>() { StringBuilder sb = new StringBuilder();
  public void onString(Schema.Entry e, Optional<String> v) { v.ifPresent(sb::append); }
  public String get() { return sb.toString(); } }, record);
```

### DAT-019 `RecordPointer`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/RecordPointer.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/services-built-in.adoc`)
- **Category / Sub-area**: DAT / Record navigation
- **Kind**: interface
- **Applies to**: service
- **Description**: Extraction logic from a JSON-Pointer-like path applied to a Record. `getValue(Record, Class<T>)` throws IllegalArgumentException if the value does not exist.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | getValue(Record target, Class<T> type) | T | - | extract and cast; type mismatch -> ClassCastException |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST implement with the semantics of DAT-020 if RecordPointerFactory is provided.
- **Server exposure**: none
- **Maturity level**: 2 - optional navigation helper.
- **Example**: none

### DAT-020 `RecordPointerFactory`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/RecordPointerFactory.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/services-built-in.adoc`)
- **Category / Sub-area**: DAT / Record navigation
- **Kind**: interface
- **Applies to**: service
- **Description**: `FunctionalInterface` `RecordPointer apply(String pointer)`. Reference implementation `RecordPointerFactoryImpl`: pointer is `""` or `"/"` (whole record) or must start with `/`; tokens split on `/`, `~1` -> `/`, `~0` -> `~`; on a Record a token is an entry name (missing -> IllegalArgumentException `contains no value for name`), on a Collection a decimal index without sign or leading zero, otherwise IllegalArgumentException. Instances are reusable and Serializable. Merged from SVC-011: injectable built-in service scoped to the plugin (applies to processor, output and service); instances are reusable and thread-safe.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | apply(String pointer) | RecordPointer | - | compiled pointer; null -> NullPointerException |

- **Contract for the Designer**: none
- **Contract for the Runtime**: SHOULD provide the reference semantics (RFC 6901 adapted to records). MUST inject a factory scoped to the plugin.
- **Server exposure**: none
- **Maturity level**: 1 - listed built-in service, needed only by components that use pointers (lowest level of the merged entries).
- **Example**:

```java
RecordPointer p = pointerFactory.apply("/address/street");
String street = p.getValue(record, String.class);
```

### DAT-021 `Deprecated schema API (service.schema.Schema and Type)`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/schema/Schema.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/record-types.adoc`)
- **Category / Sub-area**: DAT / Legacy
- **Kind**: class
- **Applies to**: service (legacy DiscoverSchema results)
- **Description**: `@Deprecated` partial implementation of `record.Schema` kept for 1.0 compatibility (`Schema(Collection<Entry>)`, `Entry(String, service.schema.Type)`). Always `RECORD`, always nullable entries, no props/order, `toBuilder()` throws. The companion enum `service.schema.Type` has only `DOUBLE`, `INT`, `BOOLEAN`, `STRING` and maps by name to `record.Schema.Type`. New code MUST use `record.Schema`.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | service.schema.Type | enum | - | DOUBLE, INT, BOOLEAN, STRING |

- **Contract for the Designer**: MUST still accept schemas returned by legacy components (same JSON shape).
- **Contract for the Runtime**: none
- **Server exposure**: Same JSON shape as DAT-004 (`type`=RECORD, `entries`).
- **Maturity level**: 2 - compatibility only.
- **Example**: none

### DAT-022 (moved) -> see ACT-007

### DAT-023 (moved) -> see ACT-008

### DAT-024 `@FixedSchema`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/service/schema/FixedSchema.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/component-registering.adoc`)
- **Category / Sub-area**: DAT / Schema discovery
- **Kind**: annotation (component metadata)
- **Applies to**: input, processor, output (component class)
- **Description**: Declares that a connector has a fixed schema computed by a `@DiscoverSchema`/`@DiscoverSchemaExtended` action with the same name. Exposed as component metadata keys `tcomp::ui::schema::fixed` (action name), `tcomp::ui::schema::flows::fixed` (comma-separated flows, default `__default__`) and `tcomp::ui::schema::fixed::watch` (comma-separated parameter paths).
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | value | String | "" | name of the DiscoverSchema/DiscoverSchemaExtended action |
  | flows | String[] | {} | flows concerned (input: main flow only; output: main, reject, others); main flow is `__default__` |
  | watch | String[] | {} | parameter paths whose change triggers a re-invocation of the action; same relative-path syntax as `@Updatable.parameters()`; requires `value` |

- **Contract for the Designer**: MUST NOT let the user edit the schema of the listed flows; MUST re-call the action when a `watch` path changes.
- **Contract for the Runtime**: none
- **Server exposure**: Component `metadata` map: `tcomp::ui::schema::fixed`, `tcomp::ui::schema::flows::fixed`, `tcomp::ui::schema::fixed::watch` (produced by `ComponentSchemaEnricher`).
- **Maturity level**: 1 - without it the user may edit a schema the connector ignores (wrong behaviour risk is low, UX degraded).
- **Example**:

```java
@Emitter(family = "f", name = "in")
@FixedSchema(value = "fixed", flows = {"__default__"}, watch = {"configuration/dataset/table"})
public class In { ... }
```

### DAT-025 (moved) -> see ACT-013

### DAT-026 (moved) -> see DSG-014

### DAT-027 (moved) -> see ACT-009

### DAT-028 (moved) -> see ACT-009

### DAT-029 `Component data type conversion (Record / JsonObject / POJO)`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/record/RecordConverters.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/record-types.adoc`)
- **Category / Sub-area**: DAT / Conversion
- **Kind**: convention
- **Applies to**: input, processor, output
- **Description**: Runtime wrappers convert automatically: an `@Producer` may return `Record`, `JsonObject` (converted by `json2Record`: numbers -> DOUBLE, arrays/objects nested, nulls skipped), a POJO (through JSON-B into a Record), or a `routines.system.*` row struct (Studio DI). An `@ElementListener`/`@AfterGroup` parameter typed `Record`, `JsonObject` or a POJO is produced from the incoming value (`RecordConverters.toType`). A processor output emitted through `OutputEmitter.emit(Object)` is converted to Record the same way. Primitives and `String` returned by a producer pass through unchanged (tests only).
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST convert non-Record values to Record before handing data to the engine (as `InputImpl.next()` and `BeamOutputEmitter` do); SHOULD standardize on `Record`.
- **Server exposure**: none
- **Maturity level**: 0 - components may return JsonObject or POJOs; the host must not assume Record.
- **Example**:

```java
@Producer public JsonObject next() { return jsonFactory.createObjectBuilder().add("n", 1).build(); } // runtime converts to Record(n:DOUBLE)
```

### DAT-030 `JSON-P / JSON-B mapping of Record`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/record/json/RecordJsonGenerator.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/record-types.adoc`)
- **Category / Sub-area**: DAT / Conversion
- **Kind**: convention
- **Applies to**: input, processor, output, service
- **Description**: Record to JsonObject (`RecordConverters.toJson`): STRING/INT/LONG/FLOAT/DOUBLE/BOOLEAN as JSON scalars, BYTES as Base64 string, DATETIME as ISO-8601 zoned string (`DateTimeFormatter.ISO_ZONED_DATE_TIME`), DECIMAL as string, RECORD nested object, ARRAY as JSON array (homogeneous; ZonedDateTime items as epoch millis numbers), null values omitted. JSON-B config of the runtime: `BinaryDataStrategy.BASE_64`, `johnzon.cdi.activated=false`, `johnzon.accessModeDelegate=TalendAccessMode`. `RecordJsonGenerator` lets JSON-B write a POJO directly into a Record; `PojoJsonbProvider` (a `Supplier<Jsonb>`) marks a Jsonb that can do so.
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST use a Johnzon-compatible JSON-B/JSON-P provider with the settings above when it produces JSON from records; MUST decode BYTES as Base64.
- **Server exposure**: none
- **Maturity level**: 1 - needed for JSON-typed component I/O and for tests; core Record flow is unaffected.
- **Example**:

```json
{"name": "Gary", "birth": "2011-02-06T08:00:00Z[UTC]", "blob": "SGVsbG8=", "amount": "12.50", "tags": ["a", "b"]}
```

### DAT-031 `Avro mapping (Beam runtime)`
- **Source**: `component-runtime-beam/src/main/java/org/talend/sdk/component/runtime/beam/spi/record/AvroSchemaBuilder.java`
- **Category / Sub-area**: DAT / Conversion
- **Kind**: convention
- **Applies to**: input, processor, output (when the runtime uses the Beam module)
- **Description**: `component-runtime-beam` provides `AvroRecord`/`AvroSchema` implementing Record/Schema on Avro (`IndexedRecord`). Mapping: STRING->string, BYTES->bytes (ByteBuffer), INT->int, LONG->long, FLOAT->float, DOUBLE->double, BOOLEAN->boolean, DATETIME->long+`timestamp-millis` (or int+`date` / int+`time-millis` per logical type), DECIMAL->string with logical type `decimal` (custom `Decimal` logical type) marked by prop `talend.component.DECIMAL=true`, RECORD->record, ARRAY->array of nullable union. Nullable entries become `[type, null]` unions. Extra Avro props: `talend.component.label` (raw name), `talend.field.__METADATA__` (alias marking metadata entries), `talend.component.record.entry.errorCapable`, `talend.component.record.value.on.error`. Record name is generated: `org.talend.sdk.component.schema.generated.Record_<n>_<fingerprint>`. Field order follows `talend.fields.order`.
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY use the Avro implementation; if used, MUST run with `talend.component.record.skip.sanitize=false`; MUST NOT mix implementations inside one plugin. Keys from `KeysForAvroProperty`, `AvroPropertyMapper`.
- **Server exposure**: none
- **Maturity level**: 2 - only relevant when embedding the Beam reference module.
- **Example**:

```java
// selected automatically when component-runtime-beam and jackson are on the same classloader
RecordBuilderFactory f = manager.getRecordBuilderFactoryProvider().apply("plugin-id");
```

### DAT-032 `Schema JSON serialization (SchemaConverter)`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/xbean/converter/SchemaConverter.java`
- **Category / Sub-area**: DAT / Conversion
- **Kind**: payload
- **Applies to**: configuration (an `@Option` of type Schema)
- **Description**: A `Schema`-typed option receives its value as a JSON string in the flat configuration map. Format read by `SchemaConverter`: `{type, entries[], metadatas[], elementSchema, props{}, order}`; entry: `{name, type, nullable (default true), metadata (default false), comment, rawName, defaultValue (number|boolean|string only), elementSchema (object, or type-name string for scalar element), props{}}`. `toJson` writes `entries` (all entries incl. metadata flagged `metadata:true`) and `order`.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | type | string | required | RECORD or ARRAY or primitive name |
  | entries | array | - | entries (RECORD) |
  | metadatas | array | - | metadata entries (read only) |
  | elementSchema | object\|string | - | ARRAY element |
  | props | object<string,string> | - | schema props |
  | order | string | - | comma-separated entry order |

- **Contract for the Designer**: MUST serialize a chosen schema with these keys when a component option is of type `Schema`.
- **Contract for the Runtime**: MUST accept this JSON in the option value.
- **Server exposure**: The option appears as a property of `type` object/string with the Schema Java type (see `../05-configuration-and-ui.md`).
- **Maturity level**: 1 - components with Schema-typed options need it.
- **Example**:

```json
{"type":"RECORD","entries":[{"name":"id","type":"LONG","nullable":false},{"name":"tags","type":"ARRAY","elementSchema":"STRING"}],"order":"id,tags"}
```

### DAT-033 `Type conversion rules (MappingUtils.coerce)`
- **Source**: `component-runtime-impl/src/main/java/org/talend/sdk/component/runtime/record/MappingUtils.java`
- **Category / Sub-area**: DAT / Conversion
- **Kind**: convention
- **Applies to**: input, processor, output
- **Description**: Ordered rules when a stored value is not an instance of the requested class: (1) null -> null; (2) Long -> ZonedDateTime (UTC), `Date`, `Instant` via epoch millis; (3) Number -> requested Number/BigDecimal/primitive wrapper (`BigDecimal.valueOf(double)`, narrowing via `intValue()` etc.); (4) primitive <-> wrapper; (5) any -> String via `String.valueOf`; (6) Instant -> ZonedDateTime, Timestamp, Long; (7) `long[2]` seconds/nanos -> Instant/ZonedDateTime; (8) String -> Boolean, ZonedDateTime (numeric string = epoch millis else ISO parse), Date, char, byte[] (Base64 decode, fallback `getBytes()`), BigDecimal, Integer, Long, Short, Byte, Float, Double; the literal string `null` (case-insensitive) maps to null; (9) otherwise IllegalArgumentException.
- **Attributes**: none
- **Contract for the Designer**: none
- **Contract for the Runtime**: MUST reproduce these rules if the host supplies its own Record implementation; SHOULD reuse `RecordImpl`.
- **Server exposure**: none
- **Maturity level**: 1 - rare divergence causes subtle data differences (host-specific implementations only).
- **Example**: none

### DAT-034 `RecordBuilderFactoryProvider SPI`
- **Source**: `component-runtime-manager/src/main/java/org/talend/sdk/component/runtime/manager/service/record/RecordBuilderFactoryProvider.java`
- **Category / Sub-area**: DAT / Factory service
- **Kind**: interface (SPI)
- **Applies to**: host
- **Description**: `java.util.function.Function<String,RecordBuilderFactory>` loaded by ServiceLoader in `ComponentManager`; exactly zero or one provider allowed (two -> IllegalArgumentException `Ambiguous recordBuilderFactory`). Default `RecordBuilderFactoryImpl::new`. The Beam module registers `AvroRecordBuilderFactoryProvider`, controlled by system property `talend.component.beam.record.factory.impl` = `auto` (default), `memory`/`default`, `avro`.
- **Attributes**:

  | Name | Type | Default | Meaning |
  |---|---|---|---|
  | talend.component.beam.record.factory.impl | system property | auto | auto \| memory \| default \| avro |

- **Contract for the Designer**: none
- **Contract for the Runtime**: MAY provide one implementation per JVM; MUST make it return Serializable factories (they serialize as SerializableService).
- **Server exposure**: none
- **Maturity level**: 2 - extension point for alternative record backends.
- **Example**: none

### DAT-035 `Metadata entries`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/Schema.java`
- **Category / Sub-area**: DAT / Schema
- **Kind**: convention
- **Applies to**: input, processor, output
- **Description**: `Entry.isMetadata()` marks entries that carry out-of-band data (not business columns). `Schema.getEntries()` excludes them, `getMetadata()` lists them, `getAllEntries()` returns both (metadata first). Record accessors work identically for both kinds.
- **Attributes**: none
- **Contract for the Designer**: SHOULD hide metadata entries from the default column list and expose them separately.
- **Contract for the Runtime**: MUST NOT drop metadata entries when copying records.
- **Server exposure**: Schema JSON `metadata` array (SchemaImpl) or entry flag `metadata:true`.
- **Maturity level**: 2 - optional feature, ignoring it loses only metadata.
- **Example**: none

### DAT-036 `Arrays and nested records`
- **Source**: `component-api/src/main/java/org/talend/sdk/component/api/record/Schema.java` (doc: `documentation/src/main/antora/modules/ROOT/pages/record-types.adoc`)
- **Category / Sub-area**: DAT / Schema
- **Kind**: convention
- **Applies to**: input, processor, output
- **Description**: An ARRAY entry needs an `elementSchema` and all items MUST have the same type (a map is modelled as an array of records with key/value entries). A RECORD entry needs `elementSchema` = schema of the nested record. JSON arrays of objects with different field sets are merged into a union schema by `json2Record`. Empty arrays get element type STRING.
- **Attributes**: none
- **Contract for the Designer**: MUST handle ARRAY/RECORD entries (render nested, or flatten) when propagating schemas.
- **Contract for the Runtime**: MUST handle nested types in transport and conversion.
- **Server exposure**: `elementSchema` in Schema JSON.
- **Maturity level**: 1 - nested data is common but flat-only hosts still work with flat components.
- **Example**:

```json
{"name":"address","type":"RECORD","nullable":true,"elementSchema":{"type":"RECORD","entries":[{"name":"city","type":"STRING"}]}}
```
