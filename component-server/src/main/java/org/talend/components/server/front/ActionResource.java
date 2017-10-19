// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.server.front;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.Collection;
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

import org.talend.components.runtime.manager.ComponentManager;
import org.talend.components.runtime.manager.ContainerComponentRegistry;
import org.talend.components.runtime.manager.ServiceMeta;
import org.talend.components.server.front.model.ActionItem;
import org.talend.components.server.front.model.ActionList;
import org.talend.components.server.front.model.ErrorDictionary;
import org.talend.components.server.front.model.error.ErrorPayload;
import org.talend.components.server.service.ComponentManagerService;
import org.talend.components.server.service.LocaleMapper;
import org.talend.components.server.service.PropertiesService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("action")
@ApplicationScoped
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ActionResource {

    @Inject
    private ComponentManager manager;

    @Inject
    private ComponentManagerService service;

    @Inject
    private PropertiesService propertiesService;

    @Inject
    private LocaleMapper localeMapper;

    @POST
    @Path("execute")
    public Response execute(@QueryParam("family") final String component, @QueryParam("type") final String type,
            @QueryParam("action") final String action, final Map<String, String> params) {
        if (action == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorPayload(ErrorDictionary.ACTION_MISSING, "Action can't be null")).build());
        }
        final ServiceMeta.ActionMeta actionMeta = service.findActionById(component, type, action);
        if (actionMeta == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.ACTION_MISSING, "No action with id '" + action + "'")).build());
        }
        try {
            final Object result = actionMeta.getInvoker().apply(params);
            return Response.ok(result).type(APPLICATION_JSON_TYPE).build();
        } catch (final RuntimeException re) {
            log.warn(re.getMessage(), re);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorPayload(ErrorDictionary.ACTION_ERROR, "Action execution failed with: " + re.getMessage()))
                    .build());
        }
    }

    @GET
    @Path("index") // add an index if needed or too slow
    public ActionList getIndex(@QueryParam("type") final String[] types, @QueryParam("family") final String[] components,
            @QueryParam("language") @DefaultValue("en") final String language) {
        final Predicate<ServiceMeta.ActionMeta> typeMatcher = new Predicate<ServiceMeta.ActionMeta>() {

            private final Collection<String> accepted = new HashSet<>(asList(types));

            @Override
            public boolean test(final ServiceMeta.ActionMeta actionMeta) {
                return accepted.isEmpty() || accepted.contains(actionMeta.getType());
            }
        };
        final Predicate<ServiceMeta.ActionMeta> componentMatcher = new Predicate<ServiceMeta.ActionMeta>() {

            private final Collection<String> accepted = new HashSet<>(asList(components));

            @Override
            public boolean test(final ServiceMeta.ActionMeta actionMeta) {
                return accepted.isEmpty() || accepted.contains(actionMeta.getFamily());
            }
        };
        final Locale locale = localeMapper.mapLocale(language);
        return new ActionList(manager
                .find(c -> c.get(ContainerComponentRegistry.class).getServices().stream().map(s -> s.getActions().stream())
                        .flatMap(identity()).filter(typeMatcher.and(componentMatcher))
                        .map(s -> new ActionItem(s.getFamily(), s.getType(), s.getAction(),
                                propertiesService.buildProperties(s.getParameters(), c.getLoader(), locale).collect(toList()))))
                .collect(toList()));
    }
}
