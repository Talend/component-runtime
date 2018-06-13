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

import static java.util.Optional.ofNullable;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.io.File;
import java.util.HashMap;
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
     * Where to output the .adoc.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/TALEND-INF/documentation.adoc",
            property = "talend.documentation.output")
    private File output;

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

    @Component
    private MavenProjectHelper helper;

    @Override
    public void doExecute() {
        final Map<String, String> formats = htmlAndPdf ? new HashMap<String, String>() {

            {
                put("html",
                        new File(output.getParentFile(), output.getName().replace(".adoc", ".html")).getAbsolutePath());
                put("pdf",
                        new File(output.getParentFile(), output.getName().replace(".adoc", ".pdf")).getAbsolutePath());
            }
        } : this.formats;
        new AsciidocDocumentationGenerator(new File[] { classes }, output,
                title == null ? ofNullable(project.getName()).orElse(project.getArtifactId()) : title, level, formats,
                attributes, templateDir, templateEngine, getLog(), workDir, version).run();
        if (attachDocumentations) {
            Stream
                    .concat(Stream.of(output),
                            ofNullable(this.formats).map(m -> m.values().stream().map(File::new)).orElseGet(
                                    Stream::empty))
                    .filter(File::exists)
                    .forEach(artifact -> {
                        final String artifactName = artifact.getName();
                        final int dot = artifactName.lastIndexOf('.');
                        if (dot > 0) {
                            helper.attachArtifact(project, artifact,
                                    artifactName.substring(dot + 1, artifactName.length()) + "-documentation");
                        } else {
                            helper.attachArtifact(project, artifact, artifactName + "-documentation");
                        }
                    });
        }
    }
}
