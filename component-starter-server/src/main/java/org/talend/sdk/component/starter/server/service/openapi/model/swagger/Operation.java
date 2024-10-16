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
package org.talend.sdk.component.starter.server.service.openapi.model.swagger;

import java.util.List;
import java.util.Map;

import javax.json.JsonObject;

import org.talend.sdk.component.starter.server.service.openapi.model.common.OperationBase;
import org.talend.sdk.component.starter.server.service.openapi.model.openapi.Parameter;
import org.talend.sdk.component.starter.server.service.openapi.model.openapi.Response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Operation extends OperationBase {

    /**
     * A list of MIME types the operation can consume. This overrides the consumes definition at the Swagger Object. An
     * empty value MAY be used to clear the global definition. Value MUST be as described under Mime Types.
     */
    private List<String> consumes;

    /**
     * A list of MIME types the operation can produce. This overrides the produces definition at the Swagger Object. An
     * empty value MAY be used to clear the global definition. Value MUST be as described under Mime Types.
     */
    private List<String> produces;

    /**
     * A list of parameters that are applicable for this operation. If a parameter is already defined at the Path Item,
     * the new definition will override it, but can never remove it. The list MUST NOT include duplicated parameters. A
     * unique parameter is defined by a combination of a name and location. The list can use the Reference Object to
     * link to parameters that are defined at the Swagger Object's parameters. There can be one "body" parameter at
     * most.
     */
    private List<Parameter> parameters;

    /**
     * Required. The list of possible responses as they are returned from executing this operation.
     */
    private Map<String, Response> responses;

    /**
     * The transfer protocol for the operation. Values MUST be from the list: "http", "https", "ws", "wss". The value
     * overrides the Swagger Object schemes definition.
     */
    private List<String> schemes;

    /**
     * A declaration of which security schemes are applied for this operation. The list of values describes alternative
     * security schemes that can be used (that is, there is a logical OR between the security requirements). This
     * definition overrides any declared top-level security. To remove a top-level security declaration, an empty array
     * can be used.
     */
    private List<JsonObject> security;

}
