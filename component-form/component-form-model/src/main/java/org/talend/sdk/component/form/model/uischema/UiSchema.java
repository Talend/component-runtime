/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.form.model.uischema;

import static java.util.Arrays.asList;
import static java.util.Locale.ROOT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.talend.sdk.component.form.model.jsonschema.JsonSchema;

import lombok.Data;

@Data
public class UiSchema {

    private String key;

    private String title;

    private String widget;

    private String itemWidget;

    private String type;

    private String description;

    private Collection<UiSchema> items;

    private Map<String, String> options;

    private Boolean autoFocus;

    private Boolean disabled;

    private Boolean readOnly;

    private Boolean required;

    private Boolean restricted;

    private String placeholder;

    private Collection<Trigger> triggers;

    private Collection<NameValue> titleMap;

    private Collection<Condition> conditions;

    public static Builder uiSchema() {
        return new Builder();
    }

    public static Trigger.Builder trigger() {
        return new Trigger.Builder();
    }

    public static NameValue.Builder nameValue() {
        return new NameValue.Builder();
    }

    public static Parameter.Builder parameter() {
        return new Parameter.Builder();
    }

    public static Condition.Builder condition() {
        return new Condition.Builder();
    }

    @Data
    public static class Condition {

        private String path;

        private Collection<Object> values;

        private boolean shouldBe;

        private String strategy;

        public static final class Builder {

            private String path;

            private Collection<Object> values;

            private boolean shouldBe;

            private String strategy;

            public Builder withShouldBe(final boolean value) {
                this.shouldBe = value;
                return this;
            }

            public Builder withStrategy(final String strategy) {
                this.strategy =
                        strategy == null || "DEFAULT".equals(strategy) ? null : strategy.trim().toLowerCase(ROOT);
                return this;
            }

            public Builder withPath(final String path) {
                this.path = path;
                return this;
            }

            public Builder withValues(final Collection<Object> values) {
                if (this.values == null) {
                    this.values = new ArrayList<>();
                }
                this.values.addAll(values);
                return this;
            }

            public Builder withValue(final Object value) {
                if (this.values == null) {
                    this.values = new ArrayList<>();
                }
                this.values.add(value);
                return this;
            }

            public Condition build() {
                final Condition nameValue = new Condition();
                nameValue.setPath(path);
                nameValue.setValues(values);
                nameValue.setShouldBe(shouldBe);
                nameValue.setStrategy(strategy);
                return nameValue;
            }
        }
    }

    @Data
    public static class NameValue {

        private String name;

        private String value;

        public static final class Builder {

            private String name;

            private String value;

            public Builder withName(final String name) {
                this.name = name;
                return this;
            }

            public Builder withValue(final String value) {
                this.value = value;
                return this;
            }

            public NameValue build() {
                final NameValue nameValue = new NameValue();
                nameValue.setName(name);
                nameValue.setValue(value);
                return nameValue;
            }
        }
    }

    @Data
    public static class Parameter {

        private String key;

        private String path;

        public static final class Builder {

            private String key;

            private String path;

            public Builder withKey(final String key) {
                this.key = key;
                return this;
            }

            public Builder withPath(final String path) {
                this.path = path;
                return this;
            }

            public Parameter build() {
                final Parameter parameter = new Parameter();
                parameter.setKey(key);
                parameter.setPath(path);
                return parameter;
            }
        }
    }

    @Data
    public static class Option {

        private String path;

        private String type;

        public static class Builder {

            private String path;

            private String type;

            public Builder withPath(final String path) {
                this.path = path;
                return this;
            }

            public Builder withType(final String type) {
                this.type = type;
                return this;
            }

            public Option build() {
                final Option option = new Option();
                option.setPath(path);
                option.setType(type);
                return option;
            }
        }
    }

    @Data
    public static class Trigger {

        private String action;

        private String family;

        private String type;

        private String onEvent;

        private Collection<Option> options;

        private Collection<Parameter> parameters;

        public static final class Builder {

            private String action;

            private String family;

            private String type;

            private String onEvent;

            private Collection<Option> options;

            private Collection<Parameter> parameters;

            public Builder withOnEvent(final String onEvent) {
                this.onEvent = onEvent;
                return this;
            }

            public Builder withAction(final String action) {
                this.action = action;
                return this;
            }

            public Builder withFamily(final String family) {
                this.family = family;
                return this;
            }

            public Builder withType(final String type) {
                this.type = type;
                return this;
            }

            public Builder withOption(final Option value) {
                if (this.options == null) {
                    this.options = new ArrayList<>();
                }
                this.options.add(value);
                return this;
            }

            public Builder withOptions(final Collection<Option> options) {
                if (this.options == null) {
                    this.options = new ArrayList<>();
                }
                this.options.addAll(options);
                return this;
            }

            public Builder withParameter(final String key, final String path) {
                if (this.parameters == null) {
                    this.parameters = new ArrayList<>();
                }
                this.parameters.add(new Parameter());
                return this;
            }

            public Builder withParameters(final Collection<Parameter> parameters) {
                if (this.parameters == null) {
                    this.parameters = new ArrayList<>();
                }
                this.parameters.addAll(parameters);
                return this;
            }

            public Trigger build() {
                final Trigger parameter = new Trigger();
                parameter.setAction(action);
                parameter.setFamily(family);
                parameter.setType(type);
                parameter.setParameters(parameters);
                parameter.setOptions(options);
                parameter.setOnEvent(onEvent);
                return parameter;
            }
        }
    }

    public static final class Builder {

        private String key;

        private String title;

        private String widget;

        private String itemWidget;

        private String type;

        private String description;

        private Collection<UiSchema> items;

        private Map<String, String> options;

        private Boolean autoFocus;

        private Boolean disabled;

        private Boolean readOnly;

        private Boolean required;

        private Boolean restricted;

        private String placeholder;

        private Collection<Trigger> triggers;

        private Collection<NameValue> titleMap;

        private JsonSchema schema;

        public Builder withKey(final String key) {
            this.key = key;
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

        public Builder withWidget(final String widget) {
            this.widget = widget;
            return this;
        }

        public Builder withItemWidget(final String widget) {
            this.itemWidget = widget;
            return this;
        }

        public Builder withType(final String type) {
            this.type = type;
            return this;
        }

        public Builder withItems(final Collection<UiSchema> items) {
            if (this.items == null) {
                this.items = new ArrayList<>();
            }
            this.items.addAll(items);
            return this;
        }

        public Builder withItems(final UiSchema... items) {
            return withItems(asList(items));
        }

        public Builder withOptions(final String name, final String value) {
            if (this.options == null) {
                this.options = new HashMap<>();
            }
            this.options.put(name, value);
            return this;
        }

        public Builder withOptions(final Map<String, String> options) {
            if (this.options == null) {
                this.options = new HashMap<>();
            }
            this.options.putAll(options);
            return this;
        }

        public Builder withAutoFocus(final Boolean autoFocus) {
            this.autoFocus = autoFocus;
            return this;
        }

        public Builder withDisabled(final Boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder withReadOnly(final Boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder withRequired(final Boolean required) {
            this.required = required;
            return this;
        }

        public Builder withRestricted(final Boolean restricted) {
            this.restricted = restricted;
            return this;
        }

        public Builder withPlaceholder(final String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder withTriggers(final Trigger... triggers) {
            return withTriggers(asList(triggers));
        }

        public Builder withTriggers(final Collection<Trigger> triggers) {
            if (this.triggers == null) {
                this.triggers = new ArrayList<>();
            }
            this.triggers.addAll(triggers);
            return this;
        }

        public Builder withTitleMap(final NameValue... titleMap) {
            return withTitleMap(asList(titleMap));
        }

        public Builder withTitleMap(final Collection<NameValue> titleMap) {
            if (this.titleMap == null) {
                this.titleMap = new ArrayList<>();
            }
            this.titleMap.addAll(titleMap);
            return this;
        }

        public Builder withSchema(final JsonSchema schema) {
            this.schema = schema;
            return this;
        }

        public UiSchema build() {
            final UiSchema uiSchema = new UiSchema();
            uiSchema.setKey(key);
            uiSchema.setTitle(title);
            uiSchema.setWidget(widget);
            uiSchema.setType(type);
            uiSchema.setItems(items);
            uiSchema.setOptions(options);
            uiSchema.setAutoFocus(autoFocus);
            uiSchema.setDisabled(disabled);
            uiSchema.setReadOnly(readOnly);
            uiSchema.setRequired(required);
            uiSchema.setRestricted(restricted);
            uiSchema.setPlaceholder(placeholder);
            uiSchema.setTriggers(triggers);
            uiSchema.setTitleMap(titleMap);
            uiSchema.setDescription(description);
            return uiSchema;
        }
    }
}
