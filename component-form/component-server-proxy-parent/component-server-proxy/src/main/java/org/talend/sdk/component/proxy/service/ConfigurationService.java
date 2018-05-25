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
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import org.talend.sdk.component.proxy.model.Config;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;

/**
 * This service encapsulate all the logic of the configuration transformation
 */
@ApplicationScoped
public class ConfigurationService {

    public List<Config> getRootConfiguration(final ConfigTypeNodes configs) {
        final List<ConfigTypeNode> families = ofNullable(configs)
                .map(c -> c.getNodes().values().stream())
                .orElseGet(Stream::empty)
                .filter(node -> node.getParentId() == null || node.getParentId().isEmpty())
                .collect(toList());
        return configs
                .getNodes()
                .values()
                .stream()
                .filter(node -> node.getParentId() == null || node.getParentId().isEmpty())
                .flatMap(parent -> parent.getEdges().stream().map(edgeId -> configs.getNodes().get(edgeId)))
                .map(root -> {
                    final ConfigTypeNode family =
                            families.stream().filter(f -> f.getId().equals(root.getParentId())).findFirst().orElseThrow(
                                    () -> new IllegalStateException("Can't find family of component " + root));
                    return new Config(root.getId(), family.getId(), root.getDisplayName(), family.getDisplayName(),
                            root.getEdges(), null); // todo : get family icon key
                })
                .collect(toList());
    }
}
