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
package org.talend.sdk.component.proxy.service.client;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.proxy.service.ErrorProcessor;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentIndices;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;

@ApplicationScoped
public class ComponentClient {

    @Inject
    @UiSpecProxy
    private WebTarget webTarget;

    @Inject
    private ProxyConfiguration configuration;

    private final GenericType<Map<String, ErrorPayload>> multipleErrorType =
            new GenericType<Map<String, ErrorPayload>>() {

            };

    public CompletionStage<ComponentIndices> getAllComponents(final String language,
            final Function<String, String> placeholderProvider) {
        return configuration
                .getHeaderAppender()
                .apply(this.webTarget
                        .path("component/index")
                        .queryParam("language", language)
                        .queryParam("includeIconContent", false)
                        .request(MediaType.APPLICATION_JSON_TYPE), placeholderProvider)
                .rx()
                .get(ComponentIndices.class);
    }

    public CompletionStage<byte[]> getFamilyIconById(final String id,
            final Function<String, String> placeholderProvider) {
        return configuration
                .getHeaderAppender()
                .apply(this.webTarget.path("component/icon/family/" + id).request(APPLICATION_OCTET_STREAM),
                        placeholderProvider)
                .rx()
                .get()
                .thenApply(r -> {
                    if (r.getStatus() != 200) {
                        throw new WebApplicationException(r); // propagate as error
                    }
                    return r;
                })
                .thenApply(r -> r.readEntity(byte[].class));
    }

    public CompletionStage<ComponentDetail> getComponentDetail(final String language,
            final Function<String, String> placeholderProvider, final String id) {
        return configuration
                .getHeaderAppender()
                .apply(this.webTarget
                        .path("component/details")
                        .queryParam("language", language)
                        .queryParam("identifiers", id)
                        .request(MediaType.APPLICATION_JSON_TYPE), placeholderProvider)
                .rx()
                .get(ComponentDetailList.class)
                .handle((r, e) -> {
                    if (e != null) {
                        // unwrap multiple error to single one as we request only one component
                        final WebApplicationException error =
                                WebApplicationException.class.cast(ErrorProcessor.unwrap(e));
                        Map<String, ErrorPayload> errorDetails = error.getResponse().readEntity(multipleErrorType);
                        throw new WebApplicationException(
                                Response.status(error.getResponse().getStatus()).entity(errorDetails.get(id)).build());
                    }

                    return r;
                })
                .thenApply(l -> l.getDetails().iterator().next());
    }

    public CompletionStage<byte[]> getComponentIconById(final Function<String, String> placeholderProvider,
            final String id) {
        return configuration
                .getHeaderAppender()
                .apply(this.webTarget.path("component/icon/" + id).request(APPLICATION_OCTET_STREAM),
                        placeholderProvider)
                .rx()
                .get()
                .thenApply(r -> {
                    if (r.getStatus() != 200) {
                        throw new WebApplicationException(r); // propagate as error
                    }
                    return r;
                })
                .thenApply(r -> r.readEntity(byte[].class));
    }

}
