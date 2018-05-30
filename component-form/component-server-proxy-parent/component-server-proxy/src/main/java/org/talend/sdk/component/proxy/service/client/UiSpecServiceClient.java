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
import static java.util.stream.Collectors.toList;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.enterprise.inject.Vetoed;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.form.internal.jaxrs.JAXRSClient;
import org.talend.sdk.component.proxy.service.ConfigurationService;

@Vetoed
public class UiSpecServiceClient extends JAXRSClient {

    private final ConfigurationClient configurationClient;

    private final ConfigurationService configurationService;

    private final Jsonb jsonb;

    private final String lang;

    private final Function<String, String> placeholderProvider;

    public UiSpecServiceClient(final javax.ws.rs.client.Client client, final String base,
            final ConfigurationClient configurationClient, final ConfigurationService configurationService,
            final Jsonb jsonb, final String lang, final Function<String, String> placeholderProvider) {
        super(client, base, false);
        this.configurationClient = configurationClient;
        this.configurationService = configurationService;
        this.jsonb = jsonb;
        this.lang = lang;
        this.placeholderProvider = placeholderProvider;
    }

    @Override
    public CompletableFuture<Map<String, Object>> action(final String family, final String type, final String action,
            final Map<String, Object> params) {
        if ("builtin::roots".equals(action) && "dynamic_values".equals(type) /* && whatever family */) {
            return findRoots(lang, placeholderProvider).toCompletableFuture();
        }
        return super.action(family, type, action, params);
    }

    private CompletionStage<Map<String, Object>> findRoots(final String lang,
            final Function<String, String> placeholderProvider) {
        return configurationClient
                .getAllConfigurations(lang, placeholderProvider)
                .thenApply(configs -> configurationService.getRootConfiguration(configs, ignored -> null))
                .thenApply(configs -> new ClientProducer.Values(configs
                        .getNodes()
                        .values()
                        .stream()
                        .map(it -> new ClientProducer.Values.Item(it.getId(), it.getLabel()))
                        .sorted(comparing(ClientProducer.Values.Item::getLabel))
                        .collect(toList())))
                .thenApply(values -> ((Map<String, Object>) jsonb.fromJson(jsonb.toJson(values), Map.class)));
    }
}
