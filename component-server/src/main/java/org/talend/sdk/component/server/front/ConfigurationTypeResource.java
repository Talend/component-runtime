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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;
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
import org.talend.sdk.component.classloader.ConfigurableClassLoader;
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
        final ConfigTypeNodes root = new ConfigTypeNodes();
        manager.find(Stream::of).forEach(c -> {
            RepositoryModel rp = c.get(RepositoryModel.class);
            if (rp != null) {
                rp.getFamilies().forEach(family -> {
                    ConfigTypeNode node = new ConfigTypeNode();
                    node.setId(family.getId());
                    node.setName(family.getMeta().getName());
                    FamilyBundle resourcesBundle = family.getMeta().findBundle(c.getLoader(), locale);
                    node.setDisplayName(resourcesBundle.displayName().orElse(family.getMeta().getName()));
                    if (family.getConfigs() != null) {
                        node.setEdges(family.getConfigs().stream().map(Config::getId).collect(toSet()));
                        root.getNodes().put(node.getId(), node);
                        createNode(family.getId(), family.getConfigs(), root, resourcesBundle, c.getLoader(), locale);
                    }
                });
            }
        });

        return root;
    }

    private void createNode(String parentId, List<Config> configs, ConfigTypeNodes root, FamilyBundle resourcesBundle,
            ConfigurableClassLoader loader, Locale locale) {
        if (configs == null) {
            return;
        }
        configs.stream().forEach(c -> {
            ConfigTypeNode node = new ConfigTypeNode();
            node.setId(c.getId());
            node.setName(c.getMeta().getName());
            node.setParentId(parentId);
            node.setDisplayName(resourcesBundle.configurationDisplayName(c.getKey().getConfigType(), c.getKey().getConfigName())
                    .orElse(c.getKey().getConfigName()));
            if (c.getProperties() != null) {
                node.setProperties(propertiesService.buildProperties(c.getProperties(), loader, locale).collect(toList()));
            }
            if (c.getChildConfigs() != null) {
                node.setEdges(c.getChildConfigs().stream().map(edge -> c.getId()).collect(toSet()));
                root.getNodes().put(node.getId(), node);
                createNode(c.getId(), c.getChildConfigs(), root, resourcesBundle, loader, locale);
            }
        });
    }
}
