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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.junit.jupiter.api.Test;

class UiSpecGeneratorMojoTest {

    @Test
    void generatorVersionsAreFiltered() throws MojoExecutionException {
        final Properties versions = UiSpecGeneratorMojo.loadGeneratorVersions();
        assertFalse(versions.getProperty("openwebbeans.version", "").isEmpty());
        assertFalse(versions.getProperty("beam.version", "").isEmpty());
        versions.forEach((key, value) -> assertFalse(String.valueOf(value).contains("${"), key + "=" + value));
    }

    @Test
    void generatorRunsInIsolatedLoader() throws Exception {
        final Thread thread = Thread.currentThread();
        final ClassLoader before = thread.getContextClassLoader();
        final Map<String, String> setup = new HashMap<>();

        UiSpecGeneratorMojo
                .runIsolated(testClasspath(), RecordingGenerator.class.getName(), setup, List.of("en"),
                        Paths.get("uispec.zip"));

        assertEquals("true", setup.get("tccl.is.generator.loader"));
        assertEquals("true", setup.get("loader.is.url.loader"));
        assertNotEquals(String.valueOf(System.identityHashCode(getClass().getClassLoader())),
                setup.get("generator.loader"));
        assertEquals("en", setup.get("languages"));
        assertEquals("uispec.zip", setup.get("output"));
        assertSame(before, thread.getContextClassLoader());
    }

    @Test
    void contextLoaderIsRestoredWhenGeneratorFails() {
        final Thread thread = Thread.currentThread();
        final ClassLoader before = thread.getContextClassLoader();

        final URL[] classpath = testClasspath();
        final String generator = FailingGenerator.class.getName();
        final Map<String, String> setup = new HashMap<>();
        final Path output = Paths.get("uispec.zip");

        final IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> UiSpecGeneratorMojo.runIsolated(classpath, generator, setup, List.of(), output));

        assertEquals("generator failure", error.getMessage());
        assertSame(before, thread.getContextClassLoader());
    }

    @Test
    void unknownGeneratorIsWrapped() {
        final Thread thread = Thread.currentThread();
        final ClassLoader before = thread.getContextClassLoader();

        final URL[] classpath = testClasspath();
        final Map<String, String> setup = new HashMap<>();
        final Path output = Paths.get("uispec.zip");

        final MojoExecutionException error = assertThrows(MojoExecutionException.class,
                () -> UiSpecGeneratorMojo.runIsolated(classpath, "org.talend.Missing", setup, List.of(), output));

        assertTrue(error.getCause() instanceof ClassNotFoundException);
        assertSame(before, thread.getContextClassLoader());
    }

    @Test
    void generatorWithoutExpectedConstructorIsWrapped() {
        final URL[] classpath = testClasspath();
        final String generator = WrongConstructorGenerator.class.getName();
        final Map<String, String> setup = new HashMap<>();
        final Path output = Paths.get("uispec.zip");

        final MojoExecutionException error = assertThrows(MojoExecutionException.class,
                () -> UiSpecGeneratorMojo.runIsolated(classpath, generator, setup, List.of(), output));

        assertTrue(error.getCause() instanceof NoSuchMethodException);
    }

    @Test
    void generatorInstantiationErrorIsWrapped() {
        final URL[] classpath = testClasspath();
        final String generator = BrokenConstructorGenerator.class.getName();
        final Map<String, String> setup = new HashMap<>();
        final Path output = Paths.get("uispec.zip");

        final MojoExecutionException error = assertThrows(MojoExecutionException.class,
                () -> UiSpecGeneratorMojo.runIsolated(classpath, generator, setup, List.of(), output));

        assertNotNull(error.getCause());
        assertTrue(error.getCause() instanceof InvocationTargetException);
    }

    @Test
    void classpathRequestCarriesRootsScopeAndRepositories() throws MojoExecutionException {
        final RemoteRepository repository = new RemoteRepository.Builder("central", "default", "http://repo.local")
                .build();
        final AtomicReference<DependencyRequest> request = new AtomicReference<>();
        final RepositorySystem system = repositorySystem((req, session) -> {
            request.set(req);
            return new DependencyResult(req);
        });

        final URL[] classpath =
                UiSpecGeneratorMojo.resolveClasspath(system, null, List.of(repository), "1.2.3", versions());

        assertEquals(0, classpath.length);
        final CollectRequest collect = request.get().getCollectRequest();
        assertEquals(List.of(repository), collect.getRepositories());
        assertEquals(List.of("org.talend.sdk.component:component-tools-webapp:jar:1.2.3",
                "org.talend.sdk.component:component-runtime-beam:jar:1.2.3",
                "org.apache.openwebbeans:openwebbeans-se:jar:9.9.1",
                "org.apache.beam:beam-sdks-java-core:jar:9.9.2"),
                collect.getDependencies().stream().map(d -> d.getArtifact().toString()).toList());
        assertTrue(collect.getDependencies().stream().allMatch(d -> "runtime".equals(d.getScope())));
    }

    @Test
    void resolvedArtifactsAreConvertedToUrlsInOrder() throws MojoExecutionException {
        final File first = new File("first.jar");
        final File second = new File("second.jar");
        final RepositorySystem system = repositorySystem((req, session) -> {
            final DependencyResult result = new DependencyResult(req);
            result.setArtifactResults(List.of(resolved(first), resolved(second)));
            return result;
        });

        final URL[] classpath = UiSpecGeneratorMojo.resolveClasspath(system, null, List.of(), "1.2.3", versions());

        assertEquals(2, classpath.length);
        assertEquals(first.toURI().toString(), classpath[0].toString());
        assertEquals(second.toURI().toString(), classpath[1].toString());
    }

    @Test
    void resolutionFailureIsWrapped() {
        final Properties versions = versions();
        final List<RemoteRepository> repositories = List.of();
        final RepositorySystem system = repositorySystem((req, session) -> {
            throw new DependencyResolutionException(new DependencyResult(req), new IllegalStateException("offline"));
        });

        final MojoExecutionException error = assertThrows(MojoExecutionException.class,
                () -> UiSpecGeneratorMojo.resolveClasspath(system, null, repositories, "1.2.3", versions));

        assertTrue(error.getMessage().startsWith("Can't resolve the uispec generator classpath"));
        assertTrue(error.getCause() instanceof DependencyResolutionException);
    }

    private static Properties versions() {
        final Properties versions = new Properties();
        versions.setProperty("openwebbeans.version", "9.9.1");
        versions.setProperty("beam.version", "9.9.2");
        return versions;
    }

    private static ArtifactResult resolved(final File file) {
        return new ArtifactResult(new ArtifactRequest())
                .setArtifact(new DefaultArtifact("g", "a", "jar", "1").setFile(file));
    }

    // only resolveDependencies is used by the mojo, every other method of the resolver is left unsupported
    private static RepositorySystem repositorySystem(final DependencyResolver resolver) {
        return (RepositorySystem) Proxy
                .newProxyInstance(UiSpecGeneratorMojoTest.class.getClassLoader(),
                        new Class<?>[] { RepositorySystem.class }, (proxy, method, args) -> {
                            if (!"resolveDependencies".equals(method.getName())) {
                                throw new UnsupportedOperationException(method.getName());
                            }
                            return resolver.resolve((DependencyRequest) args[1], args[0]);
                        });
    }

    @FunctionalInterface
    private interface DependencyResolver {

        DependencyResult resolve(DependencyRequest request, Object session) throws DependencyResolutionException;
    }

    // the isolated loader only has the platform loader as parent, so it loads the generators from the test classes
    private static URL[] testClasspath() {
        return new URL[] { UiSpecGeneratorMojoTest.class.getProtectionDomain().getCodeSource().getLocation() };
    }

    public static class RecordingGenerator implements Runnable {

        private final Map<String, String> setup;

        private final Collection<String> languages;

        private final Path output;

        public RecordingGenerator(final Map<String, String> setup, final Collection<String> languages,
                final Path output) {
            this.setup = setup;
            this.languages = languages;
            this.output = output;
        }

        @Override
        public void run() {
            final ClassLoader loader = getClass().getClassLoader();
            setup.put("generator.loader", String.valueOf(System.identityHashCode(loader)));
            setup.put("loader.is.url.loader", String.valueOf(loader instanceof URLClassLoader));
            setup.put("tccl.is.generator.loader",
                    String.valueOf(loader == Thread.currentThread().getContextClassLoader()));
            setup.put("languages", String.join(",", languages));
            setup.put("output", output.toString());
        }
    }

    public static class FailingGenerator implements Runnable {

        public FailingGenerator(final Map<String, String> setup, final Collection<String> languages,
                final Path output) {
            // constructor shape expected by the mojo
        }

        @Override
        public void run() {
            throw new IllegalStateException("generator failure");
        }
    }

    public static class WrongConstructorGenerator implements Runnable {

        @Override
        public void run() {
            // never reached
        }
    }

    public static class BrokenConstructorGenerator implements Runnable {

        public BrokenConstructorGenerator(final Map<String, String> setup, final Collection<String> languages,
                final Path output) {
            throw new IllegalArgumentException("broken");
        }

        @Override
        public void run() {
            // never reached
        }
    }
}
