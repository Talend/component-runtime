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
package org.talend.sdk.component.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.talend.sdk.component.maven.api.Audience.Type.TALEND_INTERNAL;

import java.io.File;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Stream;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.maven.api.Audience;
import org.talend.sdk.component.tools.DitaDocumentationGenerator;

/**
 * Generates a zip with documentation at dita format.
 */
@Audience(TALEND_INTERNAL)
@Mojo(name = "dita", defaultPhase = PROCESS_CLASSES, requiresDependencyResolution = COMPILE_PLUS_RUNTIME,
        threadSafe = true)
public class DitaMojo extends ClasspathMojoBase {

    /**
     * Where to output the zip containing the .dita.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}-dita.zip",
            property = "talend.dita.output")
    private String output;

    /**
     * Should the zip be attached to the project.
     */
    @Parameter(property = "talend.dita.attach", defaultValue = "true")
    private boolean attach;

    /**
     * Locales to generate a documentation for.
     */
    @Parameter(property = "talend.dita.locales", defaultValue = "en")
    private Collection<String> locales;

    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "talend.dita.transitive.dependencies")
    private Collection<String> includedDependencies;

    @Parameter(property = "talend.dita.excludes")
    private Collection<String> excludes;

    @Parameter(property = "talend.excludes")
    private Collection<String> sharedExcludes;

    @Parameter(property = "talend.dita.ignoreTypeColumn", defaultValue = "true")
    private boolean ignoreTypeColumn;

    @Parameter(property = "talend.dita.ignorePathColumn", defaultValue = "true")
    private boolean ignorePathColumn;

    @Component
    private MavenProjectHelper helper;

    @Override
    public void doExecute() {
        if (locales == null) {
            return;
        }
        locales.stream().map(Locale::new).map(locale -> {
            final String localeStr = locale.toString();
            final File output = new File(String.format(this.output, localeStr.isEmpty() ? "" : ("_" + localeStr)));

            final Collection<String> exclusions = getExcludes(excludes, sharedExcludes);
            new DitaDocumentationGenerator(getClasses(), locale, getLog(), output, ignoreTypeColumn, ignorePathColumn) {

                @Override
                protected Stream<Class<?>> findComponents(final AnnotationFinder finder) {
                    return super.findComponents(finder).filter(it -> !exclusions.contains(it.getName()));
                }
            }.run();
            return output;
        }).filter(it -> attach).forEach(artifact -> {
            final String artifactName = artifact.getName();
            int dot = artifactName.lastIndexOf('_');
            if (dot < 0) {
                dot = artifactName.lastIndexOf('.');
            }
            getLog().info("Attaching " + artifact.getAbsolutePath());
            if (dot > 0) {
                helper
                        .attachArtifact(project, "zip",
                                artifactName.substring(dot + 1).replace('.', '-') + "-documentation", artifact);
            } else {
                helper.attachArtifact(project, "zip", artifactName + "-documentation", artifact);
            }
        });
    }

    private File[] getClasses() {
        return (includedDependencies == null ? Stream.of(classes)
                : Stream.concat(Stream.of(classes), getJarToScan(includedDependencies))).toArray(File[]::new);
    }
}
