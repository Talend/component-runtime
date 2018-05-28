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

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.sdk.component.proxy.client.ConfigurationClient;
import org.talend.sdk.component.proxy.model.ConfigType;
import org.talend.sdk.component.proxy.model.Configurations;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;

/**
 * This service encapsulate all the logic of the configuration transformation
 */
@ApplicationScoped
public class ConfigurationService {

    @Inject
    private ConfigurationClient client;

    public Configurations getRootConfiguration(final ConfigTypeNodes configs) {
        final Map<String, ConfigTypeNode> families = ofNullable(configs)
                .map(c -> c.getNodes().values().stream())
                .orElseGet(Stream::empty)
                .filter(node -> node.getParentId() == null)
                .collect(toMap(ConfigTypeNode::getId, identity()));
        return new Configurations(configs
                .getNodes()
                .values()
                .stream()
                .flatMap(node -> node.getEdges().stream().map(edgeId -> configs.getNodes().get(edgeId)).filter(
                        config -> config.getParentId() != null && families.containsKey(config.getParentId())))
                .map(root -> {
                    final ConfigTypeNode family = families.get(root.getParentId());
                    return new ConfigType(root.getId(), family.getId(), root.getDisplayName(), family.getDisplayName(),
                            null, root.getEdges());
                })
                .collect(toMap(ConfigType::getId, identity())), Collections.emptyMap());
    }

    public ConfigTypeNode getFamilyOf(final String id, final ConfigTypeNodes nodes) {
        ConfigTypeNode family = nodes.getNodes().get(id);
        while (family != null && family.getParentId() != null) {
            family = nodes.getNodes().get(family.getParentId());
        }
        return family; // require ot null/web app ex
    }
}
