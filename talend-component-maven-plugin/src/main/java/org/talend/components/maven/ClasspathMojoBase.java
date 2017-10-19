// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.maven;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.FileArchive;
import org.talend.component.api.input.Emitter;
import org.talend.component.api.input.PartitionMapper;
import org.talend.component.api.processor.Processor;

public abstract class ClasspathMojoBase extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    protected File classes;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.packaging}", readonly = true)
    protected String packaging;

    private ClassLoader pluginLoader;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
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

        pluginLoader = Thread.currentThread().getContextClassLoader();
        try (final URLClassLoader loader = new AccessibleClassLoader(
                Stream.concat(Stream.of(classes), project.getArtifacts().stream().map(Artifact::getFile)).map(file -> {
                    try {
                        return file.toURI().toURL();
                    } catch (final MalformedURLException e) {
                        throw new IllegalStateException(e.getMessage());
                    }
                }).toArray(URL[]::new), Thread.currentThread().getContextClassLoader()) {

            {
                Thread.currentThread().setContextClassLoader(this);
            }

            @Override
            public void close() throws IOException {
                Thread.currentThread().setContextClassLoader(getParent());
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
        getLog().warn("This mojo (" + getClass().getSimpleName() + ") is deprecated, use it only if you know what you do");
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

    protected Stream<Class<? extends Annotation>> componentMarkers() {
        return Stream.of(PartitionMapper.class, Processor.class, Emitter.class);
    }

    protected AnnotationFinder newFinder() {
        return new AnnotationFinder(new FileArchive(Thread.currentThread().getContextClassLoader(), classes));
    }

    protected static class AccessibleClassLoader extends URLClassLoader {
        private AccessibleClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        public Package findPackage(final String pck) {
            return getPackage(pck);
        }
    }
}
