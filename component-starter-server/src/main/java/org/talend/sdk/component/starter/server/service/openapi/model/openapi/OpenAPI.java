/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.service.openapi.model.openapi;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;

import org.talend.sdk.component.starter.server.service.openapi.model.ApiModel;
import org.talend.sdk.component.starter.server.service.openapi.model.common.ExternalDocs;
import org.talend.sdk.component.starter.server.service.openapi.model.common.Info;
import org.talend.sdk.component.starter.server.service.openapi.model.common.Tag;

import lombok.Data;

/**
 * This is the root document object for the OAS 3.0 (OpenAPI specification).
 */
@Data
public class OpenAPI implements ApiModel {

    /**
     * REQUIRED. This string MUST be the semantic version number of the OpenAPI Specification version that the OpenAPI
     * document uses. The openapi field SHOULD be used by tooling specifications and clients to interpret the OpenAPI
     * document. This is not related to the API info.version string.
     */
    private String openapi;

    /**
     * REQUIRED. Provides metadata about the API. The metadata MAY be used by tooling as required.
     */
    private Info info;

    /**
     * An array of Server Objects, which provide connectivity information to a target server. If the servers property is
     * not provided, or is an empty array, the default value would be a Server Object with a url value of /.
     */
    @JsonbProperty(nillable = true)
    private List<Server> servers;

    /**
     * REQUIRED. The available paths and operations for the API.
     */
    private Map<String, Map<String, Operation>> paths;

    /**
     * An element to hold various schemas for the specification.
     */
    private Components components;

    /**
     * A declaration of which security mechanisms can be used across the API. The list of values includes alternative
     * security requirement objects that can be used. Only one of the security requirement objects need to be satisfied
     * to authorize a request. Individual operations can override this definition. To make security optional, an empty
     * security requirement ({}) can be included in the array.
     */
    // @JsonbProperty(nillable = true)
    // private List<Security> security;

    /**
     * A list of tags used by the specification with additional metadata. The order of the tags can be used to reflect
     * on their order by the parsing tools. Not all tags that are used by the Operation Object must be declared. The
     * tags that are not declared MAY be organized randomly or based on the tools' logic. Each tag name in the list MUST
     * be unique.
     */
    @JsonbProperty(nillable = true)
    private List<Tag> tags;

    /**
     * Additional external documentation.
     */
    @JsonbProperty(nillable = true)
    private ExternalDocs externalDocs;

    @Override
    public String getDefaultUrl() {
        return ofNullable(getServers()).map(ss -> ss.stream()
                .map(s -> s.getUrl())
                .map(url -> {
                    if (url.startsWith("/")) {
                        return "https://localhost" + url;
                    }
                    return url;
                })
                .findFirst()
                .orElse("https://localhost"))
                .orElse("https://localhost");
    }
}
