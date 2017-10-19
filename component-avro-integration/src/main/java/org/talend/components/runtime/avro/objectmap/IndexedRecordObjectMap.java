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
package org.talend.components.runtime.avro.objectmap;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.specific.SpecificData;
import org.talend.component.api.processor.data.ObjectMap;

public class IndexedRecordObjectMap implements ObjectMap {

    private final IndexedRecord delegate;

    private final BiFunction<String, IndexedRecord, Object> nestedFactory;

    private final Schema schema;

    private final ConcurrentMap<String, Constructor<?>> stringableConstructors;

    private Set<String> keys;

    public IndexedRecordObjectMap(final IndexedRecord input, final BiFunction<String, IndexedRecord, Object> nestedFactory,
            final ConcurrentMap<String, Constructor<?>> stringableConstructors) {
        this.delegate = input;
        this.nestedFactory = nestedFactory;
        this.schema = delegate.getSchema();
        this.stringableConstructors = stringableConstructors;
    }

    // just a passthrough impl for now, can need some more love
    // (pre-compilation for perf and type checking to be feature complete)
    @Override
    public Object get(final String location) {
        if (!location.contains(".")) { // fast branch
            final Schema.Field field = schema.getField(location);
            if (field == null) {
                return null;
            }

            final Object value = delegate.get(field.pos());
            if (IndexedRecord.class.isInstance(value)) { // need to subclass it as well
                return nestedFactory.apply(location, IndexedRecord.class.cast(value));
            } else if (GenericData.Array.class.isInstance(value)) {
                final GenericData.Array array = GenericData.Array.class.cast(value);
                return array.isEmpty() || !IndexedRecord.class.isInstance(array.get(0)) ? array
                        : array.stream()
                                .map(o -> IndexedRecord.class.isInstance(o)
                                        ? nestedFactory.apply(location, IndexedRecord.class.cast(o))
                                        : o)
                                .collect(toList());
            }
            return wrapIfNeeded(field, value);
        }

        IndexedRecord value = delegate;
        final String[] segments = location.split("\\."); // this is cacheable upper layer (in services)
        for (int i = 0; i < segments.length - 1; i++) {
            final Object nested = value.get(value.getSchema().getField(location).pos());
            if (IndexedRecord.class.isInstance(nested)) {
                value = IndexedRecord.class.cast(nested);
            } else {
                return null;
            }
        }
        // todo: keep browsing the object graph to have the right type to potentially subclass it (as in first if of the method)
        // note that a direct access (= being here) can also means we just access primitives
        final Schema.Field field = value.getSchema().getField(segments[segments.length - 1]);
        final Object val = value.get(field.pos());
        if (val == null) {
            return null;
        }
        return wrapIfNeeded(field, val);
    }

    private Object wrapIfNeeded(final Schema.Field field, final Object val) {
        if (field.schema().getType() == Schema.Type.UNION) {
            final List<Schema> types = field.schema().getTypes();
            final String type = types.size() == 2 ? types.get(1).getProp(SpecificData.CLASS_PROP) : null;
            if (type != null) {
                Constructor<?> constructor = stringableConstructors.get(type);
                if (constructor == null) {
                    try {
                        constructor = Thread.currentThread().getContextClassLoader().loadClass(type).getConstructor(String.class);
                    } catch (final NoSuchMethodException | ClassNotFoundException e) {
                        throw new IllegalArgumentException(e);
                    }
                    stringableConstructors.putIfAbsent(type, constructor);
                }
                try {
                    return constructor.newInstance(val.toString());
                } catch (final InstantiationException | IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalArgumentException(e.getTargetException());
                }
            }
        }
        return val;
    }

    @Override
    public ObjectMap getMap(final String location) {
        final Object value = get(location);
        if (value == null) {
            return null;
        }
        return IndexedRecord.class.isInstance(value)
                ? new IndexedRecordObjectMap(IndexedRecord.class.cast(value), nestedFactory/* to revisit */,
                        stringableConstructors)
                : null;
    }

    @Override // todo: actual impl, this was just to put something in there but it is not yet correct
    public Collection<ObjectMap> getCollection(final String location) {
        final Object value = get(location);
        if (value == null) {
            return null;
        }
        if (!GenericArray.class.isInstance(value)) {
            throw new IllegalArgumentException(value + " not an array");
        }
        final GenericArray<?> array = GenericArray.class.cast(value);
        return array.stream()
                .map(item -> new IndexedRecordObjectMap(IndexedRecord.class.cast(value), nestedFactory, stringableConstructors))
                .collect(toList());
    }

    @Override
    public Set<String> keys() {
        return keys == null ? (keys = schema.getFields().stream().map(Schema.Field::name).collect(toSet())) : keys;
    }

    public IndexedRecord getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return String.valueOf(delegate);
    }
}
