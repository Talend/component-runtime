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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * A service the component server can execute for a family. {@code properties} describe the request body: every
 * parameter and what is nested below it.
 */
public record ActionDefinition(
        String family,
        String type,
        String name,
        String displayName,
        List<PropertyDefinition> properties
) {

    public ActionDefinition {
        family = Preconditions.nullToEmpty(family);
        Preconditions.requireNonBlank(type, "type");
        Preconditions.requireNonBlank(name, "name");
        displayName = Preconditions.orDefault(displayName, name);
        properties = Preconditions.immutableCopy(properties, "properties");
    }

    /** The parameters of the action in the order the service declares them. */
    public List<PropertyDefinition> parameters() {
        return properties
                .stream()
                .filter(PropertyDefinition::isParameter)
                .sorted(Comparator.comparingInt(PropertyDefinition::parameterIndex))
                .collect(Collectors.toUnmodifiableList());
    }
}
