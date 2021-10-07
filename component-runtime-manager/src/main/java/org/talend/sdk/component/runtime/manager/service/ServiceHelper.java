/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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

import static org.talend.sdk.component.runtime.base.lang.exception.InvocationExceptionWrapper.toRuntimeException;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.talend.sdk.component.api.service.BaseService;
import org.talend.sdk.component.classloader.ThreadHelper;
import org.talend.sdk.component.runtime.manager.asm.ProxyGenerator;
import org.talend.sdk.component.runtime.manager.interceptor.InterceptorHandlerFacade;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServiceHelper {

    /** proxy generator */
    private final ProxyGenerator proxyGenerator;

    /** all current services */
    private final Map<Class<?>, Object> allServices;

    /**
     * Create an instance of a Service with serialisation system and interceptors ...
     * 
     * @param loader : class loader.
     * @param containerId : container id.
     * @param serviceClass : class of service.
     * @return instance of service.
     * @throws NoSuchMethodException if no default constructor.
     */
    public Object createServiceInstance(final ClassLoader loader, final String containerId, final Class<?> serviceClass)
            throws NoSuchMethodException {
        try {
            final Class<?> proxyClass = this.handleProxy(loader, containerId, serviceClass);
            final Object instance = proxyClass.getConstructor().newInstance();

            if (proxyGenerator.hasInterceptors(serviceClass)) {
                proxyGenerator
                        .initialize(instance,
                                new InterceptorHandlerFacade(serviceClass.getConstructor().newInstance(), allServices));
            }
            if (instance instanceof BaseService) {
                this.updateService((BaseService) instance, containerId, serviceClass.getName());
            }
            return instance;
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (final InvocationTargetException e) {
            throw toRuntimeException(e);
        }
    }

    private Class<?> handleProxy(final ClassLoader loader, final String containerId, final Class<?> type) {
        if (!proxyGenerator.hasInterceptors(type) && proxyGenerator.isSerializable(type)) {
            return type;
        }

        return ThreadHelper
                .runWithClassLoader(() -> proxyGenerator.generateProxy(loader, type, containerId, type.getName()),
                        loader);

    }

    private void updateService(final BaseService service, final String pluginId, final String key) {
        if (service.getSerializationHelper() == null) {
            service.setSerializationHelper(new SerializableService(pluginId, key));
        }
    }
}
