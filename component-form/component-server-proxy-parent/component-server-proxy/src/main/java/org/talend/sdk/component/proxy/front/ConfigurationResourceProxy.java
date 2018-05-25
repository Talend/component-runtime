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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import org.talend.sdk.component.proxy.client.ConfigurationProxyClient;
import org.talend.sdk.component.proxy.service.ConfigurationService;

@ApplicationScoped
@Path("configuration")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ConfigurationResourceProxy {

    @Inject
    private ConfigurationProxyClient configurationProxyService;

    @Inject
    private ConfigurationService configurationService;

    /**
     * Return all available Data store in component server
     */
    @GET
    @Path("roots")
    public void getRootConfig(@Suspended final AsyncResponse response,
            @QueryParam("language") @DefaultValue("en") final String language) {

        configurationProxyService
                .getAllConfigurations(language)
                .thenApply(configurationService::getRootConfiguration)
                .handle((configurations, throwable) -> handleResponse(response, configurations, throwable));
    }

    /**
     * Return configuration icon by id
     */
    @GET
    @Path("icon/{id}")
    public void getConfigurationIconById(@Suspended final AsyncResponse response, @PathParam("id") final String id) {
        configurationProxyService.getConfigurationIconById(id).handle(
                (icon, throwable) -> handleResponse(response, icon, throwable));
    }

    private Void handleResponse(final AsyncResponse response, final Object result, final Throwable throwable) {
        if (throwable != null) {
            response.resume(handleException(throwable));
        } else {
            response.resume(result);
        }
        return null;
    }

    private Throwable handleException(final Throwable throwable) {
        return throwable;
    }
}
