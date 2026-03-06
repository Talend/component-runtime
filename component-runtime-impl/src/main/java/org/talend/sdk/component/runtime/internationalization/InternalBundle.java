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

import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
abstract class InternalBundle {

    private final ResourceBundle[] bundles;

    protected final String prefix;

    protected Optional<String> readValue(final String key) {
        return readRawValue(prefix + key);
    }

    protected Optional<String> readRawValue(final String key) {
        final Optional<String> value =
                Stream.of(bundles).filter(b -> b.containsKey(key)).map(b -> b.getString(key)).findFirst();
        if (value.isPresent() || !key.contains("${index}")) {
            return value;
        }
        return readRawValue(key.replace("${index}", ""));
    }
}
