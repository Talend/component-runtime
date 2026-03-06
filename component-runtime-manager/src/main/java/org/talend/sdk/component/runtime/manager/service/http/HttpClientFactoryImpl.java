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
package org.talend.sdk.component.runtime.manager.service.http;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.talend.sdk.component.runtime.base.lang.exception.InvocationExceptionWrapper.toRuntimeException;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;

import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.HttpClientFactory;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.runtime.manager.proxy.SerializationHandlerReplacer;
import org.talend.sdk.component.runtime.manager.reflect.Copiable;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;
import org.talend.sdk.component.runtime.reflect.Defaults;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
public class HttpClientFactoryImpl implements HttpClientFactory, Serializable {

    private final String plugin;

    private final ReflectionService reflections;

    private final Jsonb jsonb;

    private final Map<Class<?>, Object> services;

    public static <T> Collection<String> createErrors(final Class<T> api) {
        final Collection<String> errors = new ArrayList<>();
        final Collection<Method> methods =
                of(api.getMethods()).filter(m -> m.getDeclaringClass() == api && !m.isDefault()).collect(toList());

        if (!HttpClient.class.isAssignableFrom(api)) {
            errors.add(api.getCanonicalName() + " should extends HttpClient");
        }
        errors
                .addAll(methods
                        .stream()
                        .filter(m -> !m.isAnnotationPresent(Request.class))
                        .map(m -> "No @Request on " + m)
                        .collect(toList()));
        return errors;
    }

    @Override
    public <T> T create(final Class<T> api, final String base) {
        if (!api.isInterface()) {
            throw new IllegalArgumentException(api + " is not an interface");
        }
        validate(api);
        final HttpHandler handler =
                new HttpHandler(api.getName(), plugin, new RequestParser(reflections, jsonb, services));
        final T instance = api
                .cast(Proxy
                        .newProxyInstance(api.getClassLoader(),
                                Stream
                                        .of(api, HttpClient.class, Serializable.class, Copiable.class)
                                        .distinct()
                                        .toArray(Class[]::new),
                                handler));
        HttpClient.class.cast(instance).base(base);
        return instance;
    }

    private <T> void validate(final Class<T> api) {
        final Collection<String> errors = createErrors(api);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid Http Proxy specification:\n" + errors.stream().collect(joining("\n- ", "- ", "\n")));
        }
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, HttpClientFactory.class.getName());
    }

    @ToString
    @RequiredArgsConstructor
    private static class HttpHandler implements InvocationHandler, Serializable {

        private final String proxyType;

        private final String plugin;

        private String base;

        private final RequestParser requestParser;

        private volatile Map<Class<?>, Object> jaxbContexts;

        private volatile ConcurrentMap<Method, ExecutionContext> invokers;

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (Copiable.class == method.getDeclaringClass()) {
                final HttpHandler httpHandler = new HttpHandler(proxyType, plugin, requestParser);
                httpHandler.base = base;
                return Proxy
                        .newProxyInstance(proxy.getClass().getClassLoader(), proxy.getClass().getInterfaces(),
                                httpHandler);
            }
            if (Defaults.isDefaultAndShouldHandle(method)) {
                return Defaults.handleDefault(method.getDeclaringClass(), method, proxy, args);
            }

            final String methodName = method.getName();
            if (Object.class == method.getDeclaringClass()) {
                switch (methodName) {
                    case "equals":
                        return args[0] != null && Proxy.isProxyClass(args[0].getClass())
                                && equals(Proxy.getInvocationHandler(args[0]));
                    case "toString":
                        return "@Request " + base;
                    default:
                        return delegate(method, args);
                }
            } else if (HttpClient.class == method.getDeclaringClass()) {
                switch (methodName) {
                    case "base":
                        this.base = String.valueOf(args[0]);
                        return null;
                    default:
                        throw new UnsupportedOperationException("HttpClient." + methodName);
                }
            }

            if (!method.isAnnotationPresent(Request.class)) {
                return delegate(method, args);
            }

            if (invokers == null) {
                synchronized (this) {
                    if (invokers == null) {
                        invokers = new ConcurrentHashMap<>();
                        jaxbContexts = new ConcurrentHashMap<>();
                    }
                }
            }

            return invokers.computeIfAbsent(method, this.requestParser::parse).apply(this.base, args);
        }

        Object writeReplace() throws ObjectStreamException {
            return new SerializationHandlerReplacer(plugin, proxyType);
        }

        private Object delegate(final Method method, final Object[] args) {
            try {
                return method.invoke(this, args);
            } catch (final InvocationTargetException ite) {
                throw toRuntimeException(ite);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

}
