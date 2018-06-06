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
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.cache.annotation.CacheResult;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.proxy.jcache.CacheResolverManager;
import org.talend.sdk.component.proxy.jcache.ProxyCacheKeyGenerator;
import org.talend.sdk.component.proxy.model.Node;
import org.talend.sdk.component.proxy.model.UiNode;
import org.talend.sdk.component.proxy.service.client.ClientProducer;
import org.talend.sdk.component.proxy.service.client.ComponentClient;
import org.talend.sdk.component.proxy.service.client.ConfigurationClient;
import org.talend.sdk.component.proxy.service.client.UiSpecContext;
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

    public CompletionStage<Map<String, Object>> createStage(final String family, final String type, final String action,
            final UiSpecContext context, final Map<String, Object> params) {
        // family is ignored since we virtually add it for all families (=local exec)
        if (isBuiltin(action)) {
            return findBuiltInAction(action, context.getLanguage(), context.getPlaceholderProvider(), params);
        }
        if ("dynamic_values".equals(type)) {
            return self.findProposable(family, type, action, context.getLanguage(), context.getPlaceholderProvider());
        }
        return client.action(family, type, action, params, context);
    }

    @CacheResult(cacheName = "org.talend.sdk.component.proxy.actions.proposables",
            cacheResolverFactory = CacheResolverManager.class, cacheKeyGenerator = ProxyCacheKeyGenerator.class)
    public CompletionStage<Map<String, Object>> findProposable(final String family, final String type,
            final String action, final String lang, final Function<String, String> placeholders) {
        // we recreate the context and don't pass it as a param to ensure the cache key is right
        return client.action(family, type, action, emptyMap(), new UiSpecContext(lang, placeholders));
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
            throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    private CompletableFuture<Map<String, Object>> createNewFormFromId(final String id, final String lang,
            final Function<String, String> placeholderProvider) {
        return findUiSpec(id, lang, placeholderProvider)
                .thenApply(this::toNewFormResponse)
                .thenApply(this::toJsonMap)
                .toCompletableFuture();
    }

    private Map<String, Object> toJsonMap(final NewForm newForm) {
        return (Map<String, Object>) jsonb.fromJson(jsonb.toJson(newForm), Map.class);
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
        return toUiSpec(detail, family, new UiSpecContext(lang, placeholderProvider))
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
            final UiSpecContext context) {
        return uiSpecService.convert(family.getName(), detail, context).thenApply(ui -> {
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
                .thenApply(configs -> new ClientProducer.Values(configs
                        .getNodes()
                        .values()
                        .stream()
                        .map(it -> new ClientProducer.Values.Item(it.getId(), it.getLabel()))
                        .sorted(comparing(ClientProducer.Values.Item::getLabel))
                        .collect(toList())))
                .thenApply(values -> ((Map<String, Object>) jsonb.fromJson(jsonb.toJson(values), Map.class)))
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
}
