/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.talend.sdk.component.runtime.manager.service.path.PathHandler;
import org.talend.sdk.component.runtime.manager.service.path.PathHandlerImpl;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * m2 discovery process is used for plugins/connectors loading.
 *
 * The default discovery process is at it follows:
 * <ul>
 * <li>read {@code talend.component.manager.m2.repository} system property</li>
 * <li>check if we're running in studio and uses its m2 if system property is not set to maven.repository=global</li>
 * <li>parse maven's {@code settings.xml} (or if provided {@code talend.component.manager.m2.settings}) for checking if
 * a property localRepositor is defined</li>
 * <li>checking for maven env properties (ie $M2_HOME)</li>
 * <li>fallbacks to ${user.home}/.m2/repository</li>
 * </ul>
 */
@Slf4j
@NoArgsConstructor
public class MavenRepositoryDefaultResolver implements MavenRepositoryResolver {

    private final PathHandler handler = new PathHandlerImpl();

    @Override
    public Path discover() {
        return Stream
                .of(fromSystemProperties(), fromStudioConfiguration(), fromMavenSettings(), fromEnvironmentVariables())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(fallback());
    }

    @Override
    public Path fallback() {
        log.warn("[fallback] default to user's default repository.");
        return handler.get(System.getProperty("user.home", "")).resolve(".m2/repository");
    }

    public Path fromSystemProperties() {
        final String m2 = System.getProperty(TALEND_COMPONENT_MANAGER_M2_REPOSITORY);
        if (m2 != null) {
            final Path m2Path = handler.get(m2);
            log.warn("[fromSystemProperties] {}={}.", TALEND_COMPONENT_MANAGER_M2_REPOSITORY, m2);
            if (java.nio.file.Files.exists(m2Path)) {
                return m2Path;
            } else {
                log.warn("[fromSystemProperties] {} value points to a non existent path: {}.",
                        TALEND_COMPONENT_MANAGER_M2_REPOSITORY,
                        m2Path);
            }
        }
        log.warn("[fromSystemProperties] Could not get m2 from System property.");
        return null;
    }

    private Path fromStudioConfiguration() {
        // check if we are in the studio process if so just grab the the studio config
        final String m2Repo = System.getProperty(STUDIO_MVN_REPOSITORY);
        if (!"global".equals(m2Repo)) {
            final Path m2 = handler.get(System.getProperty("osgi.configuration.area", ""))
                    .resolve(M2_REPOSITORY);
            if (java.nio.file.Files.exists(m2)) {
                return m2;
            }
        }
        log.warn("[fromStudioConfiguration] Could not get m2 from studio config.");
        return null;
    }

    public Path fromMavenSettings() {
        final Path settings = handler
                .get(System.getProperty(TALEND_COMPONENT_MANAGER_M2_SETTINGS,
                        System.getProperty("user.home") + "/.m2/settings.xml"));
        log.warn("[fromMavenSettings] searching for localRepository location in {}", settings);
        if (java.nio.file.Files.exists(settings)) {
            try {
                String localM2RepositoryFromSettings = null;
                // do not introduce a xml parser so will do with what we have...
                final String content = new String(java.nio.file.Files.readAllBytes(settings), StandardCharsets.UTF_8);
                final Pattern comments = Pattern.compile("(<!--.*?-->)", Pattern.DOTALL);
                final Pattern emptyLines = Pattern.compile("^\\s*$|\\n|\\r\\n");
                final Pattern localRepo =
                        Pattern.compile(".*<localRepository>(.+)</localRepository>.*", Pattern.CASE_INSENSITIVE);
                // some cleanups
                String stripped = comments.matcher(content).replaceAll("");
                stripped = emptyLines.matcher(stripped).replaceAll("");
                // localRepository present?
                final Matcher m = localRepo.matcher(stripped);
                if (!m.matches()) {
                    log.warn("[fromMavenSettings] localRepository not defined in settings.xml");
                } else {
                    localM2RepositoryFromSettings = m.group(1).trim();
                    if (localM2RepositoryFromSettings != null && !localM2RepositoryFromSettings.isEmpty()) {
                        final Path settingsM2 = handler.get(localM2RepositoryFromSettings);
                        if (java.nio.file.Files.exists(settingsM2)) {
                            return settingsM2;
                        }
                        log.warn("[fromMavenSettings] localRepository points to a non existent path: {}.", settingsM2);
                    }
                }
            } catch (final Exception ignore) {
                // fallback on default local path
            }
        }
        log.warn("[fromMavenSettings] Could not get m2 from maven settings.");
        return null;
    }

    public Path fromEnvironmentVariables() {
        final String vm2 = System.getenv("M2_HOME");
        log.warn("[fromEnvironmentVariables] M2_HOME={}", vm2);
        if (vm2 != null) {
            Path p = handler.get(vm2).resolve("repository");
            if (Files.exists(p)) {
                return p;
            }
        }
        log.warn("[fromEnvironmentVariables] Could not get m2 from environment.");
        return null;
    }

    private Path interpolated(final Path path) {
        return null;
    }

}
