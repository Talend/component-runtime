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
import static javax.json.stream.JsonCollectors.toJsonObject;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.talend.sdk.component.proxy.config.SwaggerDoc.ERROR_HEADER_DESC;
import static org.talend.sdk.component.proxy.service.ErrorProcessor.Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.NotificationOptions;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.proxy.api.persistence.OnEdit;
import org.talend.sdk.component.proxy.api.persistence.OnFindById;
import org.talend.sdk.component.proxy.api.persistence.OnPersist;
import org.talend.sdk.component.proxy.api.service.ConfigurationFormatter;
import org.talend.sdk.component.proxy.api.service.ConfigurationTypes;
import org.talend.sdk.component.proxy.api.service.RequestContext;
import org.talend.sdk.component.proxy.front.error.AutoErrorHandling;
import org.talend.sdk.component.proxy.model.EntityRef;
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
import org.talend.sdk.component.proxy.service.impl.HttpRequestContext;
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
@AutoErrorHandling
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
    private Jsonb jsonb;

    @Inject
    private Event<OnPersist> onPersistEvent;

    @Inject
    private Event<OnEdit> onEditEvent;

    @Inject
    private Event<OnFindById> onFindByIdEvent;

    @Inject
    private ConfigurationFormatter configurationFormatter;

    private final NotificationOptions notificationOptions = NotificationOptions.ofExecutor(Runnable::run);

    private final ConfigTypeNode defaultFamily = new ConfigTypeNode();

    @Override
    public CompletionStage<Nodes> findRoots(final RequestContext context) {
        return withApplyNodesAndComponents(context.language(), context::findPlaceholder,
                (nodes, components) -> configurationService.getRootConfiguration(nodes, components));
    }

    @Override
    public CompletionStage<Map<String, String>> resolveConfiguration(final RequestContext context, final String id) {
        final UiSpecContext uiSpecContext = new UiSpecContext(context.language(), context::findPlaceholder);
        return onFindByIdEvent.fireAsync(new OnFindById(context, id)).thenCompose(byId -> byId
                .getFormId()
                .thenCompose(formId -> configurationClient
                        .getDetails(context.language(), formId, context::findPlaceholder)
                        .thenCompose(detail -> configurationService.filterNestedConfigurations(detail, uiSpecContext)))
                .thenCompose(detail -> byId.getProperties().thenCompose(
                        props -> configurationService.replaceReferences(uiSpecContext, detail, props))));
    }

    @Override
    public CompletionStage<Collection<SimplePropertyDefinition>> findProperties(final RequestContext context,
            final String id) {
        return configurationClient.getDetails(context.language(), id, context::findPlaceholder).thenApply(
                ConfigTypeNode::getProperties);
    }

    @ApiOperation(value = "Return all the available root configuration (Data store like) from the component server",
            notes = "Every configuration has an icon. "
                    + "In the response an icon key is returned. this icon key can be one of the bundled icons or a custom one. "
                    + "The consumer of this endpoint will need to check if the icon key is in the icons bundle "
                    + "otherwise the icon need to be gathered using the `familyId` from this endpoint `configurations/{id}/icon`",
            response = Nodes.class, tags = { "configurations", "datastore" }, produces = "application/json",
            responseHeaders = { @ResponseHeader(name = HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class) })
    @GET
    public CompletionStage<Nodes> getRootConfig(@Context final HttpServletRequest request) {
        final String language = getLang(request);
        final Function<String, String> placeholderProvider = placeholderProviderFactory.newProvider(request);
        return findRoots(new HttpRequestContext(language, placeholderProvider, request));
    }

    @ApiOperation(value = "Return a form description ( Ui Spec ) without a specific configuration ",
            response = Nodes.class, produces = "application/json",
            tags = { "form", "ui spec", "configurations", "datastore", "dataset" },
            responseHeaders = { @ResponseHeader(name = HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class) })
    @GET
    @Path("form/initial/{type}")
    public CompletionStage<UiNode> getInitialForm(@PathParam("type") final String type,
            @Context final HttpServletRequest request) {
        if (type == null || type.isEmpty()) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ProxyErrorPayload(ProxyErrorDictionary.BAD_CONFIGURATION_TYPE.name(),
                            "No configuration type passed"))
                    .type(APPLICATION_JSON_TYPE)
                    .build());
        }
        final String language = getLang(request);
        final Function<String, String> placeholderProvider = placeholderProviderFactory.newProvider(request);
        return toUiSpecAndMetadata(language, placeholderProvider, CompletableFuture.completedFuture(
                new ConfigTypeNode(type, 0, null, type, type, type, emptySet(), new ArrayList<>(), new ArrayList<>())),
                true);
    }

    @POST
    @Path("persistence/save/{formId}")
    @ApiOperation(value = "Saves a configuration based on a form identifier.", response = EntityRef.class,
            produces = "application/json",
            tags = { "form", "ui spec", "configurations", "datastore", "dataset", "persistence" },
            responseHeaders = @ResponseHeader(name = HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class))
    public CompletionStage<EntityRef> postConfigurationFromFormId(@Context final HttpServletRequest request,
            @PathParam("formId") final String formId, final JsonObject payload) {
        return doSave(request, getLang(request), payload, formId);
    }

    @POST
    @Path("persistence/save-from-type/{type}")
    @ApiOperation(
            value = "Saves a configuration based on a type. Concretely it is the same as `/persistence/save/{formId}` "
                    + "but the `formId` is contained into the payload itself and marked in the metadata as such.",
            response = EntityRef.class, produces = "application/json",
            tags = { "form", "ui spec", "configurations", "datastore", "dataset", "persistence" },
            responseHeaders = @ResponseHeader(name = HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class))
    public CompletionStage<EntityRef> postConfigurationFromType(@Context final HttpServletRequest request,
            @PathParam("type") final String type, final JsonObject payload) {
        final String lang = getLang(request);
        final String formId = modelEnricherService
                .findEnclosedFormId(type, lang, payload)
                .orElseThrow(() -> new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(new ProxyErrorPayload(ProxyErrorDictionary.NO_CONFIGURATION_TYPE.name(),
                                "No form identifier found in the form properties"))
                        .build()));
        return doSave(request, lang, payload, formId);
    }

    @POST
    @Path("persistence/edit/{id}")
    @ApiOperation(value = "Update a configuration.", response = EntityRef.class, produces = "application/json",
            tags = { "form", "ui spec", "configurations", "datastore", "dataset", "persistence" },
            responseHeaders = @ResponseHeader(name = HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class))
    public CompletionStage<EntityRef> putConfiguration(@Context final HttpServletRequest request,
            @PathParam("id") final String id, final JsonObject payload) {
        final String lang = getLang(request);
        final Function<String, String> placeholderProvider = placeholderProviderFactory.newProvider(request);
        final HttpRequestContext requestContext = new HttpRequestContext(lang, placeholderProvider, request);
        return onFindByIdEvent
                .fireAsync(new OnFindById(requestContext, id), notificationOptions)
                .thenCompose(event -> event.getFormId().thenCompose(
                        formId -> configurationClient.getDetails(lang, formId, placeholderProvider)))
                .thenCompose(config -> {
                    final JsonObject enrichment =
                            modelEnricherService.extractEnrichment(config.getConfigurationType(), lang, payload);
                    final JsonObject configuration = enrichment.isEmpty() ? payload
                            : payload.entrySet().stream().filter(it -> !enrichment.containsKey(it.getKey())).collect(
                                    toJsonObject());
                    return onEditEvent
                            .fireAsync(new OnEdit(id, requestContext, jsonb, enrichment, config.getProperties(),
                                    configurationFormatter.flatten(configuration)), notificationOptions)
                            .thenCompose(OnEdit::getCompletionListener)
                            .thenApply(edit -> new EntityRef(id));
                });
    }

    @ApiOperation(value = "Return a form description ( Ui Spec ) of a specific configuration ", response = UiNode.class,
            tags = { "form", "ui spec", "configurations", "datastore", "dataset" }, produces = "application/json",
            responseHeaders = { @ResponseHeader(name = HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class) })
    @GET
    @Path("form/{id}")
    public CompletionStage<UiNode> getForm(@PathParam("id") final String id,
            @Context final HttpServletRequest request) {
        final String lang = getLang(request);
        final Function<String, String> placeholderProvider = placeholderProviderFactory.newProvider(request);
        final HttpRequestContext requestContext = new HttpRequestContext(lang, placeholderProvider, request);
        return onFindByIdEvent
                .fireAsync(new OnFindById(requestContext, id), notificationOptions)
                .thenCompose(event -> event
                        .getFormId()
                        .thenCompose(formId -> toUiSpecAndMetadata(lang, placeholderProvider,
                                configurationClient.getDetails(lang, formId, placeholderProvider), false)
                                        .thenCompose(uiNode -> event.getProperties().thenApply(props -> {
                                            uiNode.getUi().setProperties(props);
                                            return uiNode;
                                        }))));
    }

    @ApiOperation(value = "Return the configuration icon file in png format", tags = "icon",
            responseHeaders = { @ResponseHeader(name = HEADER_TALEND_COMPONENT_SERVER_ERROR,
                    description = ERROR_HEADER_DESC, response = Boolean.class) })
    @GET
    @Path("icon/{id}")
    @Produces({ APPLICATION_JSON, APPLICATION_OCTET_STREAM })
    public CompletionStage<byte[]> getConfigurationIconById(@PathParam("id") final String id,
            @Context final HttpServletRequest request) {
        return componentClient.getFamilyIconById(id, placeholderProviderFactory.newProvider(request));
    }

    private CompletionStage<EntityRef> doSave(final HttpServletRequest request, final String lang,
            final JsonObject payload, final String formId) {
        final Function<String, String> placeholderProvider = placeholderProviderFactory.newProvider(request);
        final HttpRequestContext requestContext = new HttpRequestContext(lang, placeholderProvider, request);
        return configurationClient
                .getDetails(lang, formId, placeholderProviderFactory.newProvider(request))
                .thenCompose(node -> {
                    final JsonObject enrichment =
                            modelEnricherService.extractEnrichment(node.getConfigurationType(), lang, payload);
                    final JsonObject configuration = enrichment.isEmpty() ? payload
                            : payload.entrySet().stream().filter(it -> !enrichment.containsKey(it.getKey())).collect(
                                    toJsonObject());
                    return onPersistEvent
                            .fireAsync(new OnPersist(requestContext, jsonb, node.getId(), enrichment,
                                    node.getProperties(), configurationFormatter.flatten(configuration)),
                                    notificationOptions)
                            .thenCompose(persist -> {
                                final CompletionStage<String> id = persist.getId();
                                if (id == null) { // wrong setup likely
                                    throw new WebApplicationException(Response
                                            .status(Response.Status.EXPECTATION_FAILED)
                                            .entity(new ProxyErrorPayload(
                                                    ProxyErrorDictionary.PERSISTENCE_FAILED.name(),
                                                    "Entity was not persisted"))
                                            .build());
                                }
                                return id.thenApply(EntityRef::new);
                            });
                });
    }

    private String getLang(final HttpServletRequest request) {
        return ofNullable(request.getLocale()).map(Locale::getLanguage).orElse("en");
    }

    private CompletionStage<UiNode> toUiSpecAndMetadata(final String language,
            final Function<String, String> placeholderProvider, final CompletionStage<ConfigTypeNode> from,
            final boolean noFamily) {
        return from
                .thenCompose(node -> withApplyNodesAndComponents(language, placeholderProvider,
                        (nodes, components) -> toUiNode(language, node, nodes, components, noFamily,
                                placeholderProvider)))
                .thenCompose(identity());
    }

    private CompletionStage<UiNode> toUiNode(final String language, final ConfigTypeNode node,
            final ConfigTypeNodes nodes, final ComponentIndices componentIndices, final boolean noFamily,
            final Function<String, String> placeholderProvider) {
        final ConfigTypeNode family =
                noFamily ? defaultFamily : configurationService.getFamilyOf(node.getParentId(), nodes);
        final String icon = noFamily ? null : configurationService.findIcon(family.getId(), componentIndices);
        final Node configType = new Node(node.getId(), node.getDisplayName(), family.getId(), family.getDisplayName(),
                icon, node.getEdges(), node.getVersion(), node.getName());
        return uiSpecService
                .convert(family.getName(), language, modelEnricherService.enrich(node, language),
                        new UiSpecContext(language, placeholderProvider))
                .thenApply(uiSpec -> new UiNode(uiSpec, configType));
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
