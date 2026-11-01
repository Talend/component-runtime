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
package org.talend.sdk.component.tcketl.domain.property;

import java.util.List;
import java.util.Map;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * A service call attached to a property. {@code type} is one of {@link ActionTypes}, {@code name} the action
 * identifier inside the family (possibly empty for a schema discovery). {@code parameters} are the references, as
 * declared, to the properties sent as the request body, in the order of the action's parameters. {@code attributes}
 * hold the other declared settings, e.g. {@code after}, {@code activeIf} or {@code labelDisplayMode}.
 */
public record ActionBinding(String type, String name, List<String> parameters, Map<String, String> attributes) {

    public ActionBinding {
        Preconditions.requireNonBlank(type, "type");
        name = Preconditions.nullToEmpty(name);
        parameters = Preconditions.immutableCopy(parameters, "parameters");
        attributes = Preconditions.sortedCopy(attributes, "attributes");
    }

    public ActionBinding(final String type, final String name) {
        this(type, name, List.of(), Map.of());
    }

    public String attribute(final String key) {
        return attributes.getOrDefault(key, "");
    }

    /** Name of the sibling after which an update button is displayed. */
    public String after() {
        return attribute("after");
    }

    public String activeIf() {
        return attribute("activeIf");
    }
}
