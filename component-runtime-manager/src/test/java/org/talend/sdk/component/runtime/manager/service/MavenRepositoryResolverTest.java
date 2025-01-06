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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.talend.sdk.component.runtime.manager.service.MavenRepositoryResolver.STUDIO_MVN_REPOSITORY;
import static org.talend.sdk.component.runtime.manager.service.MavenRepositoryResolver.TALEND_COMPONENT_MANAGER_M2_REPOSITORY;
import static org.talend.sdk.component.runtime.manager.service.MavenRepositoryResolver.TALEND_COMPONENT_MANAGER_M2_SETTINGS;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.manager.service.path.PathHandler;
import org.talend.sdk.component.runtime.manager.service.path.PathHandlerImpl;

class MavenRepositoryResolverTest {

    static String M2_REPOSITORY = ".m2" + File.separator + "repository";

    private final MavenRepositoryResolver resolver = new MavenRepositoryDefaultResolver();

    final MavenRepositoryDefaultResolver mavenSettingsOnlyResolver = new MavenRepositoryDefaultResolver() {

        @Override
        public Path discover() {
            return Stream.of(fromMavenSettings())
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(fallback());
        }

    };

    private final PathHandler handler = new PathHandlerImpl();

    private final PathHandler handlerNoExistCheck = new PathHandlerImpl() {

        @Override
        public Path get(final String path) {
            return interpolate(path);
        }
    };

    private final String fallback = System.getProperty("user.home") + File.separator + M2_REPOSITORY;

    private final Path repository = Paths.get(new File("target/test-classes").getAbsolutePath());

    @BeforeAll
    static void setup() throws IOException {
        final Path repository = Paths.get(new File("target/test-classes").getAbsolutePath());
        Files.createDirectories(repository.resolve(M2_REPOSITORY));
    }

    @Test
    void discover() {
        assertEquals(fallback, resolver.discover().toString());
    }

    @Test
    void fallback() {
        assertEquals(fallback, resolver.fallback().toString());
    }

    @Test
    void discoverFromPropertyOk() {
        System.setProperty(TALEND_COMPONENT_MANAGER_M2_REPOSITORY, repository.toString());
        final Path m2 = resolver.discover();
        assertNotNull(m2);
        assertEquals(repository, m2);
        System.clearProperty(TALEND_COMPONENT_MANAGER_M2_REPOSITORY);
    }

    @Test
    void discoverFromPropertyKo() {
        final Path repository = Paths.get("/home/zorro71");
        System.setProperty(TALEND_COMPONENT_MANAGER_M2_REPOSITORY, repository.toString());
        final Path m2 = resolver.discover();
        assertNotNull(m2);
        assertEquals(fallback, m2.toString());
        System.clearProperty(TALEND_COMPONENT_MANAGER_M2_REPOSITORY);
    }

    @Test
    void discoverFromStudioOk() {
        System.setProperty(STUDIO_MVN_REPOSITORY, "local");
        System.setProperty("osgi.configuration.area", repository.toString());
        final Path m2 = resolver.discover();
        assertNotNull(m2);
        assertEquals(repository.resolve(M2_REPOSITORY), m2);
        System.clearProperty(STUDIO_MVN_REPOSITORY);
        System.clearProperty("osgi.configuration.area");
    }

    @Test
    void discoverFromSettingsOk() {
        setSettingsProperty("settings/settings-ok.xml");
        final Path m2 = resolver.discover();
        assertNotNull(m2);
        assertEquals("/home", m2.toString());
        System.clearProperty(TALEND_COMPONENT_MANAGER_M2_SETTINGS);
    }

    @Test
    void discoverFromSettingsKo() {
        setSettingsProperty("settings/settings-ko.xml");
        final Path m2 = resolver.discover();
        assertNotNull(m2);
        assertEquals(fallback, m2.toString());
        System.clearProperty(TALEND_COMPONENT_MANAGER_M2_SETTINGS);
    }

    @Test
    void discoverFromSettingsCommented() {
        setSettingsProperty("settings/settings-commented.xml");
        final Path m2 = resolver.discover();
        assertNotNull(m2);
        assertEquals(fallback, m2.toString());
        System.clearProperty(TALEND_COMPONENT_MANAGER_M2_SETTINGS);
    }

    @Test
    void discoverFromSettingsOkSpaced() {
        setSettingsProperty("settings/settings-ok-spaced.xml");
        final Path m2 = resolver.discover();
        assertNotNull(m2);
        assertEquals("/home", m2.toString());
        System.clearProperty(TALEND_COMPONENT_MANAGER_M2_SETTINGS);
    }

    @Test
    void discoverFromSettingsOkSpacedMixedCase() {
        setSettingsProperty("settings/settings-ok-spaced-mixed-case.xml");
        final Path m2 = resolver.discover();
        assertNotNull(m2);
        assertEquals("/home", m2.toString());
        System.clearProperty(TALEND_COMPONENT_MANAGER_M2_SETTINGS);
    }

    @Test
    void discoverFromSettingsWindowsPath() {
        setSettingsProperty("settings/settings-win.xml");
        mavenSettingsOnlyResolver.setHandler(handlerNoExistCheck);
        final Path m2 = mavenSettingsOnlyResolver.discover();
        assertNotNull(m2);
        assertEquals("C:" + File.separator + "Users" + File.separator + "maven" + File.separator + "repository",
                m2.toString());
        System.clearProperty(TALEND_COMPONENT_MANAGER_M2_SETTINGS);
    }

    @Test
    void discoverFromWindowsPathWithFile() {
        System.setProperty("osgi.configuration.area", "file:/C:/Users/Talend-Studio-CICD_TEST/configuration/");
        System.setProperty(STUDIO_MVN_REPOSITORY, "studio");
        final Path m2 = resolver.discover();
        assertNotNull(m2);
        System.clearProperty("osgi.configuration.area");
        System.clearProperty(STUDIO_MVN_REPOSITORY);
    }

    @Test
    void discoverFromWindowsPathWithoutFile() {
        System.setProperty("osgi.configuration.area", "C:/Users/Talend-Studio-CICD_TEST/configuration/");
        final Path m3 = resolver.discover();
        assertNotNull(m3);
        System.clearProperty("osgi.configuration.area");
    }

    @Test
    void discoverFromWindowsPathWrongURI() {
        System.setProperty("osgi.configuration.area", "file://example .com");
        final Path m4 = resolver.discover();
        assertNotNull(m4);
        System.clearProperty("osgi.configuration.area");
    }

    @Test
    void discoverFromWindowsPathGlobal() {
        System.setProperty(STUDIO_MVN_REPOSITORY, "global");
        final Path m5 = resolver.discover();
        assertNotNull(m5);
        System.clearProperty(STUDIO_MVN_REPOSITORY);
    }

    @Test
    void discoverFromSettingsTildePath() {
        setSettingsProperty("settings/settings-tilde.xml");
        mavenSettingsOnlyResolver.setHandler(handlerNoExistCheck);
        final Path m2 = mavenSettingsOnlyResolver.discover();
        assertNotNull(m2);
        assertEquals(System.getProperty("user.home") + File.separator + "mvn_home_dev" + File.separator + "repository",
                m2.toString());
        System.clearProperty(TALEND_COMPONENT_MANAGER_M2_SETTINGS);
    }

    @Test
    void discoverFromSettingsUserHomeProperty() {
        setSettingsProperty("settings/settings-prop-user.xml");
        mavenSettingsOnlyResolver.setHandler(handlerNoExistCheck);
        final Path m2 = mavenSettingsOnlyResolver.discover();
        assertNotNull(m2);
        assertEquals(System.getProperty("user.home") + File.separator + "mvn_home_dev" + File.separator + "repository",
                m2.toString());
        System.clearProperty(TALEND_COMPONENT_MANAGER_M2_SETTINGS);
    }

    @Test
    void discoverFromSettingsEnvProperty() {
        setSettingsProperty("settings/settings-prop-env.xml");
        mavenSettingsOnlyResolver.setHandler(handlerNoExistCheck);
        final Path m2 = mavenSettingsOnlyResolver.discover();
        assertNotNull(m2);
        assertEquals(Paths.get(System.getenv("M2_HOME") + "/mvn_home_dev/repository"), m2);
        System.clearProperty(TALEND_COMPONENT_MANAGER_M2_SETTINGS);
    }

    @Test
    void discoverFromRepositoryPropertyAndSettingsKo() {
        System.setProperty(TALEND_COMPONENT_MANAGER_M2_REPOSITORY, Paths.get("/home/zorro71").toString());
        setSettingsProperty("settings/settings-ko.xml");
        final Path m2 = resolver.discover();
        assertNotNull(m2);
        assertEquals(fallback, m2.toString());
        System.clearProperty(TALEND_COMPONENT_MANAGER_M2_REPOSITORY);
        System.clearProperty(TALEND_COMPONENT_MANAGER_M2_SETTINGS);
    }

    @Test
    void discoverFromEnvironment() throws IOException {
        Files.createDirectories(repository.resolve("repository"));
        final String vm2 = System.getenv("M2_HOME");
        final Path m2 = resolver.discover();
        assertNotNull(m2);
        assertEquals(repository.resolve("repository"), m2);
        Files.delete(repository.resolve("repository"));
    }

    private void setSettingsProperty(final String path) {
        try {
            final Enumeration<URL> rsc = getClass().getClassLoader().getResources(path);
            final Path settings = Paths.get(rsc.nextElement().toURI());
            System.setProperty(TALEND_COMPONENT_MANAGER_M2_SETTINGS, settings.toString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}