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

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * Marks an object property as a reusable configuration type of its family, such as a {@code datastore} or a
 * {@code dataset}. Both values are empty when the property is not one.
 */
public record ConfigurationTypeRef(String type, String name) {

    public static final ConfigurationTypeRef NONE = new ConfigurationTypeRef("", "");

    public ConfigurationTypeRef {
        type = Preconditions.nullToEmpty(type);
        name = Preconditions.nullToEmpty(name);
    }

    public boolean isPresent() {
        return !type.isEmpty();
    }

    public boolean isType(final String expected) {
        return type.equals(expected);
    }
}
