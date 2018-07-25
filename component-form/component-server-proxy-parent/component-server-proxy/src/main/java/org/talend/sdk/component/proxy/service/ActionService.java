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
package org.talend.sdk.component.proxy.service;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.cache.annotation.CacheResult;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;

import org.eclipse.microprofile.config.Config;
import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.proxy.jcache.CacheResolverManager;
import org.talend.sdk.component.proxy.jcache.ProxyCacheKeyGenerator;
import org.talend.sdk.component.proxy.model.Node;
import org.talend.sdk.component.proxy.model.UiNode;
import org.talend.sdk.component.proxy.service.client.ComponentClient;
import org.talend.sdk.component.proxy.service.client.ConfigurationClient;
import org.talend.sdk.component.proxy.service.client.UiSpecContext;
import org.talend.sdk.component.proxy.service.lang.Substitutor;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.server.front.model.ComponentIndices;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ActionService {

    private final ConfigTypeNode datastoreNode = new ConfigTypeNode("datastore", 0, null, "datastore", "datastore",
            "datastore", emptySet(), new ArrayList<>(), new ArrayList<>());

    private final ConfigTypeNode noFamily = new ConfigTypeNode();

    @Inject
    private ErrorProcessor errorProcessor;

    @Inject
    @UiSpecProxy
    private UiSpecService<UiSpecContext> uiSpecService;

    @Inject
    private ComponentClient componentClient;

    @Inject
    private ConfigurationClient configurationClient;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    @UiSpecProxy
    private Jsonb jsonb;

    @Inject
    @UiSpecProxy
    private Client<UiSpecContext> client;

    @Inject
    private ModelEnricherService modelEnricherService;

    @Inject // to have cache activated and not handle it manually
    private ActionService self;

    @Inject
    @UiSpecProxy
    private javax.ws.rs.client.Client http;

    @Inject
    @UiSpecProxy
    private Config config;

    @Inject
    private Substitutor substitutor;

    private final GenericType<List<Map<String, Object>>> listType = new GenericType<List<Map<String, Object>>>() {
    };

    public CompletionStage<Map<String, Object>> createStage(final String family, final String type, final String action,
            final UiSpecContext context, final Map<String, Object> params) {
        // family is ignored since we virtually add it for all families (=local exec)
        if (isBuiltin(action)) {
            return findBuiltInAction(action, context.getLanguage(), context.getPlaceholderProvider(), params);
        }
        if ("dynamic_values".equals(type)) {
            return self.findProposable(family, type, action, context.getLanguage(), context.getPlaceholderProvider());
        }
        return client.action(family, type, action, context.getLanguage(), params, context);
    }

    @CacheResult(cacheName = "org.talend.sdk.component.proxy.actions.proposables",
            cacheResolverFactory = CacheResolverManager.class, cacheKeyGenerator = ProxyCacheKeyGenerator.class)
    public CompletionStage<Map<String, Object>> findProposable(final String family, final String type,
            final String action, final String lang, final Function<String, String> placeholders) {
        // we recreate the context and don't pass it as a param to ensure the cache key is right
        return client.action(family, type, action, lang, emptyMap(), new UiSpecContext(lang, placeholders));
    }

    public boolean isBuiltin(final String action) {
        return action != null && action.startsWith("builtin::");
    }

    public CompletionStage<Map<String, Object>> findBuiltInAction(final String action, final String lang,
            final Function<String, String> placeholderProvider, final Map<String, Object> params) {
        switch (action) {
        case "builtin::roots":
            return findRoots(lang, placeholderProvider);
        case "builtin::root::reloadFromId":
            return ofNullable(params.get("id"))
                    .map(id -> createNewFormFromId(String.valueOf(id), lang, placeholderProvider))
                    .orElseGet(() -> CompletableFuture.completedFuture(emptyMap()));
        default:
            if (action.startsWith("builtin::http::dynamic_values(")) {
                return http(placeholderProvider, Stream
                        .of(action.substring("builtin::http::dynamic_values(".length(), action.length() - 1).split(","))
                        .map(String::trim)
                        .filter(it -> !it.isEmpty())
                        .map(it -> it.split("="))
                        .collect(toMap(it -> it[0], it -> it[1])));
            }
            throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    // todo: cache most of that computation to do it only once, not critical for now (must use placeholders in the key)
    private CompletionStage<Map<String, Object>> http(final Function<String, String> placeholderProvider,
            final Map<String, Object> params) {
        final String url = substitutor
                .compile(requireNonNull(String.class.cast(params.get("url")), "No url specificed for a http trigger"))
                .apply(placeholderProvider);
        final List<String> headers = Stream
                .of(String.class.cast(params.getOrDefault("headers", "")).split(","))
                .map(String::trim)
                .filter(it -> !it.isEmpty())
                .collect(toList());
        Invocation.Builder request = http.target(config.getOptionalValue(url, String.class).orElse(url)).request(
                String.class.cast(params.getOrDefault("accept", APPLICATION_JSON)));
        for (final String header : headers) {
            final String headerValue = placeholderProvider.apply(header);
            if (headerValue == null) {
                continue;
            }
            request = request.header(header, headerValue);
        }

        final CompletionStageRxInvoker rx = request.rx();

        final CompletionStage<List<Map<String, Object>>> list;
        if (!Boolean.parseBoolean(String.valueOf(params.getOrDefault("object", "false")))) {
            list = rx.get(listType);
        } else {
            list = rx.get(Object.class).thenApply(
                    object -> List.class.cast(Map.class.cast(object).get(params.getOrDefault("objectKey", "items"))));
        }

        final String idName = String.class.cast(params.getOrDefault("id", "id"));
        final String labelName = String.class.cast(params.getOrDefault("name", "name"));
        return list
                .thenApply(it -> ofNullable(it)
                        .orElseGet(Collections::emptyList)
                        .stream()
                        .filter(map -> map.containsKey(idName) || map.containsKey(labelName))
                        .map(map -> new Values.Item(String.class.cast(map.getOrDefault(idName, map.get(labelName))),
                                String.class.cast(map.getOrDefault(labelName, map.get(idName)))))
                        .collect(toList()))
                .thenApply(Values::new)
                .thenApply(this::toJsonMap);
    }

    private CompletableFuture<Map<String, Object>> createNewFormFromId(final String id, final String lang,
            final Function<String, String> placeholderProvider) {
        return findUiSpec(id, lang, placeholderProvider)
                .thenApply(this::toNewFormResponse)
                .thenApply(this::toJsonMap)
                .toCompletableFuture();
    }

    private <T> Map<String, Object> toJsonMap(final T model) {
        return (Map<String, Object>) jsonb.fromJson(jsonb.toJson(model), Map.class);
    }

    private NewForm toNewFormResponse(final UiNode uiNode) {
        return new NewForm(uiNode.getUi().getJsonSchema(), uiNode.getUi().getUiSchema(), uiNode.getMetadata());
    }

    private CompletionStage<UiNode> findUiSpec(final String id, final String lang,
            final Function<String, String> placeholderProvider) {
        if (id.isEmpty()) {
            return CompletableFuture
                    .completedFuture(datastoreNode)
                    .thenApply(node -> modelEnricherService.enrich(node, lang))
                    .thenCompose(detail -> toUiNode(lang, placeholderProvider, detail, null, noFamily));
        }
        final CompletionStage<ComponentIndices> allComponents =
                componentClient.getAllComponents(lang, placeholderProvider);
        return getNode(id, lang, placeholderProvider)
                .thenApply(node -> modelEnricherService.enrich(node, lang))
                .thenCompose(detail -> configurationClient.getAllConfigurations(lang, placeholderProvider).thenCompose(
                        configs -> allComponents.thenCompose(components -> {
                            final ConfigTypeNode family = configurationService.getFamilyOf(id, configs);
                            return toUiNode(lang, placeholderProvider, detail, components, family);
                        })));
    }

    private CompletionStage<UiNode> toUiNode(final String lang, final Function<String, String> placeholderProvider,
            final ConfigTypeNode detail, final ComponentIndices iconComponents, final ConfigTypeNode family) {
        return toUiSpec(detail, family, new UiSpecContext(lang, placeholderProvider), lang)
                .thenApply(ui -> new UiNode(ui,
                        new Node(detail.getId(), detail.getDisplayName(), family.getId(), family.getDisplayName(),
                                ofNullable(family.getId())
                                        .map(id -> configurationService.findIcon(id, iconComponents))
                                        .orElse(null),
                                detail.getEdges(), detail.getVersion(), detail.getName())));
    }

    private CompletionStage<ConfigTypeNode> getNode(final String id, final String lang,
            final Function<String, String> placeholderProvider) {
        return id.isEmpty()
                ? CompletableFuture.completedFuture(new ConfigTypeNode("datastore", 0, null, "datastore", "datastore",
                        "datastore", emptySet(), new ArrayList<>(), new ArrayList<>()))
                : configurationClient.getDetails(lang, id, placeholderProvider);
    }

    private CompletionStage<Ui> toUiSpec(final ConfigTypeNode detail, final ConfigTypeNode family,
            final UiSpecContext context, final String language) {
        return uiSpecService.convert(family.getName(), language, detail, context).thenApply(ui -> {
            // drop properties for this particular callback since they are ignored on client side
            ui.setProperties(null);
            return ui;
        });
    }

    private CompletableFuture<Map<String, Object>> findRoots(final String lang,
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
                .thenApply(this::toJsonMap)
                .toCompletableFuture();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewForm {

        private JsonSchema jsonSchema;

        private Collection<UiSchema> uiSchema;

        private Node metadata;
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
