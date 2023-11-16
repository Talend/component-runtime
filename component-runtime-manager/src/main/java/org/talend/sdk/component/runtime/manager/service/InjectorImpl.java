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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.configuration.Configuration;
import org.talend.sdk.component.api.service.injector.Injector;
import org.talend.sdk.component.runtime.manager.asm.ProxyGenerator;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.AllArgsConstructor;

// internal service for now, we can refactor it later to expose it if needed
@AllArgsConstructor
public class InjectorImpl implements Serializable, Injector {

    private final String plugin;

    private final ReflectionService reflectionService;

    private final ProxyGenerator proxyGenerator;

    private final Map<Class<?>, Object> services;

    @Override
    public <T> T inject(final T instance) {
        if (instance == null) {
            return null;
        }
        doInject(instance.getClass(), unwrap(instance));
        return instance;
    }

    private Object unwrap(final Object instance) {
        if (instance.getClass().getName().endsWith("$$TalendServiceProxy")) {
            try {
                return proxyGenerator.getHandler(instance).getDelegate();
            } catch (final IllegalStateException nsfe) {
                // no-op
            }
        }
        return instance;
    }

    private <T> void doInject(final Class<?> type, final T instance) {
        if (type == Object.class || type == null) {
            return;
        }
        final Field[] fields = type.getDeclaredFields();
        Stream
                .of(fields)
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.isAnnotationPresent(Service.class))
                .peek(f -> {
                    if (!f.isAccessible()) {
                        f.setAccessible(true);
                    }
                })
                .forEach(field -> {
                    Object value = services.get(field.getType());
                    if (value == null && ParameterizedType.class.isInstance(field.getGenericType())) {
                        final ParameterizedType pt = ParameterizedType.class.cast(field.getGenericType());
                        if (Class.class.isInstance(pt.getRawType())
                                && Collection.class.isAssignableFrom(Class.class.cast(pt.getRawType()))) {
                            final Type serviceType = pt.getActualTypeArguments()[0];
                            if (Class.class.isInstance(serviceType)) {
                                final Class<?> serviceClass = Class.class.cast(serviceType);
                                value = services
                                        .entrySet()
                                        .stream()
                                        .filter(e -> serviceClass.isAssignableFrom(e.getKey()))
                                        .collect(toList());
                            }
                        }
                    }
                    if (value != null) {
                        try {
                            field.set(instance, value);
                        } catch (final IllegalAccessException e) {
                            throw new IllegalArgumentException(e);
                        }
                    }
                });
        Stream
                .of(fields)
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.isAnnotationPresent(Configuration.class))
                .peek(field -> {
                    if (Supplier.class != field.getType()
                            || !ParameterizedType.class.isInstance(field.getGenericType())) {
                        throw new IllegalArgumentException("Field " + field + " is not a Supplier<X>,\n"
                                + "it will not be injected otherwise it wouldn't be up to date,\n"
                                + "did you mean Supplier<" + field.getType() + "> ?");
                    }
                })
                .peek(f -> {
                    if (!f.isAccessible()) {
                        f.setAccessible(true);
                    }
                })
                .forEach(field -> {
                    try {
                        final Class<?> configClass = Class.class
                                .cast(ParameterizedType.class.cast(field.getGenericType()).getActualTypeArguments()[0]);
                        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
                        final Supplier<?> supplier = () -> {
                            try {
                                return reflectionService
                                        .createConfigFactory(services, loader,
                                                reflectionService.createContextualSupplier(loader), field.getName(),
                                                field.getAnnotation(Configuration.class), field.getAnnotations(),
                                                configClass)
                                        .apply(emptyMap());
                            } catch (final NoSuchMethodException e) {
                                throw new IllegalStateException(e);
                            }
                        };
                        field.set(instance, supplier);
                    } catch (final IllegalAccessException e) {
                        throw new IllegalArgumentException(e);
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
