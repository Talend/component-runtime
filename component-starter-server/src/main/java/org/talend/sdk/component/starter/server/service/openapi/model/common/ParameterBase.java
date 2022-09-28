/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.service.openapi.model.common;

import javax.json.JsonObject;

import lombok.Data;

/**
 * Describes a single operation parameter.
 *
 * A unique parameter is defined by a combination of a name and location.
 * Parameter Locations
 *
 * There are four possible parameter locations specified by the in field:
 * <ul>
 * <li>path - Used together with Path Templating, where the parameter value is actually part of the operation's URL.
 * This does not include the host or base path of the API. For example, in /items/{itemId}, the path parameter is
 * itemId.</li>
 * <li>query - Parameters that are appended to the URL. For example, in /items?id=###, the query parameter is id.</li>
 * <li>header - Custom headers that are expected as part of the request. Note that RFC7230 states header names are case
 * insensitive.</li>
 * <li>cookie - Used to pass a specific cookie value to the API.</li>
 * </ul>
 */
@Data
public class ParameterBase {

    /**
     * REQUIRED. The name of the parameter. Parameter names are case sensitive.
     * <ul>
     * <li>If in is "path", the name field MUST correspond to a template expression occurring within the path field in
     * the Paths Object. See Path Templating for further information.</li>
     * <li>If in is "header" and the name field is "Accept", "Content-Type" or "Authorization", the parameter definition
     * SHALL be ignored.</li>
     * <li>For all other cases, the name corresponds to the parameter name used by the in property.</li>
     * </ul>
     */
    private String name;

    /**
     * REQUIRED. The location of the parameter. Possible values are "query", "header", "path" or "cookie".
     */
    private String in;

    /**
     * A brief description of the parameter. This could contain examples of use. CommonMark syntax MAY be used for rich
     * text representation.
     *
     */
    private String description;

    /**
     * Determines whether this parameter is mandatory. If the parameter location is "path", this property is REQUIRED
     * and its value MUST be true. Otherwise, the property MAY be included and its default value is false.
     */
    private boolean required;

    /**
     * Specifies that a parameter is deprecated and SHOULD be transitioned out of usage. Default value is false.
     *
     */
    private boolean deprecated;

    /**
     * Sets the ability to pass empty-valued parameters. This is valid only for query parameters and allows sending a
     * parameter with an empty value. Default value is false. If style is used, and if behavior is n/a (cannot be
     * serialized), the value of allowEmptyValue SHALL be ignored. Use of this property is NOT RECOMMENDED, as it is
     * likely to be removed in a later revision.
     */
    private boolean allowEmptyValue;

    /**
     * Required. The schema defining the type used for the body parameter.
     */
    private JsonObject schema;

}
