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

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.talend.sdk.component.proxy.config.SwaggerDoc.ERROR_HEADER_DESC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.proxy.api.ConfigurationTypes;
import org.talend.sdk.component.proxy.api.RequestContext;
import org.talend.sdk.component.proxy.model.Node;
import org.talend.sdk.component.proxy.model.Nodes;
import org.talend.sdk.component.proxy.model.ProxyErrorDictionary;
import org.talend.sdk.component.proxy.model.ProxyErrorPayload;
import org.talend.sdk.component.proxy.model.UiNode;
import org.talend.sdk.component.proxy.service.ConfigurationService;
import org.talend.sdk.component.proxy.service.ErrorProcessor;
import org.talend.sdk.component.proxy.service.ModelEnricherService;
import org.talend.sdk.component.proxy.service.PlaceholderProviderFactory;
import org.talend.sdk.component.proxy.service.client.ComponentClient;
import org.talend.sdk.component.proxy.service.client.ConfigurationClient;
import org.talend.sdk.component.proxy.service.client.UiSpecContext;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.server.front.model.ComponentIndices;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ResponseHeader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Api(description = "Endpoint responsible to provide a way to navigate in the configurations and subconfigurations "
        + "to let the UI creates the corresponding entities. It is UiSpec friendly.",
        tags = { "configuration", "icon", "uispec", "form" })
@ApplicationScoped
@Path("configurations")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ConfigurationTypeResource implements ConfigurationTypes {

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

    @Inject
    private ModelEnricherService modelEnricherService;

    @Inject
    @UiSpecProxy
    private UiSpecService<UiSpecContext> uiSpecService;

    @Inject
    @UiSpecProxy
    private ExecutorService pool;

    private final ConfigTypeNode defaultFamily = new ConfigTypeNode(); // potential todo: add some defaults?

    @Override
    public CompletionStage<Nodes> findRoots(final RequestContext context) {
        return withApplyNodesAndComponents(context.language(), context::findPlaceholder,
                (nodes, components) -> configurationService.getRootConfiguration(nodes, components));
    }

    @Override
    public CompletionStage<Collection<SimplePropertyDefinition>> findProperties(final RequestContext context,
            final String id) {
        return configurationClient.getDetails(context.language(), id, context::findPlaceholder).thenApply(
                c -> c.getNodes().values().iterator().next().getProperties());
    }

    @ApiOperation(value = "Return all the available root configuration (Data store like) from the component server",
            notes = "Every configuration has an icon. "
                    + "In the response an icon key is returned. this icon key can be one of the bundled icons or a custom one. "
                    + "The consumer of this endpoint will need to check if the icon key is in the icons bundle "
                    + "otherwise the icon need to be gathered using the `familyId` from this endpoint `configurations/{id}/icon`",
            response = Nodes.class, tags = { "configurations", "datastore" }, produces = "application/json",
            responseHeaders = { @ResponseHeader(name = ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class) })
    @GET
    public void getRootConfig(@Suspended final AsyncResponse response, @Context final HttpServletRequest request) {
        final String language = ofNullable(request.getLocale()).map(Locale::getLanguage).orElse("en");
        final Function<String, String> placeholderProvider = placeholderProviderFactory.newProvider(request);
        findRoots(new RequestContext() {

            @Override
            public String language() {
                return language;
            }

            @Override
            public String findPlaceholder(final String attributeName) {
                return placeholderProvider.apply(attributeName);
            }
        }).handle((result, throwable) -> errorProcessor.handleResponse(response, result, throwable));
    }

    @ApiOperation(value = "Return a form description ( Ui Spec ) without a specific configuration ",
            response = Nodes.class, tags = { "form", "ui spec", "configurations", "datastore", "dataset" },
            produces = "application/json",
            responseHeaders = { @ResponseHeader(name = ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class) })
    @GET
    @Path("{type}/form/initial")
    public void getInitialForm(@Suspended final AsyncResponse response, @PathParam("type") final String type,
            @Context final HttpServletRequest request) {
        if (type == null || type.isEmpty()) {
            response.resume(new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ProxyErrorPayload(ProxyErrorDictionary.BAD_CONFIGURATION_TYPE.name(),
                            "No configuration type passed"))
                    .type(APPLICATION_JSON_TYPE)
                    .build()));
            return;
        }
        final String language = ofNullable(request.getLocale()).map(Locale::getLanguage).orElse("en");
        final Function<String, String> placeholderProvider = placeholderProviderFactory.newProvider(request);
        toUiSpecAndMetadata(response, language, placeholderProvider,
                CompletableFuture.supplyAsync(() -> new ConfigTypeNode(type, 0, null, type, type, type, emptySet(),
                        new ArrayList<>(), new ArrayList<>()), pool),
                true);
    }

    @ApiOperation(value = "Return a form description ( Ui Spec ) of a specific configuration ", response = UiNode.class,
            tags = { "form", "ui spec", "configurations", "datastore", "dataset" }, produces = "application/json",
            responseHeaders = { @ResponseHeader(name = ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class) })
    @GET
    @Path("{id}/form")
    public void getForm(@Suspended final AsyncResponse response, @PathParam("id") final String id,
            @Context final HttpServletRequest request) {
        if (id == null || id.isEmpty()) {
            response.resume(new UiNode());
            return;
        }
        final String language = ofNullable(request.getLocale()).map(Locale::getLanguage).orElse("en");
        final Function<String, String> placeholderProvider = placeholderProviderFactory.newProvider(request);
        toUiSpecAndMetadata(response, language, placeholderProvider,
                configurationClient.getDetails(language, id, placeholderProvider).thenApply(this::getSingleNode),
                false);
    }

    @ApiOperation(value = "Return the configuration icon file in png format", tags = "icon",
            responseHeaders = { @ResponseHeader(name = ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class) })
    @GET
    @Path("{id}/icon")
    @Produces({ APPLICATION_JSON, APPLICATION_OCTET_STREAM })
    public void getConfigurationIconById(@Suspended final AsyncResponse response, @PathParam("id") final String id,
            @Context final HttpServletRequest request) {
        componentClient.getFamilyIconById(id, placeholderProviderFactory.newProvider(request)).handle(
                (icon, throwable) -> errorProcessor.handleResponse(response, icon, throwable));
    }

    private void toUiSpecAndMetadata(final AsyncResponse response, final String language,
            final Function<String, String> placeholderProvider, final CompletionStage<ConfigTypeNode> from,
            final boolean noFamily) {
        from
                .thenCompose(node -> withApplyNodesAndComponents(language, placeholderProvider,
                        (nodes, components) -> toUiNode(language, node, nodes, components, noFamily,
                                placeholderProvider)))
                .thenCompose(identity())
                .handle((detail, throwable) -> errorProcessor.handleResponse(response, detail, throwable));
    }

    private CompletionStage<UiNode> toUiNode(final String language, final ConfigTypeNode node,
            final ConfigTypeNodes nodes, final ComponentIndices componentIndices, final boolean noFamily,
            final Function<String, String> placeholderProvider) {
        final ConfigTypeNode family =
                noFamily ? defaultFamily : configurationService.getFamilyOf(node.getParentId(), nodes);
        final String icon = noFamily ? null : configurationService.findIcon(family.getId(), componentIndices);
        final Node configType = new Node(node.getId(), Node.Type.CONFIGURATION, node.getDisplayName(), family.getId(),
                family.getDisplayName(), icon, node.getEdges(), node.getVersion(), node.getName(), null);
        return uiSpecService
                .convert(family.getName(), modelEnricherService.enrich(node, language),
                        new UiSpecContext(language, placeholderProvider))
                .thenApply(uiSpec -> new UiNode(uiSpec, configType));
    }

    private ConfigTypeNode getSingleNode(final ConfigTypeNodes configs) {
        return configs
                .getNodes()
                .entrySet()
                .stream()
                .findFirst()
                .orElseThrow(() -> new WebApplicationException(Response
                        .status(Response.Status.NOT_FOUND)
                        .entity(new ProxyErrorPayload(ProxyErrorDictionary.UNEXPECTED.name(), "No node is found"))
                        .build()))
                .getValue();
    }

    private <T> CompletionStage<T> withApplyNodesAndComponents(final String language,
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
