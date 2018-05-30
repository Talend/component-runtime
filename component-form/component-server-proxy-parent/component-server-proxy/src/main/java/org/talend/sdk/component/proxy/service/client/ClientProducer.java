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
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.jaxrs.JAXRSClient;
import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.proxy.service.ConfigurationService;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;

import lombok.AllArgsConstructor;
import lombok.Data;

@ApplicationScoped
public class ClientProducer {

    @Inject
    private ProxyConfiguration configuration;

    @Produces
    @UiSpecProxy
    @ApplicationScoped
    public javax.ws.rs.client.Client client() {
        final javax.ws.rs.client.Client client = ClientBuilder
                .newBuilder()
                .executorService(Executors.newCachedThreadPool()) // todo make the pool configurable
                .newClient();
        ofNullable(configuration.getClientProviders()).ifPresent(list -> list.forEach(client::register));
        return client;
    }

    @Produces
    @UiSpecProxy
    @ApplicationScoped
    public WebTarget webTarget(final javax.ws.rs.client.Client client) {
        return client.target(configuration.getTargetServerBase());
    }

    @Produces
    @UiSpecProxy
    @ApplicationScoped
    public Client actionClient(final javax.ws.rs.client.Client client, final ConfigurationService configurationService,
            final ConfigurationClient configurationClient, final Jsonb jsonb) {
        return new MergedClient(client, configuration.getTargetServerBase(), true, configurationClient,
                configurationService, jsonb);
    }

    public void disposeClient(@Disposes final javax.ws.rs.client.Client client) {
        client.close();
    }

    private static class MergedClient extends JAXRSClient {

        private final ConfigurationClient configurationClient;

        private final ConfigurationService configurationService;

        private final Jsonb jsonb;

        private MergedClient(final javax.ws.rs.client.Client client, final String base, final boolean closeClient,
                final ConfigurationClient configurationClient, final ConfigurationService configurationService,
                final Jsonb jsonb) {
            super(client, base, closeClient);
            this.configurationClient = configurationClient;
            this.configurationService = configurationService;
            this.jsonb = jsonb;
        }

        @Override
        public CompletableFuture<Map<String, Object>> action(final String family, final String type,
                final String action, final Map<String, Object> params) {
            if ("builtin::roots".equals(action) && "dynamic_values".equals(type) /* && whatever family */) {
                return findRoots(ofNullable(params.get("lang")).map(String::valueOf).orElse("en"),
                        Function.class.cast(params.get("placeholderProvider"))).toCompletableFuture();
            }
            return super.action(family, type, action, params);
        }

        private CompletionStage<Map<String, Object>> findRoots(final String lang,
                final Function<String, String> placeholderProvider) {
            return configurationClient
                    .getAllConfigurations(lang, placeholderProvider)
                    .thenApply(configs -> configurationService.getRootConfiguration(configs, ignored -> null))
                    .thenApply(configs -> new Values(configs
                            .getNodes()
                            .values()
                            .stream()
                            .map(it -> new Values.Item(it.getId(), it.getLabel()))
                            .sorted(comparing(Values.Item::getLabel))
                            .collect(toList())))
                    .thenApply(values -> ((Map<String, Object>) jsonb.fromJson(jsonb.toJson(values), Map.class)));
        }
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
