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
package org.talend.sdk.component.maven;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;
import static org.apache.maven.shared.utils.StringUtils.capitalise;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.io.FileUtils;
import org.talend.sdk.component.api.service.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Generates components in place.
 */
@Deprecated
@Mojo(name = "generate")
public class ComponentGeneratorMojo extends AbstractMojo {

    @Parameter(property = "talend.generator.classbase")
    private String classnameBase;

    @Parameter(property = "talend.generator.type", defaultValue = "help", required = true)
    private String type;

    @Parameter(property = "talend.generator.family")
    private String family;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDir;

    @Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true)
    private File sourceDir;

    @Parameter(defaultValue = "  ", property = "talend.generator.pom.spacing")
    private String pomUnitarySpacing;

    @Parameter(defaultValue = "UTF-8", property = "talend.generator.pom.encoding")
    private String pomEncoding;

    @Parameter(defaultValue = "UTF-8", property = "talend.generator.pom.read-only")
    private boolean pomReadOnly;

    private final Freemarkers freemarkers = new Freemarkers(getClass().getSimpleName());

    @Override
    public void execute() {
        getLog().warn("This mojo is deprecated and will be deleted in a future release");

        switch (type.toLowerCase(ENGLISH)) {
        case "input":
            initGenerationState();
            generateInput();
            enforceApiInPom();
            break;
        case "output":
            initGenerationState();
            generateOutput();
            enforceApiInPom();
            break;
        case "help":
        default:
            getLog().info("Available generator types:");
            getLog().info("- input: generates a source component");
            getLog().info("- output: generates a sink component");
            // todo: add tests types?
        }
    }

    private void enforceApiInPom() {
        if (pomReadOnly) {
            getLog().debug("Pom is in read only mode, skipping any modification");
            return;
        }

        final File pomXml = new File(baseDir, "pom.xml");
        if (!pomXml.isFile()) {
            getLog().warn("Didn't find " + pomXml + ", you can need to add component-api dependency yourself");
            return;
        }

        try {
            String content = FileUtils.fileRead(pomXml);
            if (content.contains("<artifactId>component-api</artifactId>")) {
                getLog().debug("component-api already in " + pomXml);
                return;
            }

            final String dependency = ("    <dependency>\n" + "      <groupId>org.talend.sdk.component</groupId>"
                    + "      <artifactId>component-api</artifactId>" + "      <version>"
                    + Service.class.getPackage().getImplementationVersion() + "</version>" + "    </dependency>\n")
                            .replace("  ", pomUnitarySpacing);

            final int dependencies = content.indexOf("<dependencies>");
            if (dependencies < 0) {
                final int end = content.indexOf("</project>");
                if (end < 0) {
                    throw new IllegalArgumentException(pomXml + " appears invalid, no </project> tag");
                }
                content = content.substring(0, end) + "\n" + pomUnitarySpacing + "<dependencies>\n" + dependency
                        + pomUnitarySpacing + "</dependencies>\n" + content.substring(end);
            } else {
                final int depStartEnd = dependencies + "<dependencies>".length();
                content = content.substring(0, depStartEnd) + '\n' + dependency
                        + content.substring(depStartEnd, content.length());
            }

            FileUtils.fileWrite(pomXml, pomEncoding, content);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void initGenerationState() {
        { // class
            if (classnameBase == null) {
                classnameBase =
                        capitalise(Stream.of(splitByCharacterTypeCamelCase(baseDir.getName())).collect(joining("")));
            }
            if (!classnameBase.contains(".")) { // try to guess the package
                final List<String> stopFolders = asList("input", "output");

                File current = sourceDir;
                while (true) {
                    if (!current.exists()) {
                        break; // shouldn't occur
                    }
                    final Optional<File> child = ofNullable(current
                            .listFiles((dir, name) -> new File(dir, name).isDirectory() && !name.startsWith(".")))
                                    .filter(f -> f.length == 1)
                                    .map(f -> f[0]);
                    if (!child.isPresent() || stopFolders.contains(child.get().getName())) {
                        break;
                    }
                    current = child.get();
                }

                final String base = sourceDir
                        .toPath()
                        .relativize(current.toPath())
                        .toString()
                        .replace(File.separatorChar, '/')
                        .replace('/', '.');
                final String packageBase = base + (base.isEmpty() ? "" : ".") + type.replace('-', '_');
                classnameBase = packageBase + '.' + classnameBase;
            }
        }
        if (family == null) {
            family = Stream
                    .of(splitByCharacterTypeCamelCase(
                            baseDir.getName().replace("components", "").replace("component", "")))
                    .collect(joining(""));
        }
    }

    private File createOutputFile(final File sourceDir, final String child) {
        final File file = new File(sourceDir, child);
        if (file.exists()) {
            throw new IllegalStateException(file + " exists, maybe configure the classnameBase of the generator");
        } else if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new IllegalStateException(file + " parent directory can't be created");
        }
        getLog().info("Creating " + file);
        return file;
    }

    private void generateOutput() {
        final String classPath = classnameBase.replace('.', '/') + "Output.java";
        try (final OutputStream stream =
                new BufferedOutputStream(new FileOutputStream(createOutputFile(sourceDir, classPath)))) {
            stream.write(freemarkers.templatize("output", createContext("Output")).getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        // properties
        final File i18n =
                new File(createOutputFile(sourceDir.getParentFile(), "resources/" + classPath).getParentFile(),
                        "Messages.properties");
        final String name = classnameBase.substring(classnameBase.lastIndexOf('.') + 1);
        appendI18n(i18n, family + '.' + name + "Output._displayName", name);
    }

    private void generateInput() {
        final String classPath = classnameBase.replace('.', '/');
        Stream.of("mapper", "emitter").forEach(tpl -> {
            final String template = capitalise(tpl);
            try (final OutputStream stream = new BufferedOutputStream(
                    new FileOutputStream(createOutputFile(sourceDir, classPath + template + ".java")))) {
                stream
                        .write(freemarkers
                                .templatize("input-" + tpl, createContext(template))
                                .getBytes(StandardCharsets.UTF_8));
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        });

        // properties
        final File i18n = new File(new File(sourceDir.getParentFile(), "resources/" + classPath).getParentFile(),
                "Messages.properties");
        final String name = classnameBase.substring(classnameBase.lastIndexOf('.') + 1);
        appendI18n(i18n, family + '.' + name + "Mapper._displayName", name);
    }

    private void appendI18n(final File i18n, final String... properties) {
        final Properties existing = new Properties();
        if (i18n.exists()) {
            try (final Reader reader = new FileReader(i18n)) {
                existing.load(reader);
            } catch (final IOException ioe) {
                throw new IllegalArgumentException(ioe);
            }
        } else if (!i18n.getParentFile().isDirectory() && !i18n.getParentFile().mkdirs()) {
            throw new IllegalArgumentException("Can't create " + i18n.getParentFile());
        }

        final Properties config = new Properties();
        for (int i = 0; i < properties.length; i += 2) {
            if (existing.containsKey(properties[i])) {
                continue;
            }
            config.setProperty(properties[i], properties[i + 1]);
        }

        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(i18n))) {
            writer.write("\n\n");
            config.store(writer, "generated while creating component " + classnameBase);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Map<String, Object> createContext(final String template) {
        return singletonMap("configuration", doCreateConfiguration(template));
    }

    private Configuration doCreateConfiguration(final String template) {
        if (classnameBase.contains(".")) {
            final int lastDot = classnameBase.lastIndexOf('.');
            return new Configuration(family, classnameBase.substring(0, lastDot),
                    classnameBase.substring(lastDot + 1, classnameBase.length()), template);

        }
        return new Configuration(family, "", classnameBase, template);
    }

    @Getter
    @AllArgsConstructor
    public static class Configuration {

        private final String family;

        private final String packageName;

        private final String className;

        private final String template;
    }
}
