// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.runtime.output.data;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.talend.component.api.processor.data.ObjectMap;
import org.talend.components.runtime.base.Serializer;
import org.talend.components.runtime.serialization.ContainerFinder;
import org.talend.components.runtime.serialization.EnhancedObjectInputStream;

import lombok.AllArgsConstructor;

public class ObjectMapImpl implements ObjectMap, Serializable {

    private final String plugin;

    private final Object delegate;

    private final AccessorCache cache;

    private final boolean isMap;

    // alternative is to be eager. for now this is probably better until we ensure we only have small payloads
    private final ClassLoader loader;

    private Set<String> keys;

    public ObjectMapImpl(final String plugin, final Object delegate, final AccessorCache cache) {
        this.plugin = plugin;
        this.delegate = delegate;
        this.cache = cache;
        this.isMap = Map.class.isInstance(delegate);
        this.loader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public Object get(final String location) {
        return doFn(loader, () -> {
            if (delegate == null) {
                return null;
            }
            if (isMap) {
                final int dot = location.indexOf('.');
                if (dot > 0) {
                    return new ObjectMapImpl(plugin, Map.class.cast(delegate).get(location.substring(0, dot)), cache)
                            .get(location.substring(dot + 1));
                }
                return Map.class.cast(delegate).get(location);
            }
            final Function<Object, Object> accessor = cache.getOrCreateAccessor(location, delegate.getClass());
            return accessor.apply(delegate);
        });
    }

    @Override
    public ObjectMap getMap(final String location) {
        return doFn(loader, () -> {
            final Object delegate = get(location);
            if (delegate == null) {
                return null;
            }
            return new ObjectMapImpl(plugin, delegate, cache);
        });
    }

    @Override
    public Collection<ObjectMap> getCollection(final String location) {
        return doFn(loader, () -> {
            final Object o = get(location);
            if (o == null) {
                return null;
            }
            if (!Collection.class.isInstance(o)) {
                throw new IllegalArgumentException(o + " is not a collection");
            }
            final Collection<?> items = Collection.class.cast(o);
            return items.stream().map(item -> ObjectMap.class.isInstance(item) ? ObjectMap.class.cast(item)
                    : new ObjectMapImpl(plugin, item, cache)).collect(toList());
        });
    }

    @Override
    public synchronized Set<String> keys() { // todo: map without string key?
        return doFn(loader, () -> keys == null && delegate != null
                ? (keys = Map.class.isInstance(delegate) ? Map.class.cast(delegate).keySet() : findKeys(delegate.getClass()))
                : keys);
    }

    private Set<String> findKeys(final Class<?> c) {
        // note: handle getters? should be on anemic model so shouldn't change anything
        final Set<String> staticFields = cache.getOrCreateStaticFields(c);
        final Function<Object, Map<String, Object>> any = cache.getOrCreateAny(c);
        return Stream.concat(staticFields.stream(), any == null ? Stream.empty() : any.apply(delegate).keySet().stream())
                .collect(toSet());
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializationReplacer(plugin, Serializer.toBytes(delegate));
    }

    @Override
    public String toString() {
        return String.valueOf(delegate);
    }

    private static <T> T doFn(final ClassLoader loader, final Supplier<T> supplier) { // note: one option is to let the caller
                                                                                      // handling it (for perf)
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try {
            return supplier.get();
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    @AllArgsConstructor
    private static class SerializationReplacer implements Serializable {

        private final String plugin;

        private final byte[] value;

        Object readResolve() throws ObjectStreamException {
            final ClassLoader loader = ContainerFinder.Instance.get().find(plugin).classloader();
            try {
                return doFn(loader, () -> {
                    try {
                        return new ObjectMapImpl(plugin, loadDelegate(loader),
                                ContainerFinder.Instance.get().find(plugin).findService(AccessorCache.class));

                    } catch (final IOException | ClassNotFoundException e) {
                        throw new IllegalStateException(new InvalidObjectException(e.getMessage()));
                    }
                });
            } catch (final IllegalStateException ise) {
                throw InvalidObjectException.class.cast(ise.getCause());
            }
        }

        private Object loadDelegate(final ClassLoader loader) throws IOException, ClassNotFoundException {
            try (final ObjectInputStream ois = new EnhancedObjectInputStream(new ByteArrayInputStream(value), loader)) {
                return ois.readObject();
            }
        }
    }
}
