/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
import static java.util.stream.Collectors.toMap;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.io.File;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.talend.sdk.component.tools.StudioInstaller;

// mvn talend-component:deploy-in-studio -Dtalend.component.studioHome=/path/to/studio
@Mojo(name = "deploy-in-studio", requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class DeployStudioMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(property = "talend.component.studioHome")
    private File studioHome;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (studioHome == null) {
            getLog().warn("No studioHome defined, skipping");
            return;
        }

        getLog().warn("Experimental feature");

        final Map<String, File> artifacts = project
                .getArtifacts()
                .stream()
                .filter(a -> !"org.talend.sdk.component".equals(a.getGroupId())
                        && ("compile".equals(a.getScope()) || "runtime".equals(a.getScope())))
                .collect(toMap(a -> String.format("%s:%s:%s%s:%s:%s", a.getGroupId(), a.getArtifactId(),
                        ofNullable(a.getType()).orElse("jar"),
                        a.getClassifier() == null || a.getClassifier().isEmpty() ? "" : (":" + a.getClassifier()),
                        a.getVersion(), ofNullable(a.getScope()).orElse("compile")), Artifact::getFile));
        new StudioInstaller(// group:artifact:type[:classifier]:version:scope
                String.format("%s:%s:%s", project.getGroupId(), project.getArtifactId(), project.getVersion()),
                studioHome, artifacts, getLog()).run();
    }
}
