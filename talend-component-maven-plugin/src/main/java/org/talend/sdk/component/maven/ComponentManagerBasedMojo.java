/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.maven;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.output.Branches;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;

public abstract class ComponentManagerBasedMojo extends ClasspathMojoBase {

    @Parameter(defaultValue = "${settings.localRepository}", property = "talend.manager.mavenrepository")
    protected File repository;

    @Parameter(defaultValue = "${project.artifactId}", readonly = true)
    protected String artifactId;

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        if (!classes.exists()) {
            throw new MojoExecutionException("No " + classes);
        }

        try (final ComponentManager manager = new ComponentManager(repository, "TALEND-INF/dependencies.txt",
                "org.talend.sdk.component:type=component,value=%s") {

            {
                addPlugin(artifactId, classes.getAbsolutePath());
            }
        }) {
            final Container container = manager.findPlugin(artifactId).get();
            final ContainerComponentRegistry registry = container.get(ContainerComponentRegistry.class);
            registry.getComponents().values().forEach(c -> {
                c.getPartitionMappers().forEach((k, p) -> getLog().info("Found component " + c.getName() + "#" + p.getName()));
                c.getProcessors().forEach((k, p) -> getLog().info("Found component " + c.getName() + "#" + p.getName()));
            });

            doWork(manager, container, registry);
        }
    }

    protected abstract void doWork(ComponentManager manager, Container container, ContainerComponentRegistry registry)
            throws MojoExecutionException, MojoFailureException;

    protected Method findListener(final ComponentFamilyMeta.ProcessorMeta p) {
        return Stream.of(p.getType().getMethods()).filter(m -> m.isAnnotationPresent(ElementListener.class)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No @ElementListener method in " + p.getType()));
    }

    protected Collection<String> buildInputs(final Method listener) {
        return findInputs(listener)
                .map(p -> ofNullable(p.getAnnotation(Input.class)).map(Input::value).orElse(Branches.DEFAULT_BRANCH))
                .collect(toSet());
    }

    // an input is a parameter without any @Input/@Output or an @Input parameter
    protected Stream<java.lang.reflect.Parameter> findInputs(final Method listener) {
        return Stream.of(listener.getParameters())
                     .filter(p -> p.isAnnotationPresent(Input.class) || !p.isAnnotationPresent(Output.class));
    }

    // an output is either the returned value of the method or an @Output parameter
    protected Collection<String> buildOutputs(final Method listener) {
        return Stream.concat(listener.getReturnType() != null ? Stream.of(Branches.DEFAULT_BRANCH) : Stream.empty(),
                Stream.of(listener.getParameters()).filter(p -> p.isAnnotationPresent(Output.class))
                        .map(p -> p.getAnnotation(Output.class).value()))
                .collect(toSet());
    }
}
