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
package org.talend.sdk.component.runtime.manager.interceptor;

import static java.util.stream.Collectors.joining;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.talend.sdk.component.api.service.cache.Cached;
import org.talend.sdk.component.api.service.cache.LocalCache;
import org.talend.sdk.component.api.service.interceptor.InterceptorHandler;

public class CacheHandler implements InterceptorHandler {

    private final BiFunction<Method, Object[], Object> invoker;

    private final LocalCache cache;

    private final ConcurrentMap<Method, Long> timeouts = new ConcurrentHashMap<>();

    public CacheHandler(final BiFunction<Method, Object[], Object> invoker, final LocalCache cache) {
        this.invoker = invoker;
        this.cache = cache;
    }

    @Override
    public Object invoke(final Method method, final Object[] args) {
        final String key = toKey(method, args);
        final long timeout = timeouts.computeIfAbsent(method, m -> findAnnotation(m, Cached.class).get().timeout());
        return cache.computeIfAbsent(Object.class, key, timeout, () -> invoker.apply(method, args));
    }

    // assumes toString() and hashCode() of params are representative
    private String toKey(final Method method, final Object[] args) {
        return method.getDeclaringClass().getName() + "#" + method.getName() + "("
                + (args == null ? ""
                        : Stream
                                .of(args)
                                .map(s -> String.valueOf(s) + "/" + (s == null ? 0 : s.hashCode()))
                                .collect(joining(",")))
                + ")";
    }
}
