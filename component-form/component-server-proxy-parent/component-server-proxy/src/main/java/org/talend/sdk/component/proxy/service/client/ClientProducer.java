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

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.json.bind.Jsonb;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.internal.converter.CustomPropertyConverter;
import org.talend.sdk.component.form.internal.jaxrs.JAXRSClient;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.proxy.service.ActionService;
import org.talend.sdk.component.proxy.service.ConfigurationService;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ClientProducer {

    @Produces
    @UiSpecProxy
    public UiSpecService<UiSpecContext> uiSpecService(@UiSpecProxy final Client client, @UiSpecProxy final Jsonb jsonb,
            final ConfigurationService configurationService,
            final Instance<CustomPropertyConverter> customPropertyConverters) {

        final UiSpecService<UiSpecContext> service = new UiSpecService<UiSpecContext>(client, jsonb) {

            @Override
            public CompletionStage<Ui> convert(final String family, final String lang, final ConfigTypeNode node,
                    final UiSpecContext context) {
                return configurationService
                        .filterNestedConfigurations(node, context)
                        .thenApply(configurationService::enforceFormIdInTriggersIfPresent)
                        .thenCompose(config -> super.convert(family, lang, config, context));
            }

            @Override
            public CompletionStage<Ui> convert(final ComponentDetail detail, final String lang,
                    final UiSpecContext context) {
                // todo if used:
                // return configurationService.filterNestedConfigurations(lang, context.getPlaceholderProvider(),
                // detail)
                // .thenCompose(config -> super.convert(config, lang, context));
                return super.convert(detail, lang, context);
            }
        };
        customPropertyConverters
                .stream()
                .sorted(comparing(
                        it -> ofNullable(it.getClass().getAnnotation(Priority.class)).map(Priority::value).orElse(0)))
                .forEach(service::withConverter);
        return service;
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
                    final String action, final String lang, final Map<String, Object> params,
                    final UiSpecContext context) {
                if (actionService.isBuiltin(action)) {
                    return actionService.findBuiltInAction(action, context, params).toCompletableFuture();
                }
                return super.action(family, type, action, lang, params, context);
            }
        };
    }

    public void destroyUiSpecClient(@Disposes @UiSpecProxy final Client<UiSpecContext> client) {
        client.close();
    }

    @Produces
    @UiSpecProxy
    @ApplicationScoped
    public ExecutorService clientExecutor(final ProxyConfiguration configuration) {
        final AtomicInteger counter = new AtomicInteger(0);
        return Executors.newFixedThreadPool(configuration.getClientExecutorThreads(), r -> {
            final Thread thread = new Thread(r);
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.setName("component-server-client-" + counter.incrementAndGet());
            return thread;
        });
    }

    @Produces
    @UiSpecProxy
    @ApplicationScoped
    public javax.ws.rs.client.Client client(final ProxyConfiguration configuration,
            @UiSpecProxy final ExecutorService executor) {
        final javax.ws.rs.client.Client client = ClientBuilder
                .newBuilder()
                .executorService(executor)
                .property("executorService", executor) // rx()
                .connectTimeout(configuration.getConnectTimeout(), MILLISECONDS)
                .readTimeout(configuration.getReadTimeout(), MILLISECONDS)
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
}
