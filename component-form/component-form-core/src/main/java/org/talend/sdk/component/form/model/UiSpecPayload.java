/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.form.model;

import java.util.Collection;
import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;

public class UiSpecPayload {

    private JsonSchema jsonSchema;

    private Collection<UiSchema> uiSchema;

    private Map<String, Object> properties; // defaults values

    private Map<String, String> errors;

    public JsonSchema getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(final JsonSchema jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public Collection<UiSchema> getUiSchema() {
        return uiSchema;
    }

    public void setUiSchema(final Collection<UiSchema> uiSchema) {
        this.uiSchema = uiSchema;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(final Map<String, Object> properties) {
        this.properties = properties;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(final Map<String, String> errors) {
        this.errors = errors;
    }

    public static class JsonSchema {

        private String title;

        private String type;

        private Integer minItems;

        private Integer maxItems;

        private Integer minLength;

        private Integer maxLength;

        private Double minimum;

        private Double maximum;

        private Boolean uniqueItems;

        private String pattern;

        @JsonbProperty("default")
        private String defaultValue;

        private Collection<String> required;

        private Map<String, JsonSchema> properties;

        @JsonbProperty("enum")
        private Collection<String> enumValues;

        public String getTitle() {
            return title;
        }

        public void setTitle(final String title) {
            this.title = title;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public Integer getMinItems() {
            return minItems;
        }

        public void setMinItems(final Integer minItems) {
            this.minItems = minItems;
        }

        public Integer getMaxItems() {
            return maxItems;
        }

        public void setMaxItems(final Integer maxItems) {
            this.maxItems = maxItems;
        }

        public Integer getMinLength() {
            return minLength;
        }

        public void setMinLength(final Integer minLength) {
            this.minLength = minLength;
        }

        public Integer getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(final Integer maxLength) {
            this.maxLength = maxLength;
        }

        public Double getMinimum() {
            return minimum;
        }

        public void setMinimum(final Double minimum) {
            this.minimum = minimum;
        }

        public Double getMaximum() {
            return maximum;
        }

        public void setMaximum(final Double maximum) {
            this.maximum = maximum;
        }

        public Boolean getUniqueItems() {
            return uniqueItems;
        }

        public void setUniqueItems(final Boolean uniqueItems) {
            this.uniqueItems = uniqueItems;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(final String pattern) {
            this.pattern = pattern;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(final String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public Collection<String> getRequired() {
            return required;
        }

        public void setRequired(final Collection<String> required) {
            this.required = required;
        }

        public Map<String, JsonSchema> getProperties() {
            return properties;
        }

        public void setProperties(final Map<String, JsonSchema> properties) {
            this.properties = properties;
        }

        public Collection<String> getEnumValues() {
            return enumValues;
        }

        public void setEnumValues(final Collection<String> enumValues) {
            this.enumValues = enumValues;
        }

        @Override
        public String toString() {
            return "JsonSchema{" + "title='" + title + '\'' + ", type='" + type + '\'' + ", minItems=" + minItems
                + ", maxItems=" + maxItems + ", minLength=" + minLength + ", maxLength=" + maxLength + ", minimum="
                + minimum + ", maximum=" + maximum + ", uniqueItems=" + uniqueItems + ", pattern='" + pattern + '\''
                + ", defaultValue='" + defaultValue + '\'' + ", required=" + required + ", properties=" + properties
                + ", enumValues=" + enumValues + '}';
        }
    }

    public static class UiSchema {

        private String widget;

        private String type;

        private Map<String, String> options;

        private String title;

        private String key;

        private Boolean autoFocus;

        private Boolean disabled;

        private Boolean readOnly;

        private Boolean required;

        private Boolean restricted;

        private String placeholder;

        private Collection<Trigger> triggers;

        private Collection<UiSchema> items;

        private Collection<NameValue> titleMap;

        private JsonSchema schema;

        public Boolean getRestricted() {
            return restricted;
        }

        public void setRestricted(final Boolean restricted) {
            this.restricted = restricted;
        }

        public JsonSchema getSchema() {
            return schema;
        }

        public void setSchema(final JsonSchema schema) {
            this.schema = schema;
        }

        public Collection<NameValue> getTitleMap() {
            return titleMap;
        }

        public void setTitleMap(final Collection<NameValue> titleMap) {
            this.titleMap = titleMap;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(final Boolean required) {
            this.required = required;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public String getWidget() {
            return widget;
        }

        public void setWidget(final String widget) {
            this.widget = widget;
        }

        public Map<String, String> getOptions() {
            return options;
        }

        public void setOptions(final Map<String, String> options) {
            this.options = options;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(final String title) {
            this.title = title;
        }

        public String getKey() {
            return key;
        }

        public void setKey(final String key) {
            this.key = key;
        }

        public Boolean getAutoFocus() {
            return autoFocus;
        }

        public void setAutoFocus(final Boolean autoFocus) {
            this.autoFocus = autoFocus;
        }

        public Boolean getDisabled() {
            return disabled;
        }

        public void setDisabled(final Boolean disabled) {
            this.disabled = disabled;
        }

        public Boolean getReadOnly() {
            return readOnly;
        }

        public void setReadOnly(final Boolean readOnly) {
            this.readOnly = readOnly;
        }

        public String getPlaceholder() {
            return placeholder;
        }

        public void setPlaceholder(final String placeholder) {
            this.placeholder = placeholder;
        }

        public Collection<Trigger> getTriggers() {
            return triggers;
        }

        public void setTriggers(final Collection<Trigger> triggers) {
            this.triggers = triggers;
        }

        public Collection<UiSchema> getItems() {
            return items;
        }

        public void setItems(final Collection<UiSchema> items) {
            this.items = items;
        }

        @Override
        public String toString() {
            return "UiSchema{" + "widget='" + widget + '\'' + ", type='" + type + '\'' + ", options=" + options + ", "
                + "title='" + title + '\'' + ", key='" + key + '\'' + ", autoFocus=" + autoFocus + ", disabled="
                + disabled + ", readOnly=" + readOnly + ", placeholder='" + placeholder + '\'' + ", triggers="
                + triggers + ", items=" + items + '}';
        }
    }

    public static class NameValue {

        private String name;

        private String value;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "NameValue{" + "name='" + name + '\'' + ", value='" + value + '\'' + '}';
        }
    }

    public static class Trigger {

        private String action;

        private String family;

        private String type;

        private Collection<Parameter> parameters;

        public String getAction() {
            return action;
        }

        public void setAction(final String action) {
            this.action = action;
        }

        public String getFamily() {
            return family;
        }

        public void setFamily(final String family) {
            this.family = family;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public Collection<Parameter> getParameters() {
            return parameters;
        }

        public void setParameters(final Collection<Parameter> parameters) {
            this.parameters = parameters;
        }

        public static class Parameter {

            private String key;

            private String path;

            public String getKey() {
                return key;
            }

            public void setKey(final String key) {
                this.key = key;
            }

            public String getPath() {
                return path;
            }

            public void setPath(final String path) {
                this.path = path;
            }

            @Override
            public String toString() {
                return "Parameter{" + "key='" + key + '\'' + ", path='" + path + '\'' + '}';
            }
        }

        @Override
        public String toString() {
            return "Trigger{" + "action='" + action + '\'' + ", family='" + family + '\'' + ", type='" + type + '\''
                + ", parameters=" + parameters + '}';
        }
    }

    @Override
    public String toString() {
        return "UiSpecPayload{" + "jsonSchema=" + jsonSchema + ", uiSchema=" + uiSchema + ", properties=" + properties
            + ", errors=" + errors + '}';
    }
}
