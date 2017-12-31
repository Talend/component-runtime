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
package org.talend.sdk.component.runtime.output.data;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import org.talend.sdk.component.api.processor.data.ObjectMap;
import org.talend.sdk.component.runtime.serialization.SerializableService;

public class AccessorCache implements Serializable {

    private final ConcurrentMap<Class<?>, ClassCache> cache = new ConcurrentHashMap<>();

    protected final String plugin;

    // for deserialization
    protected AccessorCache() {
        plugin = null;
    }

    public AccessorCache(final String plugin) {
        this.plugin = plugin;
    }

    public Function<Object, Object> getOrCreateAccessor(final String location, final Class<?> from) {
        final ClassCache classCache = getClassCache(from);
        Function<Object, Object> accessor = classCache.cache.get(location);
        if (accessor == null) {
            final Function<Object, Object> function = getFromClass(from, location);
            if (function != null) {
                accessor = function;
                classCache.cache.putIfAbsent(location, function);
                return accessor;
            } else { // try to find a @Any
                final Function<Object, Map<String, Object>> any = getOrCreateAny(from);
                if (any != null) {
                    return o -> any.apply(o).get(location);
                }
            }
        }
        final Function<Object, Object> provider = classCache.cache.get(location);
        return provider != null ? provider : o -> null;
    }

    public Function<Object, Map<String, Object>> getOrCreateAny(final Class<?> from) {
        final ClassCache classCache = getClassCache(from);
        Function<Object, Map<String, Object>> any = classCache.any.get();
        if (any == null) {
            any = findAny(from);
            if (any != null) {
                if (!classCache.any.compareAndSet(null, any)) {
                    any = classCache.any.get();
                }
            } // do we want to put something empty here to ensure we don't look for it again
              // and again?
        }
        return any;
    }

    public Set<String> getOrCreateStaticFields(final Class<?> from) {
        final ClassCache classCache = cache.computeIfAbsent(from, type -> new ClassCache());
        Set<String> fields = classCache.staticFields.get();
        if (fields == null) {
            fields = findStaticFields(from);
            if (fields != null) {
                if (!classCache.staticFields.compareAndSet(null, fields)) {
                    fields = classCache.staticFields.get();
                }
            }
        }
        return fields;
    }

    private ClassCache getClassCache(final Class<?> from) {
        ClassCache classCache = cache.get(from); // faster than computeIfAbsent at runtime
        if (classCache == null) {
            classCache = new ClassCache();
            final ClassCache existing = cache.putIfAbsent(from, classCache);
            if (existing != null) {
                classCache = existing;
            }
        }
        return classCache;
    }

    private Set<String> findStaticFields(final Class<?> from) {
        return Stream
                .concat(Stream
                        .of(from.getDeclaredFields())
                        .filter(f -> !f.isAnnotationPresent(ObjectMap.Any.class))
                        .filter(f -> !Modifier.isStatic(f.getModifiers()))
                        .map(Field::getName),
                        ofNullable(from.getSuperclass())
                                .filter(s -> s != Object.class && s != from)
                                .map(this::findStaticFields)
                                .orElseGet(Collections::emptySet)
                                .stream())
                .collect(toSet());
    }

    private Function<Object, Map<String, Object>> findAny(final Class<?> from) {
        return from == Object.class || from == null ? null
                : Stream
                        .of(from.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(ObjectMap.Any.class))
                        .filter(f -> {
                            final Type genericType = f.getGenericType();
                            if (!ParameterizedType.class.isInstance(genericType)) {
                                return false;
                            }
                            final ParameterizedType parameterizedType = ParameterizedType.class.cast(genericType);
                            return Map.class == parameterizedType.getRawType()
                                    && String.class == parameterizedType.getActualTypeArguments()[0];
                        })
                        .findFirst()
                        .map(field -> {
                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            return (Function<Object, Map<String, Object>>) o -> {
                                try {
                                    return (Map<String, Object>) field.get(o);
                                } catch (final IllegalAccessException e) {
                                    return null;
                                }
                            };
                        })
                        .orElseGet(() -> findAny(from.getSuperclass()));
    }

    private Function<Object, Object> getFromClass(final Class<?> containerType, final String location) {
        if (location.contains(".")) {
            final String[] segments = location.split("\\.");

            Function<Object, Object> fn = identity();
            for (int i = 0; i < segments.length; i++) {
                final int idx = i;
                fn = fn.andThen(o -> getOrCreateAccessor(segments[idx], o.getClass()).apply(o));
            }
            return fn;
        }
        Class<?> current = containerType;
        do {
            try {
                final Field field = current.getDeclaredField(location);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                return toFieldAccessor(field);
            } catch (final NoSuchFieldException e) {
                // try next
            }
            current = current.getSuperclass();
        } while (current != Object.class);

        return null;
    }

    protected Function<Object, Object> toFieldAccessor(final Field field) {
        return o -> {
            try {
                return field.get(o);
            } catch (final IllegalAccessException e) {
                return null;
            }
        };
    }

    public void reset() {
        cache.values().forEach(c -> c.cache.clear());
        cache.clear();
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, AccessorCache.class.getName());
    }

    public static class ClassCache {

        private final ConcurrentMap<String, Function<Object, Object>> cache = new ConcurrentHashMap<>();

        private final AtomicReference<Function<Object, Map<String, Object>>> any = new AtomicReference<>();

        private final AtomicReference<Set<String>> staticFields = new AtomicReference<>();
    }
}
