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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

import lombok.RequiredArgsConstructor;

/**
 * read json representing a schema and convert it to schema.
 */
@RequiredArgsConstructor
public class JsonToSchema {

    private final RecordBuilderFactory factory;

    public Schema toSchema(final JsonObject json) {
        if (json == null || json.isNull("type")) {
            return null;
        }
        final String type = json.getString("type");
        final Schema.Type schemaType = Enum.valueOf(Schema.Type.class, type);
        final Schema.Builder builder = factory.newSchemaBuilder(schemaType);

        if (schemaType == Schema.Type.RECORD) {
            this.addEntries(builder, false, json.getJsonArray("entries"));
            this.addEntries(builder, true, json.getJsonArray("metadatas"));
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
        final JsonValue elementSchema = json.get("elementSchema");
        if (elementSchema instanceof JsonObject) {
            final Schema schema = this.toSchema((JsonObject) elementSchema);
            setter.accept(schema);
        } else if (elementSchema instanceof JsonString) {
            final Schema.Type innerType = Schema.Type.valueOf(((JsonString) elementSchema).getString());
            setter.accept(this.factory.newSchemaBuilder(innerType).build());
        }
    }

    private void addEntries(final Schema.Builder schemaBuilder,
            final boolean metadata,
            final JsonArray entries) {
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

        final Schema.Entry.Builder builder = this.factory.newEntryBuilder();

        final String entryType = jsonEntry.getString("type");
        final Schema.Type schemaType = Enum.valueOf(Schema.Type.class, entryType);
        builder.withType(schemaType);
        builder.withName(jsonEntry.getString("name"));
        builder.withNullable(jsonEntry.getBoolean("nullable", true));
        builder.withMetadata(jsonEntry.getBoolean("metadata", false));

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
        if (object == null || object.get("props") == null) {
            return;
        }
        JsonObject props = object.getJsonObject("props");
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
        } else if (valueType == JsonValue.ValueType.TRUE) {
            entry.withDefaultValue(Boolean.TRUE);
        } else if (valueType == JsonValue.ValueType.STRING) {
            entry.withDefaultValue(((JsonString) value).getString());
        }
        // doesn't treat JsonArray nor JsonObject for default value.

    }
}
