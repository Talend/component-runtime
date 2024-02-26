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
package org.talend.sdk.component.runtime.di;

import java.util.Map;

import javax.json.JsonValue;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.record.RecordConverters.MappingMeta;
import org.talend.sdk.component.runtime.record.RecordConverters.MappingMetaRegistry;

public class InputsHandler extends BaseIOHandler {

    private final MappingMetaRegistry registry = new MappingMetaRegistry();

    public InputsHandler(final Jsonb jsonb, final Map<Class<?>, Object> servicesMapper) {
        super(jsonb, servicesMapper);
    }

    public InputFactory asInputFactory() {
        return name -> {
            final BaseIOHandler.IO ref = connections.get(getActualName(name));
            if (ref == null || !ref.hasNext()) {
                return null;
            }

            final Object value = ref.next();
            if (value instanceof Record) {
                return value;
            }
            final Object convertedValue;
            final MappingMeta mappingMeta;
            mappingMeta = registry.find(value.getClass());
            if (mappingMeta.isLinearMapping()) {
                return mappingMeta.newRecord(value, recordBuilderMapper);
            } else {
                if (value instanceof javax.json.JsonValue) {
                    if (JsonValue.NULL == value) { // JsonObject cant take a JsonValue so pass null
                        return null;
                    }
                    convertedValue = value.toString();
                } else {
                    convertedValue = jsonb.toJson(value);
                }
            }

            return converters.toRecord(registry, convertedValue, () -> jsonb, () -> recordBuilderMapper);
        };
    }

    public <T> void initInputValue(final String name, final T value) {
        addConnection(name, value.getClass());
        setInputValue(value.getClass().getSimpleName(), value);
    }

    public <T> void setInputValue(final String name, final T value) {
        final IO input = connections.get(getActualName(name));
        if (input != null) {
            input.add(value);
        }
    }
}
