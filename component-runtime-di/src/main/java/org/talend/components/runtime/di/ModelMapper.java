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
package org.talend.components.runtime.di;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.talend.component.api.processor.data.ObjectMap;

public class ModelMapper {

    private final ConcurrentMap<Class<?>, BiConsumer<Object, ObjectMap>> setters = new ConcurrentHashMap<>();

    public <T> T map(final ObjectMap from, final T target) {
        final BiConsumer<Object, ObjectMap> consumer = setters.computeIfAbsent(target.getClass(), type -> {
            final Collection<BiConsumer<Object, ObjectMap>> fields = Stream.of(type.getFields())
                    .filter(f -> !Modifier.isStatic(f.getModifiers())).map(field -> {
                        final Class<?> fieldType = field.getType();
                        final String name = field.getName();
                        if (!field.isAccessible()) {
                            field.setAccessible(true);
                        }

                        final boolean isPrimitive = fieldType.isPrimitive();
                        final Class<?> objectType = isPrimitive ? findWrapper(fieldType) : fieldType;
                        final boolean isNumber = Number.class.isAssignableFrom(objectType);
                        final Function<Number, Object> numberFactory = isNumber ? findNumberFactory(objectType) : null;
                        final Object defaultValue = isPrimitive ? findDefault(fieldType) : null;

                        return (BiConsumer<Object, ObjectMap>) (instance, map) -> {
                            if (!map.keys().contains(name)) {
                                return;
                            }
                            Object fieldValue = map.get(name);
                            if (fieldValue != null && !objectType.isInstance(fieldValue)) {
                                if (isNumber && Number.class.isInstance(fieldValue)) {
                                    fieldValue = numberFactory.apply(Number.class.cast(fieldValue));
                                } else {
                                    throw new IllegalArgumentException(
                                            "Bad type for " + name + ", expected " + fieldType + " and got " + fieldValue);
                                }
                            } else if (fieldValue == null && isPrimitive) {
                                fieldValue = defaultValue;
                            }

                            try {
                                field.set(instance, fieldValue);
                            } catch (final IllegalAccessException e) {
                                throw new IllegalStateException(e);
                            }
                        };
                    }).collect(toList());
            if (fields.size() == 1) {
                return fields.iterator().next();
            }
            return (instance, map) -> fields.forEach(f -> f.accept(instance, map));
        });
        consumer.accept(target, from);
        return target;
    }

    private Object findDefault(final Class<?> fieldType) {
        if (int.class == fieldType) {
            return 0;
        }
        if (long.class == fieldType) {
            return 0L;
        }
        if (double.class == fieldType) {
            return 0.;
        }
        if (float.class == fieldType) {
            return 0f;
        }
        if (short.class == fieldType) {
            return (short) 0;
        }
        if (byte.class == fieldType) {
            return (byte) 0;
        }
        if (boolean.class == fieldType) {
            return false;
        }
        throw new IllegalArgumentException("Not a primitive: " + fieldType);
    }

    private Function<Number, Object> findNumberFactory(final Class<?> fieldType) {
        if (Integer.class == fieldType) {
            return Number::intValue;
        }
        if (Long.class == fieldType) {
            return Number::longValue;
        }
        if (Double.class == fieldType) {
            return Number::doubleValue;
        }
        if (Float.class == fieldType) {
            return Number::floatValue;
        }
        if (Short.class == fieldType) {
            return Number::shortValue;
        }
        if (Byte.class == fieldType) {
            return Number::byteValue;
        }
        throw new IllegalArgumentException("Not a number: " + fieldType);
    }

    private Class<?> findWrapper(final Class<?> fieldType) {
        if (int.class == fieldType) {
            return Integer.class;
        }
        if (long.class == fieldType) {
            return Long.class;
        }
        if (double.class == fieldType) {
            return Double.class;
        }
        if (float.class == fieldType) {
            return Float.class;
        }
        if (short.class == fieldType) {
            return Short.class;
        }
        if (byte.class == fieldType) {
            return Byte.class;
        }
        if (boolean.class == fieldType) {
            return Boolean.class;
        }
        throw new IllegalArgumentException("Not a primitive: " + fieldType);
    }
}
