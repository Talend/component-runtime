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

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Finds the settings file. The first source that is set wins:
 * <ol>
 * <li>the command line option {@code --config <file>} or {@code --config=<file>} (the last occurrence wins);</li>
 * <li>the environment variable {@code TCKETL_CONFIG};</li>
 * <li>the per-user default: {@code %APPDATA%\tcketl\config.json} on Windows, otherwise
 * {@code $XDG_CONFIG_HOME/tcketl/config.json} when that variable holds an absolute path, else
 * {@code ~/.config/tcketl/config.json}.</li>
 * </ol>
 * The result is always an absolute, normalized path; the file itself may not exist yet.
 */
public final class ConfigLocator {

    public static final String OPTION = "--config";

    public static final String ENVIRONMENT_VARIABLE = "TCKETL_CONFIG";

    private static final String APPLICATION_DIRECTORY = "tcketl";

    private static final String FILE_NAME = "config.json";

    private ConfigLocator() {
        // static helpers only
    }

    /**
     * @param arguments the raw command line arguments, other options are ignored
     * @throws IllegalArgumentException when {@code --config} has no value or a path is invalid
     */
    public static Path locate(final List<String> arguments) {
        return locate(arguments, System.getenv(), System.getProperty("os.name"),
                Path.of(System.getProperty("user.home")));
    }

    static Path locate(final List<String> arguments, final Map<String, String> environment, final String osName,
            final Path userHome) {
        final String fromOption = fromOption(arguments);
        if (fromOption != null) {
            return absolute(fromOption, OPTION);
        }
        final String fromEnvironment = environment.get(ENVIRONMENT_VARIABLE);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return absolute(fromEnvironment, ENVIRONMENT_VARIABLE);
        }
        return userConfigDirectory(environment, osName, userHome)
                .resolve(APPLICATION_DIRECTORY)
                .resolve(FILE_NAME)
                .toAbsolutePath()
                .normalize();
    }

    private static String fromOption(final List<String> arguments) {
        String value = null;
        for (int i = 0; i < arguments.size(); i++) {
            final String argument = arguments.get(i);
            if (OPTION.equals(argument)) {
                value = requireValue(i + 1 < arguments.size() ? arguments.get(i + 1) : null);
                i++;
            } else if (argument.startsWith(OPTION + "=")) {
                value = requireValue(argument.substring(OPTION.length() + 1));
            }
        }
        return value;
    }

    private static String requireValue(final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(OPTION + " requires a file path");
        }
        return value;
    }

    private static Path userConfigDirectory(final Map<String, String> environment, final String osName,
            final Path userHome) {
        if (osName != null && osName.toLowerCase(Locale.ROOT).startsWith("windows")) {
            final String appData = environment.get("APPDATA");
            return appData == null || appData.isBlank() ? userHome.resolve("AppData").resolve("Roaming")
                    : path(appData, "APPDATA");
        }
        final String xdg = environment.get("XDG_CONFIG_HOME");
        if (xdg != null && !xdg.isBlank() && path(xdg, "XDG_CONFIG_HOME").isAbsolute()) {
            return path(xdg, "XDG_CONFIG_HOME");
        }
        return userHome.resolve(".config");
    }

    private static Path absolute(final String value, final String source) {
        return path(value, source).toAbsolutePath().normalize();
    }

    private static Path path(final String value, final String source) {
        try {
            return Path.of(value.strip());
        } catch (final InvalidPathException e) {
            throw new IllegalArgumentException("Invalid path in " + source + ": " + e.getReason(), e);
        }
    }
}
