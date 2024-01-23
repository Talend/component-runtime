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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import lombok.Getter;

public abstract class ClasspathMojoBase extends AudienceAwareMojo {

    @Parameter(defaultValue = "false", property = "talend.skip")
    private boolean skip;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    protected File classes;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.packaging}", readonly = true)
    protected String packaging;

    private ClassLoader pluginLoader;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        executeInLoader();
    }

    protected List<String> getExcludes(final Collection<String> excludes, final Collection<String> sharedExcludes) {
        return Stream
                .of(excludes, sharedExcludes)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    protected Stream<File> getJarToScan(final Collection<String> deps) {
        if (deps == null || deps.isEmpty()) {
            return Stream.empty();
        }
        return deps
                .stream()
                .map(it -> project
                        .getArtifacts()
                        .stream()
                        .filter(art -> it.equals(art.getGroupId() + ':' + art.getArtifactId()))
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .map(Artifact::getFile)
                .filter(Objects::nonNull);
    }

    protected void executeInLoader() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info(getClass().getSimpleName() + " is skipped");
            return;
        }
        if (getClass().isAnnotationPresent(Deprecated.class)) {
            logDeprecated();
        }

        if ("pom".equals(packaging)) {
            getLog().info("Skipping modules with packaging pom");
            return;
        }

        if (!classes.isDirectory()) {
            getLog().warn(classes + " is not a directory, skipping");
            return;
        }

        pluginInit();

        final Thread thread = Thread.currentThread();
        pluginLoader = thread.getContextClassLoader();
        final Collection<String> excludedArtifacts = Stream
                .of("container-core", "component-api", "component-spi", "component-runtime-impl",
                        "component-runtime-manager", "component-runtime-design-extension", "component-runtime-di")
                .collect(toSet());
        final List<File> classLoaderFiles = Stream
                .concat(Stream.of(classes),
                        project
                                .getArtifacts()
                                .stream()
                                .filter(a -> !"org.talend.sdk.component".equals(a.getGroupId())
                                        || !excludedArtifacts.contains(a.getArtifactId()))
                                .map(Artifact::getFile))
                .collect(toList());
        try (final URLClassLoader loader = new ExecutionClassLoader(classLoaderFiles.stream().map(file -> {
            try {
                return file.toURI().toURL();
            } catch (final MalformedURLException e) {
                throw new IllegalStateException(e.getMessage());
            }
        }).toArray(URL[]::new), classLoaderFiles, pluginLoader) {

            {
                thread.setContextClassLoader(this);
            }

            @Override
            public void close() throws IOException {
                thread.setContextClassLoader(pluginLoader);
                super.close();
            }
        }) {

            doExecute();
        } catch (final Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        } finally {
            pluginLoader = null;
        }
    }

    protected void logDeprecated() {
        getLog().warn("");
        getLog().warn("");
        getLog()
                .warn("This mojo (" + getClass().getSimpleName()
                        + ") is deprecated, use it only if you know what you do");
        getLog().warn("");
        getLog().warn("");
    }

    protected void pluginInit() throws MojoExecutionException {
        // no-op
    }

    protected <T> T inPluginContext(final Supplier<T> supplier) {
        final Thread thread = Thread.currentThread();
        final ClassLoader loader = thread.getContextClassLoader();
        thread.setContextClassLoader(pluginLoader);
        try {
            return supplier.get();
        } finally {
            thread.setContextClassLoader(loader);
        }
    }

    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;

    protected static class ExecutionClassLoader extends URLClassLoader {

        @Getter
        private final Collection<File> files;

        private ExecutionClassLoader(final URL[] urls, final Collection<File> files, final ClassLoader parent) {
            super(urls, parent);
            this.files = files;
        }
    }
}
