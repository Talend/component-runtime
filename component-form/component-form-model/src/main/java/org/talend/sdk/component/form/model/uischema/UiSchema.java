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
package org.talend.sdk.component.form.model.uischema;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.json.bind.annotation.JsonbTransient;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class UiSchema {

    private String key;

    private String title;

    private String widget;

    private String itemWidget;

    private String type;

    private String description;

    private String tooltip;

    private Collection<UiSchema> items;

    private Map<String, Object> options;

    private Boolean autoFocus;

    private Boolean disabled;

    private Boolean readOnly;

    private Boolean required; // warn: the uischema parsing handles it but it is preferred to use jsonSchema syntax

    private Boolean restricted;

    private String placeholder;

    private Collection<Trigger> triggers;

    private Collection<? extends TitleMapContent> titleMap;

    private Map<String, Collection<Object>> condition;

    @JsonbTransient
    private Consumer<UiSchema> onCopyCallback;

    @JsonbTransient
    private AtomicReference<Boolean> isStatic = new AtomicReference<>();

    /**
     * IMPORTANT: frozenStructure is propagated in items copy.
     *
     * @param frozenStructure this structure can be assumed immutable.
     * @return a copy of current structure updated with related callback if it exists.
     */
    public UiSchema copy(final boolean frozenStructure) {
        if (frozenStructure && hasNoCallback()) {
            return this;
        }

        final UiSchema copy = UiSchema
                .uiSchema()
                .withKey(key)
                .withTitle(title)
                .withWidget(widget)
                .withItemWidget(itemWidget)
                .withType(type)
                .withItems(items == null ? null : items.stream().map(it -> it.copy(frozenStructure)).collect(toList()))
                .withOptions(options)
                .withAutoFocus(autoFocus)
                .withDisabled(disabled)
                .withReadOnly(readOnly)
                .withRequired(required)
                .withRestricted(restricted)
                .withPlaceholder(placeholder)
                .withTriggers(triggers)
                .withTitleMap(titleMap)
                .withDescription(description)
                .withTooltip(tooltip)
                .withCondition(condition)
                .build();
        if (onCopyCallback != null) {
            onCopyCallback.accept(copy);
        }
        return copy;
    }

    private boolean hasNoCallback() {
        Boolean hasNoCallback = isStatic.get();
        if (hasNoCallback == null) {
            hasNoCallback =
                    onCopyCallback == null && (items == null || items.stream().noneMatch(UiSchema::hasNoCallback));
            isStatic.compareAndSet(null, hasNoCallback);
        }
        return hasNoCallback;
    }

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

    public static ConditionBuilder condition() {
        return new ConditionBuilder();
    }

    public static final class ConditionBuilder {

        private final Map<String, Collection<Object>> values = new LinkedHashMap<>();

        public ConditionValuesBuilder withOperator(final String operator) {
            return new ConditionValuesBuilder(this, operator);
        }

        public Map<String, Collection<Object>> build() {
            if (values.isEmpty()) {
                throw new IllegalArgumentException("Empty condition, did you think about calling up()?");
            }
            return unmodifiableMap(values);
        }
    }

    @AllArgsConstructor
    public static final class ConditionValuesBuilder {

        private final ConditionBuilder builder;

        private final String operator;

        private final Collection<Object> values = new ArrayList<>(2);

        public <T> ConditionValuesBuilder withVar(final String var) {
            values.add(singletonMap("var", var));
            return this;
        }

        public <T> ConditionValuesBuilder withValues(final Collection<T> values) {
            this.values.addAll(values);
            return this;
        }

        public <T> ConditionValuesBuilder withValue(final T value) {
            values.add(value);
            return this;
        }

        public ConditionBuilder up() {
            if (values.isEmpty()) {
                throw new IllegalArgumentException("No value set, call withValue()");
            }
            builder.values.put(operator, values);
            return builder;
        }

        public Map<String, Collection<Object>> build() {
            return up().build();
        }
    }

    public interface TitleMapContent {

    }

    @Data
    public static class TitledNameValue implements TitleMapContent {

        private String title;

        private Collection<NameValue> suggestions;

        public static final class Builder {

            private String title;

            private Collection<NameValue> suggestions;

            public TitledNameValue.Builder withTitle(final String title) {
                this.title = title;
                return this;
            }

            public TitledNameValue.Builder withSuggestions(final Collection<NameValue> nameValue) {
                this.suggestions = nameValue;
                return this;
            }

            public TitledNameValue build() {
                final TitledNameValue titledNameValue = new TitledNameValue();
                titledNameValue.setTitle(title);
                titledNameValue.setSuggestions(suggestions);
                return titledNameValue;
            }

        }

    }

    @Data
    public static class NameValue implements TitleMapContent {

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

        private Boolean remote;

        private Collection<Option> options;

        private Collection<Parameter> parameters;

        public static final class Builder {

            private String action;

            private String family;

            private String type;

            private String onEvent;

            private Boolean remote;

            private Collection<Option> options;

            private Collection<Parameter> parameters;

            public Builder withRemote(final boolean remote) {
                this.remote = remote;
                return this;
            }

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
                final Parameter parameter = parameter().withKey(key).withPath(path).build();
                this.parameters.add(parameter);
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
                parameter.setRemote(remote == null || remote);
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

        private String tooltip;

        private Collection<UiSchema> items;

        private Map<String, Object> options;

        private Boolean autoFocus;

        private Boolean disabled;

        private Boolean readOnly;

        private Boolean required;

        private Boolean restricted;

        private String placeholder;

        private Collection<Trigger> triggers;

        private Collection<? extends TitleMapContent> titleMap;

        private Map<String, Collection<Object>> condition;

        private Consumer<UiSchema> copyCallback;

        public Builder withCondition(final Map<String, Collection<Object>> condition) {
            if (this.condition != null) {
                throw new IllegalStateException("conditions already set");
            }
            this.condition = condition;
            return this;
        }

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

        public Builder withTooltip(final String tooltip) {
            this.tooltip = tooltip;
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
            if (items == null) {
                return this;
            }
            if (this.items == null) {
                this.items = new ArrayList<>();
            }
            this.items.addAll(items);
            return this;
        }

        public Builder withItems(final UiSchema... items) {
            if (items == null) {
                return this;
            }
            return withItems(asList(items));
        }

        public Builder withOptions(final String name, final String value) {
            if (options == null) {
                return this;
            }
            if (this.options == null) {
                this.options = new LinkedHashMap<>();
            }
            this.options.put(name, value);
            return this;
        }

        public Builder withOptions(final Map<String, ?> options) {
            if (options == null) {
                return this;
            }
            if (this.options == null) {
                this.options = new LinkedHashMap<>();
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
            if (triggers == null) {
                return this;
            }
            return withTriggers(asList(triggers));
        }

        public Builder withTriggers(final Collection<Trigger> triggers) {
            if (triggers == null) {
                return this;
            }
            if (this.triggers == null) {
                this.triggers = new ArrayList<>();
            }
            this.triggers.addAll(triggers);
            return this;
        }

        public Builder withTitleMap(final NameValue... titleMap) {
            if (titleMap == null) {
                return this;
            }
            return withTitleMap(asList(titleMap));
        }

        public Builder withTitleMap(final Collection<? extends TitleMapContent> titleMap) {
            if (titleMap == null) {
                return this;
            }
            this.titleMap = new ArrayList<>(titleMap);
            return this;
        }

        public Builder withCopyCallback(final Consumer<UiSchema> copyCallback) {
            this.copyCallback = copyCallback;
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
            uiSchema.setTooltip(tooltip);
            uiSchema.setItemWidget(itemWidget);
            uiSchema.setCondition(condition);
            uiSchema.setOnCopyCallback(copyCallback);
            return uiSchema;
        }
    }
}
