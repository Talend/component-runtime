/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.record;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

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
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

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

    public <T> Record toRecord(final T data, final Supplier<Jsonb> jsonbProvider,
            final Supplier<RecordBuilderFactory> recordBuilderProvider) {
        if (Record.class.isInstance(data)) {
            return Record.class.cast(data);
        }
        if (JsonObject.class.isInstance(data)) {
            return json2Record(recordBuilderProvider.get(), JsonObject.class.cast(data));
        }
        final Jsonb jsonb = jsonbProvider.get();
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

    public Object toType(final Object data, final Class<?> parameterType,
            final Supplier<JsonBuilderFactory> factorySupplier, final Supplier<JsonProvider> providerSupplier,
            final Supplier<Jsonb> jsonbProvider) {
        if (parameterType.isInstance(data)) {
            return data;
        }

        final Jsonb jsonb = jsonbProvider.get();
        final String inputAsJson;
        if (JsonObject.class.isInstance(data)) {
            if (JsonObject.class == parameterType) {
                return data;
            }
            inputAsJson = JsonObject.class.cast(data).toString();
        } else if (Record.class.isInstance(data)) {
            final JsonObject asJson = toJson(factorySupplier, providerSupplier, Record.class.cast(data));
            if (JsonObject.class == parameterType) {
                return asJson;
            }
            inputAsJson = asJson.toString();
        } else {
            inputAsJson = jsonb.toJson(data);
        }
        if (parameterType == JsonObject.class) {
            return jsonb.fromJson(inputAsJson, parameterType);
        }
        return jsonb.fromJson(inputAsJson, parameterType);
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
}
