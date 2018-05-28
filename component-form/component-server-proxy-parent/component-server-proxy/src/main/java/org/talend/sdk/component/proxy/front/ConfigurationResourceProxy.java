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
package org.talend.sdk.component.proxy.front;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.talend.sdk.component.proxy.client.ComponentClient;
import org.talend.sdk.component.proxy.client.ConfigurationClient;
import org.talend.sdk.component.proxy.model.ConfigType;
import org.talend.sdk.component.proxy.model.Configurations;
import org.talend.sdk.component.proxy.service.ConfigurationService;
import org.talend.sdk.component.proxy.service.ErrorProcessor;
import org.talend.sdk.component.proxy.service.PlaceholderProviderFactory;
import org.talend.sdk.component.server.front.model.ComponentIndices;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ResponseHeader;

@Api(description = "Configuration endpoint", tags = { "configuration", "icon" })
@ApplicationScoped
@Path("configuration")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ConfigurationResourceProxy {

    private static final String SWAGGER_HEADER_DESC = "This header indicate the error origin. "
            + "true indicate an error from the component server, " + "false indicate that the error is from this proxy";

    @Inject
    private ConfigurationClient configurationClient;

    @Inject
    private ComponentClient componentClient;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private ErrorProcessor errorProcessor;

    @Inject
    private PlaceholderProviderFactory placeholderProviderFactory;

    @ApiOperation(value = "Return all the available root configuration (Data store like) from the component server",
            notes = "Every configuration has an icon. "
                    + "In the response an icon key is returned. this icon key can be one of the bundled icons or a custom one. "
                    + "The consumer of this endpoint will need to check if the icon key is in the icons bundle "
                    + "otherwise the icon need to be gathered using the `familyId` from this endpoint `configuration/icon/{id}`",
            response = Configurations.class, tags = "configurations, data store", produces = "application/json",
            responseHeaders = { @ResponseHeader(name = ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = SWAGGER_HEADER_DESC, response = Boolean.class) })
    @GET
    @Path("roots")
    public void getRootConfig(@Suspended final AsyncResponse response, @Context final HttpServletRequest request) {
        final String language = ofNullable(request.getLocale()).map(Locale::getLanguage).orElse("en");
        final Function<String, String> placeholderProvider = placeholderProviderFactory.newProvider(request);
        withNodesAndComponents(language, placeholderProvider,
                (nodes, components) -> configurationService.getRootConfiguration(nodes, components))
                        .handle((result, throwable) -> errorProcessor.handleResponse(response, result, throwable));
    }

    @ApiOperation(value = "Return a configuration details using configuraiton identifiers ", notes = "",
            response = Configurations.class, tags = "configurations, data store, data set",
            produces = "application/json",
            responseHeaders = { @ResponseHeader(name = ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = SWAGGER_HEADER_DESC, response = Boolean.class) })
    @GET
    @Path("details")
    public void getDetails(@Suspended final AsyncResponse response, @QueryParam("identifiers") final String[] ids,
            @Context final HttpServletRequest request) {
        if (ids == null || ids.length == 0) {
            response.resume(new Configurations(emptyMap(), emptyMap()));
            return;
        }

        final String language = ofNullable(request.getLocale()).map(Locale::getLanguage).orElse("en");
        final Function<String, String> placeholderProvider = placeholderProviderFactory.newProvider(request);
        configurationClient
                .getDetails(language, ids, placeholderProvider)
                .thenCompose(configs -> withNodesAndComponents(language, placeholderProvider,
                        (nodes, components) -> createConfigurations(configs, nodes, components)))
                .handle((detail, throwable) -> errorProcessor.handleResponse(response, detail, throwable));
    }

    @ApiOperation(value = "Return the configuration icon file in png format", tags = "configuration icon, icon",
            produces = "image/png",
            responseHeaders = { @ResponseHeader(name = ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = SWAGGER_HEADER_DESC, response = Boolean.class) })
    @GET
    @Path("icon/{id}")
    public void getConfigurationIconById(@Suspended final AsyncResponse response, @PathParam("id") final String id,
            @Context final HttpServletRequest request) {
        componentClient.getFamilyIconById(id, placeholderProviderFactory.newProvider(request)).handle(
                (icon, throwable) -> errorProcessor.handleResponse(response, icon, throwable));
    }

    private Configurations createConfigurations(final ConfigTypeNodes configs, final ConfigTypeNodes nodes,
            final ComponentIndices componentIndices) {
        return new Configurations(configs.getNodes().entrySet().stream().map(e -> {
            final ConfigTypeNode family = configurationService.getFamilyOf(e.getValue().getParentId(), nodes);
            final String icon = configurationService.findIcon(family, componentIndices);
            return new ConfigType(e.getValue().getId(), family.getId(), e.getValue().getDisplayName(),
                    family.getDisplayName(), icon, e.getValue().getEdges());
        }).collect(toMap(ConfigType::getId, Function.identity())), emptyMap());
    }

    private <T> CompletionStage<T> withNodesAndComponents(final String language,
            final Function<String, String> placeholderProvider,
            final BiFunction<ConfigTypeNodes, ComponentIndices, T> callback) {
        final CompletionStage<ConfigTypeNodes> allConfigurations =
                configurationClient.getAllConfigurations(language, placeholderProvider);
        final CompletionStage<ComponentIndices> allComponents =
                componentClient.getAllComponents(language, placeholderProvider);
        return allConfigurations
                .thenCompose(nodes -> allComponents.thenApply(components -> callback.apply(nodes, components)));
    }
}
