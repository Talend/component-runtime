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
package org.talend.sdk.component.singer.kitap;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.talend.sdk.component.path.PathFactory;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.singer.java.SingerArgs;

import lombok.NoArgsConstructor;

/**
 * Wrapper to {@link Kitap} which is able to launch a .car.
 */
@NoArgsConstructor(access = PRIVATE)
public final class Carpates {

    public static void main(final String[] args) {
        EnvironmentSetup.init();

        final SingerArgs singerArgs = new SingerArgs(args);
        final Path archive = PathFactory
                .get(requireNonNull(
                        ofNullable(singerArgs.getOtherArgs().get("--component-archive"))
                                .orElseGet(() -> System.getenv("TALEND_SINGER_COMPONENT_ARCHIVE")),
                        "--component-archive is required"));
        if (!Files.exists(archive)) {
            throw new IllegalArgumentException("--component-archive does not exist: '" + archive + "'");
        }

        final String workDirPath = ofNullable(singerArgs.getOtherArgs().get("--work-dir"))
                .orElseGet(() -> System.getenv("TALEND_SINGER_WORK_DIR"));
        final Path workDir = ofNullable(workDirPath).map(PathFactory::get).orElseGet(() -> {
            try {
                final ZonedDateTime now = ZonedDateTime.now();
                return Files
                        .createTempDirectory(Carpates.class.getName() + "_" + now.getYear() + '-' + now.getMonthValue()
                                + '-' + now.getDayOfMonth() + '_' + now.getHour() + '_' + now.getMinute() + '_'
                                + now.getSecond());
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }).toAbsolutePath();
        final boolean deleteWorkDir = workDirPath == null || !Files.exists(workDir);
        final Path m2 = workDir.resolve("m2");
        if (!Files.exists(m2)) {
            try {
                Files.createDirectories(m2);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        final Path javaBin = PathFactory.get(System.getProperty("java.home")).resolve("bin");
        final String java = Stream
                .of("java", "java.cmd", "java.exe")
                .map(javaBin::resolve)
                .filter(Files::exists)
                .findAny()
                .map(String::valueOf)
                .orElse("java");
        try {
            final Path auditFile = workDir.resolve("maven-deploy.audit.log");
            final int exitCode = new ProcessBuilder(java, "-jar", archive.toString(), "maven-deploy", m2.toString())
                    .redirectOutput(auditFile.toFile())
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
                    .waitFor();
            if (exitCode != 0) {
                Files.copy(auditFile, System.err);
                throw new IllegalStateException("Can't setup the component in the repository, exit code=" + exitCode);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        System.setProperty("org.talend.sdk.component.slf4j.StdLogger.level", "error");
        System.setProperty("talend.component.manager.m2.repository", m2.toString());

        final String gav = loadComponentGav(archive);
        ComponentManager.instance().addPlugin(gav);

        try {
            new Kitap(args).run();
        } finally {
            if (deleteWorkDir) {
                deleteDir(workDir);
            }
        }
    }

    private static String loadComponentGav(final Path archive) {
        try (final JarFile file = new JarFile(archive.toFile());
                final InputStream stream = file.getInputStream(file.getEntry("TALEND-INF/metadata.properties"))) {
            final Properties properties = new Properties();
            properties.load(stream);
            return requireNonNull(properties.getProperty("component_coordinates"),
                    "no component_coordinates in '" + archive + "'");
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void deleteDir(final Path workDir) {
        try {
            Files.walkFileTree(workDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return super.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    Files.delete(dir);
                    return super.postVisitDirectory(dir, exc);
                }
            });
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
