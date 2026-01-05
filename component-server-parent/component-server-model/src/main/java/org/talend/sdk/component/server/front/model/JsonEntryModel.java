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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonValue;

import lombok.Getter;
import lombok.Setter;

public class JsonEntryModel {

    private final JsonObject jsonEntry;

    @Getter
    private final boolean isMetadata;

    /**
     * Type of the entry, this determine which other fields are populated.
     */
    @Setter
    @Getter
    private JsonSchemaModel.Type type;

    /**
     * Is this entry a metadata entry.
     */
    @Setter
    @Getter
    private boolean metadata;

    /**
     * For type == record, the element type.
     */
    @Getter
    @Setter
    private JsonSchemaModel elementSchema;

    /**
     * metadata
     */
    @Setter
    @Getter
    private Map<String, String> props = new LinkedHashMap<>(0);

    JsonEntryModel(final JsonObject jsonEntry, final boolean isMetadata) {
        this.jsonEntry = jsonEntry;
        this.isMetadata = isMetadata;
        this.type = JsonSchemaModel.Type.valueOf(jsonEntry.getString("type"));
        this.props = parseProps(jsonEntry.getJsonObject("props"));

        // Parse element schema if present
        this.elementSchema = jsonEntry.containsKey("elementSchema")
                ? new JsonSchemaModel(jsonEntry.getJsonObject("elementSchema").toString())
                : null;
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

    public String getName() {
        return jsonEntry.getString("name");
    }

    public String getRawName() {
        return jsonEntry.getString("rawName", getName());
    }

    public String getOriginalFieldName() {
        return getRawName() != null ? getRawName() : getName();
    }

    public boolean isNullable() {
        return !jsonEntry.containsKey("nullable") || jsonEntry.getBoolean("nullable");
    }

    public boolean isErrorCapable() {
        return jsonEntry.getBoolean("errorCapable", false);
    }

    public boolean isValid() {
        return jsonEntry.getBoolean("valid", true);
    }

    public <T> T getDefaultValue() {
        return jsonEntry.containsKey("defaultValue")
                ? (T) jsonEntry.get("defaultValue")
                : null;
    }

    public String getComment() {
        return jsonEntry.getString("comment", null);
    }

    public String getProp(final String property) {
        return props.get(property);
    }

}
