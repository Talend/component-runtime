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
package org.talend.sdk.component.form.model.jsonschema;

import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;

import lombok.Data;

// for now keep it simple,
// if we need multiple types we will
// just add serializers for the attributes
@Data
public class JsonSchema {

    public static final String DEFAULT_TYPE = "object";

    private String id;

    private String title;

    private String description;

    @JsonbProperty("$schema")
    private String schema;

    @JsonbProperty("$ref")
    private String ref;

    private String type = DEFAULT_TYPE;

    private Integer minItems;

    private Integer maxItems;

    private Integer minLength;

    private Integer maxLength;

    private Double minimum;

    private Double maximum;

    private Boolean uniqueItems;

    private String pattern;

    @JsonbProperty("default")
    private Object defaultValue;

    private Collection<String> required;

    private Map<String, JsonSchema> properties;

    private JsonSchema items;

    @JsonbProperty("enum")
    private Collection<String> enumValues;

    public static Builder jsonSchema() {
        return new Builder();
    }

    public static Builder jsonSchemaFrom(final Class<?> pojo) {
        return new PojoJsonSchemaBuilder().create(pojo);
    }

    public static final class Builder {

        private String id;

        private String title;

        private String description;

        private String schema;

        private String ref;

        private String type = DEFAULT_TYPE;

        private Integer minItems;

        private Integer maxItems;

        private Integer minLength;

        private Integer maxLength;

        private Double minimum;

        private Double maximum;

        private Boolean uniqueItems;

        private String pattern;

        private Object defaultValue;

        private Collection<String> required;

        private Map<String, JsonSchema> properties;

        private Collection<String> enumValues;

        private JsonSchema items;

        public Builder withId(final String id) {
            this.id = id;
            return this;
        }

        public Builder withItems(final JsonSchema schemas) {
            this.items = schemas;
            return this;
        }

        public Builder withTitle(final String title) {
            this.title = title;
            return this;
        }

        public Builder withDescription(final String description) {
            this.description = description;
            return this;
        }

        public Builder withSchema(final String schema) {
            this.schema = schema;
            return this;
        }

        public Builder withRef(final String ref) {
            this.ref = ref;
            return this;
        }

        public Builder withType(final String type) {
            this.type = type;
            return this;
        }

        public Builder withMinItems(final Integer minItems) {
            this.minItems = minItems;
            return this;
        }

        public Builder withMaxItems(final Integer maxItems) {
            this.maxItems = maxItems;
            return this;
        }

        public Builder withMinLength(final Integer minLength) {
            this.minLength = minLength;
            return this;
        }

        public Builder withMaxLength(final Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder withMinimum(final Double minimum) {
            this.minimum = minimum;
            return this;
        }

        public Builder withMaximum(final Double maximum) {
            this.maximum = maximum;
            return this;
        }

        public Builder withUniqueItems(final Boolean uniqueItems) {
            this.uniqueItems = uniqueItems;
            return this;
        }

        public Builder withPattern(final String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder withDefaultValue(final Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder withRequired(final Collection<String> required) {
            this.required = required;
            return this;
        }

        public Builder withRequired(final String... required) {
            this.required = asList(required);
            return this;
        }

        public Builder withProperty(final String name, final JsonSchema schema) {
            if (this.properties == null) {
                this.properties = new LinkedHashMap<>();
            }
            this.properties.put(name, schema);
            return this;
        }

        public Builder withProperties(final Map<String, JsonSchema> properties) {
            if (this.properties == null) {
                this.properties = new LinkedHashMap<>();
            }
            this.properties.putAll(properties);
            return this;
        }

        public Builder withEnumValues(final Collection<String> enumValues) {
            this.enumValues = enumValues;
            return this;
        }

        public Builder withEnumValues(final String... enumValues) {
            this.enumValues = asList(enumValues);
            return this;
        }

        public JsonSchema build() {
            final JsonSchema jsonSchema = new JsonSchema();
            jsonSchema.setId(id);
            jsonSchema.setTitle(title);
            jsonSchema.setDescription(description);
            jsonSchema.setSchema(schema);
            jsonSchema.setRef(ref);
            jsonSchema.setType(type);
            jsonSchema.setMinItems(minItems);
            jsonSchema.setMaxItems(maxItems);
            jsonSchema.setMinLength(minLength);
            jsonSchema.setMaxLength(maxLength);
            jsonSchema.setMinimum(minimum);
            jsonSchema.setMaximum(maximum);
            jsonSchema.setUniqueItems(uniqueItems);
            jsonSchema.setPattern(pattern);
            jsonSchema.setDefaultValue(defaultValue);
            jsonSchema.setRequired(required);
            jsonSchema.setProperties(properties);
            jsonSchema.setEnumValues(enumValues);
            jsonSchema.setItems(items);
            return jsonSchema;
        }
    }
}
