/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.xbean.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.JsonNumber;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;

class SchemaConverterTest {

    private JsonValue invokeToValue(final SchemaConverter converter, final Object input) throws Exception {
        final Method m = SchemaConverter.class.getDeclaredMethod("toValue", Object.class);
        m.setAccessible(true);
        return (JsonValue) m.invoke(converter, input);
    }

    @Test
    void toValueHandlesFloatWithoutClassCastException() throws Exception {
        // Regression test: previously the Double||Float branch cast Float to Double directly,
        // which throws ClassCastException. Both numeric types must be safely converted.
        final SchemaConverter converter = new SchemaConverter();

        final JsonValue fromFloat = invokeToValue(converter, 1.5f);
        assertNotNull(fromFloat);
        assertTrue(fromFloat instanceof JsonNumber, "Expected JsonNumber, got " + fromFloat);
        assertEquals(1.5d, ((JsonNumber) fromFloat).doubleValue(), 0.0001d);

        final JsonValue fromDouble = invokeToValue(converter, 2.5d);
        assertNotNull(fromDouble);
        assertTrue(fromDouble instanceof JsonNumber);
        assertEquals(2.5d, ((JsonNumber) fromDouble).doubleValue(), 0.0001d);
    }

    @Test
    void toValueHandlesOtherNumericTypesViaNumberFallback() throws Exception {
        final SchemaConverter converter = new SchemaConverter();

        final JsonValue fromShort = invokeToValue(converter, (short) 7);
        assertTrue(fromShort instanceof JsonNumber);
        assertEquals(7d, ((JsonNumber) fromShort).doubleValue(), 0.0001d);

        final JsonValue fromByte = invokeToValue(converter, (byte) 3);
        assertTrue(fromByte instanceof JsonNumber);
        assertEquals(3d, ((JsonNumber) fromByte).doubleValue(), 0.0001d);
    }

    @Test
    void toValueKeepsBigDecimalAndBigIntegerPrecision() throws Exception {
        // Guard against a regression where Number fallback would swallow BigDecimal/BigInteger
        // and silently lose precision via doubleValue().
        final SchemaConverter converter = new SchemaConverter();

        final BigDecimal bd = new BigDecimal("12345678901234567890.123456789");
        final JsonValue fromBigDecimal = invokeToValue(converter, bd);
        assertTrue(fromBigDecimal instanceof JsonNumber);
        assertEquals(bd, ((JsonNumber) fromBigDecimal).bigDecimalValue());

        final BigInteger bi = new BigInteger("123456789012345678901234567890");
        final JsonValue fromBigInteger = invokeToValue(converter, bi);
        assertTrue(fromBigInteger instanceof JsonNumber);
        assertEquals(bi, ((JsonNumber) fromBigInteger).bigIntegerValue());
    }
}

