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

import javax.json.bind.annotation.JsonbProperty;

import lombok.Data;

@Data
public class Contact {

    /**
     * The identifying name of the contact person/organization.
     */
    @JsonbProperty(nillable = true)
    private String name;

    /**
     * The URL pointing to the contact information. MUST be in the format of a URL.
     */
    @JsonbProperty(nillable = true)
    private String url;

    /**
     * The email address of the contact person/organization. MUST be in the format of an email address.
     */
    @JsonbProperty(nillable = true)
    private String email;

}
