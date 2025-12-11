/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import org.talend.sdk.component.runtime.manager.service.path.PathHandler;
import org.talend.sdk.component.runtime.manager.service.path.PathHandlerImpl;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * m2 discovery process is used for plugins/connectors loading.
 * <p>
 * The default discovery process is at it follows:
 * <ul>
 * <li>read {@code talend.component.manager.m2.repository} system property</li>
 * <li>check if we're running in studio and uses its m2 if system property is not set to maven.repository=global</li>
 * <li>parse maven's {@code settings.xml} (or if provided {@code talend.component.manager.m2.settings}) for checking if
 * a property localRepository is defined</li>
 * <li>checking for maven env properties (ie $M2_HOME)</li>
 * <li>fallbacks to ${user.home}/.m2/repository</li>
 * </ul>
 */
@Slf4j
@NoArgsConstructor
@Data
public class MavenRepositoryDefaultResolver implements MavenRepositoryResolver {

    // Path.of("/") => "C:\" on windows
    public static final Path NON_EXISTENT_PATH = Path.of("/").resolve(UUID.randomUUID().toString());

    private PathHandler handler = new PathHandlerImpl();

    private boolean hasFallback = Boolean.getBoolean("talend.component.manager.m2.fallback");

    @Override
    public Path discover() {
        return Stream
                .of(fromSystemProperties(), fromStudioConfiguration(), fromMavenSettings(), fromEnvironmentVariables())
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(this::fallback);
    }

    @Override
    public Path fallback() {
        log.debug("[fallback::{}] default to user's default repository.", hasFallback);
        if (hasFallback) {
            return Paths.get(USER_HOME).resolve(M2_REPOSITORY);
        } else {
           log.debug("[fallback] Use a non-existing repository: {}", NON_EXISTENT_PATH);
           return NON_EXISTENT_PATH;
        }
    }

    public Path fromSystemProperties() {
        final String m2 = System.getProperty(TALEND_COMPONENT_MANAGER_M2_REPOSITORY);
        if (m2 != null) {
            return handler.get(m2);
        }
        log.debug("[fromSystemProperties] Could not get m2 from System property.");
        return null;
    }

    private Path fromStudioConfiguration() {
        // check if we are in the studio process if so just grab the studio config
        final String m2Repo = System.getProperty(STUDIO_MVN_REPOSITORY);
        if (!"global".equals(m2Repo)) {
            final String osgi = System.getProperty("osgi.configuration.area", "");
            try {
                return osgi != null && osgi.startsWith("file") ? handler.get(Paths.get(new URI(osgi)).toString())
                        : handler.get(Paths.get(osgi, M2_REPOSITORY).toString());
            } catch (java.net.URISyntaxException e) {
                log.debug("[fromStudioConfiguration] Could not get m2 from studio config." + e.getMessage());
                return null;
            }
        }
        log.debug("[fromStudioConfiguration] Could not get m2 from studio config.");
        return null;
    }

    private static String parseSettings(final Path settings) {
        log.debug("[fromMavenSettings] searching for localRepository location in {}", settings);
        try {
            String localM2RepositoryFromSettings = null;
            // do not introduce a xml parser so will do with what we have...
            final String content = new String(java.nio.file.Files.readAllBytes(settings), StandardCharsets.UTF_8);
            // some cleanups
            String stripped = XML_COMMENTS_PATTERN.matcher(content).replaceAll("");
            stripped = XML_EMPTY_LINES_PATTERN.matcher(stripped).replaceAll("");
            // localRepository present?
            final Matcher m = XML_LOCAL_REPO_PATTERN.matcher(stripped);
            if (!m.matches()) {
                log.debug("[fromMavenSettings] localRepository not defined in settings.xml");
            } else {
                localM2RepositoryFromSettings = m.group(1).trim();
                if (localM2RepositoryFromSettings != null && !localM2RepositoryFromSettings.isEmpty()) {
                    return localM2RepositoryFromSettings;
                }
            }
        } catch (final Exception ignore) {
            // fallback on default local path
        }
        return null;
    }

    public Path fromMavenSettings() {
        if (hasFallback) {
            return Stream.of(
                    Optional.ofNullable(System.getProperty(TALEND_COMPONENT_MANAGER_M2_SETTINGS))
                            .map(Paths::get)
                            .orElse(null), //
                    Optional.ofNullable(System.getProperty("user.home"))
                            .map(it -> Paths.get(it, M2_SETTINGS))
                            .orElse(null),
                    Optional.ofNullable(System.getenv(MAVEN_HOME)).map(it -> Paths.get(it, CONF_SETTINGS)).orElse(null),
                    Optional.ofNullable(System.getenv(M2_HOME)).map(it -> Paths.get(it, CONF_SETTINGS)).orElse(null))
                    .filter(Objects::nonNull)
                    .filter(Files::exists)
                    .map(MavenRepositoryDefaultResolver::parseSettings)
                    .filter(Objects::nonNull)
                    .map(handler::get)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        } else {
            return null;
        }
    }

    public Path fromEnvironmentVariables() {
        if (hasFallback) {
            final String vm2 = System.getenv(M2_HOME);
            log.debug("[fromEnvironmentVariables] M2_HOME={}", vm2);
            if (vm2 != null) {
                return handler.get(Paths.get(vm2, "repository").toString());
            }
            log.debug("[fromEnvironmentVariables] Could not get m2 from environment.");
        }
        return null;
    }

}