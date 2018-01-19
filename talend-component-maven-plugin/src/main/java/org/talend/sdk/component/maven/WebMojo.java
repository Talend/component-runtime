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

import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.util.Collection;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.talend.sdk.component.tools.WebServer;

/**
 * Starts a small web server where you can test the rendering of your
 * component forms.
 */
@Mojo(name = "web", requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class WebMojo extends AbstractMojo {

    @Parameter
    private Collection<String> serverArguments;

    @Parameter(property = "talend.web.port", defaultValue = "8080")
    private Integer port;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    @Override
    public void execute() {
        final String originalRepoSystProp = System.getProperty("talend.component.server.maven.repository");
        System.setProperty("talend.component.server.maven.repository", settings.getLocalRepository());
        try {
            new WebServer(serverArguments, port, getLog(),
                    String.format("%s:%s:%s", project.getGroupId(), project.getArtifactId(), project.getVersion()))
                            .run();
        } finally {
            if (originalRepoSystProp == null) {
                System.clearProperty("talend.component.server.maven.repository");
            } else {
                System.setProperty("talend.component.server.maven.repository", originalRepoSystProp);
            }
        }
    }
}
