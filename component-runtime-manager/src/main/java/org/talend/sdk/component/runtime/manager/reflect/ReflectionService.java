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
package org.talend.sdk.component.runtime.manager.reflect;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.beans.ConstructorProperties;
import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.apache.xbean.propertyeditor.PropertyEditors;
import org.apache.xbean.recipe.ObjectRecipe;
import org.apache.xbean.recipe.UnsetPropertiesRecipe;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReflectionService {

    private final ParameterModelService parameterModelService;

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
            final Map<Class<?>, Object> precomputed) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Function<Supplier<Object>, Object> contextualSupplier = supplier -> {
            final Thread thread = Thread.currentThread();
            final ClassLoader old = thread.getContextClassLoader();
            thread.setContextClassLoader(loader);
            try {
                return supplier.get();
            } finally {
                thread.setContextClassLoader(old);
            }
        };
        final Collection<Function<Map<String, String>, Object>> factories =
                Stream.of(executable.getParameters()).map(parameter -> {
                    final String name = parameterModelService.findName(parameter);
                    final Type parameterizedType = parameter.getParameterizedType();
                    if (Class.class.isInstance(parameterizedType)) {
                        final Object value = precomputed.get(parameterizedType);
                        if (value != null) {
                            return (Function<Map<String, String>, Object>) config -> value;
                        }
                        final BiFunction<String, Map<String, Object>, Object> objectFactory =
                                createObjectFactory(loader, contextualSupplier, parameterizedType);
                        return (Function<Map<String, String>, Object>) config -> objectFactory.apply(name,
                                Map.class.cast(config));
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
                                if (!services.isEmpty()) {
                                    return (Function<Map<String, String>, Object>) config -> services;
                                }

                                // here we know we just want to instantiate a config list and not services
                                final Collector collector = Set.class == collectionType ? toSet() : toList();
                                final BiFunction<String, Map<String, Object>, Object> itemFactory =
                                        createObjectFactory(loader, contextualSupplier, itemClass);
                                return (Function<Map<String, String>, Object>) config -> createList(loader,
                                        contextualSupplier, name, collectionType, itemClass, collector, itemFactory,
                                        Map.class.cast(config));
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
                                final BiFunction<String, Map<String, Object>, Object> keyItemFactory =
                                        createObjectFactory(loader, contextualSupplier, keyItemClass);
                                final BiFunction<String, Map<String, Object>, Object> valueItemFactory =
                                        createObjectFactory(loader, contextualSupplier, valueItemClass);
                                final Collector collector = createMapCollector(mapType, keyItemClass, valueItemClass);
                                return (Function<Map<String, String>, Object>) config -> createMap(name, mapType,
                                        keyItemFactory, valueItemFactory, collector, Map.class.cast(config));
                            }
                        }
                    }

                    throw new IllegalArgumentException("Unsupported type: " + parameterizedType);
                }).collect(toList());

        return config -> {
            final Map<String, String> notNullConfig = ofNullable(config).orElseGet(Collections::emptyMap);
            return factories.stream().map(f -> f.apply(notNullConfig)).toArray(Object[]::new);
        };
    }

    private Collector createMapCollector(final Class<?> mapType, final Class<?> keyItemClass,
            final Class<?> valueItemClass) {
        final Function<Map.Entry<?, ?>, Object> keyMapper = o -> doConvert(keyItemClass, o.getKey());
        final Function<Map.Entry<?, ?>, Object> valueMapper = o -> doConvert(valueItemClass, o.getValue());
        return ConcurrentMap.class.isAssignableFrom(mapType) ? toConcurrentMap(keyMapper, valueMapper)
                : toMap(keyMapper, valueMapper);
    }

    private Object createList(final ClassLoader loader, final Function<Supplier<Object>, Object> contextualSupplier,
            final String name, final Class<?> collectionType, final Class<?> itemClass, final Collector collector,
            final BiFunction<String, Map<String, Object>, Object> itemFactory, final Map<String, Object> config) {
        final Object obj = config.get(name);
        if (collectionType.isInstance(obj)) {
            return Collection.class.cast(obj).stream().map(o -> doConvert(itemClass, o)).collect(collector);
        }

        // try to build it from the properties
        // <value>[<index>] = xxxxx
        // <value>[<index>].<property> = xxxxx
        final Collection collection = List.class.isAssignableFrom(collectionType) ? new ArrayList() : new HashSet();
        int paramIdx = 0;
        String[] args = null;
        do {
            final String configName = String.format("%s[%d]", name, paramIdx);
            if (!config.containsKey(configName)) {
                if (config.keySet().stream().anyMatch(k -> k.startsWith(configName + "."))) { // object
                                                                                              // mapping
                    if (paramIdx == 0) {
                        args = findArgsName(itemClass);
                    }
                    collection.add(createObject(loader, contextualSupplier, itemClass, args, configName, config));
                } else {
                    break;
                }
            } else {
                collection.add(itemFactory.apply(configName, config));
            }
            paramIdx++;
        } while (true);

        return collection;
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
            final Function<Supplier<Object>, Object> contextualSupplier, final Type type) {
        final Class clazz = Class.class.cast(type);
        if (clazz.isPrimitive() || Primitives.unwrap(clazz) != clazz || String.class == clazz) {
            return (name, config) -> doConvert(clazz, config.get(name));
        }

        final String[] args = findArgsName(clazz);
        return (name, config) -> contextualSupplier
                .apply(() -> createObject(loader, contextualSupplier, clazz, args, name, config));
    }

    private String[] findArgsName(final Class clazz) {
        return Stream
                .of(clazz.getConstructors())
                .filter(c -> c.isAnnotationPresent(ConstructorProperties.class))
                .findFirst()
                .map(c -> ConstructorProperties.class.cast(c.getAnnotation(ConstructorProperties.class)).value())
                .orElse(null);
    }

    private Object createObject(final ClassLoader loader, final Function<Supplier<Object>, Object> contextualSupplier,
            final Class clazz, final String[] args, final String name, final Map<String, Object> config) {
        if (PropertyEditors.canConvert(clazz) && config.size() == 1) { // direct conversion using the configured
                                                                       // converter/editor
            final Object configValue = config.values().iterator().next();
            if (String.class.isInstance(configValue)) {
                return PropertyEditors.getValue(clazz, String.class.cast(configValue));
            }
        }

        final String prefix = name + ".";
        final ObjectRecipe recipe = new ObjectRecipe(clazz);
        recipe.allow(org.apache.xbean.recipe.Option.FIELD_INJECTION);
        recipe.allow(org.apache.xbean.recipe.Option.PRIVATE_PROPERTIES);
        recipe.allow(org.apache.xbean.recipe.Option.CASE_INSENSITIVE_PROPERTIES);
        recipe.allow(org.apache.xbean.recipe.Option.IGNORE_MISSING_PROPERTIES);
        recipe.setProperty("rawProperties", new UnsetPropertiesRecipe()); // allows to access not matched properties
                                                                          // directly
        ofNullable(args).ifPresent(recipe::setConstructorArgNames);

        final Map<String, Object> specificMapping =
                config.entrySet().stream().filter(e -> e.getKey().startsWith(prefix)).collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue));

        // extract map configuration
        final Map<String, Object> mapEntries = specificMapping.entrySet().stream().filter(e -> {
            final String key = e.getKey();
            final int idxStart = key.indexOf('[', prefix.length());
            return idxStart > 0 && ((idxStart > ".key".length()
                    && key.substring(idxStart - ".key".length(), idxStart).equals(".key"))
                    || (idxStart > ".value".length()
                            && key.substring(idxStart - ".value".length(), idxStart).equals(".value")));
        }).sorted(this::sortIndexEntry).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
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

            final Type genericType =
                    findField(enclosingName.substring(enclosingName.indexOf('.') + 1), clazz).getGenericType();
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

            final Class<?> keyType = Class.class.cast(pt.getActualTypeArguments()[0]);
            final Class<?> valueType = Class.class.cast(pt.getActualTypeArguments()[1]);
            preparedMaps.put(enclosingName,
                    createMap(prefix + enclosingName, Map.class,
                            createObjectFactory(loader, contextualSupplier, keyType),
                            createObjectFactory(loader, contextualSupplier, valueType),
                            createMapCollector(Class.class.cast(pt.getRawType()), keyType, valueType),
                            new HashMap<>(mapEntries)));
        }

        // extract list configuration
        final Map<String, Object> listEntries = specificMapping.entrySet().stream().filter(e -> {
            final String key = e.getKey();
            final int idxStart = key.indexOf('[', prefix.length());
            final int idxEnd = key.indexOf(']', prefix.length());
            final int sep = key.indexOf('.', prefix.length() + 1);
            return idxStart > 0 && key.endsWith("]") && (sep > idxEnd || sep < 0);
        }).sorted(this::sortIndexEntry).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        listEntries.keySet().forEach(specificMapping::remove);
        final Map<String, Object> preparedLists = new HashMap<>();
        for (final Map.Entry<String, Object> entry : listEntries.entrySet()) {
            final String key = entry.getKey();
            final int idxStart = key.indexOf('[', prefix.length());
            final String enclosingName = key.substring(prefix.length(), idxStart);
            if (preparedLists.containsKey(enclosingName)) {
                continue;
            }

            final Type genericType = findField(enclosingName, clazz).getGenericType();
            if (Class.class.isInstance(genericType)) {
                final Class<?> arrayClass = Class.class.cast(genericType);
                if (arrayClass.isArray()) {
                    // we could use Array.newInstance but for now use the list, shouldn't impact
                    // much the perf
                    final Collection<?> list = Collection.class.cast(createList(loader, contextualSupplier,
                            prefix + enclosingName, List.class, arrayClass.getComponentType(), toList(),
                            createObjectFactory(loader, contextualSupplier, arrayClass.getComponentType()),
                            new HashMap<>(listEntries)));

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

            if (!ParameterizedType.class.isInstance(genericType)) {
                throw new IllegalArgumentException(
                        clazz + "#" + enclosingName + " should be a generic collection and not a " + genericType);
            }
            final ParameterizedType pt = ParameterizedType.class.cast(genericType);
            if (pt.getActualTypeArguments().length != 1 || !Class.class.isInstance(pt.getActualTypeArguments()[0])) {
                throw new IllegalArgumentException(clazz + "#" + enclosingName
                        + " should use concrete class items and not a " + pt.getActualTypeArguments()[0]);
            }
            final Type itemType = pt.getActualTypeArguments()[0];
            preparedLists.put(enclosingName,
                    createList(loader, contextualSupplier, prefix + enclosingName, Class.class.cast(pt.getRawType()),
                            Class.class.cast(itemType), toList(),
                            createObjectFactory(loader, contextualSupplier, itemType), new HashMap<>(listEntries)));
        }

        // extract nested Object configurations
        final Map<String, Object> objectEntries = specificMapping.entrySet().stream().filter(e -> {
            final String key = e.getKey();
            return key.indexOf('.', prefix.length() + 1) > 0;
        }).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        objectEntries.keySet().forEach(specificMapping::remove);
        final Map<String, Object> preparedObjects = new HashMap<>();
        for (final Map.Entry<String, Object> entry : objectEntries.entrySet()) {
            final String nestedName =
                    entry.getKey().substring(prefix.length(), entry.getKey().indexOf('.', prefix.length() + 1));
            if (nestedName.endsWith("]")) {
                final int idxStart = nestedName.indexOf('[');
                if (idxStart > 0) {
                    final String listName = nestedName.substring(0, idxStart);
                    final Field field = findField(listName, clazz);
                    if (ParameterizedType.class.isInstance(field.getGenericType())) {
                        final ParameterizedType pt = ParameterizedType.class.cast(field.getGenericType());
                        if (Class.class.isInstance(pt.getRawType())) {
                            final Class<?> rawType = Class.class.cast(pt.getRawType());
                            if (Set.class.isAssignableFrom(rawType)) {
                                final Collection<Object> aggregator = Collection.class
                                        .cast(preparedObjects.computeIfAbsent(listName, k -> new HashSet<>(2)));
                                final Class<?> itemType = Class.class.cast(pt.getActualTypeArguments()[0]);
                                aggregator.add(createObject(loader, contextualSupplier, itemType,
                                        findArgsName(itemType), prefix + nestedName, config));
                            } else if (Collection.class.isAssignableFrom(rawType)) {
                                final Collection<Object> aggregator = Collection.class
                                        .cast(preparedObjects.computeIfAbsent(listName, k -> new ArrayList<>(2)));
                                final Class<?> itemType = Class.class.cast(pt.getActualTypeArguments()[0]);
                                aggregator.add(createObject(loader, contextualSupplier, itemType,
                                        findArgsName(itemType), prefix + nestedName, config));
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
            final Field field = findField(nestedName, clazz);
            preparedObjects.put(nestedName, createObject(loader, contextualSupplier, field.getType(),
                    findArgsName(field.getType()), prefix + nestedName, config));
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

        // now bind it all to the recipe and create the instance
        preparedMaps.forEach(recipe::setProperty);
        preparedLists.forEach(recipe::setProperty);
        preparedObjects.forEach(recipe::setProperty);
        normalizedConfig.forEach(recipe::setProperty);
        return recipe.create(loader);
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
        throw new IllegalArgumentException("Unknown field: " + name);
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
                return Integer.parseInt(idx1) - Integer.parseInt(idx2);
            } // else not matching so use default sorting
        }
        return name1.compareTo(name2);
    }

    private Object doConvert(final Class<?> type, final Object value) {
        if (value == null) { // get the primitive default
            return getPrimitiveDefault(type);
        }
        if (type.isInstance(value)) { // no need of any conversion
            return value;
        }
        if (PropertyEditors.canConvert(type)) { // go through string to convert the value
            return PropertyEditors.getValue(type, String.valueOf(value));
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
}
