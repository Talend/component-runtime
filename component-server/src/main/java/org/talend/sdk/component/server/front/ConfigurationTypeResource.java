/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.design.extension.RepositoryModel;
import org.talend.sdk.component.design.extension.repository.Config;
import org.talend.sdk.component.runtime.internationalization.FamilyBundle;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.server.dao.ConfigurationDao;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.service.ActionsService;
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
    private ActionsService actionsService;

    @Inject
    private LocaleMapper localeMapper;

    @Inject
    private ConfigurationDao configurations;

    /**
     * Returns all available configuration type - storable models.
     *
     * @param language the language for display names.
     * @return the list of available and storable configurations (datastore, dataset, ...).
     */
    @GET
    @Path("index")
    public ConfigTypeNodes getRepositoryModel(@QueryParam("language") @DefaultValue("en") final String language) {
        final Locale locale = localeMapper.mapLocale(language);
        return manager
                .find(Stream::of)
                .filter(c -> c.get(RepositoryModel.class) != null)
                .map(c -> c
                        .get(RepositoryModel.class)
                        .getFamilies()
                        .stream()
                        .filter(f -> !f.getConfigs().isEmpty())
                        .flatMap(family -> {
                            final ConfigTypeNode node = new ConfigTypeNode();
                            node.setId(family.getId());
                            node.setName(family.getMeta().getName());

                            final FamilyBundle resourcesBundle = family.getMeta().findBundle(c.getLoader(), locale);
                            node.setDisplayName(resourcesBundle.displayName().orElse(family.getMeta().getName()));
                            if (family.getConfigs() == null) {
                                return Stream.of(node);
                            }
                            node.setEdges(family.getConfigs().stream().map(Config::getId).collect(toSet()));
                            return Stream.concat(Stream.of(node), createNode(family.getId(), family.getMeta().getName(),
                                    family.getConfigs().stream(), resourcesBundle, c, locale));
                        }))
                .collect(() -> {
                    final ConfigTypeNodes nodes = new ConfigTypeNodes();
                    nodes.setNodes(new HashMap<>());
                    return nodes;
                }, (root,
                        children) -> root.getNodes().putAll(children.collect(toMap(ConfigTypeNode::getId, identity()))),
                        (first, second) -> first.getNodes().putAll(second.getNodes()));
    }

    /**
     * Allows to migrate a configuration without calling any component execution.
     *
     * @param id the configuration identifier.
     * @param version the sent configuration version.
     * @param config the configuration in key/value form.
     * @return the new configuration or the same if no migration was needed.
     */
    @POST
    @Path("migrate/{id}/{configurationVersion}")
    public Map<String, String> migrate(@PathParam("id") final String id,
            @PathParam("configurationVersion") final int version, final Map<String, String> config) {
        return ofNullable(configurations.findById(id))
                .orElseThrow(() -> new WebApplicationException(Response
                        .status(Response.Status.NOT_FOUND)
                        .entity(new ErrorPayload(ErrorDictionary.CONFIGURATION_MISSING,
                                "Didn't find configuration " + id))
                        .build()))
                .getMigrationHandler()
                .migrate(version, config);
    }

    private Stream<ConfigTypeNode> createNode(final String parentId, final String family, final Stream<Config> configs,
            final FamilyBundle resourcesBundle, final Container container, final Locale locale) {
        final ClassLoader loader = container.getLoader();
        if (configs == null) {
            return Stream.empty();
        }
        return configs.flatMap(c -> {
            final ConfigTypeNode node = new ConfigTypeNode();
            node.setId(c.getId());
            node.setVersion(c.getVersion());
            node.setConfigurationType(c.getKey().getConfigType());
            node.setName(c.getKey().getConfigName());
            node.setParentId(parentId);
            node.setDisplayName(resourcesBundle
                    .configurationDisplayName(c.getKey().getConfigType(), c.getKey().getConfigName())
                    .orElse(c.getKey().getConfigName()));
            node.setProperties(
                    propertiesService.buildProperties(singleton(c.getMeta()), loader, locale, null).collect(toList()));
            node.setActions(actionsService.findActions(family, container, locale, c));

            if (c.getChildConfigs() == null) {
                return Stream.of(node);
            }

            node.setEdges(c.getChildConfigs().stream().map(Config::getId).collect(toSet()));
            return Stream.concat(Stream.of(node),
                    createNode(c.getId(), family, c.getChildConfigs().stream(), resourcesBundle, container, locale));
        });
    }
}
