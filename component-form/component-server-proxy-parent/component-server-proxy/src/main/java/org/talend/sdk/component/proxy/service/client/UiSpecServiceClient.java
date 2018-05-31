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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.enterprise.inject.Vetoed;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.internal.jaxrs.JAXRSClient;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.proxy.model.Node;
import org.talend.sdk.component.proxy.model.UiNode;
import org.talend.sdk.component.proxy.service.ConfigurationService;
import org.talend.sdk.component.proxy.service.ModelEnricherService;
import org.talend.sdk.component.proxy.service.UiSpecServiceProvider;
import org.talend.sdk.component.server.front.model.ComponentIndices;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Vetoed
public class UiSpecServiceClient extends JAXRSClient {

    private final ComponentClient componentClient;

    private final ConfigurationClient configurationClient;

    private final ConfigurationService configurationService;

    private final Jsonb jsonb;

    private final String lang;

    private final Function<String, String> placeholderProvider;

    private final UiSpecServiceProvider uiSpecServiceProvider;

    private final ModelEnricherService modelEnricherService;

    public UiSpecServiceClient(final javax.ws.rs.client.Client client, final String base,
            final ConfigurationClient configurationClient, final ConfigurationService configurationService,
            final Jsonb jsonb, final String lang, final Function<String, String> placeholderProvider,
            final UiSpecServiceProvider uiSpecServiceProvider, final ModelEnricherService modelEnricherService,
            final ComponentClient componentClient) {
        super(client, base, false);
        this.componentClient = componentClient;
        this.configurationClient = configurationClient;
        this.configurationService = configurationService;
        this.jsonb = jsonb;
        this.lang = lang;
        this.placeholderProvider = placeholderProvider;
        this.uiSpecServiceProvider = uiSpecServiceProvider;
        this.modelEnricherService = modelEnricherService;
    }

    @Override
    public CompletableFuture<Map<String, Object>> action(final String family, final String type, final String action,
            final Map<String, Object> params) {
        if (action.startsWith("builtin::")) { // family is ignored since we virtually add it for all families (=local
                                              // exec)
            switch (action) {
            case "builtin::roots":
                if ("dynamic_values".equals(type)) {
                    return findRoots(lang, placeholderProvider);
                }
                break;
            case "builtin::root::reloadFromId":
                if ("jsonpatch".equals(type)) {
                    return ofNullable(params.get("id")).map(id -> buildJsonPatchFromId(String.valueOf(id))).orElseGet(
                            () -> CompletableFuture.completedFuture(emptyMap()));
                }
                break;
            default:
            }
        }
        return super.action(family, type, action, params);
    }

    private CompletableFuture<Map<String, Object>> buildJsonPatchFromId(final String id) {
        return findUiSpec(id)
                .thenApply(this::wrapAsJsonPatchReplaceSingleOperation)
                .thenApply(this::toJsonMap)
                .toCompletableFuture();
    }

    private Map<String, Object> toJsonMap(final JsonPatchResponse uiJsonPatch) {
        return (Map<String, Object>) jsonb.fromJson(jsonb.toJson(uiJsonPatch), Map.class);
    }

    private JsonPatchResponse wrapAsJsonPatchReplaceSingleOperation(final UiNode uiNode) {
        return new JsonPatchResponse(
                singleton(
                        new JsonPatchOp("replace", "/", jsonb.fromJson(jsonb.toJson(uiNode.getUi()), JsonValue.class))),
                uiNode.getMetadata());
    }

    private CompletionStage<UiNode> findUiSpec(final String id) {
        final CompletionStage<ComponentIndices> allComponents =
                componentClient.getAllComponents(lang, placeholderProvider);
        return configurationClient
                .getDetails(lang, id, placeholderProvider)
                .thenApply(ConfigTypeNodes::getNodes)
                .thenApply(nodes -> {
                    if (nodes.isEmpty()) {
                        log.error("No detail for configuration {}", id);
                        throw new IllegalArgumentException("No detail for " + id);
                    }
                    return nodes.values().iterator().next();
                })
                .thenApply(node -> modelEnricherService.enrich(node, lang))
                .thenCompose(detail -> configurationClient.getAllConfigurations(lang, placeholderProvider).thenCompose(
                        configs -> allComponents.thenCompose(components -> {
                            final ConfigTypeNode family = configurationService.getFamilyOf(id, configs);
                            return toUiSpec(detail, configs, family).thenApply(ui -> new UiNode(ui,
                                    new Node(detail.getId(), Node.Type.CONFIGURATION, detail.getDisplayName(),
                                            family.getId(), family.getDisplayName(),
                                            configurationService.findIcon(family.getId(), components),
                                            detail.getEdges(), detail.getVersion(), detail.getName(),
                                            null /* not used for config, only components */)));
                        })));
    }

    private CompletionStage<Ui> toUiSpec(final ConfigTypeNode detail, final ConfigTypeNodes configs,
            final ConfigTypeNode family) {
        try (final UiSpecService uiSpecService = uiSpecServiceProvider.newInstance(lang, placeholderProvider)) {
            return uiSpecService.convert(family.getName(), detail).thenApply(ui -> {
                // drop properties for this particular callback since they are ignored on client side
                ui.setProperties(null);
                return ui;
            });
        } catch (final Exception e) {
            if (RuntimeException.class.isInstance(e)) {
                throw RuntimeException.class.cast(e);
            }
            throw new IllegalStateException(e);
        }
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
    public static class JsonPatchOp {

        private String op;

        private String path;

        private JsonValue value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JsonPatchResponse {

        private Collection<JsonPatchOp> jsonPatch;

        private Node metadata;
    }
}
