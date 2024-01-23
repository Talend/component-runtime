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
package org.talend.sdk.component.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;
import static org.talend.sdk.component.maven.api.Audience.Type.TALEND_INTERNAL;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.talend.sdk.component.maven.api.Audience;
import org.talend.sdk.component.tools.webapp.standalone.generator.StaticUiSpecGenerator;

@Audience(TALEND_INTERNAL)
@Mojo(name = "uispec", defaultPhase = PACKAGE, threadSafe = true, requiresDependencyResolution = TEST)
public class UiSpecGeneratorMojo extends BuildComponentM2RepositoryMojo {

    @Parameter(defaultValue = "en")
    private Collection<String> languages;

    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/target/talend-component-kit/uispec.zip")
    private File uiSpecZip;

    @Component
    private MavenProjectHelper helper;

    @Override
    public void doExecute() throws MojoExecutionException {
        super.doExecute();
        final Map<String, String> setup = new HashMap<>();
        setup.put("talend.component.server.maven.repository", m2Root.getAbsolutePath());
        setup.put("talend.component.server.component.registry", getRegistry().toAbsolutePath().toString());
        setup.put("talend.component.server.component.extend.dependencies", "false");
        new StaticUiSpecGenerator(setup, languages, uiSpecZip.toPath()).run();
        helper.attachArtifact(project, "zip", "uispec", uiSpecZip);
    }
}
