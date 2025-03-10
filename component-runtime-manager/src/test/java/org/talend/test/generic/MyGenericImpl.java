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
package org.talend.test.generic;

import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.record.RecordImpl;
import org.talend.sdk.component.spi.component.GenericComponentExtension;

public class MyGenericImpl implements GenericComponentExtension {

    @Override
    public boolean canHandle(final Class<?> expectedType, final String plugin, final String name) {
        return plugin.equals("my-generic") && expectedType == Mapper.class;
    }

    @Override
    public <T> T createInstance(final Class<T> type, final String plugin, final String name, final int version,
            final Map<String, String> configuration, final Map<Class<?>, Object> services) {
        return type.cast(new Mapper() {

            @Override
            public String plugin() {
                return plugin;
            }

            @Override
            public String rootName() {
                return plugin;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public void start() {
                // no-op
            }

            @Override
            public void stop() {
                // no-op
            }

            @Override
            public long assess() {
                return 1;
            }

            @Override
            public List<Mapper> split(final long desiredSize) {
                return singletonList(this);
            }

            @Override
            public Input create() {
                return new Input() {

                    private boolean done;

                    @Override
                    public Object next() {
                        if (done) {
                            return null;
                        }
                        done = true;
                        return new RecordImpl.BuilderImpl()
                                .withString("plugin", plugin)
                                .withString("name", name)
                                .withString("key", configuration.containsKey("a") ? "a" : "no a")
                                .withString("value", configuration.getOrDefault("a", "no value"))
                                .build();
                    }

                    @Override
                    public String plugin() {
                        return plugin;
                    }

                    @Override
                    public String rootName() {
                        return plugin;
                    }

                    @Override
                    public String name() {
                        return name;
                    }

                    @Override
                    public void start() {
                        // no-op
                    }

                    @Override
                    public void stop() {
                        // no-op
                    }

                    @Override
                    public void start(final Consumer<Object> checkpointCallback) {
                        throw new UnsupportedOperationException("#start()");
                    }

                    @Override
                    public Object getCheckpoint() {
                        throw new UnsupportedOperationException("#getCheckpoint()");
                    }

                    @Override
                    public boolean isCheckpointReady() {
                        throw new UnsupportedOperationException("#isCheckpointReady()");
                    }
                };
            }

            @Override
            public boolean isStream() {
                return false;
            }
        });
    }
}
