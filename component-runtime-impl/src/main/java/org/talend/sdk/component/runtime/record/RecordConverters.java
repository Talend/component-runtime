/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.talend.sdk.component.api.record.Schema.Type.ARRAY;
import static org.talend.sdk.component.api.record.Schema.Type.BOOLEAN;
import static org.talend.sdk.component.api.record.Schema.Type.BYTES;
import static org.talend.sdk.component.api.record.Schema.Type.DATETIME;
import static org.talend.sdk.component.api.record.Schema.Type.DOUBLE;
import static org.talend.sdk.component.api.record.Schema.Type.FLOAT;
import static org.talend.sdk.component.api.record.Schema.Type.INT;
import static org.talend.sdk.component.api.record.Schema.Type.LONG;
import static org.talend.sdk.component.api.record.Schema.Type.RECORD;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
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

    private static final ZoneId UTC = ZoneId.of("UTC");

    public <T> T mapNumber(final Class<T> expected, final Number from) {
        if (expected == Double.class || expected == double.class) {
            return expected.cast(from.doubleValue());
        }
        if (expected == Float.class || expected == float.class) {
            return expected.cast(from.floatValue());
        }
        if (expected == Integer.class || expected == int.class) {
            return expected.cast(from.intValue());
        }
        if (expected == Long.class || expected == long.class) {
            return expected.cast(from.longValue());
        }
        throw new IllegalArgumentException("Can't convert " + from + " to " + expected);
    }

    public <T> Record toRecord(final MappingMetaRegistry registry, final T data, final Supplier<Jsonb> jsonbProvider,
            final Supplier<RecordBuilderFactory> recordBuilderProvider) {
        if (data == null) {
            return null;
        }
        if (Record.class.isInstance(data)) {
            return Record.class.cast(data);
        }
        if (JsonObject.class.isInstance(data)) {
            return json2Record(recordBuilderProvider.get(), JsonObject.class.cast(data));
        }

        final MappingMeta meta = registry.find(data.getClass(), recordBuilderProvider);
        if (meta.isLinearMapping()) {
            return meta.newRecord(data, recordBuilderProvider.get());
        }

        final Jsonb jsonb = jsonbProvider.get();
        if (!String.class.isInstance(data) && !data.getClass().isPrimitive()
                && PojoJsonbProvider.class.isInstance(jsonb)) {
            final Jsonb pojoMapper = PojoJsonbProvider.class.cast(jsonb).get();
            final OutputRecordHolder holder = new OutputRecordHolder();
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
                                .withLabel(key)
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
                                .withLabel(key)
                                .withType(Schema.Type.RECORD)
                                .withElementSchema(record.getSchema())
                                .build(), record);
                break;
            }
            case TRUE:
            case FALSE:
                builder.withBoolean(key, JsonValue.TRUE.equals(value));
                break;
            case STRING:
                builder.withString(key, JsonString.class.cast(value).getString());
                break;
            case NUMBER:
                final JsonNumber number = JsonNumber.class.cast(value);
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
                final List<Schema.Entry> entries1 = s1.getEntries();
                final List<Schema.Entry> entries2 = s2.getEntries();
                final Set<String> names1 = entries1.stream().map(Schema.Entry::getName).collect(toSet());
                final Set<String> names2 = entries2.stream().map(Schema.Entry::getName).collect(toSet());
                if (!names1.equals(names2)) {
                    // here we are not good since values will not be right anymore,
                    // forbidden for current version anyway but potentially supported later
                    final Schema.Builder builder = factory.newSchemaBuilder(Schema.Type.RECORD);
                    entries1.forEach(builder::withEntry);
                    names2.removeAll(names1);
                    entries2.stream().filter(it -> names2.contains(it.getName())).forEach(builder::withEntry);
                    return builder.build();
                }
                return s1;
            });
        default:
            return firstSchema;
        }
    }

    private Object mapJson(final RecordBuilderFactory factory, final JsonValue it) {
        if (JsonObject.class.isInstance(it)) {
            return json2Record(factory, JsonObject.class.cast(it));
        }
        if (JsonArray.class.isInstance(it)) {
            return JsonArray.class.cast(it).stream().map(i -> mapJson(factory, i)).collect(toList());
        }
        if (JsonString.class.isInstance(it)) {
            return JsonString.class.cast(it).getString();
        }
        if (JsonNumber.class.isInstance(it)) {
            return JsonNumber.class.cast(it).numberValue();
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

    private Schema toSchema(final RecordBuilderFactory factory, final Object next) {
        if (String.class.isInstance(next) || JsonString.class.isInstance(next)) {
            return factory.newSchemaBuilder(Schema.Type.STRING).build();
        }
        if (Integer.class.isInstance(next)) {
            return factory.newSchemaBuilder(Schema.Type.INT).build();
        }
        if (Long.class.isInstance(next) || JsonLongImpl.class.isInstance(next)) {
            return factory.newSchemaBuilder(Schema.Type.LONG).build();
        }
        if (Float.class.isInstance(next)) {
            return factory.newSchemaBuilder(Schema.Type.FLOAT).build();
        }
        if (BigDecimal.class.isInstance(next) || JsonNumber.class.isInstance(next)) {
            return factory.newSchemaBuilder(Schema.Type.DOUBLE).build();
        }
        if (Double.class.isInstance(next) || JsonNumber.class.isInstance(next)) {
            return factory.newSchemaBuilder(Schema.Type.DOUBLE).build();
        }
        if (Boolean.class.isInstance(next) || JsonValue.TRUE.equals(next) || JsonValue.FALSE.equals(next)) {
            return factory.newSchemaBuilder(Schema.Type.BOOLEAN).build();
        }
        if (Date.class.isInstance(next) || ZonedDateTime.class.isInstance(next)) {
            return factory.newSchemaBuilder(Schema.Type.DATETIME).build();
        }
        if (byte[].class.isInstance(next)) {
            return factory.newSchemaBuilder(Schema.Type.BYTES).build();
        }
        if (Collection.class.isInstance(next) || JsonArray.class.isInstance(next)) {
            final Collection collection = Collection.class.cast(next);
            if (collection.isEmpty()) {
                return factory.newSchemaBuilder(Schema.Type.STRING).build();
            }
            return factory
                    .newSchemaBuilder(Schema.Type.ARRAY)
                    .withElementSchema(toSchema(factory, collection.iterator().next()))
                    .build();
        }
        if (Record.class.isInstance(next)) {
            return Record.class.cast(next).getSchema();
        }
        throw new IllegalArgumentException("unsupported type for " + next);
    }

    public Object toType(final MappingMetaRegistry registry, final Object data, final Class<?> parameterType,
            final Supplier<JsonBuilderFactory> factorySupplier, final Supplier<JsonProvider> providerSupplier,
            final Supplier<Jsonb> jsonbProvider, final Supplier<RecordBuilderFactory> recordBuilderProvider) {
        if (parameterType.isInstance(data)) {
            return data;
        }

        final JsonObject inputAsJson;
        if (JsonObject.class.isInstance(data)) {
            if (JsonObject.class == parameterType) {
                return data;
            }
            inputAsJson = JsonObject.class.cast(data);
        } else if (Record.class.isInstance(data)) {
            final Record record = Record.class.cast(data);
            if (!JsonObject.class.isAssignableFrom(parameterType)) {
                final MappingMeta mappingMeta = registry.find(parameterType, recordBuilderProvider);
                if (mappingMeta.isLinearMapping()) {
                    return mappingMeta.newInstance(record);
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
                    if (String.class.isInstance(item)) {
                        final JsonProvider jsonProvider = providerSupplier.get();
                        builder
                                .add(name, toArray(factory, v -> jsonProvider.createValue(String.class.cast(v)),
                                        collection));
                    } else if (Double.class.isInstance(item)) {
                        final JsonProvider jsonProvider = providerSupplier.get();
                        builder
                                .add(name, toArray(factory, v -> jsonProvider.createValue(Double.class.cast(v)),
                                        collection));
                    } else if (Float.class.isInstance(item)) {
                        final JsonProvider jsonProvider = providerSupplier.get();
                        builder
                                .add(name, toArray(factory, v -> jsonProvider.createValue(Float.class.cast(v)),
                                        collection));
                    } else if (Double.class.isInstance(item)) {
                        final JsonProvider jsonProvider = providerSupplier.get();
                        builder
                                .add(name, toArray(factory, v -> jsonProvider.createValue(Double.class.cast(v)),
                                        collection));
                    } else if (Integer.class.isInstance(item)) {
                        final JsonProvider jsonProvider = providerSupplier.get();
                        builder
                                .add(name, toArray(factory, v -> jsonProvider.createValue(Integer.class.cast(v)),
                                        collection));
                    } else if (Long.class.isInstance(item)) {
                        final JsonProvider jsonProvider = providerSupplier.get();
                        builder
                                .add(name, toArray(factory, v -> jsonProvider.createValue(Long.class.cast(v)),
                                        collection));
                    } else if (Boolean.class.isInstance(item)) {
                        builder
                                .add(name, toArray(factory,
                                        v -> Boolean.class.cast(v) ? JsonValue.TRUE : JsonValue.FALSE, collection));
                    } else if (ZonedDateTime.class.isInstance(item)) {
                        final JsonProvider jsonProvider = providerSupplier.get();
                        builder
                                .add(name,
                                        toArray(factory, v -> jsonProvider
                                                .createValue(ZonedDateTime.class.cast(v).toInstant().toEpochMilli()),
                                                collection));
                    } else if (Date.class.isInstance(item)) {
                        final JsonProvider jsonProvider = providerSupplier.get();
                        builder
                                .add(name, toArray(factory, v -> jsonProvider.createValue(Date.class.cast(v).getTime()),
                                        collection));
                    } else if (Record.class.isInstance(item)) {
                        builder
                                .add(name, toArray(factory,
                                        v -> buildRecord(factory, providerSupplier, Record.class.cast(v)).build(),
                                        collection));
                    } else if (JsonValue.class.isInstance(item)) {
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

        // datetime cases
        if (Long.class.isInstance(value) && expectedType != Long.class) {
            if (expectedType == ZonedDateTime.class) {
                final long epochMilli = Number.class.cast(value).longValue();
                if (epochMilli == -1L) { // not <0 which can be a bug
                    return null;
                }
                return expectedType.cast(ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), UTC));
            }
            if (expectedType == Date.class) {
                return expectedType.cast(new Date(Number.class.cast(value).longValue()));
            }
        }

        if (!expectedType.isInstance(value)) {
            if (Number.class.isInstance(value) && Number.class.isAssignableFrom(expectedType)) {
                return mapNumber(expectedType, Number.class.cast(value));
            }
            if (String.class.isInstance(value)) {
                if (ZonedDateTime.class == expectedType) {
                    return expectedType.cast(ZonedDateTime.parse(String.valueOf(value)));
                }
                if (byte[].class == expectedType) {
                    return expectedType.cast(Base64.getDecoder().decode(String.valueOf(value)));
                }
            }

            throw new IllegalArgumentException(name + " can't be converted to " + expectedType);
        }

        return expectedType.cast(value);
    }

    @Data
    public static class MappingMeta {

        private final boolean linearMapping;

        private final Constructor<?> constructor;

        private final Collection<BiConsumer<Object, Record>> instanceProvisionners;

        private final Schema recordSchema;

        private final Collection<BiConsumer<Record.Builder, Object>> recordProvisionners;

        private MappingMeta(final Class<?> type, final MappingMetaRegistry registry,
                final Supplier<RecordBuilderFactory> factory) {
            linearMapping = Stream.of(type.getInterfaces()).anyMatch(it -> it.getName().startsWith("routines.system."));
            if (!linearMapping) {
                instanceProvisionners = null;
                recordProvisionners = null;
                recordSchema = null;
                constructor = null;
            } else {
                final RecordBuilderFactory builderFactory = factory.get();
                final Schema.Builder schemaBuilder = builderFactory.newSchemaBuilder(RECORD);

                final Field[] fields = type.getFields();
                instanceProvisionners = new ArrayList<>(fields.length);
                recordProvisionners = new ArrayList<>(fields.length);
                Stream.of(fields).filter(field -> !Modifier.isStatic(field.getModifiers())).forEach(field -> {
                    final String name = field.getName();
                    final Class<?> fieldType = field.getType();
                    if (fieldType == String.class) {
                        instanceProvisionners
                                .add((instance, record) -> setField(instance, field,
                                        record.getOptionalString(name).orElse(null)));

                        final Schema.Entry entry = newEntry(builderFactory, name, true, STRING);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, String.class))
                                        .ifPresent(value -> builder.withString(entry, value)));
                    } else if (fieldType == int.class) {
                        instanceProvisionners
                                .add((instance, record) -> setField(instance, field,
                                        record.getOptionalInt(name).orElse(0)));

                        final Schema.Entry entry = newEntry(builderFactory, name, false, INT);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Integer.class))
                                        .ifPresent(value -> builder.withInt(entry, value)));
                    } else if (fieldType == Integer.class) {
                        instanceProvisionners.add((instance, record) -> {
                            final OptionalInt value = record.getOptionalInt(name);
                            setField(instance, field, value.isPresent() ? value.getAsInt() : null);
                        });

                        final Schema.Entry entry = newEntry(builderFactory, name, true, INT);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Integer.class))
                                        .ifPresent(value -> builder.withInt(entry, value)));
                    } else if (fieldType == long.class) {
                        instanceProvisionners
                                .add((instance, record) -> setField(instance, field,
                                        record.getOptionalLong(name).orElse(0L)));

                        final Schema.Entry entry = newEntry(builderFactory, name, false, LONG);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Long.class))
                                        .ifPresent(value -> builder.withLong(entry, value)));
                    } else if (fieldType == Long.class) {
                        instanceProvisionners.add((instance, record) -> {
                            final OptionalLong value = record.getOptionalLong(name);
                            setField(instance, field, value.isPresent() ? value.getAsLong() : null);
                        });

                        final Schema.Entry entry = newEntry(builderFactory, name, true, LONG);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Long.class))
                                        .ifPresent(value -> builder.withLong(entry, value)));
                    } else if (fieldType == float.class) {
                        instanceProvisionners
                                .add((instance, record) -> setField(instance, field,
                                        record.getOptionalFloat(name).orElse(0f)));

                        final Schema.Entry entry = newEntry(builderFactory, name, false, FLOAT);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Float.class))
                                        .ifPresent(value -> builder.withFloat(entry, value)));
                    } else if (fieldType == Float.class) {
                        instanceProvisionners.add((instance, record) -> {
                            final OptionalDouble value = record.getOptionalFloat(name);
                            setField(instance, field, value.isPresent() ? (float) value.getAsDouble() : null);
                        });

                        final Schema.Entry entry = newEntry(builderFactory, name, true, FLOAT);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Float.class))
                                        .ifPresent(value -> builder.withFloat(entry, value)));
                    } else if (fieldType == short.class) {
                        instanceProvisionners
                                .add((instance, record) -> setField(instance, field,
                                        (short) record.getOptionalInt(name).orElse(0)));

                        final Schema.Entry entry = newEntry(builderFactory, name, false, INT);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Short.class))
                                        .ifPresent(value -> builder.withInt(entry, value)));
                    } else if (fieldType == Short.class) {
                        instanceProvisionners.add((instance, record) -> {
                            final OptionalInt value = record.getOptionalInt(name);
                            setField(instance, field, value.isPresent() ? (short) value.getAsInt() : null);
                        });

                        final Schema.Entry entry = newEntry(builderFactory, name, true, INT);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Short.class))
                                        .ifPresent(value -> builder.withInt(entry, value)));
                    } else if (fieldType == byte.class) {
                        instanceProvisionners
                                .add((instance, record) -> setField(instance, field,
                                        (byte) record.getOptionalInt(name).orElse(0)));

                        final Schema.Entry entry = newEntry(builderFactory, name, false, INT);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Byte.class))
                                        .ifPresent(value -> builder.withInt(entry, value)));
                    } else if (fieldType == Byte.class) {
                        instanceProvisionners.add((instance, record) -> {
                            final OptionalInt value = record.getOptionalInt(name);
                            setField(instance, field, value.isPresent() ? (byte) value.getAsInt() : null);
                        });

                        final Schema.Entry entry = newEntry(builderFactory, name, true, INT);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Byte.class))
                                        .ifPresent(value -> builder.withInt(entry, value)));
                    } else if (fieldType == double.class) {
                        instanceProvisionners
                                .add((instance, record) -> setField(instance, field,
                                        record.getOptionalDouble(name).orElse(0.)));

                        final Schema.Entry entry = newEntry(builderFactory, name, false, DOUBLE);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Double.class))
                                        .ifPresent(value -> builder.withDouble(entry, value)));
                    } else if (fieldType == Double.class) {
                        instanceProvisionners.add((instance, record) -> {
                            final OptionalDouble value = record.getOptionalDouble(name);
                            setField(instance, field, value.isPresent() ? value.getAsDouble() : null);
                        });

                        final Schema.Entry entry = newEntry(builderFactory, name, true, DOUBLE);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Double.class))
                                        .ifPresent(value -> builder.withDouble(entry, value)));
                    } else if (fieldType == BigDecimal.class) {
                        handleBigDecimal(builderFactory, schemaBuilder, field, name);
                    } else if (fieldType == byte[].class) {
                        instanceProvisionners
                                .add((instance, record) -> setField(instance, field,
                                        record.getOptionalBytes(name).orElse(null)));

                        final Schema.Entry entry = newEntry(builderFactory, name, true, BYTES);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, byte[].class))
                                        .ifPresent(value -> builder.withBytes(entry, value)));
                    } else if (fieldType == boolean.class) {
                        instanceProvisionners
                                .add((instance, record) -> setField(instance, field,
                                        record.getOptionalBoolean(name).orElse(false)));

                        final Schema.Entry entry = newEntry(builderFactory, name, false, BOOLEAN);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Boolean.class))
                                        .ifPresent(value -> builder.withBoolean(entry, value)));
                    } else if (fieldType == Boolean.class) {
                        instanceProvisionners
                                .add((instance, record) -> setField(instance, field,
                                        record.getOptionalBoolean(name).orElse(null)));

                        final Schema.Entry entry = newEntry(builderFactory, name, true, BOOLEAN);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Boolean.class))
                                        .ifPresent(value -> builder.withBoolean(entry, value)));
                    } else if (fieldType == Character.class) {
                        instanceProvisionners
                                .add((instance, record) -> setField(instance, field,
                                        record
                                                .getOptionalString(name)
                                                .map(s -> s.isEmpty() ? null : s.charAt(0))
                                                .orElse(null)));

                        final Schema.Entry entry = newEntry(builderFactory, name, true, STRING);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Character.class))
                                        .ifPresent(value -> builder.withString(entry, Character.toString(value))));
                    } else if (fieldType.isArray()) {
                        handleArray(registry, factory, builderFactory, schemaBuilder, field, name, fieldType);
                    } else if (Set.class.isAssignableFrom(fieldType)) {
                        handleSet(registry, factory, builderFactory, schemaBuilder, field, name, fieldType);
                    } else if (Collection.class.isAssignableFrom(fieldType)) {
                        handleCollection(registry, factory, builderFactory, schemaBuilder, field, name, fieldType);
                    } else if (Date.class.isAssignableFrom(fieldType)) {
                        instanceProvisionners
                                .add((instance, record) -> setField(instance, field,
                                        record
                                                .getOptionalDateTime(name)
                                                .map(dt -> new Date(dt.toInstant().toEpochMilli()))
                                                .orElse(null)));

                        final Schema.Entry entry = newEntry(builderFactory, name, true, DATETIME);
                        schemaBuilder.withEntry(entry);
                        recordProvisionners
                                .add((builder, instance) -> ofNullable(getField(instance, field, Date.class))
                                        .ifPresent(value -> builder.withDateTime(entry, value)));
                    } else {
                        final MappingMeta mappingMeta = registry.find(fieldType, factory);
                        if (mappingMeta.linearMapping) {
                            instanceProvisionners
                                    .add((instance, record) -> setField(instance, field,
                                            record.getOptionalRecord(name).map(mappingMeta::newInstance).orElse(null)));

                            final Schema.Entry entry = builderFactory
                                    .newEntryBuilder()
                                    .withNullable(true)
                                    .withName(name)
                                    .withLabel(name)
                                    .withType(RECORD)
                                    .withElementSchema(mappingMeta.recordSchema)
                                    .build();
                            schemaBuilder.withEntry(entry);
                            recordProvisionners
                                    .add((builder, instance) -> ofNullable(getField(instance, field, Date.class))
                                            .ifPresent(value -> builder
                                                    .withRecord(entry, mappingMeta.newRecord(value, builderFactory))));
                        }
                    }
                });

                recordSchema = schemaBuilder.build();

                try {
                    constructor = type.getConstructor();
                } catch (final NoSuchMethodException e) {
                    throw new IllegalStateException("No constructor for " + type.getName(), e);
                }
            }
        }

        private void handleCollection(final MappingMetaRegistry registry, final Supplier<RecordBuilderFactory> factory,
                final RecordBuilderFactory builderFactory, final Schema.Builder schemaBuilder, final Field field,
                final String name, final Class<?> fieldType) {
            instanceProvisionners
                    .add((instance, record) -> setField(instance, field,
                            record.getOptionalArray(fieldType.getComponentType(), name).orElse(null)));

            final Class<?> elementType = findCollectionType(field);
            final Schema.Entry entry = builderFactory
                    .newEntryBuilder()
                    .withNullable(true)
                    .withName(name)
                    .withLabel(name)
                    .withType(ARRAY)
                    .withElementSchema(registry.find(elementType, factory).recordSchema)
                    .build();
            schemaBuilder.withEntry(entry);
            recordProvisionners
                    .add((builder, instance) -> ofNullable(getField(instance, field, Collection.class))
                            .ifPresent(value -> builder.withArray(entry, value)));
        }

        private void handleSet(final MappingMetaRegistry registry, final Supplier<RecordBuilderFactory> factory,
                final RecordBuilderFactory builderFactory, final Schema.Builder schemaBuilder, final Field field,
                final String name, final Class<?> fieldType) {
            instanceProvisionners
                    .add((instance, record) -> setField(instance, field,
                            record
                                    .getOptionalArray(fieldType.getComponentType(), name)
                                    .map(LinkedHashSet::new)
                                    .orElse(null)));

            final Class<?> elementType = findCollectionType(field);
            final Schema.Entry entry = builderFactory
                    .newEntryBuilder()
                    .withNullable(true)
                    .withName(name)
                    .withLabel(name)
                    .withType(ARRAY)
                    .withElementSchema(registry.find(elementType, factory).recordSchema)
                    .build();
            schemaBuilder.withEntry(entry);
            recordProvisionners
                    .add((builder, instance) -> ofNullable(getField(instance, field, Collection.class))
                            .ifPresent(value -> builder.withArray(entry, value)));
        }

        private void handleArray(final MappingMetaRegistry registry, final Supplier<RecordBuilderFactory> factory,
                final RecordBuilderFactory builderFactory, final Schema.Builder schemaBuilder, final Field field,
                final String name, final Class<?> fieldType) {
            instanceProvisionners
                    .add((instance, record) -> setField(instance, field,
                            record
                                    .getOptionalArray(fieldType.getComponentType(), name)
                                    .map(Collection::toArray)
                                    .orElse(null)));

            final Schema.Entry entry = builderFactory
                    .newEntryBuilder()
                    .withNullable(true)
                    .withName(name)
                    .withLabel(name)
                    .withType(ARRAY)
                    .withElementSchema(registry.find(fieldType.getComponentType(), factory).recordSchema)
                    .build();
            schemaBuilder.withEntry(entry);
            recordProvisionners
                    .add((builder, instance) -> ofNullable(getField(instance, field, Object[].class)) // todo:
                            // check
                            // this
                            // cast
                            .map(Arrays::asList)
                            .ifPresent(value -> builder.withArray(entry, value)));
        }

        private void handleBigDecimal(final RecordBuilderFactory builderFactory, final Schema.Builder schemaBuilder,
                final Field field, final String name) {
            final Map<Schema, Schema.Type> typeCache = new ConcurrentHashMap<>();
            instanceProvisionners.add((instance, record) -> {
                final Schema.Type st = typeCache
                        .computeIfAbsent(record.getSchema(),
                                s -> s
                                        .getEntries()
                                        .stream()
                                        .filter(e -> name.equals(e.getName()))
                                        .findFirst()
                                        .orElseThrow(IllegalStateException::new)
                                        .getType());
                switch (st) {
                case DOUBLE:
                    final OptionalDouble value = record.getOptionalDouble(name);
                    if (value.isPresent()) {
                        setField(instance, field, BigDecimal.valueOf(value.orElseThrow(IllegalStateException::new)));
                    }
                    break;
                case STRING:
                    record.getOptionalString(name).ifPresent(str -> setField(instance, field, new BigDecimal(str)));
                    break;
                default:
                }
            });

            final Schema.Entry entry = newEntry(builderFactory, name, true, STRING);
            schemaBuilder.withEntry(entry);
            recordProvisionners
                    .add((builder, instance) -> ofNullable(getField(instance, field, BigDecimal.class))
                            .ifPresent(value -> builder.withString(entry, value.toString())));
        }

        private Schema.Entry newEntry(final RecordBuilderFactory builderFactory, final String name,
                final boolean nullable, final Schema.Type type) {
            return builderFactory
                    .newEntryBuilder()
                    .withNullable(nullable)
                    .withName(name)
                    .withLabel(name)
                    .withType(type)
                    .build();
        }

        private Class<?> findCollectionType(final Field field) {
            // todo: make it better
            final Class<?> elementType;
            if (ParameterizedType.class.isInstance(field.getGenericType())) {
                final ParameterizedType parameterizedType = ParameterizedType.class.cast(field.getGenericType());
                if (parameterizedType.getActualTypeArguments().length == 1
                        && Class.class.isInstance(parameterizedType.getActualTypeArguments()[0])) {
                    elementType = Class.class.cast(parameterizedType.getActualTypeArguments()[0]);
                } else {
                    elementType = Object.class;
                }
            } else {
                elementType = Object.class;
            }
            return elementType;
        }

        Object newInstance(final Record record) {
            try {
                final Object instance = constructor.newInstance();
                instanceProvisionners.forEach(consumer -> consumer.accept(instance, record));
                return instance;
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getTargetException());
            }
        }

        <T> Record newRecord(final T data, final RecordBuilderFactory factory) {
            final Record.Builder builder = factory.newRecordBuilder(recordSchema);
            recordProvisionners.forEach(consumer -> consumer.accept(builder, data));
            return builder.build();
        }

        private <T> T getField(final Object instance, final Field field, final Class<T> type) {
            try {
                return type.cast(field.get(instance));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        private void setField(final Object instance, final Field field, final Object value) {
            try {
                field.set(instance, value);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Data
    public static class MappingMetaRegistry implements Serializable {

        private final Map<Class<?>, MappingMeta> registry = new ConcurrentHashMap<>();

        private Object writeReplace() throws ObjectStreamException {
            return new Factory(); // don't serialize the mapping, recalculate it lazily
        }

        public MappingMeta find(final Class<?> parameterType, final Supplier<RecordBuilderFactory> factorySupplier) {
            final MappingMeta meta = registry.get(parameterType);
            if (meta != null) {
                return meta;
            }
            final MappingMeta mappingMeta = new MappingMeta(parameterType, this, factorySupplier);
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
