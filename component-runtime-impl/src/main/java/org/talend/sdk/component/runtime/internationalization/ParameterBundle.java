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
package org.talend.sdk.component.runtime.internationalization;

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class ParameterBundle extends InternalBundle {

    private final String[] simpleNames;

    public ParameterBundle(final ResourceBundle[] bundles, final String prefix, final String... simpleNames) {
        super(bundles, prefix);
        this.simpleNames = simpleNames;
    }

    public Optional<String> documentation(final ParameterBundle parent) {
        return get(parent, "_documentation", false);
    }

    public Optional<String> displayName(final ParameterBundle parent) {
        return get(parent, "_displayName", false);
    }

    public Optional<String> enumDisplayName(final ParameterBundle parent, final String enumName) {
        return get(parent, enumName + "._displayName", true);
    }

    public Optional<String> placeholder(final ParameterBundle parent) {
        return get(parent, "_placeholder", false);
    }

    public Optional<String> gridLayoutName(final ParameterBundle parent, final String tabName) {
        return get(parent, "_gridlayout." + tabName + "._displayName", false);
    }

    private Optional<String> get(final ParameterBundle parentBundle, final String suffix,
            final boolean stripLastSegment) {
        Optional<String> v = readValue(suffix);
        if (!v.isPresent()) {
            v = Stream
                    .of(simpleNames)
                    .map(s -> !stripLastSegment || s.lastIndexOf('.') < 0 ? s : s.substring(0, s.lastIndexOf('.')))
                    .map(s -> {
                        final String k = s + "." + suffix;
                        return readRawValue(k)
                                .orElse(parentBundle == null ? null : parentBundle.readRawValue(k).orElse(null));
                    })
                    .filter(Objects::nonNull)
                    .findFirst();
        }

        return v;
    }
}
