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

import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.talend.sdk.component.proxy.config.SwaggerDoc.ERROR_HEADER_DESC;

import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.talend.sdk.component.proxy.model.Node;
import org.talend.sdk.component.proxy.model.Nodes;
import org.talend.sdk.component.proxy.model.ProxyErrorDictionary;
import org.talend.sdk.component.proxy.model.ProxyErrorPayload;
import org.talend.sdk.component.proxy.model.UiNode;
import org.talend.sdk.component.proxy.service.ErrorProcessor;
import org.talend.sdk.component.proxy.service.ModelEnricherService;
import org.talend.sdk.component.proxy.service.PlaceholderProviderFactory;
import org.talend.sdk.component.proxy.service.UiSpecServiceProvider;
import org.talend.sdk.component.proxy.service.client.ComponentClient;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentIndex;
import org.talend.sdk.component.server.front.model.ComponentIndices;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ResponseHeader;

@Api(description = "Endpoint responsible to provide a way to get components list and there ui definition"
        + "to let the UI creates the corresponding entities. It is UiSpec friendly.",
        tags = { "component", "icon", "uispec", "form" })
@ApplicationScoped
@Path("components")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ComponentResource {

    @Inject
    private PlaceholderProviderFactory placeholderProviderFactory;

    @Inject
    private ComponentClient componentClient;

    @Inject
    private ErrorProcessor errorProcessor;

    @Inject
    private ModelEnricherService modelEnricherService;

    @Inject
    private UiSpecServiceProvider uiSpecServiceProvider;

    @ApiOperation(value = "This endpoint return a list of available component",
            notes = "component has icon that need to be handled by the consumer of this endpoint "
                    + "In the response an icon key is returned. this icon key can be one of the bundled icons or a custom one. "
                    + "The consumer of this endpoint will need to check if the icon key is in the icons bundle "
                    + "otherwise the icon need to be gathered using the `id` of the component from this endpoint `components/{id}/icon/`",
            tags = "components", produces = APPLICATION_JSON, consumes = APPLICATION_JSON, response = Nodes.class,
            responseHeaders = { @ResponseHeader(name = ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class) })
    @GET
    public void listComponent(@Suspended final AsyncResponse response, @Context final HttpServletRequest request) {
        final String language = ofNullable(request.getLocale()).map(Locale::getLanguage).orElse("en");
        final Function<String, String> placeholderProvider = placeholderProviderFactory.newProvider(request);
        componentClient
                .getAllComponents(language, placeholderProvider)
                .thenApply(ComponentIndices::getComponents)
                .thenApply(components -> components.stream().map(this::toNode).collect(
                        Collectors.toMap(Node::getId, Function.identity())))
                .thenApply(Nodes::new)
                .handle((r, e) -> errorProcessor.handleResponse(response, r, e));
    }

    @ApiOperation(value = "This endpoint return the ui spec of a component identified by it's id",
            notes = "component has icon that need to be handled by the consumer of this endpoint "
                    + "In the response an icon key is returned. this icon key can be one of the bundled icons or a custom one. "
                    + "The consumer of this endpoint will need to check if the icon key is in the icons bundle "
                    + "otherwise the icon need to be gathered using the `id` of the component from this endpoint `components/{id}/icon/`",
            tags = "components", produces = APPLICATION_JSON, consumes = APPLICATION_JSON, response = UiNode.class,
            responseHeaders = { @ResponseHeader(name = ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class) })
    @GET
    @Path("{id}/form")
    public void getComponentForm(@Suspended final AsyncResponse response, @Context final HttpServletRequest request,
            @PathParam("id") final String id) {
        final String language = ofNullable(request.getLocale()).map(Locale::getLanguage).orElse("en");
        final Function<String, String> placeholderProvider = placeholderProviderFactory.newProvider(request);
        componentClient
                .getComponentDetail(language, placeholderProvider, id)
                .thenCompose(d -> withUiSpec(d, language, placeholderProvider))
                .handle((r, e) -> errorProcessor.handleResponse(response, r, e));
    }

    @ApiOperation(value = "Return the component icon file in png format", tags = "icon",
            responseHeaders = { @ResponseHeader(name = ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class) })
    @GET
    @Path("{id}/icon")
    @Produces({ APPLICATION_JSON, APPLICATION_OCTET_STREAM })
    public void getComponentIconById(@Suspended final AsyncResponse response, @PathParam("id") final String id,
            @Context final HttpServletRequest request) {
        componentClient.getComponentIconById(placeholderProviderFactory.newProvider(request), id).handle(
                (icon, throwable) -> errorProcessor.handleResponse(response, icon, throwable));
    }

    private CompletionStage<UiNode> withUiSpec(final ComponentDetail detail, final String language,
            final Function<String, String> placeholderProvider) {
        try (final UiSpecService specService = uiSpecServiceProvider.newInstance(language, placeholderProvider)) {
            return specService.convert(modelEnricherService.enrich(detail, language)).thenApply(
                    uiSpec -> new UiNode(uiSpec, toNode(detail)));
        } catch (final Exception e) {
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ProxyErrorPayload(ProxyErrorDictionary.UISPEC_SERVICE_CLOSE_FAILURE.name(),
                            "UiSpecService processing failed"))
                    .header(ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR, true)
                    .build());
        }
    }

    private Node toNode(final ComponentDetail d) {
        return new Node(d.getId().getId(), Node.Type.COMPONENT, d.getDisplayName(), d.getId().getFamilyId(), "",
                d.getIcon(), Collections.emptyList(), d.getVersion(), d.getId().getName(), d.getId().getPlugin());

    }

    private Node toNode(final ComponentIndex c) {
        return new Node(c.getId().getId(), Node.Type.COMPONENT, c.getDisplayName(), c.getId().getFamilyId(),
                c.getFamilyDisplayName(), c.getIcon().getIcon(), Collections.emptyList(), c.getVersion(),
                c.getId().getName(), c.getId().getPlugin());
    }

}
