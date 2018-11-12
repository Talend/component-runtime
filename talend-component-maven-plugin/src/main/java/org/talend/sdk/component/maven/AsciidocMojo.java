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

import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.talend.sdk.component.tools.AsciidocDocumentationGenerator;

/**
 * Generates an asiidoc documentation for the component,
 * it can also render it as HTML or PDF documents.
 */
@Mojo(name = "asciidoc", defaultPhase = PROCESS_CLASSES, requiresDependencyResolution = COMPILE_PLUS_RUNTIME,
        threadSafe = true)
public class AsciidocMojo extends ClasspathMojoBase {

    /**
     * The component version, default to pom version.
     */
    @Parameter(defaultValue = "${project.version}", property = "talend.documentation.version")
    private String version;

    /**
     * The level of the higher level title of the generated .adoc.
     */
    @Parameter(defaultValue = "2", property = "talend.documentation.level")
    private int level;

    /**
     * Where to output the .adoc, it supports "%s" variable to pass the locale.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/TALEND-INF/documentation%s.adoc",
            property = "talend.documentation.output")
    private String output;

    /**
     * If set a the generated .adoc will be included in a document ready to render.
     * Values can be pdf or html.
     * The key is the format and the value the output path.
     */
    @Parameter(property = "talend.documentation.formats")
    private Map<String, String> formats;

    /**
     * If formats is set, the asciidoc attributes to use for the rendering.
     */
    @Parameter(property = "talend.documentation.attributes")
    private Map<String, String> attributes;

    /**
     * template directory for asciidoctor rendering - if formats is set.
     */
    @Parameter(property = "talend.documentation.templateDir")
    private File templateDir;

    /**
     * template engine for asciidoctor rendering - if formats is set.
     */
    @Parameter(property = "talend.documentation.templateEngine")
    private String templateEngine;

    /**
     * The title of the documentation if rendered as pdf or html (see formats).
     */
    @Parameter(property = "talend.documentation.title")
    private String title;

    /**
     * Render html and PDF outputs flag.
     */
    @Parameter(property = "talend.documentation.htmlAndPdf", defaultValue = "false")
    private boolean htmlAndPdf;

    /**
     * The title of the documentation if rendered as pdf or html (see formats).
     */
    @Parameter(property = "talend.documentation.attach", defaultValue = "true")
    private boolean attachDocumentations;

    /**
     * template directory for asciidoctor rendering - if formats is set.
     */
    @Parameter(property = "talend.documentation.workdDir",
            defaultValue = "${project.build.directory}/talend-component/workdir")
    private File workDir;

    /**
     * Locales to generate a documentation for.
     */
    @Parameter(property = "talend.documentation.locales", defaultValue = "<root>,en")
    private Collection<String> locales;

    @Component
    private MavenProjectHelper helper;

    @Override
    public void doExecute() {
        if (locales == null || locales.isEmpty()) {
            getLog().warn("No locale set, skipping documentation generation");
            return;
        }

        final String title =
                this.title == null ? ofNullable(project.getName()).orElse(project.getArtifactId()) : this.title;
        final File[] classes = { this.classes };

        final List<File> adocs =
                locales.stream().map(it -> "<root>".equals(it) ? ROOT : new Locale(it)).flatMap(locale -> {
                    final String localeStr = locale.toString();
                    final File output =
                            new File(String.format(this.output, localeStr.isEmpty() ? "" : ("_" + localeStr)));
                    final Map<String, String> formats = htmlAndPdf ? new HashMap<String, String>() {

                        {
                            put("html", new File(output.getParentFile(), output.getName().replace(".adoc", ".html"))
                                    .getAbsolutePath());
                            put("pdf", new File(output.getParentFile(), output.getName().replace(".adoc", ".pdf"))
                                    .getAbsolutePath());
                        }
                    } : this.formats;
                    new AsciidocDocumentationGenerator(classes, output, title, level, formats, attributes, templateDir,
                            templateEngine, getLog(), workDir, version, locale).run();
                    return formats == null || formats.isEmpty() ? Stream.of(output)
                            : Stream.concat(Stream.of(output), formats.values().stream().map(File::new));
                }).collect(toList());

        if (attachDocumentations) {
            adocs.forEach(artifact -> {
                final String artifactName = artifact.getName();
                int dot = artifactName.lastIndexOf('_');
                if (dot < 0) {
                    dot = artifactName.lastIndexOf('.');
                }
                getLog().info("Attaching " + artifact.getAbsolutePath());
                if (dot > 0) {
                    helper
                            .attachArtifact(project, artifact,
                                    artifactName.substring(dot + 1).replace('.', '-') + "-documentation");
                } else {
                    helper.attachArtifact(project, artifact, artifactName + "-documentation");
                }
            });
        }
    }
}
