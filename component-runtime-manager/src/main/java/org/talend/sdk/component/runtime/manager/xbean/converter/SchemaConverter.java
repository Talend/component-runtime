/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.xbean.converter;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;

import org.apache.xbean.propertyeditor.AbstractConverter;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.manager.service.record.RecordBuilderFactoryProvider;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

public class SchemaConverter extends AbstractConverter {

    public static final String ELEMENT_SCHEMA = "elementSchema";

    public static final String PROPS = "props";

    public static final String TYPE = "type";

    public static final String ENTRIES = "entries";

    private final RecordBuilderFactory factory;

    public SchemaConverter() {
        super(Schema.class);
        try {
            final RecordBuilderFactoryProvider recordBuilderFactoryProvider;
            final Iterator<RecordBuilderFactoryProvider> spiIterator =
                    ServiceLoader.load(RecordBuilderFactoryProvider.class).iterator();
            if (spiIterator.hasNext()) {
                final RecordBuilderFactoryProvider spi = spiIterator.next();
                recordBuilderFactoryProvider = spi;
            } else {
                recordBuilderFactoryProvider = RecordBuilderFactoryImpl::new;
            }
            factory = recordBuilderFactoryProvider.apply("null");
        } catch (RuntimeException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object toObjectImpl(final String s) {
        if (!s.isEmpty()) {
            final JsonObject json = JsonProvider.provider().createReader(new StringReader(s)).readObject();
            return toSchema(json);
        }
        return null;
    }

    private Schema toSchema(final JsonObject json) {
        if (json == null || json.isNull(TYPE)) {
            return null;
        }
        final String type = json.getString(TYPE);
        final Schema.Type schemaType = Enum.valueOf(Schema.Type.class, type);
        final Schema.Builder builder = factory.newSchemaBuilder(schemaType);

        if (schemaType == Schema.Type.RECORD) {
            this.addEntries(builder, json.getJsonArray(ENTRIES));
            this.addEntries(builder, json.getJsonArray("metadatas"));
        } else if (schemaType == Schema.Type.ARRAY) {
            this.treatElementSchema(json, builder::withElementSchema);
        }
        this.addProps(builder::withProp, json);

        final JsonValue orderValue = json.get("order");
        if (orderValue instanceof JsonString) {
            final Schema.EntriesOrder order = Schema.EntriesOrder.of(((JsonString) orderValue).getString());
            return builder.build(order);
        } else {
            return builder.build();
        }
    }

    private void treatElementSchema(final JsonObject json,
            final Consumer<Schema> setter) {
        final JsonValue elementSchema = json.get(ELEMENT_SCHEMA);
        if (elementSchema instanceof JsonObject) {
            final Schema schema = this.toSchema((JsonObject) elementSchema);
            setter.accept(schema);
        } else if (elementSchema instanceof JsonString) {
            final Schema.Type innerType = Schema.Type.valueOf(((JsonString) elementSchema).getString());
            setter.accept(this.factory.newSchemaBuilder(innerType).build());
        }
    }

    private void addEntries(final Schema.Builder schemaBuilder, final JsonArray entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        entries.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .map(this::jsonToEntry)
                .forEach(schemaBuilder::withEntry);
    }

    private Schema.Entry jsonToEntry(final JsonObject jsonEntry) {
        final Schema.Entry.Builder builder = factory.newEntryBuilder();
        final Schema.Type schemaType = Enum.valueOf(Schema.Type.class, jsonEntry.getString(TYPE));
        builder.withType(schemaType)
                .withName(jsonEntry.getString("name"))
                .withNullable(jsonEntry.getBoolean("nullable", true))
                .withMetadata(jsonEntry.getBoolean("metadata", false));
        final JsonString comment = jsonEntry.getJsonString("comment");
        if (comment != null) {
            builder.withComment(comment.getString());
        }
        final JsonString rawName = jsonEntry.getJsonString("rawName");
        if (rawName != null) {
            builder.withRawName(rawName.getString());
        }
        final JsonValue jsonValue = jsonEntry.get("defaultValue");
        if (jsonValue != null) {
            this.setDefaultValue(builder, jsonValue);
        }

        if (schemaType == Schema.Type.RECORD || schemaType == Schema.Type.ARRAY) {
            this.treatElementSchema(jsonEntry, builder::withElementSchema);
        }

        this.addProps(builder::withProp, jsonEntry);
        return builder.build();
    }

    private void addProps(final BiConsumer<String, String> setter,
            final JsonObject object) {
        if (object == null || object.get(PROPS) == null) {
            return;
        }
        JsonObject props = object.getJsonObject(PROPS);
        if (props.isEmpty()) {
            return;
        }
        props.forEach(
                (String name, JsonValue value) -> setter.accept(name, ((JsonString) value).getString()));
    }

    private void setDefaultValue(final Schema.Entry.Builder entry,
            final JsonValue value) {
        final JsonValue.ValueType valueType = value.getValueType();
        if (valueType == JsonValue.ValueType.NUMBER) {
            entry.withDefaultValue(((JsonNumber) value).numberValue());
        } else if (valueType == JsonValue.ValueType.FALSE) {
            entry.withDefaultValue(Boolean.FALSE);
        } else if (valueType == JsonValue.ValueType.TRUE) {
            entry.withDefaultValue(Boolean.TRUE);
        } else if (valueType == JsonValue.ValueType.STRING) {
            entry.withDefaultValue(((JsonString) value).getString());
        }
        // doesn't treat JsonArray nor JsonObject for default value.
    }

    public JsonObject toJson(final Schema schema) {
        if (schema == null) {
            return null;
        }
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(TYPE, schema.getType().name());
        if (schema.getType() == Schema.Type.RECORD) {
            this.addEntries(builder, ENTRIES, schema.getAllEntries());
        } else if (schema.getType() == Schema.Type.ARRAY) {
            final JsonObject elementSchema = this.toJson(schema.getElementSchema());
            if (elementSchema != null) {
                builder.add(ELEMENT_SCHEMA, elementSchema);
            }
        }
        this.addProps(builder, schema.getProps());

        if (schema.getType() == Schema.Type.RECORD) {
            final Schema.EntriesOrder order = schema.naturalOrder();
            if (order != null) {
                String orderValue = order.getFieldsOrder().collect(Collectors.joining(","));
                builder.add("order", orderValue);
            }
        }
        return builder.build();
    }

    private void addEntries(final JsonObjectBuilder objectBuilder,
            final String name,
            final Stream<Schema.Entry> entries) {
        if (entries == null) {
            return;
        }
        final JsonArrayBuilder jsonEntries = Json.createArrayBuilder();
        objectBuilder.add(name, jsonEntries);
        entries.map(this::entryToJson)
                .forEach(jsonEntries::add);
    }

    private JsonObjectBuilder entryToJson(final Schema.Entry entry) {
        final JsonObjectBuilder entryBuilder = Json.createObjectBuilder();
        entryBuilder.add("name", entry.getName());
        entryBuilder.add(TYPE, entry.getType().name());
        entryBuilder.add("nullable", entry.isNullable());
        if (entry.getComment() != null) {
            entryBuilder.add("comment", entry.getComment());
        }
        if (entry.getRawName() != null) {
            entryBuilder.add("rawName", entry.getRawName());
        }
        if (entry.isMetadata()) {
            entryBuilder.add("metadata", true);
        }
        if (entry.getDefaultValue() != null) {
            final JsonValue jsonValue = this.toValue(entry.getDefaultValue());
            if (jsonValue != null) {
                entryBuilder.add("defaultValue", jsonValue);
            }
        }
        if (entry.getType() == Schema.Type.RECORD
                || entry.getType() == Schema.Type.ARRAY) {

            final Schema innerSchema = entry.getElementSchema();
            if (innerSchema.getType() == Schema.Type.ARRAY
                    || innerSchema.getType() == Schema.Type.RECORD) {
                final JsonObject elementSchema = this.toJson(innerSchema);
                entryBuilder.add(ELEMENT_SCHEMA, elementSchema);
            } else {
                entryBuilder.add(ELEMENT_SCHEMA, innerSchema.getType().name());
            }
        }
        this.addProps(entryBuilder, entry.getProps());
        return entryBuilder;
    }

    private void addProps(final JsonObjectBuilder builder,
            final Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return;
        }
        final JsonObjectBuilder jsonProps = Json.createObjectBuilder();
        properties.entrySet()
                .forEach(
                        (Map.Entry<String, String> e) -> jsonProps.add(e.getKey(), e.getValue()));
        builder.add(PROPS, jsonProps);
    }

    private JsonValue toValue(final Object object) {
        if (object == null) {
            return JsonValue.NULL;
        }
        if (object instanceof Integer) {
            return Json.createValue((Integer) object);
        }
        if (object instanceof Long) {
            return Json.createValue((Long) object);
        }
        if (object instanceof Double || object instanceof Float) {
            return Json.createValue((Double) object);
        }
        if (object instanceof BigInteger) {
            return Json.createValue((BigInteger) object);
        }
        if (object instanceof Boolean) {
            if (object == Boolean.TRUE) {
                return JsonValue.TRUE;
            }
            return JsonValue.FALSE;
        }
        if (object instanceof BigDecimal) {
            return Json.createValue((BigDecimal) object);
        }
        if (object instanceof String) {
            return Json.createValue((String) object);
        }

        return null;
    }

}
