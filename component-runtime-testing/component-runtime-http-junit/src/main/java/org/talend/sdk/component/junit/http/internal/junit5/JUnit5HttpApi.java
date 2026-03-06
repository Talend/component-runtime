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
package org.talend.sdk.component.junit.http.internal.junit5;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;
import org.talend.sdk.component.junit.base.junit5.JUnit5InjectionSupport;
import org.talend.sdk.component.junit.http.api.HttpApiHandler;
import org.talend.sdk.component.junit.http.api.ResponseLocator;
import org.talend.sdk.component.junit.http.internal.impl.DefaultResponseLocator;
import org.talend.sdk.component.junit.http.internal.impl.HandlerImpl;
import org.talend.sdk.component.junit.http.internal.impl.Handlers;
import org.talend.sdk.component.junit.http.junit5.HttpApi;
import org.talend.sdk.component.junit.http.junit5.HttpApiInject;
import org.talend.sdk.component.junit.http.junit5.HttpApiName;

public class JUnit5HttpApi extends HttpApiHandler<JUnit5HttpApi>
        implements BeforeAllCallback, AfterAllCallback, JUnit5InjectionSupport, AfterEachCallback, BeforeEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(JUnit5HttpApi.class.getName());

    @Override
    public void beforeAll(final ExtensionContext extensionContext) {
        final HttpApi config =
                AnnotationUtils.findAnnotation(extensionContext.getElement(), HttpApi.class).orElse(null);
        if (config != null) {
            setGlobalProxyConfiguration(config.globalProxyConfiguration());
            setLogLevel(config.logLevel());
            setPort(config.port());
            newInstance(config.responseLocator(), ResponseLocator.class).ifPresent(this::setResponseLocator);
            newInstance(config.headerFilter(), Predicate.class).ifPresent(this::setHeaderFilter);
            newInstance(config.executor(), Executor.class).ifPresent(this::setExecutor);
            newInstance(config.sslContext(), Supplier.class)
                    .map(s -> SSLContext.class.cast(s.get()))
                    .ifPresent(this::setSslContext);
            setSkipProxyHeaders(config.skipProxyHeaders());
            if (config.useSsl()) {
                activeSsl();
            }
        }
        extensionContext.getStore(NAMESPACE).put(HttpApiHandler.class.getName(), this);
        final HandlerImpl<JUnit5HttpApi> handler = new HandlerImpl<>(this, null, null);
        extensionContext.getStore(NAMESPACE).put(HandlerImpl.class.getName(), handler);
        handler.start();
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext) {
        HandlerImpl.class.cast(extensionContext.getStore(NAMESPACE).get(HandlerImpl.class.getName())).close();
    }

    @Override
    public Class<? extends Annotation> injectionMarker() {
        return HttpApiInject.class;
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) {
        // test name
        final ResponseLocator responseLocator = getResponseLocator();
        if (!DefaultResponseLocator.class.isInstance(responseLocator)) {
            return;
        }
        final String test = extensionContext.getTestMethod().map(m -> {
            final String displayName = sanitizeDisplayName(extensionContext.getDisplayName());
            return AnnotationUtils
                    .findAnnotation(m, HttpApiName.class)
                    .map(HttpApiName::value)
                    .map(it -> it.replace("${class}", m.getDeclaringClass().getName()))
                    .map(it -> it.replace("${method}", m.getName()))
                    .map(it -> it.replace("${displayName}", displayName))
                    .orElseGet(() -> m.getDeclaringClass().getName() + "_" + m.getName()
                            + (displayName.equals(m.getName()) ? "" : ("_" + displayName)));
        }).orElse(null);
        DefaultResponseLocator.class.cast(responseLocator).setTest(test);
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) {
        if (Handlers.isActive("capture")) {
            Optional
                    .of(getResponseLocator())
                    .filter(DefaultResponseLocator.class::isInstance)
                    .map(DefaultResponseLocator.class::cast)
                    .ifPresent(r -> r.flush(Handlers.getBaseCapture()));
        }
    }

    private String sanitizeDisplayName(final String displayName) {
        final String base = displayName.replace(" ", "_");
        final int parenthesis = base.indexOf('(');
        if (parenthesis > 0) {
            return base.substring(0, parenthesis);
        }
        return base;
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
