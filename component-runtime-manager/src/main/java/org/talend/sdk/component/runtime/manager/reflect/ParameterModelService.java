/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.manager.reflect;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.spi.parameter.ParameterExtensionEnricher;

public class ParameterModelService {

    private final Collection<ParameterExtensionEnricher> enrichers;

    public ParameterModelService() {
        enrichers =
            StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(
                    ServiceLoader.load(ParameterExtensionEnricher.class).iterator(), Spliterator.IMMUTABLE), false)
                .collect(toList());
    }

    public List<ParameterMeta> buildParameterMetas(final Executable executable, final String i18nPackage) {
        return Stream.of(executable.getParameters()).filter(p -> !p.getType().isAnnotationPresent(Service.class)
            && !p.getType().getName().startsWith("org.talend.sdk.component.api.service.")).map(parameter -> {
                final String name = findName(parameter);
                return buildParameter(name, name, parameter.getParameterizedType(),
                    parameter.getType().getAnnotations(), i18nPackage);
            }).collect(toList());
    }

    protected ParameterMeta buildParameter(final String name, final String prefix, final Type genericType,
        final Annotation[] annotations, final String i18nPackage) {
        final ParameterMeta.Type type = findType(genericType);
        final String normalizedPrefix = prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;
        final List<ParameterMeta> nested = new ArrayList<>();
        final List<String> proposals = new ArrayList<>();
        switch (type) {
        case OBJECT:
            final List<ParameterMeta> meta =
                buildParametersMetas(name, normalizedPrefix + ".", genericType, annotations, i18nPackage);
            meta.sort(Comparator.comparing(ParameterMeta::getName));
            nested.addAll(meta);
            break;
        case ARRAY:
            nested.addAll(buildParametersMetas(name + "[${index}]", normalizedPrefix + "[${index}].",
                Class.class.isInstance(genericType) && Class.class.cast(genericType).isArray()
                    ? Class.class.cast(genericType).getComponentType()
                    : ParameterizedType.class.cast(genericType).getActualTypeArguments()[0],
                annotations, i18nPackage));
            break;
        case ENUM:
            proposals.addAll(Stream.of(((Class<? extends Enum<?>>) genericType).getEnumConstants()).map(Enum::name)
                .sorted().collect(toList()));
            break;
        default:
        }
        // don't sort here to ensure we don't mess up parameter method ordering
        return new ParameterMeta(genericType, type, normalizedPrefix, name, i18nPackage, nested, proposals,
            buildExtensions(name, genericType, annotations));
    }

    private Map<String, String> buildExtensions(final String name, final Type genericType,
        final Annotation[] annotations) {
        return Stream.concat(Stream.of(annotations), Class.class.isInstance(genericType) // if a class concat its
                                                                                         // annotations
            ? Stream.of(Class.class.cast(genericType).getAnnotations())
            : (ParameterizedType.class.isInstance(genericType) // if a list concat the item type annotations
                && ParameterizedType.class.cast(genericType).getActualTypeArguments().length == 1
                && Class.class.isInstance(ParameterizedType.class.cast(genericType).getActualTypeArguments()[0])
                    ? Stream.of(Class.class.cast(ParameterizedType.class.cast(genericType).getActualTypeArguments()[0])
                        .getAnnotations())
                    : Stream.empty()))
            .distinct().flatMap(a -> enrichers.stream().map(e -> e.onParameterAnnotation(name, genericType, a)))
            .flatMap(map -> map.entrySet().stream()).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<ParameterMeta> buildParametersMetas(final String name, final String prefix, final Type type,
        final Annotation[] annotations, final String i18nPackage) {
        if (ParameterizedType.class.isInstance(type)) {
            final ParameterizedType pt = ParameterizedType.class.cast(type);
            if (!Class.class.isInstance(pt.getRawType())) {
                throw new IllegalArgumentException("Unsupported raw type in ParameterizedType parameter: " + pt);
            }
            final Class<?> raw = Class.class.cast(pt.getRawType());
            if (Collection.class.isAssignableFrom(raw)) {
                if (!Class.class.isInstance(pt.getActualTypeArguments()[0])) {
                    throw new IllegalArgumentException(
                        "Unsupported list: " + pt + ", ensure to use a concrete class as generic");
                }
                return buildParametersMetas(name, prefix, Class.class.cast(type), annotations, i18nPackage);
            }
            if (Map.class.isAssignableFrom(raw)) {
                if (!Class.class.isInstance(pt.getActualTypeArguments()[0])
                    || !Class.class.isInstance(pt.getActualTypeArguments()[1])) {
                    throw new IllegalArgumentException(
                        "Unsupported map: " + pt + ", ensure to use a concrete class as generics");
                }
                return Stream
                    .concat(
                        buildParametersMetas(name + ".key[${index}]", prefix + "key[${index}].",
                            Class.class.cast(pt.getActualTypeArguments()[0]), annotations, i18nPackage).stream(),
                        buildParametersMetas(name + ".value[${index}]", prefix + "value[${index}].",
                            Class.class.cast(pt.getActualTypeArguments()[1]), annotations, i18nPackage).stream())
                    .collect(toList());
            }
        }
        if (Class.class.isInstance(type)) {
            switch (findType(type)) {
            case ENUM:
            case STRING:
            case NUMBER:
            case BOOLEAN:
                return singletonList(buildParameter(name, prefix, type, annotations, i18nPackage));
            default:
            }
            return buildObjectParameters(prefix, Class.class.cast(type), i18nPackage);
        }
        throw new IllegalArgumentException("Unsupported parameter type: " + type);
    }

    private List<ParameterMeta> buildObjectParameters(final String prefix, final Class type, final String i18nPackage) {
        final Map<String, Field> fields = new HashMap<>();
        final List<ParameterMeta> out = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            out.addAll(Stream.of(current.getDeclaredFields())
                .filter(f -> !"$jacocoData".equals(f.getName()) && (f.getModifiers() & 0x00001000/* SYNTHETIC */) == 0)
                .filter(f -> fields.putIfAbsent(f.getName(), f) == null).map(f -> {
                    final String path = prefix + f.getName();
                    return buildParameter(f.getName(), path + ".", f.getGenericType(), f.getAnnotations(), i18nPackage);
                }).collect(toList()));
            current = current.getSuperclass();
        }
        return out;
    }

    public String findName(final Parameter parameter) {
        return ofNullable(parameter.getAnnotation(Option.class)).map(Option::value).filter(v -> !v.isEmpty())
            .orElseGet(parameter::getName);
    }

    private ParameterMeta.Type findType(final Type type) {
        if (Class.class.isInstance(type)) {
            final Class<?> clazz = Class.class.cast(type);

            // we handled char before so we only have numbers now for primitives
            if (Primitives.unwrap(clazz) == boolean.class) {
                return ParameterMeta.Type.BOOLEAN;
            }
            if (clazz.isPrimitive() || Primitives.unwrap(clazz) != clazz) {
                return ParameterMeta.Type.NUMBER;
            }

            if (clazz.isEnum()) {
                return ParameterMeta.Type.ENUM;
            }
            if (clazz.isArray()) {
                return ParameterMeta.Type.ARRAY;
            }
        }
        if (ParameterizedType.class.isInstance(type)) {
            final ParameterizedType pt = ParameterizedType.class.cast(type);
            if (Class.class.isInstance(pt.getRawType())) {
                final Class<?> raw = Class.class.cast(pt.getRawType());
                if (Collection.class.isAssignableFrom(raw)) {
                    return ParameterMeta.Type.ARRAY;
                }
                if (Map.class.isAssignableFrom(raw)) {
                    return ParameterMeta.Type.OBJECT;
                }
            }
            throw new IllegalArgumentException("Unsupported type: " + pt);
        }
        if (StringCompatibleTypes.isKnown(type)) { // flatten the config as a string
            return ParameterMeta.Type.STRING;
        }
        return ParameterMeta.Type.OBJECT;
    }
}
