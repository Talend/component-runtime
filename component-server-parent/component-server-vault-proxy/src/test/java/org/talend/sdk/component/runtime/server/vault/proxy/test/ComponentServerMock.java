/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.server.vault.proxy.test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.server.front.model.ActionItem;
import org.talend.sdk.component.server.front.model.ActionList;
import org.talend.sdk.component.server.front.model.BulkRequests;
import org.talend.sdk.component.server.front.model.BulkResponses;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.Connectors;
import org.talend.sdk.component.server.front.model.DocumentationContent;
import org.talend.sdk.component.server.front.model.Environment;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

// todo: replace with actual component-server?
@Path("mock/component")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ComponentServerMock {

    @GET
    @Path("documentation/component/{id}")
    public DocumentationContent getDoc(@PathParam("id") final String id) {
        return new DocumentationContent(id, id);
    }

    @GET
    @Path("component/details")
    public ComponentDetailList component(@QueryParam("language") final String language,
            @QueryParam("identifiers") final String[] ids) {
        return new ComponentDetailList(singletonList(
                new ComponentDetail(null, null, null, null, 1, createProperties(), null, null, null, null, null)));
    }

    @GET
    @Path("configurationtype/details")
    public ConfigTypeNodes configuration(@QueryParam("language") final String language,
            @QueryParam("identifiers") final String[] ids) {
        return new ConfigTypeNodes(singletonMap(ids[0],
                new ConfigTypeNode(null, 1, null, null, null, null, null, createProperties(), null)));
    }

    @GET
    @Path("action/index")
    public ActionList getIndex(@QueryParam("type") final String[] types, @QueryParam("family") final String[] families,
            @QueryParam("language") final String language) {
        return new ActionList(singletonList(
                new ActionItem(families.length == 0 ? "testf" : families[0], "testt", "testa", createProperties())));
    }

    @GET
    @Path("environment")
    public Environment environment() {
        return new Environment(1, "test", "test", null, new Date(0), new Connectors("1.2.3"));
    }

    @POST
    @Path("action/execute")
    public Map<String, String> execute(@QueryParam("family") final String family, @QueryParam("type") final String type,
            @QueryParam("action") final String action, @QueryParam("lang") final String lang,
            final Map<String, String> params) {
        final Map<String, String> out = new HashMap<>(params);
        out.put("family", family);
        out.put("type", type);
        out.put("action", action);
        out.put("lang", lang);
        return out;
    }

    @POST
    @Path("bulk")
    public BulkResponses execute(final BulkRequests requests) {
        if (requests.getRequests().size() != 2) {
            throw new BadRequestException();
        }
        return new BulkResponses(asList(
                new BulkResponses.Result(Response.Status.OK.getStatusCode(), emptyMap(),
                        "ok".getBytes(StandardCharsets.UTF_8)),
                new BulkResponses.Result(Response.Status.BAD_REQUEST.getStatusCode(), emptyMap(),
                        "bad".getBytes(StandardCharsets.UTF_8))));
    }

    private List<SimplePropertyDefinition> createProperties() {
        return asList(
                new SimplePropertyDefinition("configuration", "configuration", null, "OBJECT", null, null, emptyMap(),
                        null, null),
                new SimplePropertyDefinition("configuration.username", "username", null, "STRING", null, null,
                        emptyMap(), null, null),
                new SimplePropertyDefinition("configuration.password", "password", null, "STRING", null, null,
                        singletonMap("ui::credential", "true"), null, null));
    }
}
