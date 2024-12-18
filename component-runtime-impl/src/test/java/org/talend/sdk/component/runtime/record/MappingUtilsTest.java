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
package org.talend.sdk.component.runtime.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

class MappingUtilsTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    @Test
    void coerce() {
        final String name = "::testing::coerce";
        final Short shorty = 23923;
        // null
        assertNull(MappingUtils.coerce(Object.class, null, name));
        // Date Time
        assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1000l), UTC),
                MappingUtils.coerce(ZonedDateTime.class, 1000l, name));
        assertEquals(new Date(1000l), MappingUtils.coerce(Date.class, 1000l, name));
        // === non-matching types ===
        // number classes mapping
        assertEquals(shorty, MappingUtils.coerce(short.class, shorty, name));
        assertEquals(shorty, MappingUtils.coerce(Short.class, shorty.shortValue(), name));
        assertEquals(Byte.valueOf("123"), MappingUtils.coerce(Byte.class, 123l, name));
        assertEquals(Byte.valueOf("123"), MappingUtils.coerce(byte.class, 123l, name));
        assertEquals(BigDecimal.valueOf(12345.67891), MappingUtils.coerce(BigDecimal.class, 12345.67891, name));
        assertEquals(shorty.intValue(), MappingUtils.coerce(Integer.class, shorty, name));
        // ==== mapping primitive <-> Class ====
        assertEquals(Boolean.TRUE, MappingUtils.coerce(Boolean.class, true, name));
        assertEquals('c', MappingUtils.coerce(char.class, 'c', name));
        assertEquals('c', MappingUtils.coerce(Character.class, 'c', name));
        assertEquals(123, MappingUtils.coerce(int.class, 123, name));
        assertEquals(123, MappingUtils.coerce(Integer.class, 123, name));
        assertEquals(123l, MappingUtils.coerce(long.class, 123l, name));
        assertEquals(123l, MappingUtils.coerce(Long.class, 123l, name));
        assertEquals(123.456f, MappingUtils.coerce(float.class, 123.456, name));
        assertEquals(123.456f, MappingUtils.coerce(Float.class, 123.456f, name));
        assertEquals(123.456, MappingUtils.coerce(double.class, 123.456, name));
        assertEquals(123.456, MappingUtils.coerce(Double.class, 123.456, name));

        assertEquals("1000", MappingUtils.coerce(String.class, 1000l, name));
        // string mapping
        assertEquals('c', MappingUtils.coerce(char.class, "c", name));
        assertEquals('c', MappingUtils.coerce(Character.class, "c", name));
        assertEquals(Character.MIN_VALUE, MappingUtils.coerce(Character.class, "", name));
        assertEquals(true, MappingUtils.coerce(Boolean.class, "true", name));
        assertEquals(true, MappingUtils.coerce(boolean.class, "true", name));
        assertEquals(shorty, MappingUtils.coerce(Short.class, "23923", name));
        assertEquals(shorty, MappingUtils.coerce(short.class, "23923", name));
        assertEquals(BigDecimal.valueOf(12345.67891), MappingUtils.coerce(BigDecimal.class, "12345.67891", name));
        // string mapping: Date Time
        assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1000l), UTC),
                MappingUtils.coerce(ZonedDateTime.class, "1000", name));
        assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1000l), UTC),
                MappingUtils.coerce(ZonedDateTime.class, "1970-01-01T00:00:01Z[UTC]", name));
        assertEquals(new Date(1000l), MappingUtils.coerce(Date.class, "1000", name));
        assertEquals(1683286435000l, MappingUtils.coerce(Long.class,
                LocalDateTime.of(2023, 5, 5, 11, 33, 55, 666).toInstant(ZoneOffset.UTC), name));
        // string mapping: fail
        assertThrows(IllegalArgumentException.class,
                () -> MappingUtils.coerce(List.class, "1970-01-01T00:00:01Z", name));
        // string mapping: "null" literal
        assertNull(MappingUtils.coerce(Integer.class, "null", name));
        assertNull(MappingUtils.coerce(Long.class, "null  ", name));
        assertNull(MappingUtils.coerce(Double.class, "  null  ", name));
        assertNull(MappingUtils.coerce(Float.class, "   NULL  ", name));
        assertNull(MappingUtils.coerce(Short.class, "null", name));
        assertNull(MappingUtils.coerce(ZonedDateTime.class, "null", name));
        assertNull(MappingUtils.coerce(Byte.class, "null", name));
        assertNull(MappingUtils.coerce(ZonedDateTime.class, "null", name));
        assertNull(MappingUtils.coerce(Date.class, "NULL", name));
        assertNull(MappingUtils.coerce(Character.class, "null", name));
        assertNull(MappingUtils.coerce(BigDecimal.class, "null", name));
        assertNull(MappingUtils.coerce(byte[].class, "null", name));
        assertFalse((boolean) MappingUtils.coerce(Boolean.class, "null", name));
        assertThrows(IllegalArgumentException.class, () -> MappingUtils.coerce(Long.class, "nul", name));
        // incompatible mapping: fail
        assertThrows(IllegalArgumentException.class, () -> MappingUtils.coerce(List.class, 123, name));
    }
}