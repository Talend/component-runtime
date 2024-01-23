/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service.path;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PathHandlerImpl implements PathHandler {

    // maven property regexp
    private static final Pattern MVN_PROPERTY = Pattern.compile("^\\$\\{(.*)\\}(.*)");

    // interpolate predicate regexp
    private static final Pattern INTERPOLATE_P = Pattern.compile("^(\\$\\{(.*)\\}|~|/.:).*");

    /**
     * Potentially interpolate or modify part of the path to have a full parsable path for the framework.
     *
     * @param path the value to inspect and eventually modify.
     *
     * @return a {@link Path} with interpolated elements.
     */
    protected Path interpolate(final String path) {
        String p = path;
        // skip this directly if not needed...
        if (INTERPOLATE_P.matcher(path).matches()) {
            // windows
            if (p.startsWith("/") && p.indexOf(':') == 2) {
                p = p.substring(1);
            }
            // unix ~ : we shouldn't have the case as it's shell parsed only normally
            if (p.startsWith("~")) {
                p = System.getProperty("user.home") + p.substring(1);
            }
            // parse any maven property : more likely to be used
            final Matcher matcher = MVN_PROPERTY.matcher(p);
            if (matcher.matches()) {
                final String prop = matcher.group(1);
                String value;
                if (prop.startsWith("env.")) {
                    value = System.getenv(prop.substring("env.".length()));
                } else {
                    value = System.getProperty(prop);
                }
                p = value + matcher.group(2);
            }
        }

        try {
            return Paths.get(p);
        } catch (InvalidPathException e) {
            log.debug("[PathHandlerImpl] not valid path: {}.", p);
            return null;
        }
    }

    /**
     * Check path existence
     *
     * @param path value to check
     *
     * @return null if path do not exist, path otherwise
     */
    protected Path exist(final Path path) {
        if (path != null && Files.exists(path)) {
            return path;
        }
        log.debug("[PathHandlerImpl] non existent path: {}.", path);

        return null;
    }

    @Override
    public Path get(final String path) {
        return exist(interpolate(path));
    }
}
