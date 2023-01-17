/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.record;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MappingUtils {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = Stream
            .of(new AbstractMap.SimpleImmutableEntry<>(boolean.class, Boolean.class),
                    new AbstractMap.SimpleImmutableEntry<>(byte.class, Byte.class),
                    new AbstractMap.SimpleImmutableEntry<>(char.class, Character.class),
                    new AbstractMap.SimpleImmutableEntry<>(double.class, Double.class),
                    new AbstractMap.SimpleImmutableEntry<>(float.class, Float.class),
                    new AbstractMap.SimpleImmutableEntry<>(int.class, Integer.class),
                    new AbstractMap.SimpleImmutableEntry<>(long.class, Long.class),
                    new AbstractMap.SimpleImmutableEntry<>(short.class, Short.class))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    public static <T> Object coerce(final Class<T> expectedType, final Object value, final String name) {
        log.debug("[coerce] expectedType={}, value={}, name={}.", expectedType, value, name);
        // null is null, la la la la la... guess which song is it ;-)
        if (value == null) {
            return null;
        }
        // datetime cases from Long
        if (Long.class.isInstance(value) && expectedType != Long.class) {
            if (ZonedDateTime.class == expectedType) {
                final long epochMilli = Number.class.cast(value).longValue();
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), UTC);
            }
            if (Date.class == expectedType) {
                return new Date(Number.class.cast(value).longValue());
            }
            if (Instant.class == expectedType) {
                return Instant.ofEpochMilli(Number.class.cast(value).longValue());
            }
        }

        // we store decimal by string for AvroRecord case
        if ((expectedType == BigDecimal.class) && String.class.isInstance(value)) {
            return new BigDecimal(String.class.cast(value));
        }

        // non-matching types
        if (!expectedType.isInstance(value)) {
            // number classes mapping
            if (Number.class.isInstance(value)
                    && Number.class.isAssignableFrom(PRIMITIVE_WRAPPER_MAP.getOrDefault(expectedType, expectedType))) {
                return mapNumber(expectedType, Number.class.cast(value));
            }
            // mapping primitive <-> Class
            if (isAssignableTo(value.getClass(), expectedType)) {
                return mapPrimitiveWrapper(expectedType, value);
            }
            if (String.class == expectedType) {
                return String.valueOf(value);
            }
            // TCOMP-2293 support Instant
            if (Instant.class.isInstance(value) && ZonedDateTime.class == expectedType) {
                return ZonedDateTime.ofInstant((Instant) value, UTC);
            }
            if (value instanceof long[]) {
                final Instant instant = Instant.ofEpochSecond(((long[]) value)[0], ((long[]) value)[1]);
                if (ZonedDateTime.class == expectedType) {
                    return ZonedDateTime.ofInstant(instant, UTC);
                }
                if (Instant.class == expectedType) {
                    return instant;
                }
            }

            // TODO: maybe add a Date.class / ZonedDateTime.class mapping case. Should check that...
            // mainly for CSV incoming data where everything is mapped to String
            if (String.class.isInstance(value)) {
                return mapString(expectedType, String.valueOf(value));
            }

            throw new IllegalArgumentException(String
                    .format("%s can't be converted to %s as its value is '%s' of type %s.", name, expectedType, value,
                            value.getClass()));
        }
        // type should match so...
        return value;
    }

    public static <T> Object mapPrimitiveWrapper(final Class<T> expected, final Object value) {
        if (char.class == expected || Character.class == expected) {
            return expected.isPrimitive() ? Character.class.cast(value).charValue() : value;
        }
        if (Boolean.class == expected || boolean.class == expected) {
            return expected.isPrimitive() ? Boolean.class.cast(value).booleanValue() : value;
        }
        if (Integer.class == expected || int.class == expected) {
            return expected.isPrimitive() ? Integer.class.cast(value).intValue() : value;
        }
        if (Long.class == expected || long.class == expected) {
            return expected.isPrimitive() ? Long.class.cast(value).longValue() : value;
        }
        if (Short.class == expected || short.class == expected) {
            return expected.isPrimitive() ? Short.class.cast(value).shortValue() : value;
        }
        if (Byte.class == expected || byte.class == expected) {
            return expected.isPrimitive() ? Byte.class.cast(value).byteValue() : value;
        }
        if (Float.class == expected || float.class == expected) {
            return expected.isPrimitive() ? Float.class.cast(value).floatValue() : value;
        }
        if (Double.class == expected || double.class == expected) {
            return expected.isPrimitive() ? Double.class.cast(value).doubleValue() : value;
        }

        throw new IllegalArgumentException(String.format("Can't convert %s to %s.", value, expected));
    }

    public static <T> Object mapNumber(final Class<T> expected, final Number value) {
        if (expected == BigDecimal.class) {
            return BigDecimal.valueOf(value.doubleValue());
        }
        if (expected == Double.class || expected == double.class) {
            return value.doubleValue();
        }
        if (expected == Float.class || expected == float.class) {
            return value.floatValue();
        }
        if (expected == Integer.class || expected == int.class) {
            return value.intValue();
        }
        if (expected == Long.class || expected == long.class) {
            return value.longValue();
        }
        if (expected == Byte.class || expected == byte.class) {
            return value.byteValue();
        }
        if (expected == Short.class || expected == short.class) {
            return value.shortValue();
        }

        throw new IllegalArgumentException(String.format("Can't convert %s to %s.", value, expected));
    }

    public static <T> Object mapString(final Class<T> expected, final String value) {
        final boolean isNumeric = value.chars().allMatch(Character::isDigit);
        if (ZonedDateTime.class == expected) {
            if (isNumeric) {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.valueOf(value)), UTC);
            } else {
                return ZonedDateTime.parse(value);
            }
        }
        if (Date.class == expected) {
            if (isNumeric) {
                return Date.from(Instant.ofEpochMilli(Long.valueOf(value)));

            } else {
                return Date.from(ZonedDateTime.parse(value).toInstant());
            }
        }
        if (char.class == expected || Character.class == expected) {
            return value.isEmpty() ? Character.MIN_VALUE : value.charAt(0);
        }
        if (byte[].class == expected) {
            log
                    .warn("[mapString] Expecting a `byte[]` but received a `String`."
                            + " Using `Base64.getDecoder().decode()` and "
                            + "`String.getBytes()` if first fails: result may be inaccurate.");
            // json is using Base64.getEncoder()
            try {
                return Base64.getDecoder().decode(value);
            } catch (final Exception e) {
                return value.getBytes();
            }
        }
        if (BigDecimal.class == expected) {
            return new BigDecimal(value);
        }
        if (Boolean.class == expected || boolean.class == expected) {
            return Boolean.valueOf(value);
        }
        if (Integer.class == expected || int.class == expected) {
            return Integer.valueOf(value);
        }
        if (Long.class == expected || long.class == expected) {
            return Long.valueOf(value);
        }
        if (Short.class == expected || short.class == expected) {
            return Short.valueOf(value);
        }
        if (Byte.class == expected || byte.class == expected) {
            return Byte.valueOf(value);
        }
        if (Float.class == expected || float.class == expected) {
            return Float.valueOf(value);
        }
        if (Double.class == expected || double.class == expected) {
            return Double.valueOf(value);
        }

        throw new IllegalArgumentException(String.format("Can't convert %s to %s.", value, expected));
    }

    public static boolean isPrimitiveWrapperOf(final Class<?> targetClass, final Class<?> primitive) {
        if (!primitive.isPrimitive()) {
            throw new IllegalArgumentException("First argument has to be primitive type");
        }
        return PRIMITIVE_WRAPPER_MAP.get(primitive) == targetClass;
    }

    public static boolean isAssignableTo(final Class<?> from, final Class<?> to) {
        if (to.isAssignableFrom(from)) {
            return true;
        }
        if (from.isPrimitive()) {
            return isPrimitiveWrapperOf(to, from);
        }
        if (to.isPrimitive()) {
            return isPrimitiveWrapperOf(from, to);
        }
        return false;
    }

}
