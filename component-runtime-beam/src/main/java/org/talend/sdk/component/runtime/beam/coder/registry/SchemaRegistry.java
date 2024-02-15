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
package org.talend.sdk.component.runtime.beam.coder.registry;

import static lombok.AccessLevel.PRIVATE;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.talend.sdk.component.api.record.Schema;

import lombok.NoArgsConstructor;

public interface SchemaRegistry {

    void putIfAbsent(String id, Schema schema);

    Schema get(String id);

    @NoArgsConstructor(access = PRIVATE)
    class Instance {

        private static final SchemaRegistry REGISTRY = load();

        public static SchemaRegistry get() {
            return REGISTRY;
        }

        private static SchemaRegistry load() {
            final Iterator<SchemaRegistry> iterator = ServiceLoader.load(SchemaRegistry.class).iterator();
            if (iterator.hasNext()) {
                final SchemaRegistry schemaRegistry = iterator.next();
                if (iterator.hasNext()) {
                    throw new IllegalStateException(
                            "Ambiguous schema registry: " + schemaRegistry + "/" + iterator.next());
                }
                return schemaRegistry;
            }
            return new InMemorySchemaRegistry();
        }
    }
}
