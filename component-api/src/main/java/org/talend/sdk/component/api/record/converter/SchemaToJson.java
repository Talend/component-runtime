/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.record.converter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.talend.sdk.component.api.record.Schema;

/**
 * Serialize Schema into json format, whatever used implementation.
 */
public class SchemaToJson {

    public JsonObject toJson(final Schema schema) {
        if (schema == null) {
            return null;
        }
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("type", schema.getType().name());
        if (schema.getType() == Schema.Type.RECORD) {
            this.addEntries(builder, "entries", schema.getAllEntries());
        } else if (schema.getType() == Schema.Type.ARRAY) {
            final JsonObject elementSchema = this.toJson(schema.getElementSchema());
            if (elementSchema != null) {
                builder.add("elementSchema", elementSchema);
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
        entryBuilder.add("type", entry.getType().name());
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
                entryBuilder.add("elementSchema", elementSchema);
            } else {
                entryBuilder.add("elementSchema", innerSchema.getType().name());
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
        builder.add("props", jsonProps);
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
