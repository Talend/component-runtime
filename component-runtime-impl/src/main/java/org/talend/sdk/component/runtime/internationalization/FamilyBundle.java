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
package org.talend.sdk.component.runtime.internationalization;

import java.util.Optional;
import java.util.ResourceBundle;

// a simple facade to get standardized properties of a family
public class FamilyBundle extends InternalBundle {

    public FamilyBundle(final ResourceBundle bundle, final String prefix) {
        super(new ResourceBundle[] { bundle }, prefix);
    }

    public Optional<String> category(final String value) {
        return readValue(value + "._category");
    }

    public Optional<String> displayName() {
        return readValue("_displayName");
    }

    public Optional<String> actionDisplayName(final String type, final String name) {
        return readRawValue(prefix + "actions." + type + "." + name + "._displayName");
    }

    public Optional<String> configurationDisplayName(final String type, final String name) {
        return readValue(type + "." + name + "._displayName");
    }
}
