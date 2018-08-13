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

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.talend.sdk.component.proxy.model.ProxyErrorDictionary.NO_FAMILY_FOR_CONFIGURATION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.proxy.model.Node;
import org.talend.sdk.component.proxy.model.Nodes;
import org.talend.sdk.component.proxy.model.ProxyErrorPayload;
import org.talend.sdk.component.proxy.service.client.ConfigurationClient;
import org.talend.sdk.component.proxy.service.client.UiSpecContext;
import org.talend.sdk.component.server.front.model.ComponentIndices;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

/**
 * This service encapsulate all the logic of the configuration transformation
 */
@ApplicationScoped
public class ConfigurationService {

    @Inject
    private ConfigurationClient client;

    @Inject
    private PropertiesService propertiesService;

    public CompletionStage<ConfigTypeNode> filterNestedConfigurations(final ConfigTypeNode node,
            final UiSpecContext context) {
        return propertiesService
                .filterProperties(node.getProperties(), context)
                .thenApply(props -> new ConfigTypeNode(node.getId(), node.getVersion(), node.getParentId(),
                        node.getConfigurationType(), node.getName(), node.getDisplayName(), node.getEdges(), props,
                        node.getActions()));
    }

    /*
     * for components it looks like:
     * public CompletionStage<ComponentDetail> filterNestedConfigurations(final String lang,
     * final Function<String, String> placholders, final ComponentDetail node) {
     * return propertiesService.filterProperties(lang, placholders, node.getProperties())
     * .thenApply(props -> new ComponentDetail(node.getId(), node.getDisplayName(), node.getIcon(), node.getType(),
     * node.getVersion(), props, node.getActions(), node.getInputFlows(), node.getOutputFlows(),
     * node.getLinks()));
     * }
     */

    public Nodes getRootConfiguration(final ConfigTypeNodes configs, final Function<String, String> iconProvider) {
        final Map<String, ConfigTypeNode> families = ofNullable(configs)
                .map(c -> c.getNodes().values().stream())
                .orElseGet(Stream::empty)
                .filter(node -> node.getParentId() == null)
                .collect(toMap(ConfigTypeNode::getId, identity()));
        return new Nodes(configs
                .getNodes()
                .values()
                .stream()
                .flatMap(node -> node.getEdges().stream().map(edgeId -> configs.getNodes().get(edgeId)).filter(
                        config -> config.getParentId() != null && families.containsKey(config.getParentId())))
                .map(root -> {
                    final ConfigTypeNode family = families.get(root.getParentId());
                    return new Node(root.getId(), root.getDisplayName(), family.getId(), family.getDisplayName(),
                            iconProvider.apply(family.getId()), root.getEdges(), root.getVersion(), root.getName());
                })
                .collect(toMap(Node::getId, identity())));
    }

    public Nodes getRootConfiguration(final ConfigTypeNodes configs, final ComponentIndices componentIndices) {
        return getRootConfiguration(configs, family -> findIcon(family, componentIndices));
    }

    public ConfigTypeNode getFamilyOf(final String id, final ConfigTypeNodes nodes) {
        ConfigTypeNode family = nodes.getNodes().get(id);
        while (family != null && family.getParentId() != null) {
            family = nodes.getNodes().get(family.getParentId());
        }
        if (family == null) {
            throw new WebApplicationException(Response
                    .status(INTERNAL_SERVER_ERROR)
                    .entity(new ProxyErrorPayload(NO_FAMILY_FOR_CONFIGURATION.name(),
                            "No family found for this configuration identified by id:" + id))
                    .header(ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR, false)
                    .build());
        }
        return family; // require ot null/web app ex
    }

    public String findIcon(final String family, final ComponentIndices componentIndices) {
        return componentIndices
                .getComponents()
                .stream()
                .filter(component -> component.getId().getFamilyId().equals(family))
                .findFirst()
                .orElseThrow(() -> new WebApplicationException(Response
                        .status(HTTP_INTERNAL_ERROR)
                        .entity(new ProxyErrorPayload("UNEXPECTED",
                                "No icon found for this configuration family " + family))
                        .header(ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR, false)
                        .build()))
                .getIconFamily()
                .getIcon();
    }

    public CompletionStage<Map<String, String>> replaceReferences(final UiSpecContext context,
            final ConfigTypeNode detail, final Map<String, String> props) {
        return propertiesService.replaceReferences(context, detail.getProperties(), props);
    }

    public ConfigTypeNode enforceFormIdInTriggersIfPresent(final ConfigTypeNode it) {
        final Optional<SimplePropertyDefinition> idPropOpt = findFormId(it);
        if (!idPropOpt.isPresent()) {
            return it;
        }

        // H: we assume the id uses a simple path (no array etc), should be the case generally
        final String idProp = idPropOpt.get().getPath();
        it.getProperties().forEach(prop -> {
            final Map<String, String> metadata = prop.getMetadata();
            final List<String> actions = metadata
                    .keySet()
                    .stream()
                    .filter(k -> k.startsWith("action::") && k.split("::").length == 2)
                    .collect(toList());
            actions.forEach(act -> {
                final String key = act + "::parameters";
                final String originalValue = metadata.get(key);
                final Map<String, String> newMetadata = new HashMap<>(metadata);
                newMetadata.put(key, originalValue == null || originalValue.trim().isEmpty() ? idProp
                        : (originalValue + ',' + idProp));
                prop.setMetadata(newMetadata);
            });
        });
        return it;
    }

    public Optional<SimplePropertyDefinition> findFormId(final ConfigTypeNode node) {
        return node
                .getProperties()
                .stream()
                .filter(p -> "true".equals(p.getMetadata().get("proxyserver::formId")))
                .findFirst();
    }
}
