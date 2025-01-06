/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.talend.sdk.component.api.record.Schema;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InMemorySchemaRegistry implements SchemaRegistry {

    private final ConcurrentMap<String, Schema> schemas = new ConcurrentHashMap<>();

    @Override
    public void putIfAbsent(final String id, final Schema schema) {
        schemas.putIfAbsent(id, schema);
    }

    @Override
    public Schema get(final String id) {
        return schemas.get(id);
    }
}
