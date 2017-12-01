/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.service;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ValidationParameterEnricher;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

@ApplicationScoped
public class PropertiesService {

    @Inject
    private PropertyValidationService propertyValidationService;

    public Stream<SimplePropertyDefinition> buildProperties(final Collection<ParameterMeta> meta,
            final ClassLoader loader, final Locale locale, final Object rootInstance) {
        return meta.stream().flatMap(p -> {
            final String path = sanitizePropertyName(p.getPath());
            final String name = sanitizePropertyName(p.getName());
            final String type = p.getType().name();
            PropertyValidation validation = propertyValidationService.map(p.getMetadata());
            if (p.getType() == ParameterMeta.Type.ENUM) {
                if (validation == null) {
                    validation = new PropertyValidation();
                }
                validation.setEnumValues(p.getProposals());
            }
            final Map<String, String> metadata = ofNullable(p.getMetadata()).map(m -> m.entrySet().stream()
                    .filter(e -> !e.getKey().startsWith(ValidationParameterEnricher.META_PREFIX))
                    .collect(toMap(e -> e.getKey().replace("tcomp::", ""), Map.Entry::getValue))).orElse(null);
            final Object instance = createDemoInstance(rootInstance, p);
            return Stream.concat(Stream.of(
                    new SimplePropertyDefinition(path, name, p.findBundle(loader, locale).displayName().orElse(name),
                            type, findDefault(instance, p), validation, metadata)),
                    buildProperties(p.getNestedParameters(), loader, locale, instance));
        }).sorted(Comparator.comparing(SimplePropertyDefinition::getPath)); // important cause it is the way you want to
                                                                            // see it
    }

    // for now we use instantiation to find the defaults assuming it will be cached
    // but we can move it later in design module to directly read it from the bytecode
    private Object createDemoInstance(final Object rootInstance, final ParameterMeta param) {
        if (rootInstance != null) {
            return findField(rootInstance, param);
        }

        final Type javaType = param.getJavaType();
        if (Class.class.isInstance(javaType)) {
            return tryCreatingObjectInstance(javaType);
        } else if (ParameterizedType.class.isInstance(javaType)) {
            final ParameterizedType pt = ParameterizedType.class.cast(javaType);
            final Type rawType = pt.getRawType();
            if (Class.class.isInstance(rawType) && Collection.class.isAssignableFrom(Class.class.cast(rawType))
                    && pt.getActualTypeArguments().length == 1
                    && Class.class.isInstance(pt.getActualTypeArguments()[0])) {
                return tryCreatingObjectInstance(pt.getActualTypeArguments()[0]);
            }
        }
        return null;
    }

    private Object findField(final Object rootInstance, final ParameterMeta param) {
        Class<?> current = rootInstance.getClass();
        while (current != null) {
            try {
                final Field declaredField = current.getDeclaredField(param.getName());
                if (!declaredField.isAccessible()) {
                    declaredField.setAccessible(true);
                }
                return declaredField.get(rootInstance);
            } catch (final IllegalAccessException | NoSuchFieldException e) {
                // next
            }
            current = current.getSuperclass();
        }
        throw new IllegalArgumentException("Didn't find field '" + param.getName() + "' in " + rootInstance);
    }

    private Object tryCreatingObjectInstance(final Type javaType) {
        final Class<?> type = Class.class.cast(javaType);
        if (type.isPrimitive()) {
            if (int.class == type) {
                return 0;
            }
            if (long.class == type) {
                return 0L;
            }
            if (double.class == type) {
                return 0.;
            }
            if (float.class == type) {
                return 0f;
            }
            if (short.class == type) {
                return (short) 0;
            }
            if (byte.class == type) {
                return (byte) 0;
            }
            if (boolean.class == type) {
                return false;
            }
            throw new IllegalArgumentException("Not a primitive: " + type);
        }
        if (type.getName().startsWith("java.") || type.getName().startsWith("javax.")) {
            return null;
        }
        try {
            return type.getConstructor().newInstance();
        } catch (final NoSuchMethodException | InstantiationException | IllegalAccessException
                | InvocationTargetException e) {
            // ignore, we'll skip the defaults
        }
        return null;
    }

    private String findDefault(final Object instance, final ParameterMeta param) {
        if (instance == null) {
            return null;
        }
        switch (param.getType()) {
        case OBJECT:
            return null;
        case ENUM:
            return Enum.class.cast(instance).name();
        case STRING:
        case NUMBER:
        case BOOLEAN:
            return String.valueOf(instance);
        case ARRAY:
            return ((Collection<Object>) instance).stream().map(String::valueOf).collect(joining(","));
        default:
            throw new IllegalArgumentException("Unsupported type: " + param.getType());
        }
    }

    private String sanitizePropertyName(final String path) {
        return path.replace("${index}", "");
    }
}
