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
package org.talend.sdk.component.server.front.model;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import lombok.Getter;
import lombok.Setter;


public class JsonSchemaModel {

    @Getter
    @Setter
    private Type type;

    private final JsonObject jsonSchema;

    @Setter
    @Getter
    private JsonSchemaModel elementSchema;

    @Getter
    @Setter
    private List<JsonEntryModel> entries = new ArrayList<>();

    @Getter
    private List<JsonEntryModel> metadataEntries = new ArrayList<>();

    @Getter
    @Setter
    private Map<String, String> props = new HashMap<>();

    @Getter
    @Setter
    private Map<String, JsonEntryModel> entryMap = new HashMap<>();

    public JsonSchemaModel(final String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            this.jsonSchema = reader.readObject();
        }

        this.type = Type.valueOf(jsonSchema.getString("type"));
        this.props = parseProps(jsonSchema.getJsonObject("props"));

        // Parse entries
        this.entries = parseEntries(jsonSchema.getJsonArray("entries"), false);
        this.metadataEntries = parseEntries(jsonSchema.getJsonArray("metadata"), true);

        // Build entry map
        this.entryMap = new HashMap<>();
        getEntries().forEach(e -> entryMap.put(e.getName(), e));

        // Parse element schema for ARRAY types
        this.elementSchema = jsonSchema.containsKey("elementSchema")
                ? new JsonSchemaModel(jsonSchema.getJsonObject("elementSchema").toString())
                : null;
    }

    private List<JsonEntryModel> parseEntries(final JsonArray jsonArray, final boolean isMetadata) {
        if (jsonArray == null) {
            return Collections.emptyList();
        }

        return jsonArray.stream()
                .map(JsonValue::asJsonObject)
                .map(obj -> new JsonEntryModel(obj, isMetadata))
                .toList();
    }

    private Map<String, String> parseProps(final JsonObject propsObj) {
        if (propsObj == null) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();
        propsObj.forEach((key, value) ->
                result.put(key, value.getValueType() == JsonValue.ValueType.STRING
                        ? ((javax.json.JsonString) value).getString()
                        : value.toString()));
        return result;
    }

    public enum Type {

        RECORD(new Class<?>[] { Record.class }),
        ARRAY(new Class<?>[] { Collection.class }),
        STRING(new Class<?>[] { String.class, Object.class }),
        BYTES(new Class<?>[] { byte[].class, Byte[].class }),
        INT(new Class<?>[] { Integer.class }),
        LONG(new Class<?>[] { Long.class }),
        FLOAT(new Class<?>[] { Float.class }),
        DOUBLE(new Class<?>[] { Double.class }),
        BOOLEAN(new Class<?>[] { Boolean.class }),
        DATETIME(new Class<?>[] { Long.class, Date.class, Temporal.class }),
        DECIMAL(new Class<?>[] { BigDecimal.class });

        /**
         * All compatibles Java classes
         */
        private final Class<?>[] classes;

        Type(final Class<?>[] classes) {
            this.classes = classes;
        }

        /**
         * Check if input can be affected to an entry of this type.
         *
         * @param input : object.
         *
         * @return true if input is null or ok.
         */
        public boolean isCompatible(final Object input) {
            if (input == null) {
                return true;
            }
            for (final Class<?> clazz : classes) {
                if (clazz.isInstance(input)) {
                    return true;
                }
            }
            return false;
        }
    }
}
