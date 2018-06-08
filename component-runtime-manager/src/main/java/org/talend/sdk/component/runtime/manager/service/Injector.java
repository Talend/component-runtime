/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.manager.service;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.stream.Stream;

import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.AllArgsConstructor;

// internal service for now, we can refactor it later to expose it if needed
@AllArgsConstructor
public class Injector implements Serializable {

    private final String plugin;

    private final Map<Class<?>, Object> services;

    public <T> T inject(final T instance) {
        if (instance == null) {
            return null;
        }
        doInject(instance.getClass(), instance);
        return instance;
    }

    private <T> void doInject(final Class<?> type, final T instance) {
        if (type == Object.class || type == null) {
            return;
        }
        Stream
                .of(type.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.isAnnotationPresent(Service.class))
                .peek(f -> {
                    if (!f.isAccessible()) {
                        f.setAccessible(true);
                    }
                })
                .forEach(field -> {
                    final Object value = services.get(field.getType());
                    if (value != null) {
                        try {
                            field.set(instance, value);
                        } catch (final IllegalAccessException e) {
                            throw new IllegalArgumentException(e);
                        }
                    }
                });
        if (type.getSuperclass() != type) {
            doInject(type.getSuperclass(), instance);
        }
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, Injector.class.getName());
    }
}
