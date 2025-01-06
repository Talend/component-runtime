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
package org.talend.sdk.component.gradle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.TaskAction;

public class DependenciesTask extends BaseTask {

    @TaskAction // based on maven dependency:list
    public void createTalendComponentDependenciesTxt() {
        TaCoKitExtension extension = getKitExtension();
        if (extension.isSkipDependenciesFile()) {
            getLogger().warn("Skipping Talend Component Kit dependency file creation");
            return;
        }

        final ConfigurationContainer configurations = getProject().getConfigurations();
        final Configuration compileConfig = configurations.getByName("compile");
        if (compileConfig == null) {
            getLogger().info("No compile scope, skipping the creationg of Talend Component Kit dependency files.");
            return;
        }
        final Set<ResolvedArtifact> artifacts = compileConfig.getResolvedConfiguration().getResolvedArtifacts();

        final Configuration providedConfig = configurations.getByName("provided");
        if (providedConfig != null) {
            artifacts.removeAll(providedConfig.getResolvedConfiguration().getResolvedArtifacts());
        }

        final File output =
                new File(getProject().getBuildDir(), "resources/main/" + extension.getDependenciesLocation());
        output.getParentFile().mkdirs();
        try (final Writer writer = new FileWriter(output)) {
            writer.write(" \n The following files have been resolved:\n\n");
            artifacts.stream().filter(d -> "jar".equals(d.getType())).forEach(d -> {
                try {
                    writer
                            .write("    " + d.getModuleVersion().getId().getGroup() + ':' + d.getName() + ':'
                                    + d.getType() + ':' + d.getModuleVersion().getId().getVersion() + ":compile\n");
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
