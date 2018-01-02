/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit.http.internal.junit5;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.talend.sdk.component.junit.http.api.HttpApiHandler;
import org.talend.sdk.component.junit.http.api.ResponseLocator;
import org.talend.sdk.component.junit.http.internal.impl.DefaultResponseLocator;
import org.talend.sdk.component.junit.http.internal.impl.DefaultResponseLocatorCapturingHandler;
import org.talend.sdk.component.junit.http.junit5.HttpApi;
import org.talend.sdk.component.junit.http.junit5.HttpApiInject;

public class JUnit5HttpApi extends HttpApiHandler<JUnit5HttpApi>
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(JUnit5HttpApi.class.getName());

    @Override
    public void beforeAll(final ExtensionContext extensionContext) {
        final HttpApi config = extensionContext.getElement().map(c -> c.getAnnotation(HttpApi.class)).orElse(null);
        if (config != null) {
            setGlobalProxyConfiguration(config.globalProxyConfiguration());
            setLogLevel(config.logLevel());
            setPort(config.port());
            newInstance(config.responseLocator(), ResponseLocator.class).ifPresent(this::setResponseLocator);
            newInstance(config.headerFilter(), Predicate.class).ifPresent(this::setHeaderFilter);
            newInstance(config.executor(), Executor.class).ifPresent(this::setExecutor);
            newInstance(config.sslContext(), Supplier.class).map(s -> SSLContext.class.cast(s.get())).ifPresent(
                    this::setSslContext);
        }
        extensionContext.getStore(NAMESPACE).put(HttpApiHandler.class.getName(), this);
        start();
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext) {
        close();
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) {
        // injection
        Class<?> testClass = extensionContext.getRequiredTestClass();
        while (testClass != Object.class) {
            Stream.of(testClass.getDeclaredFields()).filter(c -> c.isAnnotationPresent(HttpApiInject.class)).forEach(
                    f -> {
                        f.setAccessible(true);
                        if (f.getType() != HttpApiHandler.class) {
                            throw new IllegalArgumentException(
                                    "@HttpApiInject not supported on " + f + ", type should be HttpApiHandler<?>");
                        }
                        try {
                            f.set(extensionContext.getRequiredTestInstance(), JUnit5HttpApi.this);
                        } catch (final IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
            testClass = testClass.getSuperclass();
        }

        // test name
        final ResponseLocator responseLocator = getResponseLocator();
        if (!DefaultResponseLocator.class.isInstance(responseLocator)) {
            return;
        }
        DefaultResponseLocator.class.cast(responseLocator).setTest(
                extensionContext.getTestMethod().map(m -> m.getDeclaringClass().getName() + "_" + m.getName()).orElse(
                        null));
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) {
        if (DefaultResponseLocatorCapturingHandler.isActive()) {
            Optional
                    .of(getResponseLocator())
                    .filter(DefaultResponseLocator.class::isInstance)
                    .map(DefaultResponseLocator.class::cast)
                    .ifPresent(r -> r.flush(DefaultResponseLocatorCapturingHandler.getBaseCapture()));
        }
    }

    private static <T> Optional<T> newInstance(final Class<?> type, final Class<T> api) {
        if (api == type) {
            return Optional.empty();
        }
        try {
            return Optional.of(api.cast(type.getConstructor().newInstance()));
        } catch (final NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }
}
