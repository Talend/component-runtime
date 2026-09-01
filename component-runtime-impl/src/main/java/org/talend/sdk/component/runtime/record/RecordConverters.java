/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.record;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.spi.JsonProvider;

import org.apache.johnzon.core.JsonLongImpl;
import org.apache.johnzon.jsonb.extension.JsonValueReader;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.json.OutputRecordHolder;
import org.talend.sdk.component.runtime.record.json.PojoJsonbProvider;

import lombok.Data;

public class RecordConverters implements Serializable {

    public <T> Record toRecord(final MappingMetaRegistry registry, final T data, final Supplier<Jsonb> jsonbProvider,
            final Supplier<RecordBuilderFactory> recordBuilderProvider) {
        if (data == null) {
            return null;
        }
        if (data instanceof Record record) {
            return record;
        }
        if (data instanceof JsonObject jsonObject) {
            return json2Record(recordBuilderProvider.get(), jsonObject);
        }

        final MappingMeta meta = registry.find(data.getClass());
        if (meta.isLinearMapping()) {
            return meta.newRecord(data, recordBuilderProvider.get());
        }

        final Jsonb jsonb = jsonbProvider.get();
        if (!(data instanceof String) && !data.getClass().isPrimitive()
                && jsonb instanceof PojoJsonbProvider pojoJsonbProvider) {
            final Jsonb pojoMapper = pojoJsonbProvider.get();
            final OutputRecordHolder holder = new OutputRecordHolder(data);
            try (final OutputRecordHolder stream = holder) {
                pojoMapper.toJson(data, stream);
            }
            return holder.getRecord();
        }
        return json2Record(recordBuilderProvider.get(), jsonb.fromJson(jsonb.toJson(data), JsonObject.class));
    }

    private Record json2Record(final RecordBuilderFactory factory, final JsonObject object) {
        final Record.Builder builder = factory.newRecordBuilder();
        object.forEach((key, value) -> {
            switch (value.getValueType()) {
                case ARRAY: {
                    final List<Object> items =
                            value.asJsonArray().stream().map(it -> mapJson(factory, it)).collect(toList());
                    builder
                            .withArray(factory
                                    .newEntryBuilder()
                                    .withName(key)
                                    .withType(Schema.Type.ARRAY)
                                    .withElementSchema(getArrayElementSchema(factory, items))
                                    .build(), items);
                    break;
                }
                case OBJECT: {
                    final Record record = json2Record(factory, value.asJsonObject());
                    builder
                            .withRecord(factory
                                    .newEntryBuilder()
                                    .withName(key)
                                    .withType(Schema.Type.RECORD)
                                    .withElementSchema(record.getSchema())
                                    .build(), record);
                    break;
                }
                case TRUE, FALSE:
                    builder.withBoolean(key, JsonValue.TRUE.equals(value));
                    break;
                case STRING:
                    builder.withString(key, ((JsonString) value).getString());
                    break;
                case NUMBER:
                    final JsonNumber number = (JsonNumber) value;
                    builder.withDouble(key, number.doubleValue());
                    break;
                case NULL:
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported value type: " + value);
            }
        });
        return builder.build();
    }

    private Schema getArrayElementSchema(final RecordBuilderFactory factory, final List<Object> items) {
        if (items.isEmpty()) {
            return factory.newSchemaBuilder(Schema.Type.STRING).build();
        }
        final Schema firstSchema = toSchema(factory, items.iterator().next());
        switch (firstSchema.getType()) {
            case RECORD:
                return items.stream().map(it -> toSchema(factory, it)).reduce(null, (s1, s2) -> {
                    if (s1 == null) {
                        return s2;
                    }
                    if (s2 == null) { // unlikely
                        return s1;
                    }
                    final Set<String> names1 = s1.getAllEntries().map(Schema.Entry::getName).collect(toSet());
                    final Set<String> names2 = s2.getAllEntries().map(Schema.Entry::getName).collect(toSet());
                    if (!names1.equals(names2)) {
                        // here we are not good since values will not be right anymore,
                        // forbidden for current version anyway but potentially supported later
                        final Schema.Builder builder = factory.newSchemaBuilder(Schema.Type.RECORD);
                        s1.getAllEntries().forEach(builder::withEntry);
                        s2.getAllEntries().filter(it -> !(names1.contains(it.getName()))).forEach(builder::withEntry);
                        return builder.build();
                    }
                    return s1;
                });
            default:
                return firstSchema;
        }
    }

    private Object mapJson(final RecordBuilderFactory factory, final JsonValue it) {
        if (it instanceof JsonObject jsonObject) {
            return json2Record(factory, jsonObject);
        }
        if (it instanceof JsonArray jsonValues) {
            return jsonValues.stream().map(i -> mapJson(factory, i)).collect(toList());
        }
        if (it instanceof JsonString jsonString) {
            return jsonString.getString();
        }
        if (it instanceof JsonNumber jsonNumber) {
            return jsonNumber.numberValue();
        }
        if (JsonValue.FALSE.equals(it)) {
            return false;
        }
        if (JsonValue.TRUE.equals(it)) {
            return true;
        }
        if (JsonValue.NULL.equals(it)) {
            return null;
        }
        return it;
    }

    public static Schema toSchema(final RecordBuilderFactory factory, final Object next) {
        if (next instanceof String || next instanceof JsonString) {
            return factory.newSchemaBuilder(Schema.Type.STRING).build();
        }
        if (next instanceof Integer) {
            return factory.newSchemaBuilder(Schema.Type.INT).build();
        }
        if (next instanceof Long || next instanceof JsonLongImpl) {
            return factory.newSchemaBuilder(Schema.Type.LONG).build();
        }
        if (next instanceof Float) {
            return factory.newSchemaBuilder(Schema.Type.FLOAT).build();
        }
        if (next instanceof JsonNumber) {
            return factory.newSchemaBuilder(Schema.Type.DOUBLE).build();
        }
        if (next instanceof Double || next instanceof JsonNumber) {
            return factory.newSchemaBuilder(Schema.Type.DOUBLE).build();
        }
        if (next instanceof Boolean || JsonValue.TRUE.equals(next) || JsonValue.FALSE.equals(next)) {
            return factory.newSchemaBuilder(Schema.Type.BOOLEAN).build();
        }
        if (next instanceof Date || next instanceof ZonedDateTime || next instanceof Instant) {
            return factory.newSchemaBuilder(Schema.Type.DATETIME).build();
        }
        if (next instanceof BigDecimal) {
            return factory.newSchemaBuilder(Schema.Type.DECIMAL).build();
        }
        if (next instanceof byte[]) {
            return factory.newSchemaBuilder(Schema.Type.BYTES).build();
        }
        if (next instanceof Collection || next instanceof JsonArray) {
            final Collection collection = (Collection) next;
            if (collection.isEmpty()) {
                return factory.newSchemaBuilder(Schema.Type.STRING).build();
            }
            return factory
                    .newSchemaBuilder(Schema.Type.ARRAY)
                    .withElementSchema(toSchema(factory, collection.iterator().next()))
                    .build();
        }
        if (next instanceof Record record) {
            return record.getSchema();
        }
        throw new IllegalArgumentException("unsupported type for " + next);
    }

    public Object toType(final MappingMetaRegistry registry, final Object data, final Class<?> parameterType,
            final Supplier<JsonBuilderFactory> factorySupplier, final Supplier<JsonProvider> providerSupplier,
            final Supplier<Jsonb> jsonbProvider, final Supplier<RecordBuilderFactory> recordBuilderProvider) {
        return toType(registry, data, parameterType, factorySupplier, providerSupplier, jsonbProvider,
                recordBuilderProvider, Collections.emptyMap());
    }

    public Object toType(final MappingMetaRegistry registry, final Object data, final Class<?> parameterType,
            final Supplier<JsonBuilderFactory> factorySupplier, final Supplier<JsonProvider> providerSupplier,
            final Supplier<Jsonb> jsonbProvider, final Supplier<RecordBuilderFactory> recordBuilderProvider,
            final java.util.Map<String, String> metadata) {
        if (parameterType.isInstance(data)) {
            return data;
        }

        final JsonObject inputAsJson;
        if (data instanceof JsonObject jsonObject) {
            if (JsonObject.class == parameterType) {
                return data;
            }
            inputAsJson = jsonObject;
        } else if (data instanceof Record record) {
            if (!JsonObject.class.isAssignableFrom(parameterType)) {
                final MappingMeta mappingMeta = registry.find(parameterType);
                if (mappingMeta.isLinearMapping()) {
                    return mappingMeta.newInstance(record, metadata);
                }
            }
            final JsonObject asJson = toJson(factorySupplier, providerSupplier, record);
            if (JsonObject.class == parameterType) {
                return asJson;
            }
            inputAsJson = asJson;
        } else {
            if (parameterType == Record.class) {
                return toRecord(registry, data, jsonbProvider, recordBuilderProvider);
            }
            final Jsonb jsonb = jsonbProvider.get();
            inputAsJson = jsonb.fromJson(jsonb.toJson(data), JsonObject.class);
        }
        return jsonbProvider.get().fromJson(new JsonValueReader<>(inputAsJson), parameterType);
    }

    private JsonObject toJson(final Supplier<JsonBuilderFactory> factorySupplier,
            final Supplier<JsonProvider> providerSupplier, final Record record) {
        return buildRecord(factorySupplier.get(), providerSupplier, record).build();
    }

    private JsonObjectBuilder buildRecord(final JsonBuilderFactory factory,
            final Supplier<JsonProvider> providerSupplier, final Record record) {
        final Schema schema = record.getSchema();
        final JsonObjectBuilder builder = factory.createObjectBuilder();
        schema.getEntries().forEach(entry -> {
            final String name = entry.getName();
            switch (entry.getType()) {
                case STRING: {
                    final String value = record.get(String.class, name);
                    if (value != null) {
                        builder.add(name, value);
                    }
                    break;
                }
                case INT: {
                    final Integer value = record.get(Integer.class, name);
                    if (value != null) {
                        builder.add(name, value);
                    }
                    break;
                }
                case LONG: {
                    final Long value = record.get(Long.class, name);
                    if (value != null) {
                        builder.add(name, value);
                    }
                    break;
                }
                case FLOAT: {
                    final Float value = record.get(Float.class, name);
                    if (value != null) {
                        builder.add(name, value);
                    }
                    break;
                }
                case DOUBLE: {
                    final Double value = record.get(Double.class, name);
                    if (value != null) {
                        builder.add(name, value);
                    }
                    break;
                }
                case BOOLEAN: {
                    final Boolean value = record.get(Boolean.class, name);
                    if (value != null) {
                        builder.add(name, value);
                    }
                    break;
                }
                case BYTES: {
                    final byte[] value = record.get(byte[].class, name);
                    if (value != null) {
                        builder.add(name, Base64.getEncoder().encodeToString(value));
                    }
                    break;
                }
                case DATETIME: {
                    final ZonedDateTime value = record.get(ZonedDateTime.class, name);
                    if (value != null) {
                        builder.add(name, value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
                    }
                    break;
                }
                case DECIMAL: {
                    final BigDecimal value = record.get(BigDecimal.class, name);
                    if (value != null) {
                        builder.add(name, value.toString());
                    }
                    break;
                }
                case RECORD: {
                    final Record value = record.get(Record.class, name);
                    if (value != null) {
                        builder.add(name, buildRecord(factory, providerSupplier, value));
                    }
                    break;
                }
                case ARRAY:
                    final Collection<?> collection = record.get(Collection.class, name);
                    if (collection == null) {
                        break;
                    }
                    if (collection.isEmpty()) {
                        builder.add(name, factory.createArrayBuilder().build());
                    } else { // only homogeneous collections
                        final Object item = collection.iterator().next();
                        if (item instanceof String) {
                            final JsonProvider jsonProvider = providerSupplier.get();
                            builder.add(name,
                                    toArray(factory, v -> jsonProvider.createValue((String) v), collection));
                        } else if (item instanceof Double) {
                            final JsonProvider jsonProvider = providerSupplier.get();
                            builder.add(name,
                                    toArray(factory, v -> jsonProvider.createValue((Double) v), collection));
                        } else if (item instanceof Float) {
                            final JsonProvider jsonProvider = providerSupplier.get();
                            builder.add(name,
                                    toArray(factory, v -> jsonProvider.createValue((Float) v), collection));
                        } else if (item instanceof Integer) {
                            final JsonProvider jsonProvider = providerSupplier.get();
                            builder.add(name,
                                    toArray(factory, v -> jsonProvider.createValue((Integer) v), collection));
                        } else if (item instanceof Long) {
                            final JsonProvider jsonProvider = providerSupplier.get();
                            builder.add(name,
                                    toArray(factory, v -> jsonProvider.createValue((Long) v), collection));
                        } else if (item instanceof Boolean) {
                            builder.add(name, toArray(factory,
                                    v -> (Boolean) v ? JsonValue.TRUE : JsonValue.FALSE, collection));
                        } else if (item instanceof ZonedDateTime) {
                            final JsonProvider jsonProvider = providerSupplier.get();
                            builder.add(name,
                                    toArray(factory,
                                            v -> jsonProvider.createValue(
                                                    ((ZonedDateTime) v).toInstant().toEpochMilli()),
                                            collection));
                        } else if (item instanceof Date) {
                            final JsonProvider jsonProvider = providerSupplier.get();
                            builder.add(name,
                                    toArray(factory,
                                            v -> jsonProvider.createValue(((Date) v).getTime()),
                                            collection));
                        } else if (item instanceof Record) {
                            builder.add(name,
                                    toArray(factory,
                                            v -> buildRecord(factory, providerSupplier, (Record) v).build(),
                                            collection));
                        } else if (item instanceof JsonValue) {
                            builder.add(name, toArray(factory, JsonValue.class::cast, collection));
                        } // else throw?
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type: " + entry.getType() + " for '" + name + "'");
            }
        });
        return builder;
    }

    private JsonArray toArray(final JsonBuilderFactory factory, final Function<Object, JsonValue> valueFactory,
            final Collection<?> collection) {
        final Collector<JsonValue, JsonArrayBuilder, JsonArray> collector = Collector
                .of(factory::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::addAll,
                        JsonArrayBuilder::build);
        return collection.stream().map(valueFactory).collect(collector);
    }

    public <T> T coerce(final Class<T> expectedType, final Object value, final String name) {
        if (value == null) {
            return null;
        }

        // here mean get(Object.class, name) return origin store type, like DATETIME return long, is expected?
        if (!expectedType.isInstance(value)) {
            return expectedType.cast(MappingUtils.coerce(expectedType, value, name));
        }

        return expectedType.cast(value);
    }

    @Data
    public static class MappingMeta {

        private final boolean linearMapping;

        private final Class<?> rowStruct;

        private Object recordVisitor;

        private Method visitRecord;

        private Object rowStructVisitor;

        private Method visitRowStruct;

        public MappingMeta(final Class<?> type, final MappingMetaRegistry registry) {
            linearMapping = Stream.of(type.getInterfaces()).anyMatch(it -> it.getName().startsWith("routines.system."));
            rowStruct = type;
        }

        public Object newInstance(final Record record) {
            return newInstance(record, Collections.emptyMap());
        }

        public Object newInstance(final Record record, final java.util.Map<String, String> metadata) {
            if (recordVisitor == null) {
                try {
                    final String className = "org.talend.sdk.component.runtime.di.record.DiRecordVisitor";
                    final Class<?> visitorClass = getClass().getClassLoader().loadClass(className);
                    final Constructor<?> constructor = visitorClass.getDeclaredConstructors()[0];
                    constructor.setAccessible(true);
                    recordVisitor = constructor.newInstance(rowStruct, metadata);
                    visitRecord = visitorClass.getDeclaredMethod("visit", Record.class);
                } catch (final NoClassDefFoundError | ClassNotFoundException | InstantiationException
                        | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    if (e.getMessage().matches(".*routines.system.Dynamic.*")) {
                        throw new IllegalStateException("TOS does not support dynamic type", e);
                    }
                    throw new IllegalStateException(e);
                }
            }
            try {
                return visitRecord.invoke(recordVisitor, record);
            } catch (final IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }

        public <T> Record newRecord(final T data, final RecordBuilderFactory factory) {
            if (rowStructVisitor == null) {
                try {
                    final String className = "org.talend.sdk.component.runtime.di.record.DiRowStructVisitor";
                    final Class<?> visitorClass = getClass().getClassLoader().loadClass(className);
                    final Constructor<?> constructor = visitorClass.getConstructors()[0];
                    constructor.setAccessible(true);
                    rowStructVisitor = constructor.newInstance();
                    visitRowStruct = visitorClass.getMethod("get", Object.class, RecordBuilderFactory.class);
                } catch (final NoClassDefFoundError | ClassNotFoundException | InstantiationException
                        | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    if (e.getMessage().matches(".*routines.system.Dynamic.*")) {
                        throw new IllegalStateException("TOS does not support dynamic type", e);
                    }
                    throw new IllegalStateException(e);
                }
            }
            try {
                return (Record) visitRowStruct.invoke(rowStructVisitor, data, factory);
            } catch (final IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }

    }

    @Data
    public static class MappingMetaRegistry implements Serializable {

        protected final Map<Class<?>, MappingMeta> registry = new ConcurrentHashMap<>();

        private Object writeReplace() throws ObjectStreamException {
            return new Factory(); // don't serialize the mapping, recalculate it lazily
        }

        public MappingMeta find(final Class<?> parameterType) {
            final MappingMeta meta = registry.get(parameterType);
            if (meta != null) {
                return meta;
            }
            final MappingMeta mappingMeta = new MappingMeta(parameterType, this);
            final MappingMeta existing = registry.putIfAbsent(parameterType, mappingMeta);
            if (existing != null) {
                return existing;
            }
            return mappingMeta;
        }

        public static class Factory implements Serializable {

            private Object readResolve() throws ObjectStreamException {
                return new MappingMetaRegistry();
            }
        }
    }
}
