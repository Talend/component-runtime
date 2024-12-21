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
package org.talend.sdk.component.runtime.manager.reflect;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.talend.sdk.component.runtime.manager.reflect.Constructors.findConstructor;

import java.beans.ConstructorProperties;
import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;

import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.apache.xbean.recipe.ObjectRecipe;
import org.apache.xbean.recipe.UnsetPropertiesRecipe;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.configuration.Configuration;
import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.runtime.internationalization.InternationalizationServiceFactory;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.reflect.visibility.PayloadMapper;
import org.talend.sdk.component.runtime.manager.reflect.visibility.VisibilityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ReflectionService {

    private final ParameterModelService parameterModelService;

    private final PropertyEditorRegistry propertyEditorRegistry;

    // note: we use xbean for now but we can need to add some caching inside if we
    // abuse of it at runtime.
    // not a concern for now.
    //
    // note2: compared to {@link ParameterModelService}, here we build the instance
    // and we start from the config and not the
    // model.
    //
    // IMPORTANT: ensure to be able to read all data (including collection) from a
    // map to support system properties override
    public Function<Map<String, String>, Object[]> parameterFactory(final Executable executable,
            final Map<Class<?>, Object> precomputed, final List<ParameterMeta> metas) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Function<Supplier<Object>, Object> contextualSupplier = createContextualSupplier(loader);
        final Collection<Function<Map<String, String>, Object>> factories =
                Stream.of(executable.getParameters()).map(parameter -> {
                    final String name = parameterModelService.findName(parameter, parameter.getName());
                    final Type parameterizedType = parameter.getParameterizedType();
                    if (Class.class.isInstance(parameterizedType)) {
                        if (parameter.isAnnotationPresent(Configuration.class)) {
                            try {
                                final Class configClass = Class.class.cast(parameterizedType);
                                return createConfigFactory(precomputed, loader, contextualSupplier, parameter.getName(),
                                        parameter.getAnnotation(Configuration.class), parameter.getAnnotations(),
                                        configClass);
                            } catch (final NoSuchMethodException e) {
                                throw new IllegalArgumentException("No constructor for " + parameter);
                            }
                        }
                        final Object value = precomputed.get(parameterizedType);
                        if (value != null) {
                            if (Copiable.class.isInstance(value)) {
                                final Copiable copiable = Copiable.class.cast(value);
                                return (Function<Map<String, String>, Object>) config -> copiable.copy(value);
                            }
                            return (Function<Map<String, String>, Object>) config -> value;
                        }
                        final BiFunction<String, Map<String, Object>, Object> objectFactory = createObjectFactory(
                                loader, contextualSupplier, parameterizedType, translate(metas, name), precomputed);
                        return (Function<Map<String, String>, Object>) config -> objectFactory
                                .apply(name, Map.class.cast(config));
                    }

                    if (ParameterizedType.class.isInstance(parameterizedType)) {
                        final ParameterizedType pt = ParameterizedType.class.cast(parameterizedType);
                        if (Class.class.isInstance(pt.getRawType())) {
                            if (Collection.class.isAssignableFrom(Class.class.cast(pt.getRawType()))) {
                                final Class<?> collectionType = Class.class.cast(pt.getRawType());
                                final Type itemType = pt.getActualTypeArguments()[0];
                                if (!Class.class.isInstance(itemType)) {
                                    throw new IllegalArgumentException(
                                            "For now we only support Collection<T> with T a Class<?>");
                                }
                                final Class<?> itemClass = Class.class.cast(itemType);

                                // if we have services matching this type then we return the collection of
                                // services, otherwise
                                // we consider it is a config
                                final Collection<Object> services = precomputed
                                        .entrySet()
                                        .stream()
                                        .sorted(Comparator.comparing(e -> e.getKey().getName()))
                                        .filter(e -> itemClass.isAssignableFrom(e.getKey()))
                                        .map(Map.Entry::getValue)
                                        .collect(toList());

                                // let's try to catch up built-in service
                                // here we have 1 entry per type and it is lazily created on get()
                                if (services.isEmpty()) {
                                    final Object o = precomputed.get(itemClass);
                                    if (o != null) {
                                        services.add(o);
                                    }
                                }

                                if (!services.isEmpty()) {
                                    return (Function<Map<String, String>, Object>) config -> services;
                                }

                                // here we know we just want to instantiate a config list and not services
                                final Collector collector = Set.class == collectionType ? toSet() : toList();
                                final List<ParameterMeta> parameterMetas = translate(metas, name);
                                final BiFunction<String, Map<String, Object>, Object> itemFactory = createObjectFactory(
                                        loader, contextualSupplier, itemClass, parameterMetas, precomputed);
                                return (Function<Map<String, String>, Object>) config -> createList(loader,
                                        contextualSupplier, name, collectionType, itemClass, collector, itemFactory,
                                        Map.class.cast(config), parameterMetas, precomputed);
                            }
                            if (Map.class.isAssignableFrom(Class.class.cast(pt.getRawType()))) {
                                final Class<?> mapType = Class.class.cast(pt.getRawType());
                                final Type keyItemType = pt.getActualTypeArguments()[0];
                                final Type valueItemType = pt.getActualTypeArguments()[1];
                                if (!Class.class.isInstance(keyItemType) || !Class.class.isInstance(valueItemType)) {
                                    throw new IllegalArgumentException(
                                            "For now we only support Map<A, B> with A and B a Class<?>");
                                }
                                final Class<?> keyItemClass = Class.class.cast(keyItemType);
                                final Class<?> valueItemClass = Class.class.cast(valueItemType);
                                final List<ParameterMeta> parameterMetas = translate(metas, name);
                                final BiFunction<String, Map<String, Object>, Object> keyItemFactory =
                                        createObjectFactory(loader, contextualSupplier, keyItemClass, parameterMetas,
                                                precomputed);
                                final BiFunction<String, Map<String, Object>, Object> valueItemFactory =
                                        createObjectFactory(loader, contextualSupplier, valueItemClass, parameterMetas,
                                                precomputed);
                                final Collector collector =
                                        createMapCollector(mapType, keyItemClass, valueItemClass, precomputed);
                                return (Function<Map<String, String>, Object>) config -> createMap(name, mapType,
                                        keyItemFactory, valueItemFactory, collector, Map.class.cast(config));
                            }
                        }
                    }

                    throw new IllegalArgumentException("Unsupported type: " + parameterizedType);
                }).collect(toList());

        return config -> {
            final Map<String, String> notNullConfig = ofNullable(config).orElseGet(Collections::emptyMap);
            final PayloadValidator visitor = new PayloadValidator();
            if (!visitor.skip) {
                visitor.globalPayload = new PayloadMapper((a, b) -> {
                }).visitAndMap(metas, notNullConfig);
                final PayloadMapper payloadMapper = new PayloadMapper(visitor);
                payloadMapper.setGlobalPayload(visitor.globalPayload);
                payloadMapper.visitAndMap(metas, notNullConfig);
                visitor.throwIfFailed();
            }
            return factories.stream().map(f -> f.apply(notNullConfig)).toArray(Object[]::new);
        };
    }

    public Function<Supplier<Object>, Object> createContextualSupplier(final ClassLoader loader) {
        return supplier -> {
            final Thread thread = Thread.currentThread();
            final ClassLoader old = thread.getContextClassLoader();
            thread.setContextClassLoader(loader);
            try {
                return supplier.get();
            } finally {
                thread.setContextClassLoader(old);
            }
        };
    }

    public Function<Map<String, String>, Object> createConfigFactory(final Map<Class<?>, Object> precomputed,
            final ClassLoader loader, final Function<Supplier<Object>, Object> contextualSupplier, final String name,
            final Configuration configuration, final Annotation[] allAnnotations, final Class<?> configClass)
            throws NoSuchMethodException {
        final Constructor constructor = configClass.getConstructor();
        final LocalConfiguration config = LocalConfiguration.class.cast(precomputed.get(LocalConfiguration.class));
        if (config == null) {
            return c -> null;
        }

        final String prefix = configuration.value();
        final ParameterMeta objectMeta =
                parameterModelService.buildParameter(prefix, prefix, new ParameterMeta.Source() {

                    @Override
                    public String name() {
                        return name;
                    }

                    @Override
                    public Class<?> declaringClass() {
                        return constructor.getDeclaringClass();
                    }
                }, configClass, allAnnotations, Stream
                        .of(ofNullable(constructor.getDeclaringClass().getPackage()).map(Package::getName).orElse(""))
                        .collect(toList()), true, new BaseParameterEnricher.Context(config));
        final BiFunction<String, Map<String, Object>, Object> objectFactory = createObjectFactory(loader,
                contextualSupplier, configClass, objectMeta.getNestedParameters(), precomputed);
        final Function<Map<String, Object>, Object> factory = c -> objectFactory.apply(prefix, c);
        return ignoredDependentConfig -> {
            final Map<String, Object> configMap = config
                    .keys()
                    .stream()
                    .filter(it -> objectMeta
                            .getNestedParameters()
                            .stream()
                            .anyMatch(p -> it.startsWith(prefix + '.' + p.getName())))
                    .collect(toMap(identity(), config::get));
            return factory.apply(configMap);
        };
    }

    private List<ParameterMeta> translate(final List<ParameterMeta> metas, final String name) {
        if (metas == null) {
            return null;
        }
        return metas
                .stream()
                .filter(it -> it.getName().equals(name))
                .flatMap(it -> it.getNestedParameters().stream())
                .collect(toList());
    }

    private Collector createMapCollector(final Class<?> mapType, final Class<?> keyItemClass,
            final Class<?> valueItemClass, final Map<Class<?>, Object> precomputed) {
        final Function<Map.Entry<?, ?>, Object> keyMapper = o -> doConvert(keyItemClass, o.getKey(), precomputed);
        final Function<Map.Entry<?, ?>, Object> valueMapper = o -> doConvert(valueItemClass, o.getValue(), precomputed);
        return ConcurrentMap.class.isAssignableFrom(mapType) ? toConcurrentMap(keyMapper, valueMapper)
                : toMap(keyMapper, valueMapper);
    }

    private Object createList(final ClassLoader loader, final Function<Supplier<Object>, Object> contextualSupplier,
            final String name, final Class<?> collectionType, final Class<?> itemClass, final Collector collector,
            final BiFunction<String, Map<String, Object>, Object> itemFactory, final Map<String, Object> config,
            final List<ParameterMeta> metas, final Map<Class<?>, Object> precomputed) {
        final Object obj = config.get(name);
        if (collectionType.isInstance(obj)) {
            return Collection.class
                    .cast(obj)
                    .stream()
                    .map(o -> doConvert(itemClass, o, precomputed))
                    .collect(collector);
        }

        // try to build it from the properties
        // <value>[<index>] = xxxxx
        // <value>[<index>].<property> = xxxxx
        final Collection collection = List.class.isAssignableFrom(collectionType) ? new ArrayList<>() : new HashSet<>();
        final int maxLength = getArrayMaxLength(name, config);
        int paramIdx = 0;
        String[] args = null;
        while (paramIdx < maxLength) {
            final String configName = String.format("%s[%d]", name, paramIdx);
            if (!config.containsKey(configName)) {
                if (config.keySet().stream().anyMatch(k -> k.startsWith(configName + "."))) { // object
                                                                                              // mapping
                    if (paramIdx == 0) {
                        args = findArgsName(itemClass);
                    }
                    collection
                            .add(createObject(loader, contextualSupplier, itemClass, args, configName, config, metas,
                                    precomputed));
                } else {
                    break;
                }
            } else {
                collection.add(itemFactory.apply(configName, config));
            }
            paramIdx++;
        }

        return collection;
    }

    private Integer getArrayMaxLength(final String prefix, final Map<String, Object> config) {
        return ofNullable(config.get(prefix + "[length]"))
                .map(String::valueOf)
                .map(Integer::parseInt)
                .orElse(Integer.MAX_VALUE);
    }

    private Object createMap(final String name, final Class<?> mapType,
            final BiFunction<String, Map<String, Object>, Object> keyItemFactory,
            final BiFunction<String, Map<String, Object>, Object> valueItemFactory, final Collector collector,
            final Map<String, Object> config) {
        final Object obj = config.get(name);
        if (mapType.isInstance(obj)) {
            return Map.class.cast(obj).entrySet().stream().collect(collector);
        }

        // try to build it from the properties
        // <value>.key[<index>] = xxxxx
        // <value>.key[<index>].<property> = xxxxx
        // <value>.value[<index>] = xxxxx
        // <value>.value[<index>].<property> = xxxxx
        final Map map = ConcurrentMap.class.isAssignableFrom(mapType) ? new ConcurrentHashMap() : new HashMap();
        int paramIdx = 0;
        do {
            final String keyConfigName = String.format("%s.key[%d]", name, paramIdx);
            final String valueConfigName = String.format("%s.value[%d]", name, paramIdx);
            if (!config.containsKey(keyConfigName) || !config.containsKey(valueConfigName)) { // quick test first
                if (config.keySet().stream().noneMatch(k -> k.startsWith(keyConfigName))
                        && config.keySet().stream().noneMatch(k -> k.startsWith(valueConfigName))) {
                    break;
                }
            }
            map.put(keyItemFactory.apply(keyConfigName, config), valueItemFactory.apply(valueConfigName, config));
            paramIdx++;
        } while (true);

        return map;
    }

    private BiFunction<String, Map<String, Object>, Object> createObjectFactory(final ClassLoader loader,
            final Function<Supplier<Object>, Object> contextualSupplier, final Type type,
            final List<ParameterMeta> metas, final Map<Class<?>, Object> precomputed) {
        final Class clazz = Class.class.cast(type);
        if (clazz.isPrimitive() || Primitives.unwrap(clazz) != clazz || String.class == clazz) {
            return (name, config) -> doConvert(clazz, config.get(name), precomputed);
        }
        if (clazz.isEnum()) {
            return (name, config) -> ofNullable(config.get(name))
                    .map(String.class::cast)
                    .map(String::trim)
                    .filter(it -> !it.isEmpty())
                    .map(v -> Enum.valueOf(clazz, v))
                    .orElse(null);
        }

        final String[] args = findArgsName(clazz);
        return (name, config) -> contextualSupplier
                .apply(() -> createObject(loader, contextualSupplier, clazz, args, name, config, metas, precomputed));
    }

    private String[] findArgsName(final Class clazz) {
        return Stream
                .of(clazz.getConstructors())
                .filter(c -> c.isAnnotationPresent(ConstructorProperties.class))
                .findFirst()
                .map(c -> ConstructorProperties.class.cast(c.getAnnotation(ConstructorProperties.class)).value())
                .orElse(null);
    }

    private JsonValue createJsonValue(final Object value, final Map<Class<?>, Object> precomputed,
            final Function<Reader, JsonReader> fallbackReaderCreator) {
        final StringReader sr = new StringReader(String.valueOf(value).trim());
        try (final JsonReader reader = ofNullable(precomputed.get(JsonReaderFactory.class))
                .map(JsonReaderFactory.class::cast)
                .map(f -> f.createReader(sr))
                .orElseGet(() -> fallbackReaderCreator.apply(sr))) {
            return reader.read();
        }
    }

    private Object createObject(final ClassLoader loader, final Function<Supplier<Object>, Object> contextualSupplier,
            final Class clazz, final String[] args, final String name, final Map<String, Object> config,
            final List<ParameterMeta> metas, final Map<Class<?>, Object> precomputed) {
        final Object potentialJsonValue = config.get(name);
        if (JsonObject.class == clazz && String.class.isInstance(potentialJsonValue)) {
            return createJsonValue(potentialJsonValue, precomputed, Json::createReader).asJsonObject();
        }
        if (propertyEditorRegistry.findConverter(clazz) != null && Schema.class.isAssignableFrom(clazz)) {
            final Object configValue = config.get(name);
            if (String.class.isInstance(configValue)) {
                return propertyEditorRegistry.getValue(clazz, String.class.cast(configValue));
            }
        }
        if (propertyEditorRegistry.findConverter(clazz) != null && config.size() == 1) {
            final Object configValue = config.values().iterator().next();
            if (String.class.isInstance(configValue)) {
                return propertyEditorRegistry.getValue(clazz, String.class.cast(configValue));
            }
        }

        final String prefix = name.isEmpty() ? "" : name + ".";
        final ObjectRecipe recipe = newRecipe(clazz);
        recipe.setProperty("rawProperties", new UnsetPropertiesRecipe()); // todo: log unused props?
        ofNullable(args).ifPresent(recipe::setConstructorArgNames);

        final Map<String, Object> specificMapping = config
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(prefix) || prefix.isEmpty())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        // extract map configuration
        final Map<String, Object> mapEntries = specificMapping.entrySet().stream().filter(e -> {
            final String key = e.getKey();
            final int idxStart = key.indexOf('[', prefix.length());
            return idxStart > 0 && ((idxStart > ".key".length() && key.startsWith(".key", idxStart - ".key".length()))
                    || (idxStart > ".value".length() && key.startsWith(".value", idxStart - ".value".length())));
        })
                .sorted(this::sortIndexEntry)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, noMerge(), LinkedHashMap::new));
        mapEntries.keySet().forEach(specificMapping::remove);
        final Map<String, Object> preparedMaps = new HashMap<>();
        for (final Map.Entry<String, Object> entry : mapEntries.entrySet()) {
            final String key = entry.getKey();
            final int idxStart = key.indexOf('[', prefix.length());
            String enclosingName = key.substring(prefix.length(), idxStart);
            if (enclosingName.endsWith(".key")) {
                enclosingName = enclosingName.substring(0, enclosingName.length() - ".key".length());
            } else if (enclosingName.endsWith(".value")) {
                enclosingName = enclosingName.substring(0, enclosingName.length() - ".value".length());
            } else {
                throw new IllegalArgumentException("'" + key + "' is not supported, it is considered as a map binding");
            }
            if (preparedMaps.containsKey(enclosingName)) {
                continue;
            }
            if (isUiParam(enclosingName)) { // normally cleaned up by the UI@back integration but safeguard here
                continue;
            }

            final Type genericType =
                    findField(normalizeName(enclosingName.substring(enclosingName.indexOf('.') + 1), metas), clazz)
                            .getGenericType();
            final ParameterizedType pt = validateObject(clazz, enclosingName, genericType);

            final Class<?> keyType = Class.class.cast(pt.getActualTypeArguments()[0]);
            final Class<?> valueType = Class.class.cast(pt.getActualTypeArguments()[1]);
            preparedMaps
                    .put(enclosingName, createMap(prefix + enclosingName, Map.class,
                            createObjectFactory(loader, contextualSupplier, keyType, metas, precomputed),
                            createObjectFactory(loader, contextualSupplier, valueType, metas, precomputed),
                            createMapCollector(Class.class.cast(pt.getRawType()), keyType, valueType, precomputed),
                            new HashMap<>(mapEntries)));
        }

        // extract list configuration
        final Map<String, Object> listEntries = specificMapping.entrySet().stream().filter(e -> {
            final String key = e.getKey();
            final int idxStart = key.indexOf('[', prefix.length());
            final int idxEnd = key.indexOf(']', prefix.length());
            final int sep = key.indexOf('.', prefix.length() + 1);
            return idxStart > 0 && key.endsWith("]") && (sep > idxEnd || sep < 0);
        })
                .sorted(this::sortIndexEntry)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, noMerge(), LinkedHashMap::new));
        listEntries.keySet().forEach(specificMapping::remove);
        final Map<String, Object> preparedLists = new HashMap<>();
        for (final Map.Entry<String, Object> entry : listEntries.entrySet()) {
            final String key = entry.getKey();
            final int idxStart = key.indexOf('[', prefix.length());
            final String enclosingName = key.substring(prefix.length(), idxStart);
            if (preparedLists.containsKey(enclosingName)) {
                continue;
            }
            if (isUiParam(enclosingName)) {
                continue;
            }

            final Type genericType = findField(normalizeName(enclosingName, metas), clazz).getGenericType();
            if (Class.class.isInstance(genericType)) {
                final Class<?> arrayClass = Class.class.cast(genericType);
                if (arrayClass.isArray()) {
                    // we could use Array.newInstance but for now use the list, shouldn't impact
                    // much the perf
                    final Collection<?> list = Collection.class
                            .cast(createList(loader, contextualSupplier, prefix + enclosingName, List.class,
                                    arrayClass.getComponentType(), toList(), createObjectFactory(loader,
                                            contextualSupplier, arrayClass.getComponentType(), metas, precomputed),
                                    new HashMap<>(listEntries), metas, precomputed));

                    // we need that conversion to ensure the type matches
                    final Object array = Array.newInstance(arrayClass.getComponentType(), list.size());
                    int idx = 0;
                    for (final Object o : list) {
                        Array.set(array, idx++, o);
                    }
                    preparedLists.put(enclosingName, array);
                    continue;
                } // else let it fail with the "collection" error
            }

            // now we need an actual collection type
            final ParameterizedType pt = validateCollection(clazz, enclosingName, genericType);
            final Type itemType = pt.getActualTypeArguments()[0];
            preparedLists
                    .put(enclosingName,
                            createList(loader, contextualSupplier, prefix + enclosingName,
                                    Class.class.cast(pt.getRawType()), Class.class.cast(itemType), toList(),
                                    createObjectFactory(loader, contextualSupplier, itemType, metas, precomputed),
                                    new HashMap<>(listEntries), metas, precomputed));
        }

        // extract nested Object configurations
        final Map<String, Object> objectEntries = specificMapping.entrySet().stream().filter(e -> {
            final String key = e.getKey();
            return key.indexOf('.', prefix.length() + 1) > 0;
        }).sorted((o1, o2) -> {
            final String key1 = o1.getKey();
            final String key2 = o2.getKey();
            if (key1.equals(key2)) {
                return 0;
            }

            final String nestedName1 = key1.substring(prefix.length(), key1.indexOf('.', prefix.length() + 1));
            final String nestedName2 = key2.substring(prefix.length(), key2.indexOf('.', prefix.length() + 1));

            final int idxStart1 = nestedName1.indexOf('[');
            final int idxStart2 = nestedName2.indexOf('[');
            if (idxStart1 > 0 && idxStart2 > 0
                    && nestedName1.substring(0, idxStart1).equals(nestedName2.substring(0, idxStart2))) {
                final int idx1 = parseIndex(nestedName1.substring(idxStart1 + 1, nestedName1.length() - 1));
                final int idx2 = parseIndex(nestedName2.substring(idxStart2 + 1, nestedName2.length() - 1));
                return idx1 - idx2;
            }
            return key1.compareTo(key2);
        }).collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (o, o2) -> {
            throw new IllegalArgumentException("Can't merge " + o + " and " + o2);
        }, LinkedHashMap::new));
        objectEntries.keySet().forEach(specificMapping::remove);
        final Map<String, Object> preparedObjects = new HashMap<>();
        for (final Map.Entry<String, Object> entry : objectEntries.entrySet()) {
            final String nestedName =
                    entry.getKey().substring(prefix.length(), entry.getKey().indexOf('.', prefix.length() + 1));
            if (isUiParam(nestedName)) {
                continue;
            }
            if (nestedName.endsWith("]")) { // complex lists
                final int idxStart = nestedName.indexOf('[');
                if (idxStart > 0) {
                    final String listName = nestedName.substring(0, idxStart);
                    final Field field = findField(normalizeName(listName, metas), clazz);
                    if (ParameterizedType.class.isInstance(field.getGenericType())) {
                        final ParameterizedType pt = ParameterizedType.class.cast(field.getGenericType());
                        if (Class.class.isInstance(pt.getRawType())) {
                            final Class<?> rawType = Class.class.cast(pt.getRawType());
                            if (Set.class.isAssignableFrom(rawType)) {
                                addListElement(loader, contextualSupplier, config, prefix, preparedObjects, nestedName,
                                        listName, pt, () -> new HashSet<>(2), translate(metas, listName), precomputed);
                            } else if (Collection.class.isAssignableFrom(rawType)) {
                                addListElement(loader, contextualSupplier, config, prefix, preparedObjects, nestedName,
                                        listName, pt, () -> new ArrayList<>(2), translate(metas, listName),
                                        precomputed);
                            } else {
                                throw new IllegalArgumentException("unsupported configuration type: " + pt);
                            }
                            continue;
                        } else {
                            throw new IllegalArgumentException("unsupported configuration type: " + pt);
                        }
                    } else {
                        throw new IllegalArgumentException("unsupported configuration type: " + field.getType());
                    }
                } else {
                    throw new IllegalArgumentException("unsupported configuration type: " + nestedName);
                }
            }
            final String fieldName = normalizeName(nestedName, metas);
            if (preparedObjects.containsKey(fieldName)) {
                continue;
            }
            final Field field = findField(fieldName, clazz);
            preparedObjects
                    .put(fieldName,
                            createObject(loader, contextualSupplier, field.getType(), findArgsName(field.getType()),
                                    prefix + nestedName, config, translate(metas, nestedName), precomputed));
        }

        // other entries can be directly set
        final Map<String, Object> normalizedConfig = specificMapping
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(prefix) && e.getKey().substring(prefix.length()).indexOf('.') < 0)
                .collect(toMap(e -> {
                    final String specificConfig = e.getKey().substring(prefix.length());
                    final int index = specificConfig.indexOf('[');
                    if (index > 0) {
                        final int end = specificConfig.indexOf(']', index);
                        if (end > index) { // > 0 would work too
                            // here we need to normalize it to let xbean understand it
                            String leadingString = specificConfig.substring(0, index);
                            if (leadingString.endsWith(".key") || leadingString.endsWith(".value")) { // map
                                leadingString = leadingString.substring(0, leadingString.lastIndexOf('.'));
                            }
                            return leadingString + specificConfig.substring(end + 1);
                        }
                    }
                    return specificConfig;
                }, Map.Entry::getValue));

        // now bind it all to the recipe and builder the instance
        preparedMaps.forEach(recipe::setFieldProperty);
        preparedLists.forEach(recipe::setFieldProperty);
        preparedObjects.forEach(recipe::setFieldProperty);
        if (!normalizedConfig.isEmpty()) {
            normalizedConfig
                    .entrySet()
                    .stream()
                    .map(it -> normalize(it, metas))
                    .forEach(e -> recipe.setFieldProperty(e.getKey(), e.getValue()));
        }
        return recipe.create(loader);
    }

    private ParameterizedType validateCollection(final Class clazz, final String enclosingName,
            final Type genericType) {
        if (!ParameterizedType.class.isInstance(genericType)) {
            throw new IllegalArgumentException(
                    clazz + "#" + enclosingName + " should be a generic collection and not a " + genericType);
        }
        final ParameterizedType pt = ParameterizedType.class.cast(genericType);
        if (pt.getActualTypeArguments().length != 1 || !Class.class.isInstance(pt.getActualTypeArguments()[0])) {
            throw new IllegalArgumentException(clazz + "#" + enclosingName
                    + " should use concrete class items and not a " + pt.getActualTypeArguments()[0]);
        }
        return pt;
    }

    private ParameterizedType validateObject(final Class clazz, final String enclosingName, final Type genericType) {
        if (!ParameterizedType.class.isInstance(genericType)) {
            throw new IllegalArgumentException(
                    clazz + "#" + enclosingName + " should be a generic map and not a " + genericType);
        }
        final ParameterizedType pt = ParameterizedType.class.cast(genericType);
        if (pt.getActualTypeArguments().length != 2 || !Class.class.isInstance(pt.getActualTypeArguments()[0])
                || !Class.class.isInstance(pt.getActualTypeArguments()[1])) {
            throw new IllegalArgumentException(clazz + "#" + enclosingName
                    + " should be a generic map with a key and value class type (" + pt + ")");
        }
        return pt;
    }

    private ObjectRecipe newRecipe(final Class clazz) {
        final ObjectRecipe recipe = new ObjectRecipe(clazz);
        recipe.setRegistry(propertyEditorRegistry);
        recipe.allow(org.apache.xbean.recipe.Option.FIELD_INJECTION);
        recipe.allow(org.apache.xbean.recipe.Option.PRIVATE_PROPERTIES);
        recipe.allow(org.apache.xbean.recipe.Option.CASE_INSENSITIVE_PROPERTIES);
        recipe.allow(org.apache.xbean.recipe.Option.IGNORE_MISSING_PROPERTIES);
        return recipe;
    }

    private boolean isUiParam(final String name) {
        final int dollar = name.indexOf('$');
        if (dollar >= 0 && name.indexOf("_name", dollar) > dollar) {
            log.warn("{} is not a valid configuration, it shouldn't be passed to the runtime", name);
            return true;
        }
        return false;
    }

    private BinaryOperator<Object> noMerge() {
        return (a, b) -> {
            throw new IllegalArgumentException("Conflict");
        };
    }

    private Map.Entry<String, Object> normalize(final Map.Entry<String, Object> it, final List<ParameterMeta> metas) {
        return metas == null ? it : metas.stream().filter(m -> m.getName().equals(it.getKey())).findFirst().map(m -> {
            final String name = findName(m);
            if (name.equals(it.getKey())) {
                return it;
            }
            return new AbstractMap.SimpleEntry<>(name, it.getValue());
        }).orElse(it);
    }

    private String normalizeName(final String name, final List<ParameterMeta> metas) {
        return metas == null ? name
                : metas.stream().filter(m -> m.getName().equals(name)).findFirst().map(this::findName).orElse(name);
    }

    private String findName(final ParameterMeta m) {
        return ofNullable(m.getSource()).map(ParameterMeta.Source::name).orElse(m.getName());
    }

    // CHECKSTYLE:OFF
    private void addListElement(final ClassLoader loader, final Function<Supplier<Object>, Object> contextualSupplier,
            final Map<String, Object> config, final String prefix, final Map<String, Object> preparedObjects,
            final String nestedName, final String listName, final ParameterizedType pt, final Supplier<?> init,
            final List<ParameterMeta> metas, final Map<Class<?>, Object> precomputed) {
        // CHECKSTYLE:ON
        final Collection<Object> aggregator =
                Collection.class.cast(preparedObjects.computeIfAbsent(listName, k -> init.get()));
        final Class<?> itemType = Class.class.cast(pt.getActualTypeArguments()[0]);
        final int index = parseIndex(nestedName.substring(listName.length() + 1, nestedName.length() - 1));
        final int maxSize = getArrayMaxLength(prefix + listName, config);
        if (aggregator.size() <= index && index < maxSize) {
            aggregator
                    .add(createObject(loader, contextualSupplier, itemType, findArgsName(itemType), prefix + nestedName,
                            config, metas, precomputed));
        }
    }

    private Field findField(final String name, final Class clazz) {
        Class<?> type = clazz;
        while (type != Object.class && type != null) {
            try {
                return type.getDeclaredField(name);
            } catch (final NoSuchFieldException e) {
                // no-op
            }
            type = type.getSuperclass();
        }
        throw new IllegalArgumentException(
                String.format("Unknown field: %s in class: %s.", name, clazz != null ? clazz.getName() : "null"));
    }

    private int sortIndexEntry(final Map.Entry<String, Object> e1, final Map.Entry<String, Object> e2) {
        final String name1 = e1.getKey();
        final String name2 = e2.getKey();
        final int index1 = name1.indexOf('[');
        final int index2 = name2.indexOf('[');

        // same prefix -> sort specifically
        if (index1 > 0 && index2 == index1 && name1.substring(0, index1).equals(name2.substring(0, index1))) {
            final int end1 = name1.indexOf(']', index1);
            final int end2 = name2.indexOf(']', index2);
            if (end1 > index1 && end2 > index2) {
                final String idx1 = name1.substring(index1 + 1, end1);
                final String idx2 = name2.substring(index2 + 1, end2);
                return parseIndex(idx1) - parseIndex(idx2);
            } // else not matching so use default sorting
        }
        return name1.compareTo(name2);
    }

    private int parseIndex(final String name) {
        if ("length".equals(name)) {
            return -1; // not important, skipped anyway
        }
        return Integer.parseInt(name);
    }

    private Object doConvert(final Class<?> type, final Object value, final Map<Class<?>, Object> precomputed) {
        if (value == null) { // get the primitive default
            return getPrimitiveDefault(type);
        }
        if (type.isInstance(value)) { // no need of any conversion
            return value;
        }
        if (JsonValue.class.isAssignableFrom(type)) {
            return createJsonValue(value, precomputed, Json::createReader);
        }
        if (propertyEditorRegistry.findConverter(type) != null) { // go through string to convert the value
            return propertyEditorRegistry.getValue(type, String.valueOf(value));
        }
        throw new IllegalArgumentException("Can't convert '" + value + "' to " + type);
    }

    private Object getPrimitiveDefault(final Class<?> type) {
        final Type convergedType = Primitives.unwrap(type);
        if (char.class == convergedType || short.class == convergedType || byte.class == convergedType
                || int.class == convergedType) {
            return 0;
        }
        if (long.class == convergedType) {
            return 0L;
        }
        if (boolean.class == convergedType) {
            return false;
        }
        if (double.class == convergedType) {
            return 0.;
        }
        if (float.class == convergedType) {
            return 0f;
        }
        return null;
    }

    public static class JavascriptRegex implements Predicate<CharSequence> {

        private final String regex;

        private final String indicators;

        private JavascriptRegex(final String regex) {
            if (regex.startsWith("/") && regex.length() > 1) {
                final int end = regex.lastIndexOf('/');
                if (end < 0) {
                    this.regex = regex;
                    indicators = "";
                } else {
                    this.regex = regex.substring(1, end);
                    indicators = regex.substring(end + 1);
                }
            } else {
                this.regex = regex;
                indicators = "";
            }
        }

        @Override
        public boolean test(final CharSequence text) {
            final String script = "new RegExp(regex, indicators).test(text)";
            final Context context = Context.enter();
            try {
                final Scriptable scope = context.initStandardObjects();
                scope.put("text", scope, text);
                scope.put("regex", scope, regex);
                scope.put("indicators", scope, indicators);
                return Context.toBoolean(context.evaluateString(scope, script, "test", 0, null));
            } catch (final Exception e) {
                return false;
            } finally {
                Context.exit();
            }
        }
    }

    public interface Messages {

        String required(String property);

        String min(String property, double bound, double value);

        String max(String property, double bound, double value);

        String minLength(String property, double bound, int value);

        String maxLength(String property, double bound, int value);

        String minItems(String property, double bound, int value);

        String maxItems(String property, double bound, int value);

        String uniqueItems(String property);

        String pattern(String property, String pattern);
    }

    @RequiredArgsConstructor
    protected static class PayloadValidator implements PayloadMapper.OnParameter {

        private static final VisibilityService VISIBILITY_SERVICE = new VisibilityService(JsonProvider.provider());

        private static final Messages MESSAGES = new InternationalizationServiceFactory(Locale::getDefault)
                .create(Messages.class, PayloadValidator.class.getClassLoader());

        private final boolean skip = Boolean.getBoolean("talend.component.configuration.validation.skip");

        private final Collection<String> errors = new ArrayList<>();

        JsonObject globalPayload;

        @Override
        public void onParameter(final ParameterMeta meta, final JsonValue value) {
            if (!VISIBILITY_SERVICE.build(meta).isVisible(globalPayload)) {
                return;
            }

            if (Boolean.parseBoolean(meta.getMetadata().get("tcomp::validation::required"))
                    && value == JsonValue.NULL) {
                errors.add(MESSAGES.required(meta.getPath()));
            }
            final Map<String, String> metadata = meta.getMetadata();
            {
                final String min = metadata.get("tcomp::validation::min");
                if (min != null) {
                    final double bound = Double.parseDouble(min);
                    if (value.getValueType() == JsonValue.ValueType.NUMBER
                            && JsonNumber.class.cast(value).doubleValue() < bound) {
                        errors.add(MESSAGES.min(meta.getPath(), bound, JsonNumber.class.cast(value).doubleValue()));
                    }
                }
            }
            {
                final String max = metadata.get("tcomp::validation::max");
                if (max != null) {
                    final double bound = Double.parseDouble(max);
                    if (value.getValueType() == JsonValue.ValueType.NUMBER
                            && JsonNumber.class.cast(value).doubleValue() > bound) {
                        errors.add(MESSAGES.max(meta.getPath(), bound, JsonNumber.class.cast(value).doubleValue()));
                    }
                }
            }
            {
                final String min = metadata.get("tcomp::validation::minLength");
                if (min != null) {
                    final double bound = Double.parseDouble(min);
                    if (value.getValueType() == JsonValue.ValueType.STRING) {
                        final String val = JsonString.class.cast(value).getString();
                        if (val.length() < bound) {
                            errors.add(MESSAGES.minLength(meta.getPath(), bound, val.length()));
                        }
                    }
                }
            }
            {
                final String max = metadata.get("tcomp::validation::maxLength");
                if (max != null) {
                    final double bound = Double.parseDouble(max);
                    if (value.getValueType() == JsonValue.ValueType.STRING) {
                        final String val = JsonString.class.cast(value).getString();
                        if (val.length() > bound) {
                            errors.add(MESSAGES.maxLength(meta.getPath(), bound, val.length()));
                        }
                    }
                }
            }
            {
                final String min = metadata.get("tcomp::validation::minItems");
                if (min != null) {
                    final double bound = Double.parseDouble(min);
                    if (value.getValueType() == JsonValue.ValueType.ARRAY && value.asJsonArray().size() < bound) {
                        errors.add(MESSAGES.minItems(meta.getPath(), bound, value.asJsonArray().size()));
                    }
                }
            }
            {
                final String max = metadata.get("tcomp::validation::maxItems");
                if (max != null) {
                    final double bound = Double.parseDouble(max);
                    if (value.getValueType() == JsonValue.ValueType.ARRAY && value.asJsonArray().size() > bound) {
                        errors.add(MESSAGES.maxItems(meta.getPath(), bound, value.asJsonArray().size()));
                    }
                }
            }
            {
                final String unique = metadata.get("tcomp::validation::uniqueItems");
                if (unique != null) {
                    if (value.getValueType() == JsonValue.ValueType.ARRAY) {
                        final JsonArray array = value.asJsonArray();
                        if (new HashSet<>(array).size() != array.size()) {
                            errors.add(MESSAGES.uniqueItems(meta.getPath()));
                        }
                    }
                }
            }
            {
                final String pattern = metadata.get("tcomp::validation::pattern");
                if (pattern != null && value.getValueType() == JsonValue.ValueType.STRING) {
                    final String val = JsonString.class.cast(value).getString();
                    if (!new JavascriptRegex(pattern).test(CharSequence.class.cast(val))) {
                        errors.add(MESSAGES.pattern(meta.getPath(), pattern));
                    }
                }
            }
        }

        private void throwIfFailed() {
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("- " + String.join("\n- ", errors));
            }
        }
    }

    /**
     * Helper function for creating an instance from a configuration map.
     * 
     * @param clazz Class of the wanted instance.
     * @param <T> Type managed
     * @return function that generate the wanted instance when calling
     * {@link BiFunction#apply(java.lang.Object, java.lang.Object)} with a config name and configuration {@link Map}.
     */
    public <T> BiFunction<String, Map<String, Object>, T> createObjectFactory(final Class<T> clazz) {
        final Map precomputed = Collections.emptyMap();
        if (clazz.isPrimitive() || Primitives.unwrap(clazz) != clazz || clazz == String.class) {
            return (name, config) -> (T) doConvert(clazz, config.get(name), precomputed);
        }
        if (clazz.isEnum()) {
            return (name,
                    config) -> (T) ofNullable(config.get(name))
                            .map(String.class::cast)
                            .map(String::trim)
                            .filter(it -> !it.isEmpty())
                            .map(v -> Enum.valueOf((Class<Enum>) clazz, v))
                            .orElse(null);
        }
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Function<Supplier<Object>, Object> contextualSupplier = createContextualSupplier(loader);
        final Constructor<?> c = findConstructor(clazz);
        final ParameterModelService s = new ParameterModelService(new PropertyEditorRegistry());
        final List<ParameterMeta> metas = s.buildParameterMetas(c, c.getDeclaringClass().getPackage().getName(), null);
        final String[] args = findArgsName(clazz);
        return (name, config) -> (T) contextualSupplier
                .apply(() -> createObject(loader, contextualSupplier, clazz, args, name, config, metas, precomputed));
    }
}
