/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.QUERY;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.OBJECT;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.STRING;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.ServiceMeta;
import org.talend.sdk.component.server.dao.ComponentActionDao;
import org.talend.sdk.component.server.front.model.ActionItem;
import org.talend.sdk.component.server.front.model.ActionList;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.service.LocaleMapper;
import org.talend.sdk.component.server.service.PropertiesService;
import org.talend.sdk.component.server.service.httpurlconnection.IgnoreNetAuthenticator;

import lombok.extern.slf4j.Slf4j;

@Tag(name = "Action", description = "Endpoints related to callbacks/triggers execution.")
@Slf4j
@Path("action")
@ApplicationScoped
@IgnoreNetAuthenticator
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ActionResource {

    @Inject
    private ComponentManager manager;

    @Inject
    private ComponentActionDao actionDao;

    @Inject
    private PropertiesService propertiesService;

    @Inject
    private LocaleMapper localeMapper;

    @POST
    @Path("execute")
    @Operation(
            description = "This endpoint will execute any UI action and serialize the response as a JSON (pojo model). "
                    + "It takes as input the family, type and name of the related action to identify it and its configuration "
                    + "as a flat key value set using the same kind of mapping than for components (option path as key).")
    @APIResponse(responseCode = "200", description = "The action payload serialized in JSON.",
            content = @Content(mediaType = APPLICATION_JSON))
    @APIResponse(responseCode = "400",
            description = "If the action is not set, payload will be an ErrorPayload with the code ACTION_MISSING.",
            content = @Content(mediaType = APPLICATION_JSON))
    @APIResponse(responseCode = "404",
            description = "If the action can't be found, payload will be an ErrorPayload with the code ACTION_MISSING.",
            content = @Content(mediaType = APPLICATION_JSON))
    @APIResponse(responseCode = "520",
            description = "If the action execution failed, payload will be an ErrorPayload with the code ACTION_ERROR.",
            content = @Content(mediaType = APPLICATION_JSON))
    public Response
            execute(@QueryParam("family") @Parameter(name = "family", required = true, in = QUERY,
                    description = "the component family") final String family,
                    @QueryParam("type") @Parameter(name = "type", required = true, in = QUERY,
                            description = "the type of action") final String type,
                    @QueryParam("action") @Parameter(name = "action", required = true, in = QUERY,
                            description = "the action name") final String action,
                    @QueryParam("lang") @DefaultValue("en") @Parameter(name = "language", in = QUERY,
                            description = "the requested language (as in a Locale) if supported by the action",
                            schema = @Schema(defaultValue = "en", type = STRING)) final String lang,
                    @RequestBody(description = "the action parameters as a flat map of strings", required = true,
                            content = @Content(mediaType = APPLICATION_JSON,
                                    schema = @Schema(type = OBJECT))) final Map<String, String> params) {
        if (action == null) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorPayload(ErrorDictionary.ACTION_MISSING, "Action can't be null"))
                    .build());
        }
        final ServiceMeta.ActionMeta actionMeta = actionDao.findBy(family, type, action);
        if (actionMeta == null) {
            throw new WebApplicationException(Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.ACTION_MISSING, "No action with id '" + action + "'"))
                    .build());
        }
        try {
            final Map<String, String> runtimeParams = ofNullable(params).map(HashMap::new).orElseGet(HashMap::new);
            runtimeParams.put("$lang", localeMapper.mapLocale(lang).getLanguage());
            final Object result = actionMeta.getInvoker().apply(runtimeParams);
            return Response.ok(result).type(APPLICATION_JSON_TYPE).build();
        } catch (final RuntimeException re) {
            log.warn(re.getMessage(), re);
            throw new WebApplicationException(Response
                    .status(520, "Unexpected callback error")
                    .entity(new ErrorPayload(ErrorDictionary.ACTION_ERROR,
                            "Action execution failed with: " + ofNullable(re.getMessage())
                                    .orElseGet(() -> NullPointerException.class.isInstance(re) ? "unexpected null"
                                            : "no error message")))
                    .build());
        }
    }

    @GET
    @Path("index") // add an index if needed or too slow
    @Operation(
            description = "This endpoint returns the list of available actions for a certain family and potentially filters the "
                    + "output limiting it to some families and types of actions.")
    @APIResponse(responseCode = "200", description = "The action index.",
            content = @Content(mediaType = APPLICATION_JSON))
    public ActionList getIndex(
            @QueryParam("type") @Parameter(name = "type", in = QUERY,
                    description = "the types of actions") final String[] types,
            @QueryParam("family") @Parameter(name = "family", in = QUERY,
                    description = "the families") final String[] families,
            @QueryParam("language") @Parameter(name = "language", description = "the language to use", in = QUERY,
                    schema = @Schema(defaultValue = "en", type = STRING)) @DefaultValue("en") final String language) {
        final Predicate<ServiceMeta.ActionMeta> typeMatcher = new Predicate<ServiceMeta.ActionMeta>() {

            private final Collection<String> accepted = new HashSet<>(asList(types));

            @Override
            public boolean test(final ServiceMeta.ActionMeta actionMeta) {
                return accepted.isEmpty() || accepted.contains(actionMeta.getType());
            }
        };
        final Predicate<ServiceMeta.ActionMeta> componentMatcher = new Predicate<ServiceMeta.ActionMeta>() {

            private final Collection<String> accepted = new HashSet<>(asList(families));

            @Override
            public boolean test(final ServiceMeta.ActionMeta actionMeta) {
                return accepted.isEmpty() || accepted.contains(actionMeta.getFamily());
            }
        };
        final Locale locale = localeMapper.mapLocale(language);
        return new ActionList(manager
                .find(c -> c
                        .get(ContainerComponentRegistry.class)
                        .getServices()
                        .stream()
                        .map(s -> s.getActions().stream())
                        .flatMap(identity())
                        .filter(typeMatcher.and(componentMatcher))
                        .map(s -> new ActionItem(s.getFamily(), s.getType(), s.getAction(),
                                propertiesService
                                        .buildProperties(s.getParameters(), c.getLoader(), locale, null)
                                        .collect(toList()))))
                .collect(toList()));
    }
}
