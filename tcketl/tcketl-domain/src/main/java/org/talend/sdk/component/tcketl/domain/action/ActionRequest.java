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
package org.talend.sdk.component.tcketl.domain.action;

import java.util.Map;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * One service call: the technical family of the component, the action {@code type} and {@code name}, and the flat
 * request body (keys are the option names of the action method, e.g. {@code datastore.url}).
 */
public record ActionRequest(String family, String type, String name, Map<String, String> body) {

    public ActionRequest {
        Preconditions.requireNonBlank(family, "family");
        Preconditions.requireNonBlank(type, "type");
        Preconditions.requireNonBlank(name, "name");
        body = Preconditions.sortedCopy(body, "body");
    }
}
