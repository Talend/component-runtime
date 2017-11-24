/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.server.front;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.design.extension.RepositoryModel;
import org.talend.sdk.component.design.extension.repository.Config;
import org.talend.sdk.component.runtime.internationalization.FamilyBundle;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.service.LocaleMapper;
import org.talend.sdk.component.server.service.PropertiesService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("configurationtype")
@ApplicationScoped
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ConfigurationTypeResource {

    @Inject
    private ComponentManager manager;

    @Inject
    private PropertiesService propertiesService;

    @Inject
    private LocaleMapper localeMapper;

    @GET
    @Path("index")
    @Documentation("Returns all available configuration type - storable models.")
    public ConfigTypeNodes getRepositoryModel(@QueryParam("language") @DefaultValue("en") final String language) {
        final Locale locale = localeMapper.mapLocale(language);
        return manager.find(Stream::of).filter(c -> c.get(RepositoryModel.class) != null)
                .map(c -> c.get(RepositoryModel.class).getFamilies().stream().flatMap(family -> {
                    final ConfigTypeNode node = new ConfigTypeNode();
                    node.setId(family.getId());
                    node.setName(family.getMeta().getName());

                    final FamilyBundle resourcesBundle = family.getMeta().findBundle(c.getLoader(), locale);
                    node.setDisplayName(resourcesBundle.displayName().orElse(family.getMeta().getName()));
                    if (family.getConfigs() == null) {
                        return Stream.of(node);
                    }
                    node.setEdges(family.getConfigs().stream().map(Config::getId).collect(toSet()));
                    return Stream.concat(Stream.of(node),
                            createNode(family.getId(), family.getConfigs().stream(), resourcesBundle, c.getLoader(), locale));
                })).collect(() -> {
                    final ConfigTypeNodes nodes = new ConfigTypeNodes();
                    nodes.setNodes(new HashMap<>());
                    return nodes;
                }, (root, children) -> root.getNodes().putAll(children.collect(toMap(ConfigTypeNode::getId, identity()))),
                        (first, second) -> first.getNodes().putAll(second.getNodes()));
    }

    private Stream<ConfigTypeNode> createNode(final String parentId, final Stream<Config> configs,
            final FamilyBundle resourcesBundle, final ClassLoader loader, final Locale locale) {
        if (configs == null) {
            return Stream.empty();
        }
        return configs.flatMap(c -> {
            final ConfigTypeNode node = new ConfigTypeNode();
            node.setId(c.getId());
            node.setConfigurationType(c.getMeta().getMetadata().get("tcomp::configurationtype::type"));
            node.setName(c.getMeta().getMetadata().getOrDefault("tcomp::configurationtype::name", c.getMeta().getName()));
            node.setParentId(parentId);
            node.setDisplayName(resourcesBundle.configurationDisplayName(c.getKey().getConfigType(), c.getKey().getConfigName())
                    .orElse(c.getKey().getConfigName()));
            if (c.getProperties() != null) {
                node.setProperties(propertiesService.buildProperties(c.getProperties(), loader, locale).collect(toList()));
            }

            if (c.getChildConfigs() == null) {
                return Stream.of(node);
            }

            node.setEdges(c.getChildConfigs().stream().map(Config::getId).collect(toSet()));
            return Stream.concat(Stream.of(node),
                    createNode(c.getId(), c.getChildConfigs().stream(), resourcesBundle, loader, locale));
        });
    }
}
