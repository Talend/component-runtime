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
package org.talend.sdk.component.junit.http.internal.impl;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.talend.sdk.component.junit.http.api.HttpApiHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class HandlerImpl<T extends HttpApiHandler<?>> implements AutoCloseable {

    private final HttpApiHandler<T> handler;

    private Thread instance;

    private Runnable shutdown;

    public synchronized HandlerImpl<T> start() {
        if (instance != null) {
            throw new IllegalStateException("Instance already started");
        }

        if (handler.getPort() <= 0) {
            handler.setPort(newRandomPort());
        }

        final CountDownLatch startingPistol = new CountDownLatch(1);
        final int nProcessors = Math.max(1, Runtime.getRuntime().availableProcessors());
        final ExecutorService boosExecutor =
                Executors.newFixedThreadPool(1, new DefaultThreadFactory("talend-api-boss"));
        final ExecutorService workerExecutor =
                Executors.newFixedThreadPool(nProcessors, new DefaultThreadFactory("talend-api-worker"));
        instance = new Thread(() -> {
            // todo: config
            final EventLoopGroup bossGroup = new NioEventLoopGroup(1, boosExecutor);
            final EventLoopGroup workerGroup = new NioEventLoopGroup(nProcessors, workerExecutor);
            try {
                final ServerBootstrap b = new ServerBootstrap();
                b
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ProxyInitializer(handler))
                        .bind("localhost", handler.getPort())
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
                close();
                Thread.currentThread().interrupt();
            }
        }) {

            {
                setName("Talend-API-monitor_" + HandlerImpl.this.getClass().getSimpleName() + "_"
                        + HandlerImpl.this.hashCode());
            }
        };
        log.info("Starting Talend API server on port {}", handler.getPort());
        instance.start();
        try {
            if (!startingPistol.await(Integer.getInteger("talend.junit.http.starting.timeout", 60), SECONDS)) {
                log
                        .warn("API server took more than the expected timeout to start, you can tune it "
                                + "setting talend.junit.http.starting.timeout system property");
            }
        } catch (final InterruptedException e) {
            log.warn(e.getMessage());
            Thread.currentThread().interrupt();
        }

        if (shutdown != null && handler.isGlobalProxyConfiguration()) {
            final String pt = Integer.toString(handler.getPort());

            Stream.of("", "s").forEach(s -> {
                shutdown = decorate(setProperty("http" + s + ".proxyHost", "localhost"), shutdown);
                shutdown = decorate(setProperty("http" + s + ".proxyPort", pt), shutdown);
                shutdown = decorate(setProperty("http" + s + ".nonProxyHosts", "local|*.local"), shutdown);
            });

            if (handler.getSslContext() != null) {
                try {
                    final SSLContext defaultSslContext = SSLContext.getDefault();
                    final HostnameVerifier defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
                    shutdown = decorate(() -> SSLContext.setDefault(defaultSslContext), shutdown);
                    shutdown = decorate(() -> {
                        HttpsURLConnection.setDefaultSSLSocketFactory(defaultSslContext.getSocketFactory());
                        HttpsURLConnection.setDefaultHostnameVerifier(defaultHostnameVerifier);
                    }, shutdown);
                    shutdown = decorate(
                            () -> setProperty("jdk.internal.httpclient.disableHostnameVerification", "true"), shutdown);

                    SSLContext.setDefault(handler.getSslContext());
                    HttpsURLConnection.setDefaultSSLSocketFactory(handler.getSslContext().getSocketFactory());
                    HttpsURLConnection.setDefaultHostnameVerifier((host, sslSession) -> true);
                } catch (final NoSuchAlgorithmException e) {
                    throw new IllegalStateException(e);
                }
            }
            log
                    .info("Configured the JVM to use the {} API proxy localhost:{}",
                            handler.getSslContext() != null ? "SSL" : "plain", handler.getPort());
        }
        return this;
    }

    @Override
    public synchronized void close() {
        ofNullable(shutdown).ifPresent(Runnable::run);
        if (instance != null) {
            log.info("Stopping Talend API server (port {})", handler.getPort());
            try {
                instance.join(TimeUnit.MINUTES.toMillis(5));
            } catch (final InterruptedException e) {
                log.warn(e.getMessage(), e);
                Thread.currentThread().interrupt();
            } finally {
                instance = null;
                shutdown = null;
            }
        }
        Stream
                .of(handler.getResponseLocator(), handler.getExecutor())
                .filter(AutoCloseable.class::isInstance)
                .map(AutoCloseable.class::cast)
                .forEach(c -> {
                    try {
                        c.close();
                    } catch (final Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
        if (!AutoCloseable.class.isInstance(handler.getExecutor())
                && ExecutorService.class.isInstance(handler.getExecutor())) {
            final ExecutorService executorService = ExecutorService.class.cast(handler.getExecutor());
            executorService.shutdownNow(); // we don't need to wait here
        }
    }

    private Runnable decorate(final Runnable last, final Runnable first) {
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
}
