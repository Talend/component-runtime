/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.beam.spi.record;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PRIVATE;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.avro.Schema;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE) // todo: don't keep it static, make it pluggable and Record based (not avro)
public class SchemaIdGenerator {

    public static String generateRecordName(final List<Schema.Field> fields) {
        return "org.talend.sdk.component.schema.generated.Record"
                + new XXHash64(0/* always the same output */).id(fields
                        .stream()
                        .sorted(comparing(org.apache.avro.Schema.Field::pos)
                                .thenComparing(org.apache.avro.Schema.Field::name))
                        // surely not enough but "ok" to start
                        .map(f -> mapField(f.name(), f.schema().getType().name(), extractNestedName(f)))
                        .collect(joining("\n")));
    }

    private static String extractNestedName(final Schema.Field f) {
        if (f.schema().getType() == Schema.Type.RECORD) {
            return f.schema().getName();
        }
        if (f.schema().getType() == Schema.Type.ARRAY) {
            return f.schema().getElementType().getName();
        }
        return null;
    }

    private static String mapField(final String fieldName, final String type, final String recordName) {
        return "_" + fieldName + "::" + type + (recordName == null ? "" : ("::" + recordName)) + "_";
    }

    // from kanzi (asf v2 license)
    private static class XXHash64 {

        private final static long PRIME64_1 = 0x9E3779B185EBCA87L;

        private final static long PRIME64_2 = 0xC2B2AE3D27D4EB4FL;

        private final static long PRIME64_3 = 0x165667B19E3779F9L;

        private final static long PRIME64_4 = 0x85EBCA77C2b2AE63L;

        private final static long PRIME64_5 = 0x27D4EB2F165667C5L;

        private static final int SHIFT64_0 = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? 56 : 0;

        private static final int SHIFT64_1 = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? 48 : 8;

        private static final int SHIFT64_2 = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? 40 : 16;

        private static final int SHIFT64_3 = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? 32 : 24;

        private static final int SHIFT64_4 = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? 24 : 32;

        private static final int SHIFT64_5 = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? 16 : 40;

        private static final int SHIFT64_6 = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? 8 : 48;

        private static final int SHIFT64_7 = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? 0 : 56;

        private static final int SHIFT32_0 = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? 24 : 0;

        private static final int SHIFT32_1 = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? 16 : 8;

        private static final int SHIFT32_2 = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? 8 : 16;

        private static final int SHIFT32_3 = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? 0 : 24;

        private final long seed;

        private XXHash64(final long seed) {
            this.seed = seed;
        }

        private String id(final String data) {
            return Long.toHexString(hash(data.getBytes(StandardCharsets.UTF_8)));
        }

        private long hash(final byte[] data) {
            return this.hash(data, data.length);
        }

        private long hash(final byte[] data, final int length) {
            long h64;
            int idx = 0;

            if (length >= 32) {
                final int end32 = length - 32;
                long v1 = this.seed + PRIME64_1 + PRIME64_2;
                long v2 = this.seed + PRIME64_2;
                long v3 = this.seed;
                long v4 = this.seed - PRIME64_1;

                do {
                    v1 = round(v1, readLong64(data, idx));
                    v2 = round(v2, readLong64(data, idx + 8));
                    v3 = round(v3, readLong64(data, idx + 16));
                    v4 = round(v4, readLong64(data, idx + 24));
                    idx += 32;
                } while (idx <= end32);

                h64 = ((v1 << 1) | (v1 >>> 31));
                h64 += ((v2 << 7) | (v2 >>> 25));
                h64 += ((v3 << 12) | (v3 >>> 20));
                h64 += ((v4 << 18) | (v4 >>> 14));

                h64 = mergeRound(h64, v1);
                h64 = mergeRound(h64, v2);
                h64 = mergeRound(h64, v3);
                h64 = mergeRound(h64, v4);
            } else {
                h64 = this.seed + PRIME64_5;
            }

            h64 += length;

            while (idx + 8 <= length) {
                h64 ^= round(0, readLong64(data, idx));
                h64 = ((h64 << 27) | (h64 >>> 37)) * PRIME64_1 + PRIME64_4;
                idx += 8;
            }

            while (idx + 4 <= length) {
                h64 ^= (readInt32(data, idx) * PRIME64_1);
                h64 = ((h64 << 23) | (h64 >>> 41)) * PRIME64_2 + PRIME64_3;
                idx += 4;
            }

            while (idx < length) {
                h64 ^= ((data[idx] & 0xFF) * PRIME64_5);
                h64 = ((h64 << 11) | (h64 >>> 53)) * PRIME64_1;
                idx++;
            }

            // Finalize
            h64 ^= (h64 >>> 33);
            h64 *= PRIME64_2;
            h64 ^= (h64 >>> 29);
            h64 *= PRIME64_3;
            return h64 ^ (h64 >>> 32);
        }

        private static long readLong64(final byte[] buf, final int offset) {
            return ((((long) buf[offset + 7]) & 0xFF) << SHIFT64_7) | ((((long) buf[offset + 6]) & 0xFF) << SHIFT64_6)
                    | ((((long) buf[offset + 5]) & 0xFF) << SHIFT64_5)
                    | ((((long) buf[offset + 4]) & 0xFF) << SHIFT64_4)
                    | ((((long) buf[offset + 3]) & 0xFF) << SHIFT64_3)
                    | ((((long) buf[offset + 2]) & 0xFF) << SHIFT64_2)
                    | ((((long) buf[offset + 1]) & 0xFF) << SHIFT64_1) | ((((long) buf[offset]) & 0xFF) << SHIFT64_0);
        }

        private static int readInt32(final byte[] buf, final int offset) {
            return (((buf[offset + 3]) & 0xFF) << SHIFT32_3) | (((buf[offset + 2]) & 0xFF) << SHIFT32_2)
                    | (((buf[offset + 1]) & 0xFF) << SHIFT32_1) | (((buf[offset]) & 0xFF) << SHIFT32_0);
        }

        private static long round(final long acc, final long val) {
            final long v = acc + (val * PRIME64_2);
            return ((v << 13) | (v >>> 19)) * PRIME64_1;
        }

        private static long mergeRound(final long acc, final long val) {
            return (acc ^ round(0, val)) * PRIME64_1 + PRIME64_4;
        }
    }
}
