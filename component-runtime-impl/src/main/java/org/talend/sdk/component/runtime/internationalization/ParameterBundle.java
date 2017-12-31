/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.internationalization;

import java.util.Optional;
import java.util.ResourceBundle;

public class ParameterBundle extends InternalBundle {

    private final String simpleName;

    public ParameterBundle(final ResourceBundle bundle, final String prefix, final String simpleName) {
        super(bundle, prefix);
        this.simpleName = simpleName;
    }

    public Optional<String> displayName() {
        return readValue("_displayName");
    }

    public Optional<String> displayName(final String child) {
        return readRawValue(simpleName + "." + child + "._displayName");
    }
}
