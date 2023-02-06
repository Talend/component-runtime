/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.reflect.parameterenricher;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Locale.ENGLISH;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayouts;
import org.talend.sdk.component.api.configuration.ui.meta.Ui;
import org.talend.sdk.component.api.configuration.ui.widget.DateTime;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UiParameterEnricher extends BaseParameterEnricher {

    public static final String META_PREFIX = "tcomp::ui::";

    @Override
    public Map<Type, Collection<Annotation>> getImplicitAnnotationForTypes() {
        final Collection<Annotation> annotations = singletonList(new DateTimeAnnotation());
        return Stream
                .<Type> of(ZonedDateTime.class, LocalDateTime.class, LocalDate.class, LocalTime.class)
                .collect(toMap(identity(), it -> annotations));
    }

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName, final Type parameterType,
            final Annotation annotation) {
        final Ui condition = annotation.annotationType().getAnnotation(Ui.class);
        if (condition != null) {
            final String prefix = META_PREFIX + annotation.annotationType().getSimpleName().toLowerCase(ENGLISH) + "::";
            if (GridLayouts.class == annotation.annotationType()) {
                return Stream
                        .of(GridLayouts.class.cast(annotation).value())
                        .flatMap(a -> toConfig(a, prefix.substring(0, prefix.length() - 3) + "::").entrySet().stream())
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            if (DateTime.class == annotation.annotationType()) {
                final String key = META_PREFIX + "datetime";
                final DateTime dateTime = DateTime.class.cast(annotation);
                log.warn("[onParameterAnnotation] key: {} -- {} {} {}", key, dateTime.dateFormat(),
                        dateTime.useSeconds(), dateTime.useUTC());
                final Map<String, String> dtmap = new HashMap<>();
                if (parameterType == LocalTime.class) {
                    dtmap.put(key, "time");
                    dtmap.put(key + "::useSeconds", String.valueOf(dateTime.useSeconds()));
                    return dtmap;
                }
                if (parameterType == LocalDate.class) {
                    dtmap.put(key, "date");
                    dtmap.put(key + "::dateFormat", String.valueOf(dateTime.dateFormat()));
                    return dtmap;
                }
                if (parameterType == LocalDateTime.class) {
                    dtmap.put(key, "datetime");
                    dtmap.put(key + "::dateFormat", String.valueOf(dateTime.dateFormat()));
                    dtmap.put(key + "::useSeconds", String.valueOf(dateTime.useSeconds()));
                    dtmap.put(key + "::useUTC", String.valueOf(dateTime.useUTC()));
                    return dtmap;
                }
                if (parameterType == ZonedDateTime.class || parameterType == Object.class /* unsafe */) {
                    dtmap.put(key, "zoneddatetime");
                    dtmap.put(key + "::dateFormat", String.valueOf(dateTime.dateFormat()));
                    dtmap.put(key + "::useSeconds", String.valueOf(dateTime.useSeconds()));
                    dtmap.put(key + "::useUTC", String.valueOf(dateTime.useUTC()));
                    return dtmap;
                }
                throw new IllegalArgumentException(
                        "Unsupported type for @DateTime option: " + parameterType + " on " + parameterName);
            }
            return toConfig(annotation, prefix);
        }
        return emptyMap();
    }

    private Map<String, String> toConfig(final Annotation annotation, final String prefix) {
        if (GridLayout.class == annotation.annotationType()) {
            final GridLayout layout = GridLayout.class.cast(annotation);
            return Stream
                    .of(layout.names())
                    .flatMap(name -> Stream
                            .of(annotation.annotationType().getMethods())
                            .filter(m -> m.getDeclaringClass() == annotation.annotationType()
                                    && !"names".equals(m.getName()))
                            .collect(toMap(m -> prefix + name + "::" + m.getName(),
                                    m -> toString(annotation, m, invoke -> {
                                        if (invoke.getClass().isArray()) {
                                            final Class<?> component = invoke.getClass().getComponentType();
                                            if (!Annotation.class.isAssignableFrom(component)) {
                                                return null;
                                            }
                                            final int length = Array.getLength(invoke);
                                            if (length == 0) {
                                                return "";
                                            }
                                            final Collection<Method> mtds = Stream
                                                    .of(component.getMethods())
                                                    .filter(mtd -> mtd.getDeclaringClass() == component
                                                            && "value".equals(mtd.getName()))
                                                    .collect(toList());
                                            final StringBuilder builder = new StringBuilder("");
                                            for (int i = 0; i < length; i++) {
                                                final Object annot = Array.get(invoke, i);
                                                mtds
                                                        .forEach(p -> builder
                                                                .append(toString(Annotation.class.cast(annot), p,
                                                                        o -> null)));
                                                if (i + 1 < length) {
                                                    builder.append('|');
                                                }
                                            }
                                            return builder.toString();
                                        }
                                        return null;
                                    })))
                            .entrySet()
                            .stream())
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        final Map<String, String> config = Stream
                .of(annotation.annotationType().getMethods())
                .filter(m -> m.getDeclaringClass() == annotation.annotationType())
                .collect(toMap(m -> prefix + m.getName(), m -> toString(annotation, m, invoke -> null)));
        return config.isEmpty() ? singletonMap(prefix.substring(0, prefix.length() - "::".length()), "true") : config;
    }

    private String toString(final Annotation annotation, final Method m,
            final Function<Object, String> customConversions) {
        try {
            final Object invoke = m.invoke(annotation);
            final String custom = customConversions.apply(invoke);
            if (custom != null) {
                return custom;
            }
            if (String.class.isInstance(invoke)) {
                final String string = String.valueOf(invoke);
                if (string.startsWith("local_configuration:")) {
                    return getContext()
                            .map(context -> context
                                    .getConfiguration()
                                    .get(string.substring("local_configuration:".length())))
                            .orElse(string);
                }
                return string;
            }
            if (Class.class.isInstance(invoke)) {
                return Class.class.cast(invoke).getSimpleName().toLowerCase(ENGLISH);
            }
            if (String[].class.isInstance(invoke)) {
                return Stream.of(String[].class.cast(invoke)).collect(joining(","));
            }
            return String.valueOf(invoke);
        } catch (final InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Data
    private static class DateTimeAnnotation implements DateTime {

        public String dateFormat = "YYYY/MM/DD";

        boolean useSeconds = true;

        boolean useUTC = true;

        @Override
        public Class<? extends Annotation> annotationType() {
            return DateTime.class;
        }

        @Override
        public String dateFormat() {
            return dateFormat;
        }

        @Override
        public boolean useSeconds() {
            return useSeconds;
        }

        @Override
        public boolean useUTC() {
            return useUTC;
        }
    }
}
