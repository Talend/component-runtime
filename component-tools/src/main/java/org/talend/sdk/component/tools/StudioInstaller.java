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
package org.talend.sdk.component.tools;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.dependencies.maven.MvnCoordinateToFileConverter;

public class StudioInstaller implements Runnable {

    private final String mainGav;

    private final File studioHome;

    private final Map<String, File> artifacts;

    private final Log log;

    private final boolean enforceDeployment;

    private final File m2;

    public StudioInstaller(final String mainGav, final File studioHome, final Map<String, File> artifacts,
            final Object log, final boolean enforceDeployment, final File m2) {
        this.mainGav = mainGav;
        this.studioHome = studioHome;
        this.artifacts = artifacts;
        this.m2 = m2;
        this.enforceDeployment = enforceDeployment;
        try {
            this.log = Log.class.isInstance(log) ? Log.class.cast(log) : new ReflectiveLog(log);
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void run() {
        log.info("Installing development version of " + mainGav + " in " + studioHome);

        final List<String> artifacts = this.artifacts.values().stream().map(File::getName).collect(toList());
        final String mvnMeta[] = mainGav.split(":");
        final String artifact = mvnMeta[1];
        final String version = mvnMeta[2];

        // 0. check if component can be installed.
        final File configIni = new File(studioHome, "configuration/config.ini");
        final Properties config = new Properties();
        if (configIni.exists()) {
            try (final InputStream is = new FileInputStream(configIni)) {
                config.load(is);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        String registry = config
                .getProperty("component.java.registry",
                        config.getProperty("talend.component.server.component.registry"));
        if (!enforceDeployment && registry != null && new File(registry).exists()) {
            final Properties components = new Properties();
            try (final Reader reader = new FileReader(registry)) {
                components.load(reader);
                if (components.containsKey(artifact)) {
                    final String installedVersion = components.getProperty(artifact).split(":")[2];
                    if (!version.equals(installedVersion)) {
                        throw new IllegalStateException("Can't deploy this component. A different version '"
                                + installedVersion
                                + "' is already installed.\nYou can enforce the deployment by using -Dtalend.component.enforceDeployment=true");
                    }
                }
            } catch (final IOException e) {
                log.error("Can't load registered component from the studio " + e.getMessage());
            }
        }

        // 1. remove staled libs from the cache (configuration/org.eclipse.osgi)
        final File osgiCache = new File(studioHome, "configuration/org.eclipse.osgi");
        if (osgiCache.isDirectory()) {
            ofNullable(osgiCache.listFiles(child -> {
                try {
                    return child.isDirectory() && Integer.parseInt(child.getName()) > 0;
                } catch (final NumberFormatException nfe) {
                    return false;
                }
            }))
                    .map(Stream::of)
                    .orElseGet(Stream::empty)
                    .map(id -> new File(id, ".cp"))
                    .filter(File::exists)
                    .flatMap(cp -> ofNullable(cp.listFiles((dir, name) -> name.endsWith(".jar")))
                            .map(Stream::of)
                            .orElseGet(Stream::empty))
                    .filter(jar -> artifacts.contains(jar.getName()))
                    .forEach(this::tryDelete);
        }

        // 2. install the runtime dependency tree (scope compile+runtime) in the studio m2 repo
        final String repoType = config.getProperty("maven.repository");
        if (!"global".equals(repoType)) {
            final MvnCoordinateToFileConverter converter = new MvnCoordinateToFileConverter();
            this.artifacts.forEach((gav, file) -> {
                final Artifact dependency = converter.toArtifact(gav);
                final File target = m2 == null || !m2.exists()
                        ? new File(studioHome, "configuration/.m2/repository/" + dependency.toPath())
                        : new File(m2, dependency.toPath());
                try {
                    if (target.exists() && !dependency.getVersion().endsWith("-SNAPSHOT")) {
                        log.info(gav + " already exists, skipping");
                        return;
                    }
                    copy(file, target);
                    log.info("Installed " + gav + " at " + target.getAbsolutePath());
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } else {
            log
                    .info("Studio " + studioHome
                            + " configured to use global maven repository, skipping artifact installation");
        }

        // 3. register component adding them into the registry
        if (registry == null) {
            final File registryLocation = new File(configIni.getParentFile(), "components-registration.properties");
            registryLocation.getParentFile().mkdirs();
            registry = registryLocation.getAbsolutePath().replace('\\', '/');

            config.setProperty("component.java.registry", registry);
            try {
                final File backup = new File(configIni.getParentFile(), "backup/" + configIni.getName() + "_"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-mm-dd_HH-mm-ss")));
                log.info("Saving configuration in " + backup);
                copy(configIni, backup);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            try (final Writer writer = new FileWriter(configIni)) {
                config
                        .store(writer, "File rewritten by " + getClass().getName()
                                + " utility to add component.java.registry entry");
                log.info("Updated " + configIni + " to add the component registry entry");
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            try {
                Files.write(registryLocation.toPath(), new byte[0], StandardOpenOption.CREATE_NEW);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        final Properties components = new Properties();
        try (final Reader reader = new FileReader(registry)) {
            components.load(reader);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        if (!components.containsKey(artifact) || enforceDeployment) {
            components.setProperty(artifact, mainGav);
            try (final Writer writer = new FileWriter(registry)) {
                components.store(writer, "File rewritten to add " + mainGav);
                log.info("Updated " + registry + " with '" + mainGav + "'");
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void copy(final File source, final File target) throws IOException {
        mkdirP(target.getParentFile());
        try (final OutputStream to = new BufferedOutputStream(new FileOutputStream(target))) {
            Files.copy(source.toPath(), to);// this copy don't lock on win
        }
    }

    private void mkdirP(final File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Can't create " + dir);
        }
    }

    private void tryDelete(final File jar) {
        if (!jar.delete()) {
            log.error("Can't delete " + jar.getAbsolutePath());
        } else {
            log.info("Deleting " + jar.getAbsolutePath());
        }
    }

}
