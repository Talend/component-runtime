/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.serialization;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

public class DynamicContainerFinder implements ContainerFinder {

    public static final Map<String, ClassLoader> LOADERS = new ConcurrentHashMap<>();

    @Override
    public LightContainer find(final String plugin) {
        return new LightContainer() {

            @Override
            public ClassLoader classloader() {
                return plugin == null ? Thread.currentThread().getContextClassLoader() : LOADERS.get(plugin);
            }

            @Override
            public <T> T findService(final Class<T> key) {
                try {
                    if (key == Jsonb.class) {
                        return key.cast(JsonbBuilder.create());
                    }
                    if (key == RecordBuilderFactory.class) {
                        return key.cast(new RecordBuilderFactoryImpl("test"));
                    }
                    return key.isInterface() ? null : key.getConstructor().newInstance();
                } catch (final InstantiationException | IllegalAccessException | InvocationTargetException
                        | NoSuchMethodException e) {
                    return null; // if there is a constructor let the default be handled in the caller
                }
            }
        };
    }
}
