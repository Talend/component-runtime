/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.front;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@Path("demo")
public class ApiDemoEndpoints {

    private static final String RES_ROOT = "demo";

    private static final String RES_VERSION = "/{version}";

    private static final String PATH_ENVIRONMENT = "/api/v1/environment";

    private static final String PATH_CACHE_RESET = "/api/v1/cache/clear";

    private static final String PATH_ACTION_INDEX = "/api/v1/action/index";

    private static final String PATH_ACTION_EXECUTE = "/api/v1/action/execute";

    private static final String PATH_COMPONENT_INDEX = "/api/v1/component/index";

    private static final String PATH_COMPONENT_DETAILS = "/api/v1/component/details";

    private static final String PATH_COMPONENT_DEPENDENCIES = "/api/v1/component/dependencies";

    private static final String PATH_COMPONENT_DEPENDENCY = "/api/v1/component/dependency";

    private static final String PATH_COMPONENT_ICON = "/api/v1/component/icon";

    private static final String PATH_COMPONENT_ICON_FAMILY = "/api/v1/component/icon/family";

    private static final String PATH_COMPONENT_MIGRATE = "/api/v1/component/migrate";

    private static final String PATH_CONFIGURATIONTYPE_INDEX = "/api/v1/configurationtype/index";

    private static final String PATH_CONFIGURATIONTYPE_DETAILS = "/api/v1/configurationtype/details";

    private static final String PATH_CONFIGURATIONTYPE_MIGRATE = "/api/v1/configurationtype/migrate";

    public static final String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    public static final String HEADER_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    public static final String HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    public static final String VALUE_ACCESS_CONTROL_ALLOW_HEADERS = "Content-Type, api_key, Authorization";

    public static final String VALUE_ACCESS_CONTROL_ALLOW_ORIGIN = "*";

    public static final String VALUE_ACCESS_CONTROL_ALLOW_METHODS = "GET, POST, DELETE, PUT, PATCH, OPTIONS";

    private final byte[] ENVIRONMENT;

    private final Object ACTION_INDEX;

    private final Object ACTION_EXECUTE;

    private final Object COMPONENT_INDEX;

    private final Object COMPONENT_DETAILS;

    private final Object COMPONENT_DEPENDENCIES;

    private final Object COMPONENT_DEPENDENCY;

    private final Object COMPONENT_ICON;

    private final Object COMPONENT_ICON_FAMILY;

    private final Object COMPONENT_MIGRATE;

    private final Object CONFIGURATIONTYPE_INDEX;

    private final Object CONFIGURATIONTYPE_DETAILS;

    private final Object CONFIGURATIONTYPE_MIGRATE;

    public ApiDemoEndpoints() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ENVIRONMENT = getResponseContent(loader, RES_ROOT + PATH_ENVIRONMENT);
        ACTION_INDEX = getResponseContent(loader, RES_ROOT + PATH_ACTION_INDEX);
        ACTION_EXECUTE = getResponseContent(loader, RES_ROOT + PATH_ACTION_EXECUTE);
        COMPONENT_INDEX = getResponseContent(loader, RES_ROOT + PATH_COMPONENT_INDEX);
        COMPONENT_DETAILS = getResponseContent(loader, RES_ROOT + PATH_COMPONENT_DETAILS);
        COMPONENT_DEPENDENCIES = getResponseContent(loader, RES_ROOT + PATH_COMPONENT_DEPENDENCIES);
        COMPONENT_DEPENDENCY = getResponseContent(loader, RES_ROOT + PATH_COMPONENT_DEPENDENCY);
        COMPONENT_ICON =
                getResponseContent(loader, RES_ROOT + PATH_COMPONENT_ICON + "/Y29tcG9uZW50cyNNb2NrI01vY2tJbnB1dA");
        COMPONENT_ICON_FAMILY =
                getResponseContent(loader, RES_ROOT + PATH_COMPONENT_ICON_FAMILY + "/Y29tcG9uZW50cyNNb2Nr");
        COMPONENT_MIGRATE =
                getResponseContent(loader, RES_ROOT + PATH_COMPONENT_MIGRATE + "/Y29tcG9uZW50cyNNb2NrI01vY2tJbnB1dA/1");
        CONFIGURATIONTYPE_INDEX = getResponseContent(loader, RES_ROOT + PATH_CONFIGURATIONTYPE_INDEX);
        CONFIGURATIONTYPE_DETAILS = getResponseContent(loader, RES_ROOT + PATH_CONFIGURATIONTYPE_DETAILS);
        CONFIGURATIONTYPE_MIGRATE = getResponseContent(loader,
                RES_ROOT + PATH_CONFIGURATIONTYPE_MIGRATE + "/Y29tcG9uZW50cyNNb2NrI2RhdGFzZXQjdGFibGU/1");
    }

    private byte[] getResponseContent(final ClassLoader loader, final String request) {
        try (final InputStream stream = loader.getResourceAsStream(request)) {
            final byte[] result = new byte[stream.available()];
            stream.read(result);
            log.debug(String.format("%s - %d bytes.", request, result.length));
            return result;
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    @GET
    @Path(RES_VERSION + PATH_ENVIRONMENT)
    @Produces({ APPLICATION_JSON })
    public Response environment(@PathParam("version") final String version) {
        JsonObject env = Json
                .createObjectBuilder()
                .add("commit", "daf300ad598d186007007984481df02a5b21dcaa")
                .add("lastUpdated", "2020-12-18T18:06:03.43Z[UTC]")
                .add("latestApiVersion", 1)
                .add("time", ZonedDateTime.now().toString())
                .add("version", version)
                .build();
        return Response
                .ok(env)
                .header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, VALUE_ACCESS_CONTROL_ALLOW_ORIGIN)
                .header(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS)
                .type(APPLICATION_JSON_TYPE)
                .build();
    }

    @GET
    @Path(RES_VERSION + PATH_CACHE_RESET)
    public Response clearCaches() {
        return Response.noContent().build();
    }

    @GET
    @Path(RES_VERSION + PATH_ACTION_INDEX)
    @Produces({ APPLICATION_JSON })
    public Response actionIndex(@PathParam("version") final String version, @QueryParam("type") final String[] types,
            @QueryParam("family") final String[] families, @QueryParam("language") final String lang) {
        return Response
                .ok(ACTION_INDEX)
                .header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, VALUE_ACCESS_CONTROL_ALLOW_ORIGIN)
                .header(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS)
                .type(APPLICATION_JSON_TYPE)
                .build();
    }

    @POST
    @Path(RES_VERSION + PATH_ACTION_EXECUTE)
    @Produces({ APPLICATION_JSON })
    public Response actionExecute(@PathParam("version") final String version, @QueryParam("family") final String family,
            @QueryParam("type") final String type, @QueryParam("action") final String action,
            @QueryParam("lang") final String lang) {
        return Response
                .ok(ACTION_EXECUTE)
                .header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, VALUE_ACCESS_CONTROL_ALLOW_ORIGIN)
                .header(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS)
                .type(APPLICATION_JSON_TYPE)
                .build();
    }

    @GET
    @Path(RES_VERSION + PATH_COMPONENT_INDEX)
    @Produces({ APPLICATION_JSON })
    public Response componentIndex(@PathParam("version") final String version,
            @QueryParam("language") @DefaultValue("en") final String language,
            @QueryParam("includeIconContent") @DefaultValue("false") final boolean includeIconContent,
            @QueryParam("q") final String query) {
        return Response
                .ok(COMPONENT_INDEX)
                .header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, VALUE_ACCESS_CONTROL_ALLOW_ORIGIN)
                .header(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS)
                .type(APPLICATION_JSON_TYPE)
                .build();
    }

    @GET
    @Path(RES_VERSION + PATH_COMPONENT_DETAILS)
    @Produces({ APPLICATION_JSON })
    public Response componentDetails(@PathParam("version") final String version,
            @QueryParam("language") @DefaultValue("en") final String language,
            @QueryParam("identifiers") final String[] ids) {
        return Response
                .ok(COMPONENT_DETAILS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, VALUE_ACCESS_CONTROL_ALLOW_ORIGIN)
                .header(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS)
                .type(APPLICATION_JSON_TYPE)
                .build();
    }

    @GET
    @Path(RES_VERSION + PATH_COMPONENT_DEPENDENCIES)
    @Produces({ APPLICATION_JSON })
    public Response componentDependencies(@PathParam("version") final String version,
            @QueryParam("identifier") final String[] ids) {
        return Response
                .ok(COMPONENT_DEPENDENCIES)
                .header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, VALUE_ACCESS_CONTROL_ALLOW_ORIGIN)
                .header(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS)
                .type(APPLICATION_JSON_TYPE)
                .build();
    }

    @GET
    @Path(RES_VERSION + PATH_COMPONENT_DEPENDENCY + "/{id}")
    @Produces({ APPLICATION_OCTET_STREAM })
    public Response componentDependency(@PathParam("version") final String version, @PathParam("id") final String id) {
        return Response
                .ok(COMPONENT_DEPENDENCIES)
                .header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, VALUE_ACCESS_CONTROL_ALLOW_ORIGIN)
                .header(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS)
                .type(APPLICATION_OCTET_STREAM_TYPE)
                .build();
    }

    @GET
    @Path(RES_VERSION + PATH_COMPONENT_ICON + "/{id}")
    @Produces({ APPLICATION_OCTET_STREAM })
    public Response componentIcon(@PathParam("version") final String version, @PathParam("id") final String id) {
        return Response
                .ok(COMPONENT_ICON)
                .header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, VALUE_ACCESS_CONTROL_ALLOW_ORIGIN)
                .header(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS)
                .type(APPLICATION_OCTET_STREAM_TYPE)
                .build();
    }

    @GET
    @Path(RES_VERSION + PATH_COMPONENT_ICON_FAMILY + "/{id}")
    @Produces({ APPLICATION_OCTET_STREAM })
    public Response componentIconFamily(@PathParam("version") final String version, @PathParam("id") final String id) {
        return Response
                .ok(COMPONENT_ICON_FAMILY)
                .header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, VALUE_ACCESS_CONTROL_ALLOW_ORIGIN)
                .header(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS)
                .type(APPLICATION_OCTET_STREAM_TYPE)
                .build();
    }

    @POST
    @Path(RES_VERSION + PATH_COMPONENT_MIGRATE + "/{id}/{configurationVersion}")
    @Produces({ APPLICATION_JSON })
    public Response componentMigrate(@PathParam("version") final String version, @PathParam("id") final String id,
            @PathParam("configurationVersion") final int migrateVersion, final Map<String, String> config) {
        return Response
                .ok(COMPONENT_MIGRATE)
                .header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, VALUE_ACCESS_CONTROL_ALLOW_ORIGIN)
                .header(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS)
                .type(APPLICATION_JSON_TYPE)
                .build();
    }

    @GET
    @Path(RES_VERSION + PATH_CONFIGURATIONTYPE_INDEX)
    @Produces({ APPLICATION_JSON })
    public Response configurationtypeIndex(@PathParam("version") final String version,
            @QueryParam("language") @DefaultValue("en") final String language,
            @QueryParam("lightPayload") @DefaultValue("true") final boolean lightPayload,
            @QueryParam("q") final String query) {
        return Response
                .ok(CONFIGURATIONTYPE_INDEX)
                .header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, VALUE_ACCESS_CONTROL_ALLOW_ORIGIN)
                .header(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS)
                .type(APPLICATION_JSON_TYPE)
                .build();
    }

    @GET
    @Path(RES_VERSION + PATH_CONFIGURATIONTYPE_DETAILS)
    @Produces({ APPLICATION_JSON })
    public Response configurationtypeDetails(@PathParam("version") final String version,
            @QueryParam("language") @DefaultValue("en") final String language,
            @QueryParam("identifiers") final String[] ids) {
        return Response
                .ok(CONFIGURATIONTYPE_DETAILS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, VALUE_ACCESS_CONTROL_ALLOW_ORIGIN)
                .header(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS)
                .type(APPLICATION_JSON_TYPE)
                .build();
    }

    @POST
    @Path(RES_VERSION + PATH_CONFIGURATIONTYPE_MIGRATE + "/{id}/{configurationVersion}")
    @Produces({ APPLICATION_JSON })
    public Response configurationtypeMigrate(@PathParam("version") final String version,
            @PathParam("id") final String id, @PathParam("configurationVersion") final int migrateVersion,
            final Map<String, String> config) {
        return Response
                .ok(CONFIGURATIONTYPE_MIGRATE)
                .header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, VALUE_ACCESS_CONTROL_ALLOW_ORIGIN)
                .header(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS)
                .header(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS)
                .type(APPLICATION_JSON_TYPE)
                .build();
    }
}