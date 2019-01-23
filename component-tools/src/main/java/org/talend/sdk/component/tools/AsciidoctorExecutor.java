/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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

import static java.util.Collections.emptyMap;
import static java.util.Collections.list;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.ziplock.JarLocation.jarLocation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.apache.ziplock.IO;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;

// indirection to not load asciidoctor if not in the classpath
public class AsciidoctorExecutor implements AutoCloseable {

    private Asciidoctor asciidoctor;

    private File extractedResources;

    private Runnable onClose = () -> {
    };

    public void render(final File workDir, final String version, final Log log, final String backend, final File source,
            final File output, final String title, final Map<String, String> attributes, final File templateDir,
            final String templateEngine) {
        log.info("Rendering '" + source.getName() + "' to '" + output + "'");
        if (asciidoctor == null) {
            asciidoctor = getAsciidoctor();
        }
        final OptionsBuilder optionsBuilder = OptionsBuilder
                .options()
                .baseDir(source.getParentFile())
                .toFile(output)
                .backend(backend)
                .safe(SafeMode.UNSAFE)
                .docType("book") // todo: config
                .mkDirs(true);

        final AttributesBuilder attrs =
                AttributesBuilder.attributes().attributeMissing("skip").attributeUndefined("drop");
        if (attributes == null) {
            configureDefaultsAttributes(workDir, log, backend, attrs, emptyMap());
        } else {
            attributes.forEach((k, v) -> {
                if (v == null) {
                    attrs.attribute(k);
                } else {
                    attrs.attribute(k, v);
                }
            });
            configureDefaultsAttributes(workDir, log, backend, attrs, attributes);
        }
        optionsBuilder.attributes(attrs);

        ofNullable(templateDir).ifPresent(optionsBuilder::templateDir);
        ofNullable(templateEngine).ifPresent(optionsBuilder::templateEngine);

        log.debug("Options: " + optionsBuilder.asMap());
        asciidoctor.render(wrap(title, version, source), optionsBuilder);
    }

    private void configureDefaultsAttributes(final File workDir, final Log log, final String backend,
            final AttributesBuilder attrs, final Map<String, String> attributes) {
        if (extractedResources == null) {
            extractResources(workDir, log);
        }
        switch (backend.toLowerCase(ENGLISH)) {
        case "html":
        case "html5":
            if (!attributes.containsKey("stylesheet")) {
                attrs.attribute("stylesheet", "talend.css");
            }
            if (!attributes.containsKey("stylesdir")) {
                attrs.attribute("stylesdir", new File(extractedResources, "resources/html").getAbsolutePath());
            }
            if (!attributes.containsKey("data-uri")) {
                attrs.attribute("data-uri");
            }
            break;
        case "pdf":
            if (!attributes.containsKey("pdf-style")) {
                attrs.attribute("pdf-style", "talend.yml");
            }
            if (!attributes.containsKey("pdf-stylesdir")) {
                attrs.attribute("pdf-stylesdir", new File(extractedResources, "resources/pdf").getAbsolutePath());
            }
            break;
        default:
        }

        if (!attributes.containsKey("icons")) {
            attrs.attribute("icons", "font");
        }
        if (!attributes.containsKey("source-highlighter")) {
            attrs.attribute("source-highlighter", "coderay");
        }
        if (!attributes.containsKey("toc")) {
            attrs.attribute("toc", "left");
        }
    }

    private void extractResources(final File workDir, final Log log) {
        final boolean workdirCreated = !workDir.exists();
        workDir.mkdirs();
        extractedResources = new File(workDir, getClass().getSimpleName() + "_resources");
        onClose = () -> {
            try {
                org.apache.ziplock.Files.remove(workdirCreated ? workDir : extractedResources);
            } catch (final IllegalStateException e) {
                log.error(e.getMessage());
            }
        };
        final File file = jarLocation(AsciidoctorExecutor.class);
        if (file.isDirectory()) {
            Stream.of("html", "pdf").forEach(backend -> {
                final File[] children = new File(file, "resources/" + backend).listFiles();
                if (children == null) {
                    throw new IllegalStateException("No resources folder for: " + backend);
                }
                Stream.of(children).forEach(child -> {
                    try {
                        copyResource(new File(extractedResources, "resources/" + backend + '/' + child.getName()),
                                new FileInputStream(child));
                    } catch (final FileNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                });
            });
        } else {
            try (final JarFile jar = new JarFile(file)) {
                list(jar.entries())
                        .stream()
                        .filter(e -> e.getName().startsWith("resources/") && !e.isDirectory())
                        .forEach(e -> {
                            try {
                                copyResource(new File(extractedResources, e.getName()), jar.getInputStream(e));
                            } catch (final IOException e1) {
                                throw new IllegalStateException(e1);
                            }
                        });
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void copyResource(final File out, final InputStream inputStream) {
        out.getParentFile().mkdirs();
        try (final InputStream is = new BufferedInputStream(inputStream);
                final OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) {
            IO.copy(is, os);
        } catch (final IOException e1) {
            throw new IllegalStateException(e1);
        }
    }

    private String wrap(final String title, final String version, final File source) {
        try {
            final String content =
                    Files.readAllLines(source.toPath(), StandardCharsets.UTF_8).stream().collect(joining("\n"));
            if (content.startsWith("= ")) {
                return content;
            }
            return "= " + title + "\n:revnumber: " + version + "\n\n" + content;
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Asciidoctor getAsciidoctor() {
        return asciidoctor == null ? asciidoctor = Asciidoctor.Factory.create() : asciidoctor;
    }

    @Override
    public void close() {
        onClose.run();
    }
}
