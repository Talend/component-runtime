/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.gradle;

import java.io.File;
import java.util.Map;

import org.gradle.api.tasks.TaskAction;

public class DeployInStudioTask extends TaCoKitTask {

    @TaskAction
    public void deployInStudio() {
        executeInContext(() -> {
            try {
                doDeployInStudio();
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private void doDeployInStudio() throws Exception {
        final TaCoKitExtension extension =
                TaCoKitExtension.class.cast(getProject().getExtensions().findByName("talendComponentKit"));
        if (extension.getStudioHome() == null) {
            getLogger().info("No studioHome, skipping");
            return;
        }

        getLogger().warn("Experimental feature");

        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        final Class<?> impl = tccl.loadClass("org.talend.sdk.component.tools.StudioInstaller");
        final Map<String, File> artifacts = artifacts();
        final String mainGav = mainGav();
        final Runnable runnable = Runnable.class
                .cast(impl
                        .getConstructor(String.class, File.class, Map.class, Object.class, boolean.class, File.class)
                        .newInstance(mainGav, extension.getStudioHome(), artifacts, getLogger(),
                                extension.isEnforceDeployment(), extension.getStudioM2()));
        runnable.run();
    }
}
