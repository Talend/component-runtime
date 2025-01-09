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

import java.util.Map;

import javax.json.JsonObject;

import lombok.Data;

@Data
public class Components {
    /*
     * schemas Map[string, javax.xml.validation.Schema Object | Reference Object] An object to hold reusable Schema
     * Objects.
     * responses Map[string, Response Object | Reference Object] An object to hold reusable Response Objects.
     * parameters Map[string, Parameter Object | Reference Object] An object to hold reusable Parameter Objects.
     * examples Map[string, Example Object | Reference Object] An object to hold reusable Example Objects.
     * requestBodies Map[string, Request Body Object | Reference Object] An object to hold reusable Request Body
     * Objects.
     * headers Map[string, Header Object | Reference Object] An object to hold reusable Header Objects.
     * securitySchemes Map[string, Security Scheme Object | Reference Object] An object to hold reusable Security Scheme
     * Objects.
     * links Map[string, Link Object | Reference Object] An object to hold reusable Link Objects.
     * callbacks Map[string, Callback Object | Reference Object] An object to hold reusable Callback Objects.
     *
     */

    /**
     * An object to hold reusable Schema Objects.
     *
     * Map[string, Schema Object | Reference Object]
     */
    private Map<String, JsonObject> schemas;

    private Map<String, JsonObject> responses;

    private Map<String, JsonObject> parameters;

    private Map<String, JsonObject> examples;

    private Map<String, JsonObject> requestBodies;

    private Map<String, JsonObject> headers;

    private Map<String, JsonObject> securitySchemes;

    private Map<String, JsonObject> links;

    private Map<String, JsonObject> callbacks;

}
