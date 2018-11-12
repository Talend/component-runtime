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

import static java.util.stream.Collectors.joining;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Creates the file descriptor listing dependencies needed at runtime for the module.
 */
@Mojo(name = "dependencies", requiresDependencyResolution = COMPILE_PLUS_RUNTIME, threadSafe = true)
public class DependencyMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "compile,runtime", readonly = true)
    private List<String> scopes;

    @Parameter(defaultValue = "${project.build.outputDirectory}/TALEND-INF/dependencies.txt",
            property = "talend.dependency.output")
    private File output;

    @Override
    public void execute() throws MojoExecutionException {
        final String content = project
                .getArtifacts()
                .stream()
                .filter(a -> scopes == null || scopes.contains(a.getScope()))
                .map(a -> String
                        .format("%s:%s:%s%s:%s:%s", a.getGroupId(), a.getArtifactId(), a.getType(),
                                a.getClassifier() == null || a.getClassifier().isEmpty() ? ""
                                        : (":" + a.getClassifier()),
                                a.getBaseVersion(), a.getScope()))
                .collect(joining("\n"));
        output.getParentFile().mkdirs();
        try (final Writer writer = new BufferedWriter(new FileWriter(output))) {
            writer.write(content);
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        getLog().info("Generated " + output.getAbsolutePath());
    }
}
