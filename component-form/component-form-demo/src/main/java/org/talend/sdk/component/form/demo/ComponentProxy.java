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
package org.talend.sdk.component.form.demo;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.form.api.ActionService;
import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.api.WebException;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.UiActionResult;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentIndices;

@Path("proxy")
@ApplicationScoped
public class ComponentProxy {

    @Inject
    private Client client;

    @Inject
    private UiSpecService uiSpecService;

    @Inject
    private ActionService actionService;

    @POST
    @Path("action")
    public Map<String, Object> action(@QueryParam("family") final String family, @QueryParam("type") final String type,
            @QueryParam("action") final String action, final Map<String, Object> params) {
        try {
            return actionService.map(type, client.action(family, type, action, params));
        } catch (final WebException exception) {
            final UiActionResult payload = actionService.map(exception);
            throw new WebApplicationException(Response.status(exception.getStatus()).entity(payload).build());
        }
    }

    @GET
    @Path("index")
    public ComponentIndices getIndex(@QueryParam("language") @DefaultValue("en") final String language) {
        final ComponentIndices index = client.index(language);
        // our mapping is a bit different so rewrite links
        index.getComponents().stream().flatMap(c -> c.getLinks().stream()).forEach(
                link -> link.setPath(link.getPath().replaceFirst("\\/component\\/", "\\/proxy\\/").replace(
                        "/details?identifiers=", "/detail/")));
        return index;
    }

    @GET
    @Path("detail/{id}") // bulk mode to avoid to fetch components one by one when reloading a
                         // pipeline/job
    public Ui getDetail(@QueryParam("language") @DefaultValue("en") final String language,
            @PathParam("id") final String id) {
        final List<ComponentDetail> details = client.details(language, id, new String[0]).getDetails();
        if (details.isEmpty()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        return uiSpecService.convert(details.iterator().next());
    }
}
