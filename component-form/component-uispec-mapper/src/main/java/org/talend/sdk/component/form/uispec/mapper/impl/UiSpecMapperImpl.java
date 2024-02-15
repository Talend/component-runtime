/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.form.uispec.mapper.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.form.uispec.mapper.api.model.View;
import org.talend.sdk.component.form.uispec.mapper.api.provider.TitleMapProvider;
import org.talend.sdk.component.form.uispec.mapper.api.service.UiSpecMapper;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UiSpecMapperImpl implements UiSpecMapper {

    private final Configuration configuration;

    private final ConcurrentMap<Key, Supplier<Ui>> forms = new ConcurrentHashMap<>();

    @Override
    public Supplier<Ui> createFormFor(final Class<?> clazz) {
        return doCreateForm(clazz);
    }

    private Supplier<Ui> doCreateForm(final Class<?> clazz) {
        return forms.computeIfAbsent(new Key(clazz), key -> {
            final AtomicBoolean isDynamic = new AtomicBoolean(false);

            final Ui ui;
            if (clazz.isAnnotationPresent(View.class)) {
                ui = generateUi(clazz.getAnnotation(View.class).value(), isDynamic);
            } else {
                ui = generateUi(clazz, isDynamic);
            }

            if (isDynamic.get()) {
                return () -> Ui
                        .ui()
                        .withJsonSchema(ui.getJsonSchema())
                        .withUiSchema(ui.getUiSchema().stream().map(it -> it.copy(true)).collect(toList()))
                        .withProperties(emptyMap())
                        .build();
            }
            return () -> ui;
        });
    }

    private Ui generateUi(final Class<?> clazz, final AtomicBoolean isDynamic) {
        final Ui ui = new Ui();
        ui.setJsonSchema(generateJsonSchema(clazz, clazz, null, null));
        ui.setUiSchema(singletonList(generateUiSchemas("", clazz, clazz, isDynamic)));
        return ui;
    }

    private UiSchema generateUiSchemas(final String keyPrefix, final AnnotatedElement element, final Class<?> clazz,
            final AtomicBoolean isDynamic) {
        if (clazz == boolean.class) {
            final UiSchema.Builder builder = new UiSchema.Builder();
            builder.withKey(keyPrefix);
            builder.withWidget("checkbox");
            applyConfig(element, builder, isDynamic);
            return builder.build();
        }
        if (isText(clazz) || hasReference(element)) {
            final UiSchema.Builder builder = new UiSchema.Builder();
            builder.withKey(keyPrefix);
            builder.withWidget("text");
            applyConfig(element, builder, isDynamic);
            return builder.build();
        }
        if (isNumber(clazz)) {
            final UiSchema.Builder builder = new UiSchema.Builder();
            builder.withKey(keyPrefix);
            builder.withWidget("text");
            builder.withType("number");
            applyConfig(element, builder, isDynamic);
            return builder.build();
        }
        if (clazz.isPrimitive()) {
            throw new IllegalArgumentException("Unsupported (yet) type: " + clazz);
        }

        final List<UiSchema> properties = new ArrayList<>();
        final Map<String, Integer> positions = new HashMap<>();
        final UiSchema.Builder builder = new UiSchema.Builder();
        builder.withKey(keyPrefix.isEmpty() ? null : keyPrefix).withWidget("fieldset");

        Class<?> current = clazz;
        while (current != Object.class && current != null) {
            properties.addAll(Stream.of(current.getDeclaredFields()).filter(this::isIncluded).map(it -> {
                final String nextKey = keyPrefix + (keyPrefix.isEmpty() ? "" : ".") + it.getName();
                final int pos = ofNullable(it.getAnnotation(View.Schema.class)).map(View.Schema::position).orElse(-1);
                positions.put(it.getName(), pos < 0 ? Integer.MAX_VALUE : pos);
                return generateUiSchemas(nextKey, it, it.getType(), isDynamic);
            }).collect(toList()));
            current = current.getSuperclass();
        }
        return applyConfig(element, builder, isDynamic)
                .withItems(ofNullable(element.getAnnotation(View.Schema.class))
                        .map(View.Schema::order)
                        .filter(order -> order.length > 0)
                        .map(order -> {
                            final List<String> orderIdx = asList(order);
                            properties.sort(comparingInt((final UiSchema a) -> {
                                final int idx = orderIdx.indexOf(keyToName(a.getKey()));
                                return idx < 0 ? Integer.MAX_VALUE : idx;
                            }).thenComparing(UiSchema::getKey));
                            return properties;
                        })
                        .orElseGet(() -> {
                            properties
                                    .sort(comparingInt((final UiSchema a) -> positions.get(keyToName(a.getKey())))
                                            .thenComparing(UiSchema::getKey));
                            return properties;
                        }))
                .build();
    }

    private boolean hasReference(final AnnotatedElement element) {
        return ofNullable(element.getAnnotation(View.Schema.class))
                .map(View.Schema::reference)
                .map(it -> !"".equals(it))
                .orElse(false);
    }

    private String keyToName(final String key) {
        return key.substring(key.lastIndexOf('.') + 1);
    }

    private UiSchema.Builder applyConfig(final AnnotatedElement element, final UiSchema.Builder builder,
            final AtomicBoolean isDynamic) {
        ofNullable(element.getAnnotation(View.Schema.class)).ifPresent(config -> {
            final String type = config.type();
            if (!type.isEmpty()) {
                builder.withType(type);
            }

            final String title = config.title();
            if (!title.isEmpty()) {
                builder.withTitle(title);
            }

            final String widget = config.widget();
            if (!widget.isEmpty()) {
                builder.withWidget(widget);
            }

            if (config.readOnly()) {
                builder.withReadOnly(true);
            }

            final String refModel = config.reference();
            if (!"".equals(refModel)) {
                if (widget.isEmpty()) {
                    builder.withWidget("datalist");
                }
                isDynamic.set(true);
                builder.withCopyCallback(schema -> schema.setTitleMap(findTitleMap(refModel)));
            }
        });
        return builder;
    }

    protected Collection<UiSchema.NameValue> findTitleMap(final String refModel) {
        return ofNullable(configuration.getTitleMapProviders())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .filter(it -> refModel.equals(it.reference()))
                .min(comparing(TitleMapProvider::ordinal))
                .map(TitleMapProvider::get)
                .orElseGet(() -> {
                    log.warn("No TitleMapProvider for reference '{}'", refModel);
                    return emptyList();
                });
    }

    private JsonSchema generateJsonSchema(final AnnotatedElement element, final Class<?> clazz, final JsonSchema parent,
            final String name) {
        if (clazz == boolean.class) {
            final JsonSchema.Builder builder = JsonSchema.jsonSchema();
            Stream
                    .of(element.getAnnotationsByType(View.Schema.class))
                    .forEach(schema -> applyConfig(parent, name, builder, schema));
            return builder.withType("boolean").withDefaultValue(false).build();
        }
        if (isText(clazz)) {
            final JsonSchema.Builder builder = JsonSchema.jsonSchema();
            Stream
                    .of(element.getAnnotationsByType(View.Schema.class))
                    .forEach(schema -> applyConfig(parent, name, builder, schema));
            return builder.withType("string").build();
        }
        if (isNumber(clazz)) {
            final JsonSchema.Builder builder = JsonSchema.jsonSchema().withType("number");
            ofNullable(element.getAnnotation(View.Schema.class))
                    .ifPresent(schema -> applyConfig(parent, name, builder, schema));
            return builder.build();
        }
        if (clazz.isPrimitive()) {
            throw new IllegalArgumentException("Unsupported (yet) type: " + clazz);
        }

        final JsonSchema.Builder builder = JsonSchema.jsonSchema().withProperties(new LinkedHashMap<>());
        ofNullable(element.getAnnotation(View.Schema.class))
                .ifPresent(schema -> applyConfig(parent, name, builder, schema));
        final JsonSchema schema = builder.build();
        final Map<String, JsonSchema> properties = schema.getProperties();

        Class<?> current = clazz;
        while (current != Object.class && current != null) {
            final Map<String, JsonSchema> collected = new TreeMap<>(Stream
                    .of(current.getDeclaredFields())
                    .filter(this::isIncluded)
                    .collect(toMap(Field::getName, it -> generateJsonSchema(it, it.getType(), schema, it.getName()))));
            properties.putAll(collected);
            current = current.getSuperclass();
        }
        return schema;
    }

    private void applyConfig(final JsonSchema parent, final String name, final JsonSchema.Builder builder,
            final View.Schema schema) {
        final int maxLength = schema.length();
        if (maxLength >= 0) {
            builder.withMaxLength(maxLength);
        }

        if (schema.required() && parent != null) {
            if (parent.getRequired() == null) {
                parent.setRequired(new ArrayList<>());
            }
            parent.getRequired().add(name);
        }

        final String pattern = schema.pattern();
        if (!pattern.isEmpty()) {
            builder.withPattern(pattern);
        }
    }

    private boolean isNumber(final Class<?> clazz) {
        return clazz == long.class || clazz == int.class;
    }

    private boolean isText(final Class<?> clazz) {
        return clazz == String.class || clazz == Date.class;
    }

    private boolean isIncluded(final Field field) {
        // parameterized type are relationships -> specific pages
        return !"$jacocoData".equals(field.getName()) && !field.isAnnotationPresent(View.Skip.class)
                && Class.class.isInstance(field.getGenericType());
    }

    private static class Key {

        private final Class<?> model;

        private final int hash;

        private Key(final Class<?> model) {
            this.model = model;
            this.hash = Objects.hash(model);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Key key = Key.class.cast(o);
            return model.equals(key.model);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    @Data
    public static class Configuration {

        private final Collection<TitleMapProvider> titleMapProviders;
    }
}
