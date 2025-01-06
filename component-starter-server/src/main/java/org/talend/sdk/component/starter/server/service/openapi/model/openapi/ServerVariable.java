/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
 * An object representing a Server Variable for server URL template substitution.
 */
@Data
public class ServerVariable {

    /**
     * An enumeration of string values to be used if the substitution options are from a limited set.
     * The array SHOULD NOT be empty.
     */
    @JsonbProperty(value = "enum")
    private List<String> enumValue;

    /**
     * <strong>REQUIRED</strong>. The default value to use for substitution,
     * which SHALL be sent if an alternate value is not supplied. Note this behavior
     * is different than the Schema Object's treatment of default values, because
     * in those cases parameter values are optional.
     * If the enum is defined, the value SHOULD exist in the enum's values.
     */
    @JsonbProperty("default")
    private String defaultValue;

    /**
     * An optional description for the server variable.
     * CommonMark syntax MAY be used for rich text representation.
     */
    private String description;

}
