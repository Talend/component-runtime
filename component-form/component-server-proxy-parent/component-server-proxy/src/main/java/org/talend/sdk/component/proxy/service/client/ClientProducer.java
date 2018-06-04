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
package org.talend.sdk.component.proxy.service.client;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.json.bind.Jsonb;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.internal.jaxrs.JAXRSClient;
import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.proxy.service.ActionService;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ClientProducer {

    @Produces
    @UiSpecProxy
    public UiSpecService<UiSpecContext> uiSpecService(@UiSpecProxy final Client client,
            @UiSpecProxy final Jsonb jsonb) {
        return new UiSpecService<>(client, jsonb);
    }

    public void destroyUiSpecService(@Disposes @UiSpecProxy final UiSpecService<UiSpecContext> client) {
        try {
            client.close();
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Produces
    @UiSpecProxy
    @ApplicationScoped
    public Client<UiSpecContext> uiSpecClient(@UiSpecProxy final javax.ws.rs.client.Client client,
            final ProxyConfiguration configuration, final ActionService actionService) {
        return new JAXRSClient<UiSpecContext>(client, configuration.getTargetServerBase(), false) {

            @Override
            public CompletableFuture<Map<String, Object>> action(final String family, final String type,
                    final String action, final Map<String, Object> params, final UiSpecContext context) {
                if (actionService.isBuiltin(action)) {
                    return actionService
                            .findBuiltInAction(action, context.getLanguage(), context.getPlaceholderProvider(), params)
                            .toCompletableFuture();
                }
                return super.action(family, type, action, params, context);
            }
        };
    }

    public void destroyUiSpecClient(@Disposes @UiSpecProxy final Client<UiSpecContext> client) {
        client.close();
    }

    @Produces
    @UiSpecProxy
    @ApplicationScoped
    public ExecutorService pool(final ProxyConfiguration configuration) {
        final int core = configuration.getClientPoolCore();
        final int max = configuration.getClientPoolMax();
        final long keepAlive = configuration.getClientPoolKeepAlive();
        return new ThreadPoolExecutor(core, max, keepAlive, SECONDS, new LinkedBlockingQueue<>(), new ThreadFactory() {

            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(final Runnable r) {
                final Thread thread = new Thread(r);
                thread.setDaemon(false);
                thread.setPriority(Thread.NORM_PRIORITY);
                thread.setName("talend-component-uispec-server-" + counter.incrementAndGet());
                return thread;
            }
        });
    }

    public void releasePool(@Disposes @UiSpecProxy final ExecutorService pool) {
        pool.shutdownNow();
    }

    @Produces
    @UiSpecProxy
    @ApplicationScoped
    public javax.ws.rs.client.Client client(@UiSpecProxy final ExecutorService pool,
            final ProxyConfiguration configuration) {
        final javax.ws.rs.client.Client client = ClientBuilder
                .newBuilder()
                .connectTimeout(configuration.getConnectTimeout(), MILLISECONDS)
                .readTimeout(configuration.getReadTimeout(), MILLISECONDS)
                .executorService(pool)
                .build();
        ofNullable(configuration.getClientProviders()).ifPresent(list -> list.forEach(client::register));
        return client;
    }

    @Produces
    @UiSpecProxy
    @ApplicationScoped
    public WebTarget webTarget(@UiSpecProxy final javax.ws.rs.client.Client client,
            final ProxyConfiguration configuration) {
        return client.target(configuration.getTargetServerBase());
    }

    public void disposeClient(@Disposes final javax.ws.rs.client.Client client) {
        client.close();
    }

    @Data
    @AllArgsConstructor
    public static class Values {

        private Collection<Item> items;

        @Data
        @AllArgsConstructor
        public static class Item {

            private String id;

            private String label;
        }
    }
}
