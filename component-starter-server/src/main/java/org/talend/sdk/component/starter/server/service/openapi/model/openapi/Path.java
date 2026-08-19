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

import java.util.List;

import javax.json.bind.annotation.JsonbProperty;

import lombok.Data;

/**
 * A relative path to an individual endpoint. The field name MUST begin with a forward slash (/). The path is appended
 * (no relative URL resolution) to the expanded URL from the Server Object's url field in order to construct the full
 * URL. Path templating is allowed. When matching URLs, concrete (non-templated) paths would be matched before their
 * templated counterparts. Templated paths with the same hierarchy but different templated names MUST NOT exist as they
 * are identical. In case of ambiguous matching, it's up to the tooling to decide which one to use.
 */
@Data
public class Path {

    /*
     * $ref string Allows for an external definition of this path item. The referenced structure MUST be in the format
     * of a Path Item Object. In case a Path Item Object field appears both in the defined object and the referenced
     * object, the behavior is undefined.
     * summary string An optional, string summary, intended to apply to all operations in this path.
     * description string An optional, string description, intended to apply to all operations in this path. CommonMark
     * syntax MAY be used for rich text representation.
     * get Operation Object A definition of a GET operation on this path.
     * put Operation Object A definition of a PUT operation on this path.
     * post Operation Object A definition of a POST operation on this path.
     * delete Operation Object A definition of a DELETE operation on this path.
     * options Operation Object A definition of a OPTIONS operation on this path.
     * head Operation Object A definition of a HEAD operation on this path.
     * patch Operation Object A definition of a PATCH operation on this path.
     * trace Operation Object A definition of a TRACE operation on this path.
     * servers [Server Object] An alternative server array to service all operations in this path.
     * parameters [Parameter Object | Reference Object] A list of parameters that are applicable for all the operations
     * described under this path. These parameters can be overridden at the operation level, but cannot be removed
     * there. The list MUST NOT include duplicated parameters. A unique parameter is defined by a combination of a name
     * and location. The list can use the Reference Object to link to parameters that are defined at the OpenAPI
     * Object's components/parameters.
     */

    /**
     * Allows for an external definition of this path item. The referenced structure MUST be in the format of a Path
     * Item Object. In case a Path Item Object field appears both in the defined object and the referenced object, the
     * behavior is undefined.
     */
    @JsonbProperty("$ref")
    private String ref;

    /**
     * An optional, string summary, intended to apply to all operations in this path.
     */
    private String summary;

    /**
     * An optional, string description, intended to apply to all operations in this path. CommonMark syntax MAY be used
     * for rich text representation.
     */
    private String description;

    /**
     *
     */

    /**
     * An alternative server array to service all operations in this path.
     */
    List<Server> servers;

}
