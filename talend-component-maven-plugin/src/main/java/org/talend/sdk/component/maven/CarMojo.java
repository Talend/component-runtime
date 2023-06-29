/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
import static org.talend.sdk.component.maven.api.Constants.CAR_EXTENSION;

import java.io.File;
import java.util.Map;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.talend.sdk.component.maven.api.Audience;
import org.talend.sdk.component.tools.CarBundler;

/**
 * Bundles the component as a component archive (.car).
 */
@Audience(PUBLIC)
@Mojo(name = "car", requiresDependencyResolution = COMPILE_PLUS_RUNTIME, threadSafe = true)
public class CarMojo extends DependencyAwareMojo {

    @Parameter(defaultValue = "${project.packaging}")
    private String packaging;

    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.car",
            property = "talend.car.output")
    private File output;

    /**
     * Should the component archive be attached.
     */
    @Parameter(defaultValue = "true", property = "talend.car.attach")
    private boolean attach;

    /**
     * The classifier to use if attach is true.
     */
    @Parameter(defaultValue = "component", property = "talend.car.classifier")
    private String classifier;

    /**
     * Additional custom metadata to bundle in the component archive.
     */
    @Parameter
    private Map<String, String> metadata;

    /**
     * Should this execution be skipped.
     */
    @Parameter(defaultValue = "false", property = "talend.car.skip")
    private boolean skip;

    /**
     * Is it a `connector' or an `extension' bundled?
     */
    @Parameter(defaultValue = "connector", property = "talend.car.type")
    private String type;

    @Component
    private MavenProjectHelper helper;

    @Override
    public void execute() throws MojoFailureException {
        if (skip) {
            return;
        }
        if ("pom".equals(packaging)) {
            getLog().info("Skipping car creation since the packaging is of type pom");
            return;
        }

        final CarBundler.Configuration configuration = new CarBundler.Configuration();
        configuration.setMainGav(mainGav());
        configuration.setOutput(output);
        configuration.setArtifacts(artifacts());
        configuration.setVersion(project.getVersion());
        configuration.setType(type);
        configuration.setCustomMetadata(metadata);
        new CarBundler(configuration, getLog()).run();
        if (attach) {
            helper.attachArtifact(project, CAR_EXTENSION, classifier, output);
            getLog().info("Attached " + output + " with classifier " + classifier);
        }
    }
}
