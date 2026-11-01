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
package org.talend.sdk.component.tcketl.config;

import java.util.Optional;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * Outcome of reading the settings. Reading never fails: when the file is missing the defaults are used silently, and
 * when it cannot be used the defaults are used together with a {@code warning} to show to the user.
 */
public record ConfigLoadResult(ApplicationConfig config, Optional<String> warning) {

    public ConfigLoadResult {
        Preconditions.requireNonNull(config, "config");
        Preconditions.requireNonNull(warning, "warning");
    }

    public static ConfigLoadResult loaded(final ApplicationConfig config) {
        return new ConfigLoadResult(config, Optional.empty());
    }

    public static ConfigLoadResult fallback(final ApplicationConfig config, final String warning) {
        return new ConfigLoadResult(config, Optional.of(Preconditions.requireNonBlank(warning, "warning")));
    }
}
