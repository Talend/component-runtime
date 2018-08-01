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
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.PATH;
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.QUERY;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.BOOLEAN;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.OBJECT;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.STRING;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.design.extension.RepositoryModel;
import org.talend.sdk.component.design.extension.repository.Config;
import org.talend.sdk.component.runtime.internationalization.FamilyBundle;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.server.dao.ConfigurationDao;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.service.ActionsService;
import org.talend.sdk.component.server.service.LocaleMapper;
import org.talend.sdk.component.server.service.PropertiesService;

import lombok.extern.slf4j.Slf4j;

@Tag(name = "Configuration Type",
        description = "Endpoints related to configuration types (reusable configuration) metadata access.")
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

    @GET
    @Path("index")
    @Operation(description = "Returns all available configuration type - storable models. "
            + "Note that the lightPayload flag allows to load all of them at once when you eagerly need "
            + " to create a client model for all configurations.")
    @APIResponse(responseCode = "200",
            description = "the list of available and storable configurations (datastore, dataset, ...).",
            content = @Content(mediaType = APPLICATION_JSON))
    public ConfigTypeNodes getRepositoryModel(
            @QueryParam("language") @DefaultValue("en") @Parameter(name = "language",
                    description = "the language for display names.", in = QUERY,
                    schema = @Schema(type = STRING, defaultValue = "en")) final String language,
            @QueryParam("lightPayload") @DefaultValue("true") @Parameter(name = "lightPayload",
                    description = "should the payload skip the forms and actions associated to the configuration.",
                    in = QUERY, schema = @Schema(type = BOOLEAN, defaultValue = "true")) final boolean lightPaylaod) {
        return toNodes(language, s -> true, lightPaylaod);
    }

    @GET
    @Path("details")
    @Operation(description = "Returns all available configuration type - storable models. "
            + "Note that the lightPayload flag allows to load all of them at once when you eagerly need "
            + " to create a client model for all configurations.")
    @APIResponse(responseCode = "200",
            description = "the list of available and storable configurations (datastore, dataset, ...).",
            content = @Content(mediaType = APPLICATION_JSON))
    public ConfigTypeNodes getDetail(
            @QueryParam("language") @DefaultValue("en") @Parameter(name = "language",
                    description = "the language for display names.", in = QUERY,
                    schema = @Schema(type = STRING, defaultValue = "en")) final String language,
            @QueryParam("identifiers") @Parameter(name = "identifiers",
                    description = "the comma separated list of identifiers to request.",
                    in = QUERY) final String[] ids) {
        final Predicate<String> filter = ids == null ? s -> false : new Predicate<String>() {

            private final Collection<String> values = Stream.of(ids).collect(toSet());

            @Override
            public boolean test(final String s) {
                return values.contains(s);
            }
        };

        return toNodes(language, filter, false);
    }

    @POST
    @Path("migrate/{id}/{configurationVersion}")
    @Operation(description = "Allows to migrate a configuration without calling any component execution.")
    @APIResponse(responseCode = "200",
            description = "the new values for that configuration (or the same if no migration was needed).",
            content = @Content(mediaType = APPLICATION_JSON))
    @APIResponse(responseCode = "404", description = "The configuration is not found",
            content = @Content(mediaType = APPLICATION_JSON))
    public Map<String, String>
            migrate(@PathParam("id") @Parameter(name = "id", description = "the configuration identifier",
                    in = PATH) final String id,
                    @PathParam("configurationVersion") @Parameter(name = "configurationVersion",
                            description = "the configuration version you send", in = PATH) final int version,
                    @RequestBody(description = "the actual configuration in key/value form.", required = true,
                            content = @Content(mediaType = APPLICATION_JSON,
                                    schema = @Schema(type = OBJECT))) final Map<String, String> config) {
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
            final FamilyBundle resourcesBundle, final Container container, final Locale locale,
            final Predicate<String> idFilter, final boolean lightPayload) {
        final ClassLoader loader = container.getLoader();
        if (configs == null) {
            return Stream.empty();
        }
        return configs.flatMap(c -> {
            final Stream<ConfigTypeNode> configNode;
            if (idFilter.test(c.getId())) {
                final ConfigTypeNode node = new ConfigTypeNode();
                node.setId(c.getId());
                node.setVersion(c.getVersion());
                node.setConfigurationType(c.getKey().getConfigType());
                node.setName(c.getKey().getConfigName());
                node.setParentId(parentId);
                node.setDisplayName(resourcesBundle
                        .configurationDisplayName(c.getKey().getConfigType(), c.getKey().getConfigName())
                        .orElse(c.getKey().getConfigName()));
                if (!lightPayload) {
                    node.setActions(actionsService.findActions(family, container, locale, c));

                    // force configuration as root prefix
                    final int prefixLen = c.getMeta().getPath().length();
                    final String forcedPrefix = c.getMeta().getName();
                    node.setProperties(propertiesService
                            .buildProperties(singleton(c.getMeta()), loader, locale, null)
                            .map(p -> new SimplePropertyDefinition(forcedPrefix + p.getPath().substring(prefixLen),
                                    p.getName(), p.getDisplayName(), p.getType(), p.getDefaultValue(),
                                    p.getValidation(), p.getMetadata(), p.getPlaceholder(),
                                    p.getProposalDisplayNames()))
                            .collect(toList()));
                }

                node.setEdges(c.getChildConfigs().stream().map(Config::getId).collect(toSet()));

                configNode = Stream.of(node);
            } else {
                configNode = Stream.empty();
            }

            return Stream.concat(configNode, createNode(c.getId(), family, c.getChildConfigs().stream(),
                    resourcesBundle, container, locale, idFilter, lightPayload));
        });
    }

    private ConfigTypeNodes toNodes(final String language, final Predicate<String> filter, final boolean lightPayload) {
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
                            final FamilyBundle resourcesBundle = family.getMeta().findBundle(c.getLoader(), locale);

                            final Stream<ConfigTypeNode> familyNode;
                            if (filter.test(family.getId())) {
                                final ConfigTypeNode node = new ConfigTypeNode();
                                node.setId(family.getId());
                                node.setName(family.getMeta().getName());

                                node.setDisplayName(resourcesBundle.displayName().orElse(family.getMeta().getName()));

                                node.setEdges(family.getConfigs().stream().map(Config::getId).collect(toSet()));
                                familyNode = Stream.of(node);
                            } else {
                                familyNode = Stream.empty();
                            }
                            return Stream.concat(familyNode, createNode(family.getId(), family.getMeta().getName(),
                                    family.getConfigs().stream(), resourcesBundle, c, locale, filter, lightPayload));
                        }))
                .collect(() -> {
                    final ConfigTypeNodes nodes = new ConfigTypeNodes();
                    nodes.setNodes(new HashMap<>());
                    return nodes;
                }, (root,
                        children) -> root.getNodes().putAll(children.collect(toMap(ConfigTypeNode::getId, identity()))),
                        (first, second) -> first.getNodes().putAll(second.getNodes()));
    }
}
