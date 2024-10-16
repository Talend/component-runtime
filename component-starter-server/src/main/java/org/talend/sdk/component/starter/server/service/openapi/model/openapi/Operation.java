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
package org.talend.sdk.component.starter.server.service.openapi.model.openapi;

import java.util.List;
import java.util.Map;

import javax.json.JsonValue;

import org.talend.sdk.component.starter.server.service.openapi.model.common.OperationBase;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Describes a single API operation on a path.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Operation extends OperationBase {

    /**
     * A list of parameters that are applicable for this operation. If a parameter is already defined at the Path Item,
     * the new definition will override it but can never remove it. The list MUST NOT include duplicated parameters. A
     * unique parameter is defined by a combination of a name and location. The list can use the Reference Object to
     * link to parameters that are defined at the OpenAPI Object's components/parameters.
     */
    private List<Parameter> parameters;

    /**
     * The request body applicable for this operation. The requestBody is only supported in HTTP methods where the HTTP
     * 1.1 specification RFC7231 has explicitly defined semantics for request bodies. In other cases where the HTTP spec
     * is vague, requestBody SHALL be ignored by consumers.
     */
    private RequestBody requestBody;

    /**
     * REQUIRED. The list of possible responses as they are returned from executing this operation.
     *
     */
    private Map<String, Response> responses;

    /**
     * A map of possible out-of band callbacks related to the parent operation. The key is a unique identifier for the
     * Callback Object. Each value in the map is a Callback Object that describes a request that may be initiated by the
     * API provider and the expected responses.
     */
    private Map<String, Map<String, Operation>> callbacks;

    /**
     * A declaration of which security mechanisms can be used for this operation. The list of values includes
     * alternative security requirement objects that can be used. Only one of the security requirement objects need to
     * be satisfied to authorize a request. To make security optional, an empty security requirement ({}) can be
     * included in the array. This definition overrides any declared top-level security. To remove a top-level security
     * declaration, an empty array can be used.
     *
     */
    private List<JsonValue> security;

    /**
     * An alternative server array to service all operations in this path.
     */
    List<Server> servers;

}
