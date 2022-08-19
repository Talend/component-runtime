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
package org.talend.sdk.component.singer.kitap;

import static javax.json.stream.JsonCollectors.toJsonObject;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.talend.sdk.component.api.record.Schema;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JsonSchemaGenerator implements Supplier<JsonObject> {

    private final Collection<Schema.Entry> properties;

    private final JsonBuilderFactory jsonBuilderFactory;

    @Override
    public JsonObject get() {
        final JsonObjectBuilder json = jsonBuilderFactory.createObjectBuilder();
        json.add("type", jsonBuilderFactory.createArrayBuilder().add("null").add("object").build());
        json.add("additionalProperties", false);
        json.add("properties", properties.stream().map(this::toJson).collect(toJsonObject()));
        return json.build();
    }

    private Map.Entry<String, JsonValue> toJson(final Schema.Entry entry) {
        final JsonObject schema;
        switch (entry.getType()) {
        case BYTES:
        case STRING:
            schema = jsonBuilderFactory.createObjectBuilder().add("type", types("string", entry.isNullable())).build();
            break;
        case DATETIME:
            schema = jsonBuilderFactory
                    .createObjectBuilder()
                    .add("type", types("string", entry.isNullable()))
                    .add("format", "date-time")
                    .build();
            break;
        case DECIMAL: // use string as JSON number data loss risk for decimal
            schema = jsonBuilderFactory
                    .createObjectBuilder()
                    .add("type", types("string", entry.isNullable()))
                    .build();
            break;
        case BOOLEAN:
            schema = jsonBuilderFactory.createObjectBuilder().add("type", types("boolean", entry.isNullable())).build();
            break;
        case FLOAT:
        case DOUBLE:
            schema = jsonBuilderFactory.createObjectBuilder().add("type", types("number", entry.isNullable())).build();
            break;
        case INT:
        case LONG:
            schema = jsonBuilderFactory.createObjectBuilder().add("type", types("integer", entry.isNullable())).build();
            break;
        case RECORD:
            schema = new JsonSchemaGenerator(entry.getElementSchema().getEntries(), jsonBuilderFactory).get();
            break;
        case ARRAY:
            schema = jsonBuilderFactory
                    .createObjectBuilder()
                    .add("type", types("array", entry.isNullable()))
                    .add("items",
                            new JsonSchemaGenerator(entry.getElementSchema().getEntries(), jsonBuilderFactory).get())
                    .build();
            break;
        default:
            throw new IllegalArgumentException("Invalid entry: " + entry);
        }
        return new AbstractMap.SimpleImmutableEntry<>(entry.getName(), schema);
    }

    private JsonArrayBuilder types(final String type, final boolean nullable) {
        final JsonArrayBuilder arrayBuilder = jsonBuilderFactory.createArrayBuilder();
        if (nullable) {
            arrayBuilder.add("null");
        }
        arrayBuilder.add(type);
        return arrayBuilder;
    }
}
