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
     * The name of this entry.
     */
    @Setter
    @Getter
    private String name;

    /**
     * The raw name of this entry.
     */
    @Setter
    @Getter
    private String rawName;

    /**
     * Type of the entry, this determine which other fields are populated.
     */
    @Setter
    @Getter
    private JsonSchemaModel.Type type;

    /**
     * Is this entry nullable or always valued.
     */
    @Setter
    @Getter
    private boolean nullable;

    /**
     * Is this entry can be in error.
     */
    private boolean errorCapable;

    /**
     * Is this entry a metadata entry.
     */
    @Setter
    @Getter
    private boolean metadata;

    /**
     * Default value for this entry.
     */
    @Setter
    @Getter
    private Object defaultValue;

    /**
     * For type == record, the element type.
     */
    @Getter
    @Setter
    private JsonSchemaModel elementSchema;

    /**
     * Allows to associate to this field a comment - for doc purposes, no use in the runtime.
     */
    @Setter
    @Getter
    private String comment;

    @Setter
    @Getter
    private boolean valid;

    /**
     * metadata
     */
    @Setter
    @Getter
    private Map<String, String> props = new LinkedHashMap<>(0);

    JsonEntryModel(JsonObject jsonEntry, boolean isMetadata) {
        this.jsonEntry = jsonEntry;
        this.isMetadata = isMetadata;
        this.type = JsonSchemaModel.Type.valueOf(jsonEntry.getString("type"));
        this.props = parseProps(jsonEntry.getJsonObject("props"));

        // Parse element schema if present
        this.elementSchema = jsonEntry.containsKey("elementSchema")
                ? new JsonSchemaModel(jsonEntry.getJsonObject("elementSchema").toString())
                : null;
    }

    private Map<String, String> parseProps(JsonObject propsObj) {
        if (propsObj == null) return Collections.emptyMap();

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
        return jsonEntry.getBoolean("nullable", true);
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

    public String getProp(String property) {
        return props.get(property);
    }

}
