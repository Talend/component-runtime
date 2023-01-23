/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;

import lombok.Data;

/**
 * An object representing a Server.
 */
@Data
public class Server {

    /**
     * REQUIRED. A URL to the target host.
     * This URL supports Server Variables and MAY be relative, to indicate that the host location
     * is relative to the location where the OpenAPI document is being served. Variable
     * substitutions will be made when a variable is named in {brackets}.
     */
    private String url;

    /**
     * An optional string describing the host designated by the URL.
     * CommonMark syntax MAY be used for rich text representation.
     */
    @JsonbProperty(nillable = true)
    private String description;

    /**
     * A map between a variable name and its value. The value is used for substitution in
     * the server's URL template.
     */
    @JsonbProperty(nillable = true)
    private Map<String, ServerVariable> variables;

}
