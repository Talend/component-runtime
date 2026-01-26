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
import org.apache.beam.sdk.coders.AvroCoder;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
class AvroCoderCache {

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
                        final Schema schema = Schema.class.cast(key);
                        coder = AvroCoder.of(IndexedRecord.class, schema);
                        put(schema, coder);
                    }
                    return coder;
                }
            };

    static AvroCoder<IndexedRecord> getCoder(final Schema avro) {
        return CACHED_CODERS.get(avro);
    }
}
