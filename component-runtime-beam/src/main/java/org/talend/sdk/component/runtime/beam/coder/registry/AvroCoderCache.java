/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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

import java.util.concurrent.ExecutionException;

import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.repackaged.beam_sdks_java_core.com.google.common.cache.CacheBuilder;
import org.apache.beam.repackaged.beam_sdks_java_core.com.google.common.cache.CacheLoader;
import org.apache.beam.repackaged.beam_sdks_java_core.com.google.common.cache.LoadingCache;
import org.apache.beam.sdk.coders.AvroCoder;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
class AvroCoderCache {

    private static final LoadingCache<Schema, AvroCoder<IndexedRecord>> CACHED_CODERS = CacheBuilder
            .newBuilder()
            .maximumSize(Integer.getInteger("component.runtime.beam.avrocoder.cache.size", 1024))
            .build(new CacheLoader<Schema, AvroCoder<IndexedRecord>>() {

                @Override
                public AvroCoder<IndexedRecord> load(final Schema schema) {
                    return AvroCoder.of(IndexedRecord.class, schema);
                }
            });

    static AvroCoder<IndexedRecord> getCoder(final Schema avro) {
        try {
            return CACHED_CODERS.get(avro);
        } catch (ExecutionException e) { // more than unlikely
            log.warn(e.getMessage(), e);
            return AvroCoder.of(IndexedRecord.class, avro);
        }
    }
}
