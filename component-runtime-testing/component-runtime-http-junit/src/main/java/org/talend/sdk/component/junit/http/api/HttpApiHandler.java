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
package org.talend.sdk.component.junit.http.api;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.talend.sdk.component.junit.http.internal.impl.DefaultHeaderFilter;
import org.talend.sdk.component.junit.http.internal.impl.DefaultResponseLocator;
import org.talend.sdk.component.junit.http.internal.impl.ProxyInitializer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.DefaultThreadFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler used to customize the behavior of the mock server during the test.
 *
 * @param <T> fluent API type.
 */
@Slf4j
public class HttpApiHandler<T extends HttpApiHandler<?>> implements AutoCloseable {

    private Executor executor;

    private boolean globalProxyConfiguration;

    private int port;

    private SSLContext sslContext;

    private ResponseLocator responseLocator;

    private String logLevel;

    private Predicate<String> headerFilter;

    private Thread instance;

    private Runnable shutdown;

    public HttpApiHandler() {
        this.executor = Runnable::run;
        this.globalProxyConfiguration = true;
        this.port = 0;
        this.sslContext = null;
        this.responseLocator = new DefaultResponseLocator(DefaultResponseLocator.PREFIX, null);
        this.logLevel = "DEBUG";
        this.headerFilter = new DefaultHeaderFilter();
    }

    public synchronized HttpApiHandler start() {
        if (instance != null) {
            throw new IllegalStateException("Instance already started");
        }

        if (port <= 0) {
            port = newRandomPort();
        }

        final CountDownLatch startingPistol = new CountDownLatch(1);
        instance = new Thread(() -> {
            // todo: config
            final EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("talend-api-boss"));
            final EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(),
                    new DefaultThreadFactory("talend-api-worker"));
            try {
                final ServerBootstrap b = new ServerBootstrap();
                b
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ProxyInitializer(this))
                        .bind("localhost", port)
                        .sync()
                        .addListener((ChannelFutureListener) f -> {
                            if (f.isSuccess()) {
                                shutdown = () -> {
                                    bossGroup.shutdownGracefully();
                                    workerGroup.shutdownGracefully();
                                };
                            } else {
                                log.error("Can't start API server");
                            }
                            startingPistol.countDown();
                        })
                        .channel()
                        .closeFuture()
                        .sync();
            } catch (final InterruptedException e) {
                Thread.interrupted();
                close();
            }
        }) {

            {
                setName("Talend-API-monitor_" + HttpApiHandler.this.getClass().getSimpleName() + "_"
                        + HttpApiHandler.this.hashCode());
            }
        };
        log.info("Starting Talend API server on port {}", port);
        instance.start();
        try {
            if (!startingPistol.await(Integer.getInteger("talend.junit.http.starting.timeout", 60), SECONDS)) {
                log.warn("API server took more than the expected timeout to start, you can tune it "
                        + "setting talend.junit.http.starting.timeout system property");
            }
        } catch (final InterruptedException e) {
            Thread.interrupted();
            log.warn(e.getMessage());
        }

        if (shutdown != null && globalProxyConfiguration) {
            final String protocol = "http" + (sslContext != null ? "s" : "");
            final String pt = Integer.toString(port);

            shutdown = append(setProperty(protocol + ".proxyHost", "localhost"), shutdown);
            shutdown = append(setProperty(protocol + ".proxyPort", pt), shutdown);
            shutdown = append(setProperty(protocol + ".nonProxyHosts", "local|*.local"), shutdown);

            if (sslContext != null) {
                try {
                    final SSLContext defaultSslContext = SSLContext.getDefault();
                    shutdown = append(() -> SSLContext.setDefault(defaultSslContext), shutdown);
                    shutdown = append(
                            () -> HttpsURLConnection.setDefaultSSLSocketFactory(defaultSslContext.getSocketFactory()),
                            shutdown);

                    SSLContext.setDefault(sslContext);
                    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                } catch (final NoSuchAlgorithmException e) {
                    throw new IllegalStateException(e);
                }
            }
            log.info("Configured the JVM to use the {} proxy localhost:{}", protocol, port);
        }

        return this;
    }

    @Override
    public synchronized void close() {
        ofNullable(shutdown).ifPresent(Runnable::run);
        if (instance != null) {
            log.info("Stopping Talend API server (port {})", port);
            try {
                instance.join(TimeUnit.MINUTES.toMillis(5));
            } catch (final InterruptedException e) {
                Thread.interrupted();
                log.warn(e.getMessage(), e);
            } finally {
                instance = null;
                shutdown = null;
            }
        }
        Stream
                .of(responseLocator, executor)
                .filter(AutoCloseable.class::isInstance)
                .map(AutoCloseable.class::cast)
                .forEach(c -> {
                    try {
                        c.close();
                    } catch (final Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
    }

    private Runnable append(final Runnable last, final Runnable first) {
        return () -> {
            first.run();
            last.run();
        };
    }

    private Runnable setProperty(final String name, final String value) {
        final String val = System.getProperty(name);
        System.setProperty(name, value);
        return () -> {
            if (val == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, val);
            }
        };
    }

    private int newRandomPort() {
        try (final ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Predicate<String> getHeaderFilter() {
        return headerFilter;
    }

    public T setHeaderFilter(final Predicate<String> headerFilter) {
        this.headerFilter = headerFilter;
        return (T) this;
    }

    public Executor getExecutor() {
        return executor;
    }

    public T setExecutor(final Executor executor) {
        this.executor = executor;
        return (T) this;
    }

    public T activeSsl() {
        try {
            final SelfSignedCertificate certificate = new SelfSignedCertificate();
            final SslContext nettyContext = SslContextBuilder
                    .forServer(certificate.certificate(), certificate.privateKey())
                    .sslProvider(SslProvider.JDK)
                    .build();
            sslContext = JdkSslContext.class.cast(nettyContext).context();
        } catch (final SSLException | CertificateException e) {
            throw new IllegalStateException(e);
        }
        return (T) this;
    }

    public boolean isGlobalProxyConfiguration() {
        return globalProxyConfiguration;
    }

    public T setGlobalProxyConfiguration(final boolean globalProxyConfiguration) {
        this.globalProxyConfiguration = globalProxyConfiguration;
        return (T) this;
    }

    public int getPort() {
        return port;
    }

    public T setPort(final int port) {
        this.port = port;
        return (T) this;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public T setSslContext(final SSLContext sslContext) {
        this.sslContext = sslContext;
        return (T) this;
    }

    public ResponseLocator getResponseLocator() {
        return responseLocator;
    }

    public T setResponseLocator(final ResponseLocator responseLocator) {
        this.responseLocator = responseLocator;
        return (T) this;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public T setLogLevel(final String logLevel) {
        this.logLevel = logLevel;
        return (T) this;
    }
}
