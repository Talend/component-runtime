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
package org.talend.sdk.component.starter.server.service.openapi.model.swagger;

import static java.util.Optional.ofNullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.json.JsonObject;

import org.talend.sdk.component.starter.server.service.openapi.model.ApiModel;
import org.talend.sdk.component.starter.server.service.openapi.model.common.ExternalDocs;
import org.talend.sdk.component.starter.server.service.openapi.model.common.Info;
import org.talend.sdk.component.starter.server.service.openapi.model.common.Tag;

import lombok.Data;

/**
 * This is the root document object for the OAS 2.0 (OpenAPI specification).
 */
@Data
public class SwaggerAPI implements ApiModel {

    /**
     * Required. Specifies the Swagger Specification version being used. It can be used by the Swagger UI and other
     * clients to interpret the API listing. The value MUST be "2.0".
     */
    private String swagger;

    /**
     * Required. Provides metadata about the API. The metadata can be used by the clients if needed.
     */
    private Info info;

    /**
     * The host (name or ip) serving the API. This MUST be the host only and does not include the scheme nor sub-paths.
     * It MAY include a port. If the host is not included, the host serving the documentation is to be used (including
     * the port). The host does not support path templating.
     */
    private String host;

    /**
     * The base path on which the API is served, which is relative to the host. If it is not included, the API is served
     * directly under the host. The value MUST start with a leading slash (/). The basePath does not support path
     * templating.
     */
    private String basePath;

    /**
     * The transfer protocol of the API. Values MUST be from the list: "http", "https", "ws", "wss". If the schemes is
     * not included, the default scheme to be used is the one used to access the Swagger definition itself.
     */
    private List<String> schemes;

    /**
     * A list of MIME types the APIs can consume. This is global to all APIs but can be overridden on specific API
     * calls. Value MUST be as described under Mime Types.
     */
    private List<String> consumes;

    /**
     * Required. The available paths and operations for the API.
     *
     * A relative path to an individual endpoint. The field name MUST begin with a slash. The path is appended to the
     * basePath in order to construct the full URL. Path templating is allowed.
     */
    private Map<String, Map<String, Operation>> paths;

    /**
     * An object to hold data types produced and consumed by operations.
     */
    private Map<String, JsonObject> definitions;

    /**
     * A list of tags used by the specification with additional metadata. The order of the tags can be used to reflect
     * on their order by the parsing tools. Not all tags that are used by the Operation Object must be declared. The
     * tags that are not declared may be organized randomly or based on the tools' logic. Each tag name in the list MUST
     * be unique.
     */
    private List<Tag> tags;

    /**
     * Additional external documentation.
     */
    private ExternalDocs externalDocs;

    @Override
    public String getDefaultUrl() {
        final String host = ofNullable(getHost()).orElse("localhost");
        final String basePath = ofNullable(getBasePath()).orElse("");
        final String protocol = getSchemes()
                .stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .filter(s -> !s.startsWith("ws"))
                .sorted(Comparator.reverseOrder())
                .findFirst()
                .orElse("https");
        return String.format("%s://%s%s", protocol, host, basePath);
    }
}
