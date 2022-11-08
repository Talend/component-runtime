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
package org.talend.sdk.component.runtime.manager.interceptor;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.talend.sdk.component.api.service.cache.Cached;
import org.talend.sdk.component.api.service.cache.LocalCache;
import org.talend.sdk.component.api.service.interceptor.InterceptorHandler;
import org.talend.sdk.component.api.service.interceptor.Intercepts;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class InterceptorHandlerFacade implements InterceptorHandler {

    @Getter
    private final Object delegate;

    private final Map<Class<?>, Object> services;

    private final ConcurrentMap<Method, BiFunction<Method, Object[], Object>> invocations = new ConcurrentHashMap<>();

    @Override
    public Object invoke(final Method method, final Object[] args) {
        return invocations.computeIfAbsent(method, m -> {
            final Collection<InvokerHandler> handlers = Stream
                    .of(method.getAnnotations())
                    .filter(a -> interceptsConfig(a) != null)
                    .sorted(comparing(a -> interceptsConfig(a).priority()))
                    .map(a -> Optional
                            .of(interceptsConfig(a).value())
                            .filter(v -> v != InterceptorHandler.class)
                            .map(handler -> {
                                Optional<Constructor<?>> constructor = findConstructor(handler, BiFunction.class);
                                if (constructor.isPresent()) {
                                    return new InvokerHandler(constructor.get(), true, null);
                                }
                                constructor = findConstructor(handler, Object.class);
                                if (constructor.isPresent()) { // any, assume all params are services
                                    return new InvokerHandler(constructor.get(), false, null);
                                }
                                constructor = findConstructor(handler, null);
                                if (constructor.isPresent()) {
                                    return new InvokerHandler(constructor.get(), false, null);
                                }
                                throw new IllegalArgumentException("No available constructor for " + handler);
                            })
                            .map(InvokerHandler.class::cast)
                            .orElseGet(() -> { // built-in interceptors
                                if (a.annotationType() == Cached.class) {
                                    try {
                                        return new InvokerHandler(
                                                CacheHandler.class.getConstructor(BiFunction.class, LocalCache.class),
                                                true, null);
                                    } catch (final NoSuchMethodException e) {
                                        throw new IllegalStateException("Bad classpath", e);
                                    }
                                }
                                throw new IllegalArgumentException("No handler for " + a);
                            }))
                    .collect(toList());
            if (handlers.isEmpty()) {
                return (mtd, arguments) -> doInvoke(method, args);
            }

            // init all InvokerHandler
            final List<InvokerHandler> invokerHandlers =
                    handlers.stream().filter(i -> i.invoker).map(InvokerHandler.class::cast).collect(toList());
            if (invokerHandlers.isEmpty() && handlers.size() > 1) {
                throw new IllegalArgumentException("Interceptors not compatible for " + m + ": "
                        + handlers.stream().filter(i -> !invokerHandlers.contains(i)).collect(toList()));
            }
            if (invokerHandlers.isEmpty()) {
                return handlers.iterator().next()::invoke;
            }

            if (invokerHandlers.size() != handlers.size()) {
                throw new IllegalArgumentException("Some handlers don't take an invoker as parameter for method " + m
                        + ": " + handlers.stream().filter(i -> !invokerHandlers.contains(i)).collect(toList()));
            }
            for (int i = 0; i < invokerHandlers.size(); i++) {
                final InvokerHandler invokerHandler = invokerHandlers.get(i);
                invokerHandler
                        .init(i == invokerHandlers.size() - 1 ? this::doInvoke : invokerHandlers.get(i + 1)::invoke,
                                delegate, services);
            }
            return invokerHandlers.get(0)::invoke;
        }).apply(method, args);
    }

    private Intercepts interceptsConfig(final Annotation a) {
        return a.annotationType().getAnnotation(Intercepts.class);
    }

    private Optional<Constructor<?>> findConstructor(final Class<? extends InterceptorHandler> handler,
            final Class<?> firstParamType) {
        return Stream
                .of(handler.getConstructors())
                .filter(c -> firstParamType == null
                        || (c.getParameterCount() > 0 && c.getParameterTypes()[0] == firstParamType))
                .findFirst();
    }

    private Object doInvoke(final Method method, final Object[] args) {
        try {
            return method.invoke(delegate, args);
        } catch (final InvocationTargetException ite) {
            final Throwable cause = ite.getCause();
            if (RuntimeException.class.isInstance(cause)) {
                throw RuntimeException.class.cast(cause);
            }
            throw new IllegalStateException(cause.getMessage());
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @AllArgsConstructor
    public static class InvokerHandler implements InterceptorHandler {

        private final Constructor<?> constructor;

        private final boolean invoker;

        private InterceptorHandler delegate;

        @Override
        public Object invoke(final Method method, final Object[] args) {
            return delegate.invoke(method, args);
        }

        private void init(final BiFunction<Method, Object[], Object> invoker, final Object delegate,
                final Map<Class<?>, Object> services) {
            try {
                final Object[] args = buildArgs(invoker, delegate, services);
                this.delegate = InterceptorHandler.class.cast(constructor.newInstance(args));
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getCause());
            }
        }

        private Object[] buildArgs(final BiFunction<Method, Object[], Object> invoker, final Object delegate,
                final Map<Class<?>, Object> services) {
            final Object[] args = new Object[constructor.getParameterCount()];
            for (int i = 0; i < constructor.getParameterCount(); i++) {
                final Class<?> type = constructor.getParameterTypes()[i];
                if (BiFunction.class == type) {
                    args[i] = invoker;
                } else if (Object.class == type) {
                    args[i] = delegate;
                } else {
                    args[i] = services.get(type);
                }
            }
            return args;
        }
    }
}
