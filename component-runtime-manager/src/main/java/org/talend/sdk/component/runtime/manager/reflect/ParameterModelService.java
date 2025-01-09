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
package org.talend.sdk.component.runtime.manager.reflect;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.constraint.Max;
import org.talend.sdk.component.api.configuration.constraint.Min;
import org.talend.sdk.component.api.configuration.ui.widget.DateTime;
import org.talend.sdk.component.api.internationalization.Internationalized;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.configuration.Configuration;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.spi.parameter.ParameterExtensionEnricher;

public class ParameterModelService {

    private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

    private final Collection<ParameterExtensionEnricher> enrichers;

    private final PropertyEditorRegistry registry;

    private final Map<Type, Collection<Annotation>> implicitAnnotationsMapping;

    protected ParameterModelService(final Collection<ParameterExtensionEnricher> enrichers,
            final PropertyEditorRegistry registry) {
        this.enrichers = enrichers;
        this.registry = registry;
        this.implicitAnnotationsMapping = enrichers
                .stream()
                .flatMap(enricher -> enricher.getImplicitAnnotationForTypes().entrySet().stream())
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(toSet())));
    }

    public ParameterModelService(final PropertyEditorRegistry registry) {
        this(StreamSupport
                .stream(Spliterators
                        .spliteratorUnknownSize(ServiceLoader.load(ParameterExtensionEnricher.class).iterator(),
                                Spliterator.IMMUTABLE),
                        false)
                .collect(toList()), registry);
    }

    public boolean isService(final Param parameter) {
        final Class<?> type;
        if (Class.class.isInstance(parameter.type)) {
            type = Class.class.cast(parameter.type);
        } else if (ParameterizedType.class.isInstance(parameter.type)) {
            final Type rawType = ParameterizedType.class.cast(parameter.type).getRawType();
            if (Class.class.isInstance(rawType)) {
                type = Class.class.cast(rawType);
            } else {
                return false;
            }
        } else {
            return false;
        }
        return !parameter.isAnnotationPresent(Option.class) && (type.isAnnotationPresent(Service.class)
                || parameter.isAnnotationPresent(Configuration.class)
                || type.isAnnotationPresent(Internationalized.class)
                || Stream.of(type.getMethods()).anyMatch(m -> m.isAnnotationPresent(Request.class))
                || (type.getName().startsWith("org.talend.sdk.component.") && type.getName().contains(".service."))
                || type.getName().startsWith("javax."));
    }

    public List<ParameterMeta> buildParameterMetas(final Stream<Param> parameters, final Class<?> declaringClass,
            final String i18nPackage, final boolean ignoreI18n, final BaseParameterEnricher.Context context) {
        return parameters.filter(p -> !isService(p)).map(parameter -> {
            final String name = findName(parameter, parameter.name);
            return buildParameter(name, name, new ParameterMeta.Source() {

                @Override
                public String name() {
                    return parameter.name;
                }

                @Override
                public Class<?> declaringClass() {
                    return declaringClass;
                }
            }, parameter.type,
                    Stream
                            .concat(extractTypeAnnotation(parameter), Stream.of(parameter.getAnnotations()))
                            .distinct()
                            .toArray(Annotation[]::new),
                    new ArrayList<>(singletonList(i18nPackage)), ignoreI18n, context);
        }).collect(toList());
    }

    private Stream<Annotation> extractTypeAnnotation(final Param parameter) {
        if (Class.class.isInstance(parameter.type)) {
            return Stream.of(Class.class.cast(parameter.type).getAnnotations());
        }
        if (ParameterizedType.class.isInstance(parameter.type)) {
            final ParameterizedType parameterizedType = ParameterizedType.class.cast(parameter.type);
            if (Class.class.isInstance(parameterizedType.getRawType())) {
                return Stream.of(Class.class.cast(parameterizedType.getRawType()).getAnnotations());
            }
        }
        return Stream.empty();
    }

    private List<ParameterMeta> doBuildParameterMetas(final Executable executable, final String i18nPackage,
            final boolean ignoreI18n, final BaseParameterEnricher.Context context) {
        return buildParameterMetas(Stream.of(executable.getParameters()).map(Param::new),
                executable.getDeclaringClass(), i18nPackage, ignoreI18n, context);
    }

    public List<ParameterMeta> buildServiceParameterMetas(final Executable executable, final String i18nPackage,
            final BaseParameterEnricher.Context context) {
        return doBuildParameterMetas(executable, i18nPackage, true, context);
    }

    public List<ParameterMeta> buildParameterMetas(final Executable executable, final String i18nPackage,
            final BaseParameterEnricher.Context context) {
        return doBuildParameterMetas(executable, i18nPackage, false, context);
    }

    protected ParameterMeta buildParameter(final String name, final String prefix, final ParameterMeta.Source source,
            final Type genericType, final Annotation[] annotations, final Collection<String> i18nPackages,
            final boolean ignoreI18n, final BaseParameterEnricher.Context context) {
        final ParameterMeta.Type type = findType(genericType);
        final String normalizedPrefix = prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;
        final List<ParameterMeta> nested = new ArrayList<>();
        final List<String> proposals = new ArrayList<>();
        switch (type) {
            case OBJECT:
                addI18nPackageIfPossible(i18nPackages, genericType);
                final List<ParameterMeta> meta = buildParametersMetas(name, normalizedPrefix + ".", genericType,
                        annotations, i18nPackages, ignoreI18n, context);
                meta.sort(Comparator.comparing(ParameterMeta::getName));
                nested.addAll(meta);
                break;
            case ARRAY:
                final Type nestedType = Class.class.isInstance(genericType) && Class.class.cast(genericType).isArray()
                        ? Class.class.cast(genericType).getComponentType()
                        : ParameterizedType.class.cast(genericType).getActualTypeArguments()[0];
                addI18nPackageIfPossible(i18nPackages, nestedType);
                nested
                        .addAll(buildParametersMetas(name + "[${index}]", normalizedPrefix + "[${index}].", nestedType,
                                Class.class.isInstance(nestedType) ? Class.class.cast(nestedType).getAnnotations()
                                        : NO_ANNOTATIONS,
                                i18nPackages, ignoreI18n, context));
                break;
            case ENUM:
                addI18nPackageIfPossible(i18nPackages, genericType);
                proposals
                        .addAll(Stream
                                .of(((Class<? extends Enum<?>>) genericType).getEnumConstants())
                                .map(Enum::name)
                                // sorted() // don't sort, let the dev use the order he wants
                                .collect(toList()));
                break;
            default:
        }
        // don't sort here to ensure we don't mess up parameter method ordering
        return new ParameterMeta(source, genericType, type, normalizedPrefix, name,
                i18nPackages.toArray(new String[i18nPackages.size()]), nested, proposals,
                buildExtensions(name, genericType, annotations, context), false);
    }

    private void addI18nPackageIfPossible(final Collection<String> i18nPackages, final Type type) {
        if (Class.class.isInstance(type)) {
            final Package typePck = Class.class.cast(type).getPackage();
            if (typePck != null && !typePck.getName().isEmpty() && !i18nPackages.contains(typePck.getName())) {
                i18nPackages.add(typePck.getName());
            }
        }
    }

    private Map<String, String> buildExtensions(final String name, final Type genericType,
            final Annotation[] annotations, final BaseParameterEnricher.Context context) {
        return getAnnotations(genericType, annotations).distinct().flatMap(a -> enrichers.stream().map(e -> {
            if (BaseParameterEnricher.class.isInstance(e)) {
                final BaseParameterEnricher bpe = BaseParameterEnricher.class.cast(e);
                return bpe.withContext(context, () -> bpe.onParameterAnnotation(name, genericType, a));
            }
            return e.onParameterAnnotation(name, genericType, a);
        })).flatMap(map -> map.entrySet().stream()).collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
            if (a.equals(b)) {
                return a;
            }
            throw new IllegalArgumentException("Ambiguous metadata: '" + a + "'/'" + b + "'");
        }));
    }

    private Stream<Annotation> getAnnotations(final Type type, final Annotation[] annotations) {
        // we have a few constraints that are defined by implicit annotations. those constraints can be overwritten by
        // explicit annotations.
        // to avoid ambiguous exception we should filter out constraints that are provided by implicit annotation
        final Set<Class<? extends Annotation>> overwrittableAnnotations =
                new HashSet<>(Arrays.asList(Min.class, Max.class, DateTime.class));
        final Set<? extends Class<? extends Annotation>> skipImplicit = Stream.of(annotations)
                .map(Annotation::annotationType)
                .filter(overwrittableAnnotations::contains)
                .collect(toSet());
        return Stream
                .concat(getReflectionAnnotations(type, annotations),
                        implicitAnnotationsMapping.getOrDefault(type, emptyList())
                                .stream()
                                .filter(it -> !skipImplicit.contains(it.annotationType())));
    }

    private Stream<Annotation> getReflectionAnnotations(final Type genericType, final Annotation[] annotations) {
        return Stream
                .concat(Stream.of(annotations),
                        // if a class concat its annotations
                        Class.class
                                .isInstance(genericType)
                                        ? getClassAnnotations(genericType, annotations)
                                        : (hasAClassFirstParameter(genericType) ? getClassAnnotations(
                                                ParameterizedType.class.cast(genericType).getActualTypeArguments()[0],
                                                annotations) : Stream.empty()));
    }

    private boolean hasAClassFirstParameter(final Type genericType) {
        return ParameterizedType.class.isInstance(genericType) // if a list concat the item type annotations
                && ParameterizedType.class.cast(genericType).getActualTypeArguments().length == 1
                && Class.class.isInstance(ParameterizedType.class.cast(genericType).getActualTypeArguments()[0]);
    }

    private Stream<Annotation> getClassAnnotations(final Type genericType, final Annotation[] annotations) {
        return Stream
                .of(Class.class.cast(genericType).getAnnotations())
                .filter(a -> Stream.of(annotations).noneMatch(o -> o.annotationType() == a.annotationType()));
    }

    private List<ParameterMeta> buildParametersMetas(final String name, final String prefix, final Type type,
            final Annotation[] annotations, final Collection<String> i18nPackages, final boolean ignoreI18n,
            final BaseParameterEnricher.Context context) {
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
                return buildParametersMetas(name, prefix, Class.class.cast(type), annotations, i18nPackages, ignoreI18n,
                        context);
            }
            if (Map.class.isAssignableFrom(raw)) {
                if (!Class.class.isInstance(pt.getActualTypeArguments()[0])
                        || !Class.class.isInstance(pt.getActualTypeArguments()[1])) {
                    throw new IllegalArgumentException(
                            "Unsupported map: " + pt + ", ensure to use a concrete class as generics");
                }
                return Stream
                        .concat(buildParametersMetas(name + ".key[${index}]", prefix + "key[${index}].",
                                Class.class.cast(pt.getActualTypeArguments()[0]), annotations, i18nPackages, ignoreI18n,
                                context).stream(),
                                buildParametersMetas(name + ".value[${index}]", prefix + "value[${index}].",
                                        Class.class.cast(pt.getActualTypeArguments()[1]), annotations, i18nPackages,
                                        ignoreI18n, context).stream())
                        .collect(toList());
            }
        }
        if (Class.class.isInstance(type)) {
            switch (findType(type)) {
                case ENUM:
                case STRING:
                case NUMBER:
                case BOOLEAN:
                    return singletonList(buildParameter(name, prefix, new ParameterMeta.Source() {

                        @Override
                        public String name() {
                            return name;
                        }

                        @Override
                        public Class<?> declaringClass() {
                            return Class.class.cast(type);
                        }
                    }, type, annotations, i18nPackages, ignoreI18n, context));
                default:
            }
            return buildObjectParameters(prefix, Class.class.cast(type), i18nPackages, ignoreI18n, context);
        }
        throw new IllegalArgumentException("Unsupported parameter type: " + type);
    }

    private List<ParameterMeta> buildObjectParameters(final String prefix, final Class<?> type,
            final Collection<String> i18nPackages, final boolean ignoreI18n,
            final BaseParameterEnricher.Context context) {
        final Map<String, Field> fields = new HashMap<>();
        final List<ParameterMeta> out = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            out
                    .addAll(Stream
                            .of(current.getDeclaredFields())
                            .filter(f -> f.isAnnotationPresent(Option.class))
                            .filter(f -> !"$jacocoData".equals(f.getName()) && !Modifier.isStatic(f.getModifiers())
                                    && (f.getModifiers() & 0x00001000/* SYNTHETIC */) == 0)
                            .filter(f -> fields.putIfAbsent(f.getName(), f) == null)
                            .map(f -> {
                                final String name = findName(f, f.getName());
                                final String path = prefix + name;
                                return buildParameter(name, path + ".", new ParameterMeta.Source() {

                                    @Override
                                    public String name() {
                                        return f.getName();
                                    }

                                    @Override
                                    public Class<?> declaringClass() {
                                        return f.getDeclaringClass();
                                    }
                                }, f.getGenericType(), f.getAnnotations(), i18nPackages, ignoreI18n, context);
                            })
                            .collect(toList()));
            current = current.getSuperclass();
        }
        return out;
    }

    public String findName(final AnnotatedElement parameter, final String defaultName) {
        return ofNullable(parameter.getAnnotation(Option.class))
                .map(Option::value)
                .filter(v -> !v.isEmpty())
                .orElse(defaultName);
    }

    private ParameterMeta.Type findType(final Type type) {
        if (Class.class.isInstance(type)) {
            final Class<?> clazz = Class.class.cast(type);

            // we handled char before so we only have numbers now for primitives
            if (Primitives.unwrap(clazz) == boolean.class) {
                return ParameterMeta.Type.BOOLEAN;
            }
            if (Primitives.unwrap(clazz) == char.class) {
                return ParameterMeta.Type.STRING;
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
        if (StringCompatibleTypes.isKnown(type, registry)) { // flatten the config as a string
            return ParameterMeta.Type.STRING;
        }
        return ParameterMeta.Type.OBJECT;
    }

    public static class Param implements AnnotatedElement {

        private final Type type;

        private final String name;

        private final Annotation[] annotations;

        public Param(final Type type, final Annotation[] annotations, final String name) {
            this.type = type;
            this.annotations = annotations;
            this.name = name;
        }

        public Param(final Parameter parameter) {
            this(parameter.getParameterizedType(), parameter.getAnnotations(), parameter.getName());
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
            return Stream
                    .of(getAnnotations())
                    .filter(it -> it.annotationType() == annotationClass)
                    .findFirst()
                    .map(annotationClass::cast)
                    .orElse(null);
        }

        @Override
        public Annotation[] getAnnotations() {
            return annotations;
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return getAnnotations();
        }
    }
}
