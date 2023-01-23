/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

import javax.json.JsonBuilderFactory;
import javax.json.bind.Jsonb;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.record.RecordConverters.MappingMetaRegistry;

public class OutputsHandler extends BaseIOHandler {

    private final JsonProvider jsonProvider;

    private final JsonBuilderFactory jsonBuilderFactory;

    private final MappingMetaRegistry registry = new MappingMetaRegistry();

    public OutputsHandler(final Jsonb jsonb, final Map<Class<?>, Object> servicesMapper) {
        super(jsonb, servicesMapper);
        this.jsonProvider = (JsonProvider) servicesMapper.get(JsonProvider.class);
        this.jsonBuilderFactory = (JsonBuilderFactory) servicesMapper.get(JsonBuilderFactory.class);
    }

    public OutputFactory asOutputFactory() {
        return name -> value -> {
            final BaseIOHandler.IO ref = connections.get(getActualName(name));
            if (ref != null && value != null) {
                if (value instanceof javax.json.JsonValue) {
                    ref.add(jsonb.fromJson(value.toString(), ref.getType()));
                } else if (value instanceof Record) {
                    ref.add(registry.find(ref.getType()).newInstance(Record.class.cast(value)));
                } else {
                    ref.add(jsonb.fromJson(jsonb.toJson(value), ref.getType()));
                }
            }
        };
    }

    /**
     * Guess schema special use-case for processor Studio mock.
     * Same as asOutputFactory but stores the record'schema or schema as the pojo class isn't available.
     *
     * @return GuessSchema OutputFactory
     */
    public OutputFactory asOutputFactoryForGuessSchema() {
        return name -> value -> {
            final BaseIOHandler.IO ref = connections.get(getActualName(name));
            if (ref != null && value != null) {
                if (value instanceof javax.json.JsonValue) {
                    ref.add(jsonb.fromJson(value.toString(), ref.getType()));
                } else if (value instanceof Record) {
                    ref.add(((Record) value).getSchema());
                } else if (value instanceof Schema) {
                    ref.add(value);
                } else {
                    ref.add(jsonb.fromJson(jsonb.toJson(value), ref.getType()));
                }
            }
        };
    }

}
