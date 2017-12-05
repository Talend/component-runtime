/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.manager.tools;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.dependencies.maven.MvnCoordinateToFileConverter;

// @Experimental
public class StudioInstaller implements Runnable {

    private final String mainGav;

    private final File studioHome;

    private final Map<String, File> artifacts;

    private final Log log;

    public StudioInstaller(final String mainGav, final File studioHome, final Map<String, File> artifacts,
            final Object log) {
        this.mainGav = mainGav;
        this.studioHome = studioHome;
        this.artifacts = artifacts;
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

        // 0. remove staled libs from the cache (configuration/org.eclipse.osgi)
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

        // 1. remove staled libs from the build (workspace/.Java/lib/)
        final File javaLib = new File(studioHome, "workspace/.Java/lib");
        if (javaLib.isDirectory()) {
            ofNullable(javaLib.listFiles((d, n) -> artifacts.contains(n)))
                    .map(Stream::of)
                    .orElseGet(Stream::empty)
                    .forEach(this::tryDelete);
        }

        // 2. install the runtime dependency tree (scope compile+runtime) in the studio m2 repo
        final File configIni = new File(studioHome, "configuration/config.ini");
        final Properties config = new Properties();
        if (configIni.exists()) {
            try (final InputStream is = new FileInputStream(configIni)) {
                config.load(is);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        final String repoType = config.getProperty("maven.repository");
        if (!"global".equals(repoType)) {
            final MvnCoordinateToFileConverter converter = new MvnCoordinateToFileConverter();
            this.artifacts.forEach((gav, file) -> {
                final Artifact artifact = converter.toArtifact(gav);
                try {
                    final File target = new File(studioHome,
                            "configuration/.m2/repository/" + artifact.getGroup() + '/' + artifact.getArtifact() + '/'
                                    + artifact.getVersion() + '/' + artifact.getArtifact() + '-' + artifact.getVersion()
                                    + (artifact.getClassifier() != null && !artifact.getClassifier().isEmpty()
                                            ? '-' + artifact.getClassifier()
                                            : "")
                                    + "." + (artifact.getType() != null ? artifact.getType() : "jar"));
                    mkdirP(target.getParentFile());
                    Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    log.info("Installed " + gav + " at " + target.getAbsolutePath());
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } else {
            log.info("Studio " + studioHome
                    + " configured to use global maven repository, skipping artifact installation");
        }

        // 3. register component adding them into the registry
        String registry = config.getProperty("component.java.registry",
                config.getProperty("talend.component.server.component.registry"));
        if (registry == null) {
            final File registryLocation = new File(configIni.getParentFile(), "components-registration.properties");
            registryLocation.mkdirs();
            registry = registryLocation.getAbsolutePath();

            config.setProperty("component.java.registry", registry);
            try {
                final File backup = new File(configIni.getParentFile(),
                        "backup/" + configIni.getName() + "_" + LocalDateTime.now());
                mkdirP(backup.getParentFile());
                log.info("Saving configuration in " + backup);
                Files.copy(configIni.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            try (final Writer writer = new FileWriter(configIni)) {
                config.store(writer,
                        "File rewritten by " + getClass().getName() + " utility to add component.java.registry entry");
                log.info("Updated " + configIni + " to add the component registry entry");
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
        if (!components.containsKey(mainGav)) {
            components.setProperty(mainGav.split(":")[1], mainGav);
            try (final Writer writer = new FileWriter(registry)) {
                components.store(writer, "File rewritten to add " + mainGav);
                log.info("Updated " + registry + " with '" + mainGav + "'");
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
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
