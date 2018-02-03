/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit.beam;

import static lombok.AccessLevel.PRIVATE;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;

import org.apache.beam.sdk.transforms.Create;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.LightContainer;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Data {

    public static Create.Values<JsonObject> of(final String plugin, final Iterable<Map<String, List<?>>> elems) {
        return Create.of(() -> {
            final Iterator<Map<String, List<?>>> delegate = elems.iterator();
            return new Iterator<JsonObject>() {

                private transient volatile Jsonb jsonb;

                private transient volatile JsonBuilderFactory jsonBuilderFactory;

                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public JsonObject next() {
                    return map(delegate.next());
                }

                private JsonObject map(final Map<String, List<?>> next) {
                    if (next == null) {
                        return null;
                    }
                    if (jsonBuilderFactory == null) {
                        synchronized (this) {
                            if (jsonBuilderFactory == null) {
                                final LightContainer container = ContainerFinder.Instance.get().find(plugin);
                                jsonBuilderFactory = container.findService(JsonBuilderFactory.class);
                                jsonb = container.findService(Jsonb.class);
                            }
                        }
                    }
                    return next
                            .entrySet()
                            .stream()
                            .collect(jsonBuilderFactory::createObjectBuilder,
                                    (objBuilder, entry) -> objBuilder.add(entry.getKey(), entry
                                            .getValue()
                                            .stream()
                                            .collect(jsonBuilderFactory::createArrayBuilder,
                                                    (array, list) -> array.add(JsonValue.class.isInstance(list)
                                                            ? JsonValue.class.cast(list)
                                                            : jsonb.fromJson(jsonb.toJson(list), JsonObject.class)),
                                                    JsonArrayBuilder::addAll)
                                            .build()),
                                    JsonObjectBuilder::addAll)
                            .build();
                }
            };
        }).withCoder(JsonpJsonObjectCoder.of(plugin));
    }
}
