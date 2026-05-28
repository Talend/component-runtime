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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

class SchemaConverterTest {

    private final RecordBuilderFactory factory = new RecordBuilderFactoryImpl("test");

    private final SchemaConverter converter = new SchemaConverter();

    /**
     * Exercises {@link SchemaConverter#toValue(Object)} via the public
     * {@link SchemaConverter#toJson(Schema)} API by building a single-entry record schema whose entry has
     * the requested default value, and returning the {@code defaultValue} JSON node produced by the
     * converter.
     * <p>
     * The {@code entryType} only drives entry construction; the converter routes the default value
     * through {@link SchemaConverter#toValue(Object)} regardless of the declared type.
     */
    private JsonValue defaultValueOf(final Schema.Type entryType, final Object defaultValue) {
        final Schema.Entry entry = factory
                .newEntryBuilder()
                .withName("field")
                .withType(entryType)
                .withNullable(true)
                .withDefaultValue(defaultValue)
                .build();
        final Schema schema = factory.newSchemaBuilder(Schema.Type.RECORD).withEntry(entry).build();
        final JsonObject json = converter.toJson(schema);
        return json.getJsonArray(SchemaConverter.ENTRIES).getJsonObject(0).get("defaultValue");
    }

    @Test
    void toJsonHandlesFloatAndDoubleDefaultValues() {
        // Regression: previously a Float default value triggered ClassCastException in toValue.
        final JsonNumber fromFloat = (JsonNumber) defaultValueOf(Schema.Type.FLOAT, 1.5f);
        assertEquals(1.5d, fromFloat.doubleValue(), 0.0001d);

        final JsonNumber fromDouble = (JsonNumber) defaultValueOf(Schema.Type.DOUBLE, 2.5d);
        assertEquals(2.5d, fromDouble.doubleValue(), 0.0001d);
    }

    @Test
    void toJsonHandlesIntegralNumberSubtypesWithoutDecimalRepresentation() {
        // Short / Byte must serialize as integers (no .0 suffix), via the Number/longValue() fallback.
        final JsonNumber fromShort = (JsonNumber) defaultValueOf(Schema.Type.INT, (short) 7);
        assertTrue(fromShort.isIntegral(), "Short must serialize as integral");
        assertEquals(7L, fromShort.longValue());

        final JsonNumber fromByte = (JsonNumber) defaultValueOf(Schema.Type.INT, (byte) 3);
        assertTrue(fromByte.isIntegral(), "Byte must serialize as integral");
        assertEquals(3L, fromByte.longValue());
    }

    @Test
    void toJsonKeepsBigDecimalAndBigIntegerPrecision() {
        // Guard against a regression where the Number fallback would swallow BigDecimal/BigInteger
        // and silently lose precision through doubleValue()/longValue().
        final BigDecimal bd = new BigDecimal("12345678901234567890.123456789");
        final JsonNumber fromBigDecimal = (JsonNumber) defaultValueOf(Schema.Type.DECIMAL, bd);
        assertEquals(bd, fromBigDecimal.bigDecimalValue());

        // No dedicated Schema.Type for BigInteger; the entry type is irrelevant here, only the
        // default-value routing through toValue() matters.
        final BigInteger bi = new BigInteger("123456789012345678901234567890");
        final JsonNumber fromBigInteger = (JsonNumber) defaultValueOf(Schema.Type.STRING, bi);
        assertEquals(bi, fromBigInteger.bigIntegerValue());
    }

    @Test
    void toJsonPreservesLargeAtomicLongValues() {
        // AtomicLong falls into the Number fallback. longValue() must be used (not doubleValue())
        // so that values beyond 2^53 (not exactly representable as double) are preserved.
        final long big = (1L << 53) + 1L; // 9007199254740993, +1 beyond double precision
        final JsonNumber fromAtomicLong =
                (JsonNumber) defaultValueOf(Schema.Type.LONG, new AtomicLong(big));
        assertTrue(fromAtomicLong.isIntegral(), "AtomicLong must serialize as integral");
        assertEquals(big, fromAtomicLong.longValue());
    }
}

