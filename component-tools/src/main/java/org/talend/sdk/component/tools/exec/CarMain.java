/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools.exec;

import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.joining;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;

// IMPORTANT: this class MUST not have ANY dependency, not even slf4j!
public class CarMain {

    private CarMain() {
        // no-op
    }

    public static void main(final String[] args) {
        if (args == null || args.length < 2) {
            help();
            return;
        }
        if (Stream.of(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("No argument can be null");
        }
        switch (args[0].toLowerCase(ROOT)) {
        case "studio-deploy":
            deployInStudio(args[1]);
            break;
        case "maven-deploy":
            deployInM2(args[1]);
            break;
        default:
            help();
            throw new IllegalArgumentException("Unknown command '" + args[0] + "'");
        }
    }

    private static void deployInM2(final String m2) {
        final File m2File = new File(m2);
        if (m2File.exists()) {
            throw new IllegalArgumentException(m2 + " doesn't exist");
        }
        final String component = installJars(m2File);
        System.out.println("Installed " + jarLocation(CarMain.class).getName() + " in " + m2 + ", "
                + "you can now register '" + component + "' component in your application.");
    }

    private static File findServerM2(final String serverHome) {
        // talend.component.server.maven.repository
        return null;
    }

    private static void deployInStudio(final String studioLocation) {
        final File root = new File(studioLocation);
        if (!root.isDirectory()) {
            throw new IllegalArgumentException(studioLocation + " is not a valid directory");
        }

        final File m2Root = new File(studioLocation, "configuration/.m2/repository/");
        if (!m2Root.isDirectory()) {
            throw new IllegalArgumentException(m2Root + " does not exist, did you specify a valid studio home?");
        }

        final File config = new File(studioLocation, "configuration/config.ini");
        if (!config.exists()) {
            throw new IllegalArgumentException("No " + config + " found, is your studio location right?");
        }

        // install jars
        final String mainGav = installJars(m2Root);

        // register the component
        final Properties configuration = readProperties(config);
        final String components = configuration.getProperty("component.java.coordinates");
        try {
            final List<String> configLines = Files.readAllLines(config.toPath());
            if (components == null) {
                final String original = configLines.stream().collect(joining("\n"));
                try (final Writer writer = new FileWriter(config)) {
                    writer.write(original + "\ncomponent.java.coordinates = " + mainGav);
                }
            } else {
                // backup
                final File backup = new File(config.getParentFile(), "backup/" + config.getName() + "_"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-mm-dd_HH-mm-ss")));
                backup.getParentFile().mkdirs();
                Files.copy(config.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING/* unlikely */);

                boolean skip = false;
                try (final Writer writer = new FileWriter(config)) {
                    for (final String line : configLines) {
                        if (line.trim().startsWith("component.java.coordinates")) {
                            skip = true;
                            continue;
                        } else if (skip && line.trim().contains("=")) {
                            skip = false;
                        } else if (skip) {
                            continue;
                        }
                        writer.write(line);
                    }
                    writer.write("component.java.coordinates = "
                            + (components.trim().isEmpty() ? "" : (components + ',')) + mainGav);
                }
            }
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    private static String installJars(final File m2Root) {
        String mainGav = null;
        try (final JarInputStream jar =
                new JarInputStream(new BufferedInputStream(new FileInputStream(jarLocation(CarMain.class))))) {
            JarEntry entry;
            while ((entry = jar.getNextJarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (entry.getName().startsWith("MAVEN-INF/repository/")) {
                    final String path = entry.getName().substring("MAVEN-INF/repository/".length());
                    final File output = new File(m2Root, path);
                    if (output.exists()) {
                        System.out.println(output + " already exists, skipping");
                    } else {
                        output.getParentFile().mkdirs();
                        Files.copy(jar, output.toPath());
                    }
                } else if ("TALEND-INF/metadata.properties".equals(entry.getName())) {
                    // mainGav
                    final Properties properties = new Properties();
                    properties.load(jar);
                    mainGav = properties.getProperty("component_coordinates");
                }
            }
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        if (mainGav == null || mainGav.trim().isEmpty()) {
            throw new IllegalArgumentException("Didn't find the component coordinates");
        }
        return mainGav;
    }

    private static Properties readProperties(final File config) {
        final Properties configuration = new Properties();
        try (final InputStream stream = new FileInputStream(config)) {
            configuration.load(stream);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        return configuration;
    }

    private static void help() {
        System.err.println("Usage:\n\n   java -jar " + jarLocation(CarMain.class).getName()
                + " [studio-deploy|maven-deploy] /path/to/[studio|.m2]");
    }

    private static File jarLocation(final Class clazz) {
        try {
            final String classFileName = clazz.getName().replace(".", "/") + ".class";
            final ClassLoader loader = clazz.getClassLoader();
            return jarFromResource(loader, classFileName);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static File jarFromResource(final ClassLoader loader, final String resourceName) {
        try {
            final URL url = loader.getResource(resourceName);
            if (url == null) {
                throw new IllegalStateException("didn't find " + resourceName);
            }
            if ("jar".equals(url.getProtocol())) {
                final String spec = url.getFile();
                final int separator = spec.indexOf('!');
                return new File(decode(new URL(spec.substring(0, separator)).getFile()));

            } else if ("file".equals(url.getProtocol())) {
                return toFile(resourceName, url);
            } else {
                throw new IllegalArgumentException("Unsupported URL scheme: " + url.toExternalForm());
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static File toFile(final String classFileName, final URL url) {
        final String path = url.getFile();
        return new File(decode(path.substring(0, path.length() - classFileName.length())));
    }

    private static String decode(final String fileName) {
        if (fileName.indexOf('%') == -1) {
            return fileName;
        }

        final StringBuilder result = new StringBuilder(fileName.length());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (int i = 0; i < fileName.length();) {
            final char c = fileName.charAt(i);

            if (c == '%') {
                out.reset();
                do {
                    if (i + 2 >= fileName.length()) {
                        throw new IllegalArgumentException("Incomplete % sequence at: " + i);
                    }

                    final int d1 = Character.digit(fileName.charAt(i + 1), 16);
                    final int d2 = Character.digit(fileName.charAt(i + 2), 16);

                    if (d1 == -1 || d2 == -1) {
                        throw new IllegalArgumentException(
                                "Invalid % sequence (" + fileName.substring(i, i + 3) + ") at: " + String.valueOf(i));
                    }

                    out.write((byte) ((d1 << 4) + d2));

                    i += 3;

                } while (i < fileName.length() && fileName.charAt(i) == '%');

                result.append(out.toString());

                continue;
            } else {
                result.append(c);
            }

            i++;
        }
        return result.toString();
    }
}
