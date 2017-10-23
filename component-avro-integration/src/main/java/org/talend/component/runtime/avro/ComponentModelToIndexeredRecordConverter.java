/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.component.runtime.avro;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.reflect.Stringable;
import org.apache.avro.specific.SpecificData;
import org.talend.component.api.processor.data.ObjectMap;
import org.talend.component.runtime.avro.objectmap.IndexedRecordObjectMap;
import org.talend.component.runtime.manager.asm.ProxyGenerator;
import org.talend.component.runtime.manager.processor.SubclassesCache;

import lombok.Data;

// todo: review org.talend.core.model.metadata.DiSchemaConstants for a better studio integration
public class ComponentModelToIndexeredRecordConverter { // todo: support avro.reflect annotations

    private final ConcurrentMap<Class<?>, Meta> metas = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Constructor<?>> stringableConstructors = new ConcurrentHashMap<>();

    public <T> T reverseMapping(final SubclassesCache cache, final IndexedRecord input, final Class<T> expectedType) {
        try {
            final IndexedRecordObjectMap objectMap = new IndexedRecordObjectMap(input, (field, ir) -> {

                final Meta meta = computeMeta(expectedType, new HashMap<>(), null, null);
                final FieldMeta fm = meta.fieldsByName.get(field);
                if (fm == null) {
                    return null;
                }
                return reverseMapping(cache, ir, fm.getRecordType());
            }, stringableConstructors);

            if (ObjectMap.class.isAssignableFrom(expectedType)) {
                return expectedType.cast(objectMap);
            }

            return expectedType.cast(cache.find(expectedType).newInstance(objectMap));
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }

    public IndexedRecord map(final Object input) {
        if (ObjectMap.class.isInstance(input)) {
            throw new IllegalArgumentException("ObjectMap not yet supported as output");
        }
        return IndexedRecord.class.cast(mapWithCyclicReferences(input, new HashMap<>()));
    }

    private GenericContainer mapWithCyclicReferences(final Object input, final Map<Object, IndexedRecord> visited) {
        if (input == null) {
            return null;
        }

        final IndexedRecord existing = visited.get(input);
        if (existing != null) {
            return existing;
        }

        if (ProxyGenerator.GetObjectMap.class.isInstance(input)) {
            final ObjectMap objectMap = ProxyGenerator.GetObjectMap.class.cast(input).getObjectMap();
            if (IndexedRecordObjectMap.class.isInstance(objectMap)) {
                return IndexedRecordObjectMap.class.cast(objectMap).getDelegate();
            }
        }

        final Class<?> inputClass = input.getClass();
        if (Map.class.isAssignableFrom(inputClass)) {
            final Map<String, ?> mappedMap = mapToIR(Map.class.cast(input), visited);
            final SchemaBuilder.TypeBuilder<Schema> rootBuilder = SchemaBuilder.builder();
            final SchemaBuilder.FieldAssembler<Schema> builder = rootBuilder.record("talend.dynamic.Map")
                    .prop(SpecificData.CLASS_PROP, "java.util.HashMap").fields();
            mappedMap.forEach((k, v) -> toAvroField(k, v.getClass(), new HashMap<>(), builder, rootBuilder));
            final GenericData.Record record = new GenericData.Record(builder.endRecord());
            visited.put(input, record);
            record.getSchema().getFields().forEach(f -> record.put(f.pos(), mappedMap.get(f.name())));
            return record;
        }

        if (Collection.class.isAssignableFrom(inputClass)) {
            final Collection<?> collection = Collection.class.cast(input);
            final Object first = collection.isEmpty() ? null : collection.iterator().next();
            if (first == null) {
                return new GenericData.Array<>(0, Schema.createArray(SchemaBuilder.builder().record("empty_record")
                        .prop(SpecificData.CLASS_PROP, "java.lang.Object").fields().endRecord()));
            }
            final SchemaBuilder.TypeBuilder<Schema> rootBuilder = SchemaBuilder.builder();
            final Class<?> itemType = first.getClass();
            final SchemaBuilder.FieldAssembler<Schema> builder = rootBuilder.record(toAvroRecordName(itemType))
                    .prop(SpecificData.CLASS_PROP, itemType

                            .getName())
                    .fields();
            final GenericData.Array array = new GenericData.Array<>(collection.size(),
                    Schema.createArray(computeMeta(itemType, new HashMap<>(), builder, rootBuilder).schema));
            collection.forEach(o -> {
                final Object item = doMap(o, visited);
                array.add(item);
            });
            return array;
        }

        final Class<?> type = unwrap(inputClass);
        final Meta meta = getMeta(type);
        Schema schema = meta.schema;
        if (meta.hasAny) {
            // dynamic, need to create the schema each time - todo: add a @Any(static = true) to be able to cache it as well
            schema = createDynamicMapSchema(input, meta, schema);
        }

        final GenericData.Record record = new GenericData.Record(schema);
        visited.put(input, record);
        for (final FieldMeta fm : meta.fields.values()) {
            try {
                final Object value = fm.field.get(input);
                if (value == null) {
                    continue;
                }

                if (fm.any) { // here we are safe since we already updated the schema to support it so just assume schema is ready
                    final Map<String, ?> values = Map.class.cast(value);
                    if (values == null) {
                        continue;
                    }

                    values.forEach((k, v) -> {
                        final Schema.Field field = record.getSchema().getField(k);
                        record.put(field.pos(), doMap(v, visited));
                    });
                    continue;
                }

                final Schema.Type sType = fm.avroField.schema().getType();
                if (sType == Schema.Type.RECORD) {
                    record.put(fm.avroField.pos(), doMap(value, visited));
                } else if (sType == Schema.Type.ARRAY) {
                    if (GenericData.Array.class.isInstance(value)) {
                        final GenericData.Array array = GenericData.Array.class.cast(value);
                        if (array.isEmpty() || !IndexedRecord.class.isInstance(array.get(0))) {
                            record.put(fm.avroField.pos(), doMap(value, visited));
                        } else {
                            record.put(fm.avroField.pos(), array.stream().map(o -> doMap(o, visited)).collect(toList()));
                        }
                    } else {
                        record.put(fm.avroField.pos(), doMap(value, visited));
                    }
                } else {
                    record.put(fm.avroField.pos(), value);
                }
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return record;
    }

    private Schema createDynamicMapSchema(final Object input, final Meta meta, final Schema schema) {
        final SchemaBuilder.TypeBuilder<Schema> rootBuilder = SchemaBuilder.builder();
        final SchemaBuilder.FieldAssembler<Schema> builder = rootBuilder.record(schema.getName())
                .prop(SpecificData.CLASS_PROP, input.getClass().getName()).namespace(schema.getNamespace()).doc(schema.getDoc())
                .fields();
        for (final FieldMeta fm : meta.fields.values()) {
            try {
                if (!fm.any) {
                    continue;
                }
                final Object value = fm.field.get(input);
                final Map<String, ?> values = Map.class.cast(value);
                if (values == null) {
                    continue;
                }

                values.forEach((key, val) -> toAvroField(key, val.getClass(), new HashMap<>(), builder, rootBuilder));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return builder.endRecord();
    }

    private Object doMap(final Object input, final Map<Object, IndexedRecord> visited) {
        final IndexedRecord existing = visited.get(input);
        if (existing != null) {
            return existing;
        }

        final Class<?> inputClass = input.getClass();
        if (Map.class.isAssignableFrom(inputClass)) {
            // todo: add in component a @StaticModel (like in groovy for instance) to compute once the model

            // for now always compute a new schema - perf are bad but we'll enhance it later if needed
            // here we have 2 cases:
            // 1. the map is strongly typed (generics) -> compute a static model
            // 2. the map is loosely typed (<String, Object> for instance) -> stay dynamic and slow

            final Type map = Stream.of(inputClass.getGenericInterfaces()).filter(t -> {
                if (!ParameterizedType.class.isInstance(t)) {
                    return false;
                }
                final Type rawType = ParameterizedType.class.cast(t).getRawType();
                return Class.class.isInstance(rawType) && Map.class.isAssignableFrom(Class.class.cast(rawType));
            }).findFirst().orElse(null);

            final Map<?, ?> asMap = Map.class.cast(input);
            if (map != null) { // todo: check org.apache.avro.reflect.ReflectData.createSchema() out to support more cases
                final ParameterizedType pt = ParameterizedType.class.cast(map);
                if (pt.getActualTypeArguments().length == 2 && Class.class.isInstance(pt.getActualTypeArguments()[0])
                        && Class.class.isInstance(pt.getActualTypeArguments()[1])) {
                    final Class<?> key = Class.class.cast(pt.getActualTypeArguments()[0]);
                    if (key != String.class) {
                        throw new IllegalArgumentException("Map keys can only be string for now, found: " + key);
                    }
                    return mapToIR(asMap, visited);
                } else {
                    dynamicMapValidation(input, asMap);
                }
            } else {
                dynamicMapValidation(input, asMap);
            }

            return mapToIR(asMap, visited);
        }
        if (isPrimitive(inputClass)) {
            return input;
        }
        if (isStringable(inputClass)) {
            return input.toString();
        }
        return mapWithCyclicReferences(input, visited);
    }

    private void dynamicMapValidation(final Object input, final Map<?, ?> asMap) {
        if (asMap.isEmpty()) {
            throw new IllegalArgumentException("Empty Map<?, ?> unsupported: " + input);
        }
        if (!asMap.keySet().stream().allMatch(String.class::isInstance)) {
            throw new IllegalArgumentException("Only Map<String, ?> are supported, got " + input);
        }
        final Optional<? extends Class<?>> valueType = asMap.values().stream().map(Object::getClass).reduce((t1, t2) -> {
            if (t1 == Object.class) {
                return t2;
            }
            if (t2 == Object.class) {
                return t1;
            }
            if (t1.isAssignableFrom(t2)) {
                return t2;
            }
            if (t2.isAssignableFrom(t1)) {
                return t1;
            }
            throw new IllegalArgumentException(
                    t1 + " and " + t2 + " are not compatible, can't convert " + input + " to an IndexedRecord");
        });
        if (!valueType.isPresent()) {
            throw new IllegalArgumentException("Can't determine value type of " + input);
        }
    }

    private boolean isStringable(final Class<?> type) {
        return File.class == type || URI.class == type || URL.class == type || BigInteger.class == type
                || BigDecimal.class == type
                /* we shouldn't support it strictly speaking since it means component are avro dependent which is wrong */
                || type.isAssignableFrom(Stringable.class);
    }

    private boolean isPrimitive(final Class<?> type) {
        return type == int.class || type == long.class || type == double.class || type == float.class || type == boolean.class
                || type == Integer.class || type == Long.class || type == Double.class || type == Float.class
                || type == Boolean.class || type == String.class;
    }

    private Map<String, ?> mapToIR(final Map<?, ?> input, final Map<Object, IndexedRecord> visited) {
        final Set<? extends Map.Entry<?, ?>> entrySet = input.entrySet();
        return entrySet.stream().collect(toMap(e -> String.class.cast(e.getKey()), e -> doMap(e.getValue(), visited)));
    }

    public Meta getMeta(final Class<?> type) {
        Meta meta = metas.get(type);
        if (meta == null) {
            meta = computeMeta(type, new HashMap<>(), null, null);
            metas.putIfAbsent(type, meta); // not important if for a few iterations/threads we use different meta instances
        }
        return meta;
    }

    private Class<?> unwrap(final Class<?> type) {
        if (type.getName().contains("$$")) {
            return unwrap(type.getSuperclass());
        }
        return type;
    }

    private Meta computeMeta(final Class<?> type, final Map<Class<?>, String> schemaNames,
            final SchemaBuilder.FieldAssembler<Schema> builder, final SchemaBuilder.TypeBuilder<Schema> providedRootBuilder) {
        final Map<Integer, FieldMeta> reflects = new HashMap<>();
        Class<?> current = type;
        final String recordName = toAvroRecordName(type);
        schemaNames.putIfAbsent(current, recordName);
        final SchemaBuilder.TypeBuilder<Schema> rootBuilder = providedRootBuilder == null ? SchemaBuilder.builder()
                : providedRootBuilder;
        SchemaBuilder.FieldAssembler<Schema> record = builder == null
                ? rootBuilder.record(recordName).prop(SpecificData.CLASS_PROP, type.getName()).fields()
                : builder;
        final Collection<Field> fields = new ArrayList<>();
        while (current != null && current != Object.class) {
            for (final Field f : current.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }

                if (!f.isAccessible()) {
                    f.setAccessible(true);
                }

                fields.add(f);

                if (f.isAnnotationPresent(ObjectMap.Any.class)) {
                    continue;
                }

                record = toAvroField(f.getName(), f.getGenericType(), schemaNames, record, rootBuilder);
            }
            current = current.getSuperclass();
        }
        final Schema schema = record.endRecord();
        final AtomicInteger anyCounter = new AtomicInteger(schema.getFields().size());
        fields.forEach(f -> {
            if (f.isAnnotationPresent(ObjectMap.Any.class)) {
                reflects.put(anyCounter.getAndIncrement(), new FieldMeta(f, null, true, f.getType()));
            } else {
                final Schema.Field field = schema.getField(f.getName());
                final FieldMeta fieldMeta = toMeta(f, field);
                reflects.put(field.pos(), fieldMeta);
            }
        });
        return new Meta(schema, reflects.values().stream().anyMatch(f -> f.any), reflects,
                reflects.values().stream().collect(toMap(f -> f.field.getName(), identity())));
    }

    private String toAvroRecordName(final Class<?> type) {
        return type.getName().replace("$", "_");
    }

    private SchemaBuilder.FieldAssembler<Schema> toAvroField(final String name, final Type fType,
            final Map<Class<?>, String> schemaNames, final SchemaBuilder.FieldAssembler<Schema> builder,
            final SchemaBuilder.TypeBuilder<Schema> rootBuilder) {
        if (fType == int.class) {
            return builder.name(name).type().intType().intDefault(0);
        } else if (fType == long.class) {
            return builder.name(name).type().longType().longDefault(0);
        } else if (fType == double.class) {
            return builder.name(name).type().doubleType().doubleDefault(0);
        } else if (fType == float.class) {
            return builder.name(name).type().floatType().floatDefault(0);
        } else if (fType == boolean.class) {
            return builder.name(name).type().booleanType().booleanDefault(false);
        } else if (fType == Integer.class) {
            return builder.name(name).type().unionOf().nullType().and().intType().endUnion().nullDefault();
        } else if (fType == Long.class) {
            return builder.name(name).type().unionOf().nullType().and().longType().endUnion().nullDefault();
        } else if (fType == Double.class) {
            return builder.name(name).type().unionOf().nullType().and().doubleType().endUnion().nullDefault();
        } else if (fType == Float.class) {
            return builder.name(name).type().unionOf().nullType().and().floatType().endUnion().nullDefault();
        } else if (fType == Boolean.class) {
            return builder.name(name).type().unionOf().nullType().and().booleanType().endUnion().nullDefault();
        } else if (fType == String.class) {
            return builder.name(name).type().unionOf().nullType().and().stringType().endUnion().nullDefault();
        }

        if (Class.class.isInstance(fType)) {
            final Class<?> clazz = Class.class.cast(fType);
            if (isStringable(clazz)) {
                final SchemaBuilder.StringBldr<SchemaBuilder.UnionAccumulator<SchemaBuilder.NullDefault<Schema>>> sBuilder = builder
                        .name(name).type().unionOf().nullType().and().stringBuilder();
                if (fType != String.class) {
                    sBuilder.prop(SpecificData.CLASS_PROP, clazz.getName());
                }
                return sBuilder.endString().endUnion().nullDefault();
            }

            if (clazz.isArray()) {
                return builder.name(name).prop("di.column.talendType", "id_List")
                        .type(Schema.createArray(getSchema(schemaNames, rootBuilder, clazz.getComponentType()))).noDefault();
            }

            String schemaName = schemaNames.get(clazz);
            if (schemaName == null) {
                schemaName = toAvroRecordName(clazz);
                computeMeta(clazz, schemaNames,
                        rootBuilder.record(schemaName).prop(SpecificData.CLASS_PROP, clazz.getName()).fields(), rootBuilder);
                schemaNames.put(clazz, schemaName);
            }
            return builder.name(name).type(schemaName).noDefault();
        }

        if (ParameterizedType.class.isInstance(fType)) {
            final ParameterizedType pt = ParameterizedType.class.cast(fType);
            if (Class.class.isInstance(pt.getRawType()) && Collection.class.isAssignableFrom(Class.class.cast(pt.getRawType()))
                    && pt.getActualTypeArguments().length == 1 && Class.class.isInstance(pt.getActualTypeArguments()[0])) {
                final Class<?> component = Class.class.cast(pt.getActualTypeArguments()[0]);
                return builder.name(name).prop("di.column.talendType", "id_List")
                        .type(Schema.createArray(getSchema(schemaNames, rootBuilder, component))).noDefault();
            }
        }

        throw new IllegalArgumentException("Unsupported type: " + fType);
    }

    // could be cached
    private Schema getSchema(final Map<Class<?>, String> schemaNames, final SchemaBuilder.TypeBuilder<Schema> rootBuilder,
            final Class<?> fType) {
        if (isPrimitive(fType)) {
            if (fType == int.class) {
                return Schema.create(Schema.Type.INT);
            } else if (fType == long.class) {
                return Schema.create(Schema.Type.LONG);
            } else if (fType == double.class) {
                return Schema.create(Schema.Type.DOUBLE);
            } else if (fType == float.class) {
                return Schema.create(Schema.Type.FLOAT);
            } else if (fType == boolean.class) {
                return Schema.create(Schema.Type.BOOLEAN);
            } else if (fType == Integer.class) {
                return Schema.createUnion(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.INT));
            } else if (fType == Long.class) {
                return Schema.createUnion(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.LONG));
            } else if (fType == Double.class) {
                return Schema.createUnion(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.DOUBLE));
            } else if (fType == Float.class) {
                return Schema.createUnion(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.FLOAT));
            } else if (fType == Boolean.class) {
                return Schema.createUnion(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.BOOLEAN));
            } else if (fType == String.class) {
                return Schema.create(Schema.Type.STRING);
            }
        }
        return computeMeta(fType, schemaNames,
                rootBuilder.record(toAvroRecordName(fType)).prop(SpecificData.CLASS_PROP, fType.getName()).fields(),
                rootBuilder).schema;
    }

    private FieldMeta toMeta(final Field f, final Schema.Field avro) {
        final Type genericType = f.getGenericType();
        if (ParameterizedType.class.isInstance(genericType)) {
            final ParameterizedType pt = ParameterizedType.class.cast(genericType);
            if (Class.class.isInstance(pt.getRawType()) && Collection.class.isAssignableFrom(Class.class.cast(pt.getRawType()))) {
                final Class<?> fType = Class.class.cast(pt.getActualTypeArguments()[0]);
                return new FieldMeta(f, avro, f.isAnnotationPresent(ObjectMap.Any.class), fType);
            }
        }
        return new FieldMeta(f, avro, f.isAnnotationPresent(ObjectMap.Any.class), f.getType());
    }

    @Data
    public static class Meta {

        private final Schema schema;

        private final boolean hasAny;

        private final Map<Integer, FieldMeta> fields;

        private final Map<String, FieldMeta> fieldsByName;
    }

    @Data
    public static class FieldMeta {

        private final Field field;

        private final Schema.Field avroField;

        private final boolean any;

        private final Class<?> recordType;
    }
}
