/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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

import java.io.File;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.talend.sdk.component.maven.api.Audience;
import org.talend.sdk.component.tools.StudioInstaller;

// mvn talend-component:deploy-in-studio -Dtalend.component.studioHome=/path/to/studio

/**
 * Deploys into a local Talend Studio Integration instance the current component module.
 */
@Audience(PUBLIC)
@Mojo(name = "deploy-in-studio", requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class DeployStudioMojo extends DependencyAwareMojo {

    @Parameter(property = "talend.component.studioHome")
    private File studioHome;

    @Parameter(property = "talend.component.studioM2")
    private File studioM2;

    @Parameter(property = "talend.component.enforceDeployment", defaultValue = "false")
    private boolean enforceDeployment;

    @Override
    public void execute() throws MojoFailureException {
        if (studioHome == null) {
            getLog().warn("No studioHome defined, skipping");
            return;
        }

        new StudioInstaller( // group:artifact:type[:classifier]:version:scope
                mainGav(), studioHome, artifacts(), getLog(), enforceDeployment, studioM2).run();
    }
}
