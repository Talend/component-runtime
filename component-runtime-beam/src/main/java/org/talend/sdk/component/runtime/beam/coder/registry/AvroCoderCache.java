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
package org.talend.sdk.component.runtime.beam.coder.registry;

import static lombok.AccessLevel.PRIVATE;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.beam.sdk.extensions.avro.coders.AvroCoder;
import org.apache.beam.sdk.extensions.avro.io.AvroDatumFactory;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
class AvroCoderCache {

    /**
     * Beam 2.61 registers java-time conversions by default, which turns date/time-millis fields into
     * LocalDate/LocalTime
     * and timestamps into Instant on decoding. Records are expected to keep Avro raw values (int/long) as with Beam
     * 2.46, so no logical type conversion is registered.
     */
    private static class RawIndexedRecordDatumFactory extends AvroDatumFactory<IndexedRecord> {

        private RawIndexedRecordDatumFactory() {
            super(IndexedRecord.class);
        }

        @Override
        public DatumReader<IndexedRecord> apply(final Schema writer, final Schema reader) {
            return new ReflectDatumReader<>(writer, reader, new ReflectData(type.getClassLoader()));
        }

        @Override
        public DatumWriter<IndexedRecord> apply(final Schema writer) {
            return new ReflectDatumWriter<>(writer, new ReflectData(type.getClassLoader()));
        }
    }

    private static final AvroDatumFactory<IndexedRecord> DATUM_FACTORY = new RawIndexedRecordDatumFactory();

    private static final Map<Schema, AvroCoder<IndexedRecord>> CACHED_CODERS =
            new LinkedHashMap<Schema, AvroCoder<IndexedRecord>>() {

                private final int MAX_SIZE = Integer.getInteger("component.runtime.beam.avrocoder.cache.size", 1024);

                @Override
                protected boolean removeEldestEntry(final Map.Entry<Schema, AvroCoder<IndexedRecord>> eldest) {
                    return size() > MAX_SIZE;
                }

                @Override
                public synchronized AvroCoder<IndexedRecord> get(final Object key) {
                    AvroCoder<IndexedRecord> coder = super.get(key);
                    if (coder == null) {
                        final Schema schema = (Schema) key;
                        coder = AvroCoder.of(DATUM_FACTORY, schema);
                        put(schema, coder);
                    }
                    return coder;
                }
            };

    static AvroCoder<IndexedRecord> getCoder(final Schema avro) {
        return CACHED_CODERS.get(avro);
    }
}
