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

import javax.json.bind.annotation.JsonbProperty;

import lombok.Data;

@Data
public class Info {

    /**
     * REQUIRED. The title of the API.
     */
    private String title;

    /**
     * A short description of the API. CommonMark syntax MAY be used for rich text representation.
     */
    @JsonbProperty(nillable = true)
    private String description;

    /**
     * A URL to the Terms of Service for the API. MUST be in the format of a URL.
     */
    @JsonbProperty(nillable = true)
    private String termsOfService;

    /**
     * The contact information for the exposed API.
     */
    @JsonbProperty(nillable = true)
    private Contact contact;

    /**
     * The license information for the exposed API.
     */
    @JsonbProperty(nillable = true)
    private License license;

    /**
     * REQUIRED. The version of the OpenAPI document (which is distinct from the OpenAPI Specification version or the
     * API implementation version).
     */
    private String version;

}
