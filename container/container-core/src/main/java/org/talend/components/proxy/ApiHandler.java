// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.proxy;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;

import org.talend.components.lang.UnsafeFunction;
import org.talend.components.lang.UnsafeSupplier;

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
        // for now we don't need to wrap the returned instance in another proxy layer (same as this one)
        // if we start to propagate not shared API we'll need to go this way
        return context.apply(() -> targetMethod.invoke(delegate, args));
    }
}
