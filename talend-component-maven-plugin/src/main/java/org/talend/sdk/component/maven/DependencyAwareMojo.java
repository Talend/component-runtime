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
package org.talend.sdk.component.maven;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Bundles the component as a component archive (.car).
 */
public abstract class DependencyAwareMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    protected Map<String, File> artifacts() {
        final Map<String, File> artifacts = project
                .getArtifacts()
                .stream()
                .filter(a -> !"org.talend.sdk.component".equals(a.getGroupId())
                        && ("compile".equals(a.getScope()) || "runtime".equals(a.getScope())))
                .collect(toMap(a -> String
                        .format("%s:%s:%s%s:%s:%s", a.getGroupId(), a.getArtifactId(),
                                ofNullable(a.getType()).orElse("jar"),
                                a.getClassifier() == null || a.getClassifier().isEmpty() ? ""
                                        : (":" + a.getClassifier()),
                                a.getVersion(), ofNullable(a.getScope()).orElse("compile")),
                        Artifact::getFile));

        final String mainGav = mainGav();
        artifacts
                .putIfAbsent(mainGav, new File(project.getBuild().getDirectory(), project.getBuild().getFinalName()
                        + "." + ("bundle".equals(project.getPackaging()) ? "jar" : project.getPackaging())));
        return artifacts;
    }

    protected String mainGav() {
        return String.format("%s:%s:%s", project.getGroupId(), project.getArtifactId(), project.getVersion());
    }
}
