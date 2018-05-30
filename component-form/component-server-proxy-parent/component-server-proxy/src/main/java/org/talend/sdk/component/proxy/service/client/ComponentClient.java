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

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentIndices;

@ApplicationScoped
public class ComponentClient {

    @Inject
    @UiSpecProxy
    private WebTarget webTarget;

    @Inject
    private ProxyConfiguration configuration;

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

    public CompletionStage<Response> getFamilyIconById(final String id,
            final Function<String, String> placeholderProvider) {
        return configuration
                .getHeaderAppender()
                .apply(this.webTarget.path("component/icon/family/" + id).request(), placeholderProvider)
                .rx()
                .get();
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
                .thenApply(l -> l.getDetails().iterator().next());
    }

    public CompletionStage<Response> getComponentIconById(final Function<String, String> placeholderProvider,
            final String id) {
        return configuration
                .getHeaderAppender()
                .apply(this.webTarget.path("component/icon/" + id).request(), placeholderProvider)
                .rx()
                .get();
    }

}
