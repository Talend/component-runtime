/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;

import org.talend.sdk.component.lang.UnsafeFunction;
import org.talend.sdk.component.lang.UnsafeSupplier;

public class ApiHandler extends DelegateHandler {

    private final Map<Method, Method> methodMapping;

    private final UnsafeFunction<UnsafeSupplier<Object>, Object> context;

    public ApiHandler(final Object delegate, final Class<?> api,
            final UnsafeFunction<UnsafeSupplier<Object>, Object> executionWrapper) {
        super(delegate);
        methodMapping = Stream.of(api.getMethods()).collect(toMap(identity(), (Method m) -> {
            try {
                return delegate.getClass().getMethod(m.getName(), m.getParameterTypes());
            } catch (final NoSuchMethodException e) {
                throw new IllegalStateException("Invalid proxy type: " + delegate + ", for api: " + api);
            }
        }));
        context = executionWrapper;
    }

    @Override
    protected Object doInvoke(final Method method, final Object[] args) throws Throwable {
        final Method targetMethod = ofNullable(methodMapping.get(method)).orElse(method);
        // for now we don't need to wrap the returned instance in another proxy layer
        // (same as this one)
        // if we start to propagate not shared API we'll need to go this way
        return context.apply(() -> targetMethod.invoke(delegate, args));
    }
}
