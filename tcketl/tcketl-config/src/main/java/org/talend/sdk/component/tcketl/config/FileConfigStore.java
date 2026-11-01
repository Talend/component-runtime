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
import java.nio.file.Files;
import java.nio.file.Path;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.persistence.FileFormatException;
import org.talend.sdk.component.tcketl.persistence.JsonFiles;

/**
 * {@link ConfigStore} backed by one JSON file. A file that cannot be used (invalid JSON, invalid value, unsupported
 * format version, unreadable) is left untouched: the defaults are used until the user saves new settings.
 */
public final class FileConfigStore implements ConfigStore {

    private final Path file;

    public FileConfigStore(final Path file) {
        this.file = Preconditions.requireNonNull(file, "file").toAbsolutePath().normalize();
    }

    @Override
    public Path file() {
        return file;
    }

    @Override
    public ConfigLoadResult load() {
        if (Files.notExists(file)) {
            return ConfigLoadResult.loaded(ApplicationConfig.defaults());
        }
        try {
            return ConfigLoadResult
                    .loaded(JsonFiles.readOptionalVersion(file, ConfigFile.class, ConfigFile.CURRENT).toConfig());
        } catch (final IOException e) {
            return ConfigLoadResult
                    .fallback(ApplicationConfig.defaults(),
                            "Cannot use the settings file " + file + " (" + reason(e)
                                    + "). Default settings are used.");
        }
    }

    @Override
    public void save(final ApplicationConfig config) throws IOException {
        JsonFiles.write(file, ConfigFile.from(Preconditions.requireNonNull(config, "config")));
    }

    private static String reason(final IOException error) {
        if (error instanceof FileFormatException) {
            return error.getMessage();
        }
        final String type = error.getClass().getSimpleName();
        return error.getMessage() == null ? type : type + ": " + error.getMessage();
    }
}
