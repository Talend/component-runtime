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
package org.talend.sdk.component.tcketl.web;

import java.nio.file.Path;

import org.talend.sdk.component.tcketl.config.ApplicationConfig;
import org.talend.sdk.component.tcketl.config.ConfigLoadResult;
import org.talend.sdk.component.tcketl.config.ConfigStore;

/** Keeps the settings of one browser tab for as long as it lives: there is no settings file in the web version. */
final class MemoryConfigStore implements ConfigStore {

    private ApplicationConfig config;

    MemoryConfigStore(final ApplicationConfig initial) {
        this.config = initial;
    }

    @Override
    public Path file() {
        return Path.of("memory");
    }

    @Override
    public ConfigLoadResult load() {
        return ConfigLoadResult.loaded(config);
    }

    @Override
    public void save(final ApplicationConfig newConfig) {
        this.config = newConfig;
    }
}
