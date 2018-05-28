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
package org.talend.sdk.component.proxy.client;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.server.front.model.ComponentIndices;

@ApplicationScoped
public class ComponentClient {

    @Inject
    private WebTarget webTarget;

    @Inject
    private ProxyConfiguration configuration;

    public CompletionStage<ComponentIndices> getAllComponents(final String language) {
        return configuration
                .getHeaderAppender()
                .apply(this.webTarget
                        .path("component/index")
                        .queryParam("language", language)
                        .queryParam("includeIconContent", false)
                        .request(MediaType.APPLICATION_JSON_TYPE))
                .rx()
                .get(ComponentIndices.class);
    }

    public CompletionStage<Response> getFamilyIconById(final String id) {
        return configuration
                .getHeaderAppender()
                .apply(this.webTarget.path("component/icon/family/" + id).request())
                .rx()
                .get();
    }

}
