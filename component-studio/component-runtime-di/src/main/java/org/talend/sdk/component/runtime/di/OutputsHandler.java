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
package org.talend.sdk.component.runtime.di;

import java.util.Iterator;
import java.util.Map;

import javax.json.bind.Jsonb;

import org.talend.sdk.component.api.processor.MultiOutputIterator;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.TaggedOutput;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.record.RecordConverters.MappingMetaRegistry;

public class OutputsHandler extends BaseIOHandler {

    private final MappingMetaRegistry registry = new MappingMetaRegistry();

    public OutputsHandler(final Jsonb jsonb, final Map<Class<?>, Object> servicesMapper) {
        super(jsonb, servicesMapper);
    }

    public OutputFactory asOutputFactory() {
        return new OutputFactory() {

            @Override
            public OutputEmitter create(final String name) {
                final BaseIOHandler.IO ref = connections.get(getActualName(name));
                return value -> {
                    if (ref != null && value != null) {
                        ref.add(convert(value, ref));
                    }
                };
            }

            @Override
            public <T> MultiOutputIterator<T> createMultiOutputIterator() {
                return new MultiOutputIterator<T>() {

                    @Override
                    public void setIterator(final Iterator<TaggedOutput<T>> iterator) {
                        setTaggedSource(() -> {
                            if (!iterator.hasNext()) {
                                return false;
                            }
                            final TaggedOutput<T> tagged = iterator.next();
                            final String name = getActualName(tagged.getOutputName());
                            final BaseIOHandler.IO ref = connections.get(name);
                            setPending(name,
                                    ref != null ? convert(tagged.getRecord(), ref) : tagged.getRecord());
                            return true;
                        });
                    }

                    @Override
                    public void setIterator(final String outputName, final Iterator<T> iterator) {
                        final String name = getActualName(outputName);
                        final BaseIOHandler.IO ref = connections.get(name);
                        if (ref == null) {
                            return;
                        }
                        ref.setSource(new Iterator() {

                            @Override
                            public boolean hasNext() {
                                return iterator.hasNext();
                            }

                            @Override
                            public Object next() {
                                return convert(iterator.next(), ref);
                            }
                        });
                    }
                };
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
                } else if (value instanceof Record rec) {
                    ref.add(rec.getSchema());
                } else if (value instanceof Schema) {
                    ref.add(value);
                } else {
                    ref.add(jsonb.fromJson(jsonb.toJson(value), ref.getType()));
                }
            }
        };
    }

    private Object convert(final Object value, final BaseIOHandler.IO ref) {
        if (value == null) {
            return null;
        } else if (value instanceof javax.json.JsonValue) {
            return jsonb.fromJson(value.toString(), ref.getType());
        } else if (value instanceof Record rec) {
            return registry.find(ref.getType()).newInstance(rec);
        } else {
            return jsonb.fromJson(jsonb.toJson(value), ref.getType());
        }
    }

}
