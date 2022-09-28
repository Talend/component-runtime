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
package org.talend.sdk.component.runtime.internationalization;

import java.util.Optional;
import java.util.ResourceBundle;

// a simple facade to get standardized properties of a component
public class ComponentBundle extends InternalBundle {

    public ComponentBundle(final ResourceBundle bundle, final String prefix) {
        super(new ResourceBundle[] { bundle }, prefix);
    }

    public Optional<String> displayName() {
        return readValue("_displayName");
    }

    public Optional<String> displayName(final String meta) {
        return readValue(meta + "._displayName");
    }

    public Optional<String> documentation() {
        return readValue("_documentation");
    }
}
