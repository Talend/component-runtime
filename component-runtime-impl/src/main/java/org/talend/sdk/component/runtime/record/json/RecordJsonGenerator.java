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
package org.talend.sdk.component.runtime.record.json;

import static java.util.stream.Collectors.toList;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.apache.johnzon.core.JsonProviderImpl;
import org.apache.johnzon.mapper.MapperBuilder;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.RecordConverters;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecordJsonGenerator implements JsonGenerator {

    private final RecordBuilderFactory factory;

    private final OutputRecordHolder holder;

    private final LinkedList<Object> builders = new LinkedList<>();

    private Record.Builder objectBuilder;

    private Collection<Object> arrayBuilder;

    private final RecordConverters recordConverters = new RecordConverters();

    private final RecordConverters.MappingMetaRegistry mappingRegistry = new RecordConverters.MappingMetaRegistry();

    @Override
    public JsonGenerator writeStartObject() {
        objectBuilder = factory.newRecordBuilder();
        builders.add(objectBuilder);
        arrayBuilder = null;
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(final String name) {
        objectBuilder = factory.newRecordBuilder();
        builders.add(new NamedBuilder<>(objectBuilder, name));
        arrayBuilder = null;
        return this;
    }

    @Override
    public JsonGenerator writeStartArray() {
        arrayBuilder = new ArrayList<>();
        builders.add(arrayBuilder);
        objectBuilder = null;
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(final String name) {
        arrayBuilder = new ArrayList<>();
        builders.add(new NamedBuilder<>(arrayBuilder, name));
        objectBuilder = null;
        return this;
    }

    @Override
    public JsonGenerator writeKey(final String name) {
        throw new UnsupportedOperationException();
    }

    private Schema.Builder buildSchemaFromJsonObject(final JsonObject json) {
        Schema.Builder builder = factory.newSchemaBuilder(Type.RECORD);
        json.entrySet().stream().forEach(entry -> {
            String k = entry.getKey();
            JsonValue v = entry.getValue();
            builder.withEntry(factory.newEntryBuilder().withName(k).withType(findType(v.getClass())).build());
        });
        return builder;
    }

    @Override
    public JsonGenerator write(final String name, final JsonValue value) {
        switch (value.getValueType()) {
        case ARRAY:
            List values = JsonArray.class.cast(value).stream().collect(toList());
            objectBuilder.withArray(createEntryForJsonArray(name, values), values);
            break;
        case OBJECT:
            Record r = recordConverters.toRecord(mappingRegistry, value, () -> getJsonb(createJsonb()), () -> factory);
            objectBuilder.withRecord(name, r);
            break;
        case STRING:
            objectBuilder.withString(name, JsonString.class.cast(value).getString());
            break;
        case NUMBER:
            objectBuilder.withDouble(name, JsonNumber.class.cast(value).numberValue().doubleValue());
            break;
        case TRUE:
            objectBuilder.withBoolean(name, true);
            break;
        case FALSE:
            objectBuilder.withBoolean(name, false);
            break;
        case NULL:
            break;
        default:
            throw new IllegalStateException("Unexpected value: " + value.getValueType());
        }
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final String value) {
        objectBuilder.withString(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final BigInteger value) {
        objectBuilder.withLong(name, value.longValue());
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final BigDecimal value) {
        objectBuilder.withDouble(name, value.doubleValue());
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final int value) {
        objectBuilder.withInt(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final long value) {
        objectBuilder.withLong(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final double value) {
        objectBuilder.withDouble(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final boolean value) {
        objectBuilder.withBoolean(name, value);
        return this;
    }

    @Override
    public JsonGenerator writeNull(final String name) {
        // skipped
        return this;
    }

    @Override
    public JsonGenerator write(final JsonValue value) {
        switch (value.getValueType()) {
        case ARRAY:
            arrayBuilder.add(JsonArray.class.cast(value).stream().collect(toList()));
            break;
        case OBJECT:
            Record r = recordConverters.toRecord(mappingRegistry, value, () -> getJsonb(createJsonb()), () -> factory);
            arrayBuilder.add(factory.newRecordBuilder(r.getSchema(), r));
            break;
        case STRING:
            arrayBuilder.add(JsonString.class.cast(value).getString());
            break;
        case NUMBER:
            arrayBuilder.add(JsonNumber.class.cast(value).numberValue().doubleValue());
            break;
        case TRUE:
            arrayBuilder.add(true);
            break;
        case FALSE:
            arrayBuilder.add(false);
            break;
        case NULL:
            break;
        default:
            throw new IllegalStateException("Unexpected value: " + value.getValueType());
        }
        return this;
    }

    @Override
    public JsonGenerator write(final String value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator write(final BigDecimal value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator write(final BigInteger value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator write(final int value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator write(final long value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator write(final double value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator write(final boolean value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator writeEnd() {
        if (builders.size() == 1) {
            return this;
        }

        final Object last = builders.removeLast();

        /*
         * Previous potential cases:
         * 1. json array -> we add the builder directly
         * 2. NamedBuilder{array|object} -> we add the builder in the previous object
         */

        final String name;
        Object previous = builders.getLast();
        if (NamedBuilder.class.isInstance(previous)) {
            final NamedBuilder namedBuilder = NamedBuilder.class.cast(previous);
            name = namedBuilder.name;
            previous = namedBuilder.builder;
        } else {
            name = null;
        }

        if (List.class.isInstance(last)) {
            final List array = List.class.cast(last);
            if (Collection.class.isInstance(previous)) {
                arrayBuilder = Collection.class.cast(previous);
                objectBuilder = null;
                arrayBuilder.add(array);
            } else if (Record.Builder.class.isInstance(previous)) {
                objectBuilder = Record.Builder.class.cast(previous);
                arrayBuilder = null;
                objectBuilder.withArray(createEntryBuilderForArray(name, array).build(), prepareArray(array));
            } else {
                throw new IllegalArgumentException("Unsupported previous builder: " + previous);
            }
        } else if (Record.Builder.class.isInstance(last)) {
            final Record.Builder object = Record.Builder.class.cast(last);
            if (Collection.class.isInstance(previous)) {
                arrayBuilder = Collection.class.cast(previous);
                objectBuilder = null;
                arrayBuilder.add(object);
            } else if (Record.Builder.class.isInstance(previous)) {
                objectBuilder = Record.Builder.class.cast(previous);
                arrayBuilder = null;
                objectBuilder.withRecord(name, objectBuilder.build());
            } else {
                throw new IllegalArgumentException("Unsupported previous builder: " + previous);
            }
        } else if (NamedBuilder.class.isInstance(last)) {
            final NamedBuilder<?> namedBuilder = NamedBuilder.class.cast(last);
            if (Record.Builder.class.isInstance(previous)) {
                objectBuilder = Record.Builder.class.cast(previous);
                if (List.class.isInstance(namedBuilder.builder)) {
                    final List array = List.class.cast(namedBuilder.builder);
                    objectBuilder
                            .withArray(createEntryBuilderForArray(namedBuilder.name, array).build(),
                                    prepareArray(array));
                    arrayBuilder = null;
                } else if (Record.Builder.class.isInstance(namedBuilder.builder)) {
                    objectBuilder
                            .withRecord(namedBuilder.name, Record.Builder.class.cast(namedBuilder.builder).build());
                    arrayBuilder = null;
                } else {
                    throw new IllegalArgumentException("Unsupported previous builder: " + previous);
                }
            } else {
                throw new IllegalArgumentException(
                        "Unsupported previous builder, expected object builder: " + previous);
            }
        } else {
            throw new IllegalArgumentException("Unsupported previous builder: " + previous);
        }
        return this;
    }

    private List prepareArray(final List array) {
        return ((Collection<?>) array)
                .stream()
                .map(it -> Record.Builder.class.isInstance(it) ? Record.Builder.class.cast(it).build() : it)
                .collect(toList());
    }

    private Schema.Entry createEntryForJsonArray(final String name, final List array) {
        final Schema.Type type = findType(array);
        final Schema.Entry.Builder builder = factory.newEntryBuilder().withName(name).withType(Schema.Type.ARRAY);
        if (type == Schema.Type.RECORD) {
            final JsonObject first = JsonObject.class.cast(array.iterator().next());
            Schema schema = buildSchemaFromJsonObject(first).build();
            builder.withElementSchema(schema);
        } else {
            builder.withElementSchema(factory.newSchemaBuilder(type).build());
        }
        return builder.build();
    }

    private Schema.Entry.Builder createEntryBuilderForArray(final String name, final List array) {
        final Schema.Type type = findType(array);
        final Schema.Entry.Builder builder = factory.newEntryBuilder().withName(name).withType(Schema.Type.ARRAY);
        if (type == Schema.Type.RECORD) {
            final Record first = Record.Builder.class.cast(array.iterator().next()).build();
            array.set(0, factory.newRecordBuilder(first.getSchema(), first)); // copy since build() resetted it
            builder.withElementSchema(first.getSchema());
        } else {
            builder.withElementSchema(factory.newSchemaBuilder(type).build());
        }
        return builder;
    }

    private Schema.Type findType(final Collection<?> array) {
        if (array.isEmpty()) {
            return Schema.Type.STRING;
        }
        final Class<?> clazz = array.stream().filter(Objects::nonNull).findFirst().map(Object::getClass).orElse(null);
        return findType(clazz);
    }

    private Schema.Type findType(final Class<?> clazz) {
        if (clazz == null) {
            return Schema.Type.STRING;
        }
        if (Collection.class.isAssignableFrom(clazz)) {
            return Schema.Type.ARRAY;
        }
        if (CharSequence.class.isAssignableFrom(clazz)) {
            return Schema.Type.STRING;
        }
        if (int.class == clazz || Integer.class == clazz) {
            return Schema.Type.INT;
        }
        if (long.class == clazz || Long.class == clazz) {
            return Schema.Type.LONG;
        }
        if (boolean.class == clazz || Boolean.class == clazz) {
            return Schema.Type.BOOLEAN;
        }
        if (float.class == clazz || Float.class == clazz) {
            return Schema.Type.FLOAT;
        }
        if (double.class == clazz || Double.class == clazz) {
            return Schema.Type.DOUBLE;
        }
        if (byte[].class == clazz) {
            return Schema.Type.BYTES;
        }
        if (ZonedDateTime.class == clazz) {
            return Schema.Type.DATETIME;
        }
        if (JsonArray.class.isAssignableFrom(clazz)) {
            return Schema.Type.ARRAY;
        }
        if (JsonObject.class.isAssignableFrom(clazz)) {
            return Schema.Type.RECORD;
        }
        if (JsonNumber.class.isAssignableFrom(clazz)) {
            return Schema.Type.DOUBLE;
        }
        if (JsonString.class.isAssignableFrom(clazz)) {
            return Schema.Type.STRING;
        }
        if (JsonValue.class.isAssignableFrom(clazz)) {
            return Schema.Type.STRING;
        }

        return Schema.Type.RECORD;
    }

    @Override
    public JsonGenerator writeNull() {
        // skipped
        return this;
    }

    @Override
    public void close() {
        holder.setRecord(Record.Builder.class.cast(builders.getLast()).build());
    }

    @Override
    public void flush() {
        // no-op
    }

    private Jsonb createJsonb() {
        final JsonbBuilder jsonbBuilder = JsonbBuilder.newBuilder().withProvider(new JsonProviderImpl() {

            @Override
            public JsonGeneratorFactory createGeneratorFactory(final Map<String, ?> config) {
                return new RecordJsonGenerator.Factory(() -> factory, config);
            }
        });
        try { // to passthrough the writer, otherwise RecoderJsonGenerator is broken
            final Field mapper = jsonbBuilder.getClass().getDeclaredField("builder");
            if (!mapper.isAccessible()) {
                mapper.setAccessible(true);
            }
            MapperBuilder.class.cast(mapper.get(jsonbBuilder)).setDoCloseOnStreams(true);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        return jsonbBuilder.build();
    }

    private Jsonb getJsonb(final Jsonb jsonb) {
        return Jsonb.class
                .cast(Proxy
                        .newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                new Class<?>[] { Jsonb.class, PojoJsonbProvider.class }, (proxy, method, args) -> {
                                    if (method.getDeclaringClass() == Supplier.class) {
                                        return jsonb;
                                    }
                                    return method.invoke(jsonb, args);
                                }));
    }

    @RequiredArgsConstructor
    public static class Factory implements JsonGeneratorFactory {

        private final Supplier<RecordBuilderFactory> factory;

        private final Map<String, ?> configuration;

        @Override
        public JsonGenerator createGenerator(final Writer writer) {
            if (OutputRecordHolder.class.isInstance(writer)) {
                return new RecordJsonGenerator(factory.get(), OutputRecordHolder.class.cast(writer));
            }
            throw new IllegalArgumentException("Unsupported writer: " + writer);
        }

        @Override
        public JsonGenerator createGenerator(final OutputStream out) {
            return createGenerator(out, StandardCharsets.UTF_8);
        }

        @Override
        public JsonGenerator createGenerator(final OutputStream out, final Charset charset) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, ?> getConfigInUse() {
            return configuration;
        }
    }

    @RequiredArgsConstructor
    private static class NamedBuilder<T> {

        private final T builder;

        private final String name;
    }
}
