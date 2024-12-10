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

import javax.json.JsonObject;
import javax.json.bind.annotation.JsonbProperty;

import org.talend.sdk.component.starter.server.service.openapi.model.common.ParameterBase;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Parameter extends ParameterBase {

    /**
     * Required. The type of the parameter. Since the parameter is not located at the request body, it is limited to
     * simple types (that is, not an object). The value MUST be one of "string", "number", "integer", "boolean", "array"
     * or "file". If type is "file", the consumes MUST be either "multipart/form-data", "
     * application/x-www-form-urlencoded" or both and the parameter MUST be in "formData".
     */
    private String type;

    /**
     * The extending format for the previously mentioned type. See Data Type Formats for further details.
     */
    private String format;

    /**
     * Declares the value of the parameter that the server will use if none is provided, for example a "count" to
     * control the number of results per page might default to 100 if not supplied by the client in the request. (Note:
     * "default" has no meaning for required parameters.) See
     * https://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-6.2. Unlike JSON Schema this value MUST
     * conform to the defined type for this parameter.
     */
    @JsonbProperty("default")
    private JsonObject defaultValue;
}
