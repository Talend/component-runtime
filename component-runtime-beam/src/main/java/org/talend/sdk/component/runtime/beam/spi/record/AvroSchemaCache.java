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
package org.talend.sdk.component.runtime.beam.spi.record;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.record.SchemaImpl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AvroSchemaCache {

    private static final int MAX_SIZE = 5;

    private final Function<SchemaImpl, AvroSchema> transform;

    private final ConcurrentMap<SchemaImpl, ChronoValue> cache = new ConcurrentHashMap<>(AvroSchemaCache.MAX_SIZE);

    public AvroSchema find(final Schema schema) {
        if (schema == null || schema instanceof AvroSchema) {
            return (AvroSchema) schema;
        }
        if (schema instanceof SchemaImpl) {
            final SchemaImpl realSchema = (SchemaImpl) schema;
            if ((!this.cache.containsKey(realSchema))
                    && this.cache.size() >= AvroSchemaCache.MAX_SIZE) {
                this.removeOldest();
            }

            final ChronoValue chronoValue =
                    this.cache.computeIfAbsent(realSchema, this.transform.andThen(ChronoValue::new));

            return chronoValue.getSchema();
        }
        return null;
    }

    private synchronized void removeOldest() {
        while (this.cache.size() >= AvroSchemaCache.MAX_SIZE) {
            final Optional<Map.Entry<SchemaImpl, ChronoValue>> oldest = cache.entrySet() //
                    .stream() //
                    .min(Comparator.comparing((Map.Entry<SchemaImpl, ChronoValue> e) -> e.getValue().getLastUsage()));
            if (oldest.isPresent()) {
                final SchemaImpl key = oldest.get().getKey();
                this.cache.remove(key);
            }
        }
    }

    @RequiredArgsConstructor
    private static class ChronoValue {

        private final AvroSchema schema;

        @Getter(AccessLevel.PACKAGE)
        private Long lastUsage = System.currentTimeMillis();

        public AvroSchema getSchema() {
            this.lastUsage = System.currentTimeMillis();
            return schema;
        }
    }
}
