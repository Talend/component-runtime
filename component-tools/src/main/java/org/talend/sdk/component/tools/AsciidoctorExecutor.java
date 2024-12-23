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
package org.talend.sdk.component.tools;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.list;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.apache.ziplock.IO;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.jruby.internal.JRubyAsciidoctor;
import org.talend.sdk.component.path.PathFactory;

// indirection to not load asciidoctor if not in the classpath
public class AsciidoctorExecutor implements AutoCloseable {

    private Asciidoctor asciidoctor;

    private File extractedResources;

    private Runnable onClose = () -> {
    };

    public static void main(final String[] args) throws IOException {
        final Collection<String> params = new ArrayList<>(asList(args));
        try (final AsciidoctorExecutor executor = new AsciidoctorExecutor()) {
            if (params.contains("--continue")) {
                params.remove("--continue");
                final String[] newArgs = params.toArray(new String[0]);
                do {
                    executor.doMain(newArgs);

                    final String line = System.console().readLine();
                    if (line == null || "exit".equalsIgnoreCase(line.trim())) {
                        return;
                    }
                    executor.doMain(newArgs);
                } while (true);
            } else {
                executor.doMain(args);
            }
        }
    }

    public void doMain(final String[] args) throws IOException {
        final Path adoc = PathFactory.get(args[0]).toAbsolutePath();
        final File output =
                adoc.getParent().resolve(args.length > 1 ? args[1] : args[0].replace(".adoc", ".pdf")).toFile();
        final List<String> lines = Files.lines(adoc).collect(toList());
        final String version = lines
                .stream()
                .filter(it -> it.startsWith("v"))
                .findFirst()
                .map(it -> it.substring(1))
                .orElse(args.length > 2 ? args[2] : "");
        render(args.length > 3 ? new File(args[3]) : new File("target/pdf"), version, new Log() {

            @Override
            public void debug(final String s) {
                // no-op
            }

            @Override
            public void error(final String s) {
                System.err.println(s);
            }

            @Override
            public void info(final String s) {
                System.out.println(s);
            }
        }, "pdf", adoc.toFile(), output,
                lines
                        .stream()
                        .filter(it -> it.startsWith("= "))
                        .findFirst()
                        .orElseGet(() -> args.length > 4 ? args[4] : "Document"),
                new HashMap<>(), args.length > 6 ? new File(args[6]) : null, args.length > 7 ? args[7] : null,
                args.length > 5 ? args[5] : null);
    }

    public void render(final File workDir, final String version, final Log log, final String backend, final File source,
            final File output, final String title, final Map<String, String> attributes, final File templateDir,
            final String templateEngine) {
        render(workDir, version, log, backend, source, output, title, attributes, templateDir, templateEngine, null);
    }

    // CHECKSTYLE:OFF
    private void render(final File workDir, final String version, final Log log, final String backend,
            final File source, final File output, final String title, final Map<String, String> attributes,
            final File templateDir, final String templateEngine, final String libraries) {
        // CHECKSTYLE:ON
        log.info("Rendering '" + source.getName() + "' to '" + output + "'");
        if (asciidoctor == null) {
            asciidoctor = getAsciidoctor();
            if (libraries != null) {
                Stream
                        .of(libraries.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(asciidoctor::requireLibrary);
            }
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
        asciidoctor.convert(wrap(title, version, source), optionsBuilder);
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
            final String content = String.join("\n", Files.readAllLines(source.toPath(), StandardCharsets.UTF_8));
            if (content.startsWith("= ")) {
                return content;
            }
            return "= " + title + "\n:revnumber: " + version + "\n\n" + content;
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected Asciidoctor getAsciidoctor() {
        return asciidoctor == null ? asciidoctor = Asciidoctor.Factory.create() : asciidoctor;
    }

    @Override
    public void close() {
        onClose.run();
        if (asciidoctor != null && !Boolean.getBoolean("talend.component.tools.jruby.teardown.skip")) {
            if (AutoCloseable.class.isInstance(asciidoctor)) {
                try {
                    AutoCloseable.class.cast(asciidoctor).close();
                } catch (final Exception e) {
                    throw new IllegalStateException(e);
                }
            } else if (org.asciidoctor.jruby.internal.JRubyAsciidoctor.class.isInstance(asciidoctor)) {
                JRubyAsciidoctor.class.cast(asciidoctor).getRubyRuntime().tearDown();
            }
        }
    }
}
