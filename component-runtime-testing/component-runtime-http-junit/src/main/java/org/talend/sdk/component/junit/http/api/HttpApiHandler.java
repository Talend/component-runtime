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
package org.talend.sdk.component.junit.http.api;

import java.security.cert.CertificateException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.talend.sdk.component.junit.http.internal.impl.DefaultHeaderFilter;
import org.talend.sdk.component.junit.http.internal.impl.DefaultResponseLocator;

import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler used to customize the behavior of the mock server during the test.
 *
 * @param <T> fluent API type.
 */
@Getter
@Slf4j
public class HttpApiHandler<T extends HttpApiHandler<?>> {

    private Executor executor = Executors.newCachedThreadPool();

    private boolean globalProxyConfiguration = true;

    private int port;

    private SSLContext sslContext;

    private ResponseLocator responseLocator = new DefaultResponseLocator(DefaultResponseLocator.PREFIX, null);

    private String logLevel = "DEBUG";

    private Predicate<String> headerFilter = new DefaultHeaderFilter();

    private boolean skipProxyHeaders;

    public T activeSsl() {
        if (sslContext == null) {
            try {
                final SelfSignedCertificate certificate = new SelfSignedCertificate();
                final SslContext nettyContext = SslContext
                        .newServerContext(SslProvider.JDK, null, InsecureTrustManagerFactory.INSTANCE,
                                certificate.certificate(), certificate.privateKey(), null, null, null,
                                IdentityCipherSuiteFilter.INSTANCE, null, 0, 0);
                sslContext = JdkSslContext.class.cast(nettyContext).context();
            } catch (final SSLException | CertificateException e) {
                throw new IllegalStateException(e);
            }
        }
        return (T) this;
    }

    public T setHeaderFilter(final Predicate<String> headerFilter) {
        this.headerFilter = headerFilter;
        return (T) this;
    }

    public T setExecutor(final Executor executor) {
        this.executor = executor;
        return (T) this;
    }

    public T setGlobalProxyConfiguration(final boolean globalProxyConfiguration) {
        this.globalProxyConfiguration = globalProxyConfiguration;
        return (T) this;
    }

    public T setPort(final int port) {
        this.port = port;
        return (T) this;
    }

    public T setSslContext(final SSLContext sslContext) {
        this.sslContext = sslContext;
        return (T) this;
    }

    public T setResponseLocator(final ResponseLocator responseLocator) {
        this.responseLocator = responseLocator;
        return (T) this;
    }

    public T setLogLevel(final String logLevel) {
        this.logLevel = logLevel;
        return (T) this;
    }

    public T setSkipProxyHeaders(final boolean skipProxyHeaders) {
        this.skipProxyHeaders = skipProxyHeaders;
        return (T) this;
    }
}
