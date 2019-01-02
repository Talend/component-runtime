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
package org.talend.sdk.component.gradle;

import static java.util.Collections.singleton;

import java.util.HashMap;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
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

        // create the configuration for our task execution
        final Configuration configuration = project.getConfigurations().maybeCreate("talendComponentKit");
        configuration.getIncoming().beforeResolve(resolvableDependencies -> {
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
        });

        // create the web configuration for our web task
        final Configuration webConfiguration = project.getConfigurations().maybeCreate("talendComponentKitWeb");
        webConfiguration.getIncoming().beforeResolve(resolvableDependencies -> {
            TaCoKitExtension extension =
                    TaCoKitExtension.class.cast(project.getExtensions().findByName("talendComponentKitWeb"));
            if (extension == null) {
                extension = new TaCoKitExtension();
            }

            final DependencyHandler dependencyHandler = project.getDependencies();
            final DependencySet dependencies = configuration.getDependencies();
            dependencies
                    .add(dependencyHandler
                            .create("org.talend.sdk.component:component-tools-webapp:" + extension.getSdkVersion()));
        });

        // tasks
        final String group = "Talend Component Kit";

        // TALEND-INF/dependencies.txt
        project.task(new HashMap<String, Object>() {

            {
                put("type", DependenciesTask.class);
                put("group", group);
                put("description",
                        "Creates the Talend Component Kit dependencies file used by the runtime to build the component classloader");
            }
        }, "talendComponentKitDependencies");
        project
                .afterEvaluate(p -> p
                        .getTasksByName("compileJava", false)
                        .stream()
                        .findFirst()
                        .ifPresent(compileJava -> compileJava
                                .setFinalizedBy(singleton("talendComponentKitDependencies"))));

        // validation
        project.task(new HashMap<String, Object>() {

            {
                put("type", ValidateTask.class);
                put("group", group);
                put("description", "Validates that the module components are respecting the component standards.");
            }
        }, "talendComponentKitValidation");
        project
                .afterEvaluate(p -> p
                        .getTasksByName("classes", false)
                        .stream()
                        .findFirst()
                        .ifPresent(
                                compileJava -> compileJava.setFinalizedBy(singleton("talendComponentKitValidation"))));

        // documentation
        project.task(new HashMap<String, Object>() {

            {
                put("type", DocumentationTask.class);
                put("group", group);
                put("description", "Generates an asciidoc file with the documentation of the components.");
            }
        }, "talendComponentKitDocumentation");
        project
                .afterEvaluate(p -> p
                        .getTasksByName("classes", false)
                        .stream()
                        .findFirst()
                        .ifPresent(compileJava -> compileJava
                                .setFinalizedBy(singleton("talendComponentKitDocumentation"))));

        // web
        project.task(new HashMap<String, Object>() {

            {
                put("type", WebTask.class);
                put("group", group);
                put("description",
                        "Starts a web server allowing you to browse your components (requires the component to be installed before).");
            }
        }, "talendComponentKitWebServer");

        // car
        project.task(new HashMap<String, Object>() {

            {
                put("type", CarTask.class);
                put("group", group);
                put("description", "Creates a Component ARchive (.car) based on current project.");
            }
        }, "talendComponentKitComponentArchive");
    }
}
