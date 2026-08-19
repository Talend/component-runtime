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
package org.talend.sdk.component.starter.server.service.openapi.model.common;

import java.util.List;

import lombok.Data;

@Data
public class OperationBase {

    /**
     * A list of tags for API documentation control. Tags can be used for logical grouping of operations by resources or
     * any other qualifier.
     */
    private List<String> tags;

    /**
     * A short summary of what the operation does.
     */
    private String summary;

    /**
     * A verbose explanation of the operation behavior. CommonMark syntax MAY be used for rich text representation.
     */
    private String description;

    /**
     * Additional external documentation for this operation.
     */
    private ExternalDocs externalDocs;

    /**
     * Unique string used to identify the operation. The id MUST be unique among all operations described in the API.
     * The operationId value is case-sensitive. Tools and libraries MAY use the operationId to uniquely identify an
     * operation, therefore, it is RECOMMENDED to follow common programming naming conventions.
     */
    private String operationId;

    /**
     * Declares this operation to be deprecated. Consumers SHOULD refrain from usage of the declared operation. Default
     * value is false.
     */
    private boolean deprecated;

}
