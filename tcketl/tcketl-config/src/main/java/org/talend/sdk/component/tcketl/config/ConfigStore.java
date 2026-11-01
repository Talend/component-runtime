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

import java.io.IOException;
import java.nio.file.Path;

/** Where the {@link ApplicationConfig} is kept. */
public interface ConfigStore {

    /** The location of the settings, whether or not it exists yet. */
    Path file();

    /** Reads the settings. Never fails: see {@link ConfigLoadResult}. */
    ConfigLoadResult load();

    /** Replaces the stored settings. */
    void save(ApplicationConfig config) throws IOException;
}
