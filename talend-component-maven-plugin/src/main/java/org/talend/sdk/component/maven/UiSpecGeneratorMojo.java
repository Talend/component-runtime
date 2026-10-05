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

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;
import static org.talend.sdk.component.maven.api.Audience.Type.TALEND_INTERNAL;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.talend.sdk.component.maven.api.Audience;

@Audience(TALEND_INTERNAL)
@Mojo(name = "uispec", defaultPhase = PACKAGE, threadSafe = true, requiresDependencyResolution = TEST)
public class UiSpecGeneratorMojo extends BuildComponentM2RepositoryMojo {

    private static final String GENERATOR_CLASS =
            "org.talend.sdk.component.tools.webapp.standalone.generator.StaticUiSpecGenerator";

    private static final String VERSIONS_RESOURCE = "uispec-generator.properties";

    @Parameter(defaultValue = "en")
    private Collection<String> languages;

    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/target/talend-component-kit/uispec.zip")
    private File uiSpecZip;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor pluginDescriptor;

    @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true)
    private List<RemoteRepository> pluginRepositories;

    @Component
    private MavenProjectHelper helper;

    @Component
    private RepositorySystem uiSpecRepositorySystem;

    @Override
    public void doExecute() throws MojoExecutionException {
        super.doExecute();
        final Map<String, String> setup = new HashMap<>();
        setup.put("talend.component.server.maven.repository", m2Root.getAbsolutePath());
        setup.put("talend.component.server.component.registry", getRegistry().toAbsolutePath().toString());
        setup.put("talend.component.server.component.extend.dependencies", "false");
        generate(setup);
        helper.attachArtifact(project, "zip", "uispec", uiSpecZip);
    }

    /**
     * The plugin classloader also contains the dependencies of the components declared as plugin dependencies,
     * which can shadow the libraries the embedded server needs (servlet APIs, CXF, ...) or add unrelated CDI
     * extensions. The generator therefore runs in its own classloader, built like the component server image:
     * the webapp graph plus the CDI container and Beam, which the components expect from the runtime.
     */
    private void generate(final Map<String, String> setup) throws MojoExecutionException {
        final Thread thread = Thread.currentThread();
        final ClassLoader contextLoader = thread.getContextClassLoader();
        try (final URLClassLoader loader =
                new URLClassLoader(isolatedClasspath(), ClassLoader.getPlatformClassLoader())) {
            thread.setContextClassLoader(loader);
            final Runnable generator = (Runnable) loader
                    .loadClass(GENERATOR_CLASS)
                    .getConstructor(Map.class, Collection.class, Path.class)
                    .newInstance(setup, languages, uiSpecZip.toPath());
            generator.run();
        } catch (final IOException | ReflectiveOperationException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            thread.setContextClassLoader(contextLoader);
        }
    }

    private URL[] isolatedClasspath() throws MojoExecutionException {
        final Properties versions = loadGeneratorVersions();
        final String pluginVersion = pluginDescriptor.getVersion();
        final CollectRequest collect = new CollectRequest().setRepositories(pluginRepositories);
        Stream
                .of(new DefaultArtifact("org.talend.sdk.component", "component-tools-webapp", "jar", pluginVersion),
                        new DefaultArtifact("org.talend.sdk.component", "component-runtime-beam", "jar",
                                pluginVersion),
                        new DefaultArtifact("org.apache.openwebbeans", "openwebbeans-se", "jar",
                                versions.getProperty("openwebbeans.version")),
                        new DefaultArtifact("org.apache.beam", "beam-sdks-java-core", "jar",
                                versions.getProperty("beam.version")))
                .map(artifact -> new Dependency(artifact, "runtime"))
                .forEach(collect::addDependency);
        try {
            return uiSpecRepositorySystem
                    .resolveDependencies(repositorySystemSession, new DependencyRequest(collect, null))
                    .getArtifactResults()
                    .stream()
                    .map(ArtifactResult::getArtifact)
                    .map(a -> toUrl(a.getFile()))
                    .toArray(URL[]::new);
        } catch (final DependencyResolutionException e) {
            throw new MojoExecutionException("Can't resolve the uispec generator classpath: " + e.getMessage(), e);
        }
    }

    private Properties loadGeneratorVersions() throws MojoExecutionException {
        try (final InputStream stream = UiSpecGeneratorMojo.class.getResourceAsStream(VERSIONS_RESOURCE)) {
            if (stream == null) {
                throw new MojoExecutionException("Missing " + VERSIONS_RESOURCE + " in the plugin");
            }
            final Properties versions = new Properties();
            versions.load(stream);
            return versions;
        } catch (final IOException e) {
            throw new MojoExecutionException("Can't read " + VERSIONS_RESOURCE, e);
        }
    }

    private static URL toUrl(final File file) {
        try {
            return file.toURI().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
}
