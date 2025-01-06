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

import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.talend.sdk.component.maven.api.Audience.Type.PUBLIC;

import java.util.Collection;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.talend.sdk.component.maven.api.Audience;
import org.talend.sdk.component.tools.WebServer;

import lombok.Data;

/**
 * Starts a small web server where you can test the rendering of your
 * component forms.
 */
@Audience(PUBLIC)
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

    @Parameter
    private UiConfiguration uiConfiguration;

    @Parameter(defaultValue = "true", property = "talend.web.openBrowser")
    private boolean openBrowser;

    @Parameter(defaultValue = "false", property = "talend.web.batch")
    private boolean batchMode;

    @Parameter(defaultValue = "2", property = "talend.web.batch.timeout")
    private int batchTimeout;

    @Override
    public void execute() {
        final String originalRepoSystProp = System.getProperty("talend.component.server.maven.repository");
        System.setProperty("talend.component.server.maven.repository", settings.getLocalRepository());
        if (uiConfiguration != null) {
            if (uiConfiguration.getCssLocation() == null || uiConfiguration.getJsLocation() == null) {
                throw new IllegalArgumentException("Either don't set <uiConfiguration /> or set js AND css locations");
            }
            System.setProperty("talend.tools.web.ui.js", uiConfiguration.getJsLocation());
            System.setProperty("talend.tools.web.ui.css", uiConfiguration.getCssLocation());
        }
        try {
            final WebServer webServer = new WebServer(serverArguments, port, getLog(),
                    String.format("%s:%s:%s", project.getGroupId(), project.getArtifactId(), project.getVersion()));
            if (!batchMode && openBrowser) {
                webServer.openBrowserWhenReady();
            }
            webServer.run();
        } finally {
            if (originalRepoSystProp == null) {
                System.clearProperty("talend.component.server.maven.repository");
            } else {
                System.setProperty("talend.component.server.maven.repository", originalRepoSystProp);
            }
            System.clearProperty("talend.tools.web.ui.js");
            System.clearProperty("talend.tools.web.ui.css");
        }
    }

    @Data
    public static class UiConfiguration {

        @Parameter(property = "talend.web.ui.jsLocation")
        private String jsLocation;

        @Parameter(property = "talend.web.ui.cssLocation")
        private String cssLocation;
    }
}
