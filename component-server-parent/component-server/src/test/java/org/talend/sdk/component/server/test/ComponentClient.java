/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.test;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;

import org.talend.sdk.component.server.front.model.ComponentIndices;

@ApplicationScoped
public class ComponentClient {

    @Inject
    private WebTarget base;

    public ComponentIndices fetchIndex() {
        return base
                .path("component/index")
                .queryParam("includeIconContent", true)
                .request(APPLICATION_JSON_TYPE)
                .header("Accept-Encoding", "gzip")
                .get(ComponentIndices.class);
    }

    public String getComponentId(final String family, final String component) {
        return fetchIndex()
                .getComponents()
                .stream()
                .filter(c -> c.getId().getFamily().equals(family) && c.getId().getName().equals(component))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no " + family + "#" + component + " component"))
                .getId()
                .getId();
    }

    public String getFamilyId(final String family) {
        return fetchIndex()
                .getComponents()
                .stream()
                .filter(c -> c.getId().getFamily().equals(family))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no " + family))
                .getId()
                .getFamilyId();
    }

    public String getJdbcId() {
        return getComponentId("jdbc", "input");
    }

    public String getBeamSampleId() {
        return getComponentId("beamsample", "Input");
    }

    public String getStandaloneId() {
        return getComponentId("chain", "standalone");
    }

    public String getStreamingId() {
        return getComponentId("streaming", "infinite");
    }

    public String getStreamingStoppableId() {
        return getComponentId("streaming", "infiniteStoppable");
    }
}
