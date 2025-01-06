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

import java.util.HashMap;
import java.util.Optional;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public class TaCoKitPlugin implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        // setup the global config
        project.getExtensions().add("talendComponentKit", TaCoKitExtension.class);

        // ensure we can find our dependencies
        project.afterEvaluate(actionProject -> actionProject.getRepositories().mavenCentral());

        { // create the configuration for our task execution
            final Configuration configuration = project.getConfigurations().maybeCreate("talendComponentKit");
            TaCoKitExtension extension =
                    TaCoKitExtension.class.cast(project.getExtensions().findByName("talendComponentKit"));
            if (extension == null) {
                extension = new TaCoKitExtension();
            }

            final DependencyHandler dependencyHandler = project.getDependencies();
            final DependencySet dependencies = configuration.getDependencies();
            dependencies
                    .add(dependencyHandler
                            .create("org.talend.sdk.component:component-api:" + extension.getApiVersion()));
            dependencies
                    .add(dependencyHandler
                            .create("org.talend.sdk.component:component-tools:" + extension.getSdkVersion()));
            dependencies
                    .add(dependencyHandler
                            .create("org.talend.sdk.component:component-runtime-design-extension:"
                                    + extension.getSdkVersion()));
        }

        { // create the web configuration for our web task
            final Configuration webConfiguration = project.getConfigurations().maybeCreate("talendComponentKitWeb");
            TaCoKitExtension extension =
                    TaCoKitExtension.class.cast(project.getExtensions().findByName("talendComponentKitWeb"));
            if (extension == null) {
                extension = new TaCoKitExtension();
            }

            final DependencyHandler dependencyHandler = project.getDependencies();
            final DependencySet dependencies = webConfiguration.getDependencies();
            dependencies
                    .add(dependencyHandler
                            .create("org.talend.sdk.component:component-tools-webapp:" + extension.getSdkVersion()));
        }

        // tasks
        final String group = "Talend Component Kit";

        // TALEND-INF/dependencies.txt
        project.task(new HashMap<String, Object>() {

            {
                put(Task.TASK_TYPE, DependenciesTask.class);
                put(Task.TASK_GROUP, group);
                put(Task.TASK_DESCRIPTION,
                        "Creates the Talend Component Kit dependencies file used by the runtime to build the component classloader");
                put(Task.TASK_DEPENDS_ON, "classes");
            }
        }, "talendComponentKitDependencies");

        // validation
        project.task(new HashMap<String, Object>() {

            {
                put(Task.TASK_TYPE, ValidateTask.class);
                put(Task.TASK_GROUP, group);
                put(Task.TASK_DESCRIPTION,
                        "Validates that the module components are respecting the component standards.");
                put(Task.TASK_DEPENDS_ON,
                        "talendComponentKitDependencies,talendComponentKitDocumentation,talendComponentKitSVG2PNG");
            }
        }, "talendComponentKitValidation");

        // documentation
        project.task(new HashMap<String, Object>() {

            {
                put(Task.TASK_TYPE, DocumentationTask.class);
                put(Task.TASK_GROUP, group);
                put(Task.TASK_DESCRIPTION, "Generates an asciidoc file with the documentation of the components.");
                put(Task.TASK_DEPENDS_ON, "classes");
            }
        }, "talendComponentKitDocumentation");

        // convert SVG into PNG when needed
        project.task(new HashMap<String, Object>() {

            {
                put(Task.TASK_TYPE, SVG2PngTask.class);
                put(Task.TASK_GROUP, group);
                put(Task.TASK_DESCRIPTION, "Converts the SVG into PNG when needed (icons).");
                put(Task.TASK_DEPENDS_ON, "classes");
            }
        }, "talendComponentKitSVG2PNG");

        // web
        project.task(new HashMap<String, Object>() {

            {
                put(Task.TASK_TYPE, WebTask.class);
                put(Task.TASK_GROUP, group);
                put(Task.TASK_DESCRIPTION,
                        "Starts a web server allowing you to browse your components (requires the component to be installed before).");
                put(Task.TASK_DEPENDS_ON, "jar");
            }
        }, "talendComponentKitWebServer");

        // car
        project.task(new HashMap<String, Object>() {

            {
                put(Task.TASK_TYPE, CarTask.class);
                put(Task.TASK_GROUP, group);
                put(Task.TASK_DESCRIPTION, "Creates a Component ARchive (.car) based on current project.");
                put(Task.TASK_DEPENDS_ON, "jar");
            }
        }, "talendComponentKitComponentArchive");

        // deploy in studio
        project.task(new HashMap<String, Object>() {

            {
                put(Task.TASK_TYPE, DeployInStudioTask.class);
                put(Task.TASK_GROUP, group);
                put(Task.TASK_DESCRIPTION, "Deploys the module components to the Studio.");
                put(Task.TASK_DEPENDS_ON, "jar");
            }
        }, "talendComponentKitDeployInStudio");

        project.afterEvaluate(p -> {
            final Task car = getTask(p, "talendComponentKitComponentArchive").orElseThrow(IllegalStateException::new);
            final Task dependencies =
                    getTask(p, "talendComponentKitDependencies").orElseThrow(IllegalStateException::new);
            final Task documentation =
                    getTask(p, "talendComponentKitDocumentation").orElseThrow(IllegalStateException::new);
            final Task svg2PNG = getTask(p, "talendComponentKitSVG2PNG").orElseThrow(IllegalStateException::new);
            final Task validation = getTask(p, "talendComponentKitValidation").orElseThrow(IllegalStateException::new);

            getTask(p, "classes").orElseThrow(IllegalStateException::new).doLast(task -> {
                DependenciesTask.class.cast(dependencies).createTalendComponentDependenciesTxt();
                DocumentationTask.class.cast(documentation).asciidoc();
                SVG2PngTask.class.cast(svg2PNG).convertSVGIconsToPng();
            });
            getTask(p, "jar").orElseThrow(IllegalStateException::new).doLast(task -> {
                ValidateTask.class.cast(validation).validateTalendComponents();
                CarTask.class.cast(car).createCar();
            });
        });
    }

    private Optional<Task> getTask(final Project p, final String name) {
        return p.getTasksByName(name, false).stream().findFirst();
    }
}
