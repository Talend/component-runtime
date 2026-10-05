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

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
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

        final IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> UiSpecGeneratorMojo
                        .runIsolated(testClasspath(), FailingGenerator.class.getName(), new HashMap<>(), List.of(),
                                Paths.get("uispec.zip")));

        assertEquals("generator failure", error.getMessage());
        assertSame(before, thread.getContextClassLoader());
    }

    @Test
    void unknownGeneratorIsWrapped() {
        final Thread thread = Thread.currentThread();
        final ClassLoader before = thread.getContextClassLoader();

        final MojoExecutionException error = assertThrows(MojoExecutionException.class,
                () -> UiSpecGeneratorMojo
                        .runIsolated(testClasspath(), "org.talend.Missing", new HashMap<>(), List.of(),
                                Paths.get("uispec.zip")));

        assertTrue(error.getCause() instanceof ClassNotFoundException);
        assertSame(before, thread.getContextClassLoader());
    }

    @Test
    void generatorWithoutExpectedConstructorIsWrapped() {
        final MojoExecutionException error = assertThrows(MojoExecutionException.class,
                () -> UiSpecGeneratorMojo
                        .runIsolated(testClasspath(), WrongConstructorGenerator.class.getName(), new HashMap<>(),
                                List.of(), Paths.get("uispec.zip")));

        assertTrue(error.getCause() instanceof NoSuchMethodException);
    }

    @Test
    void generatorInstantiationErrorIsWrapped() {
        final MojoExecutionException error = assertThrows(MojoExecutionException.class,
                () -> UiSpecGeneratorMojo
                        .runIsolated(testClasspath(), BrokenConstructorGenerator.class.getName(), new HashMap<>(),
                                List.of(), Paths.get("uispec.zip")));

        assertNotNull(error.getCause());
        assertTrue(error.getCause() instanceof InvocationTargetException);
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
