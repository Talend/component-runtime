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
package org.talend.sdk.component.dependencies.maven;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.dependencies.Resolver;
import org.talend.sdk.component.path.PathFactory;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// super light maven resolver, actually just a coordinate file converter
@Data
@Slf4j
public class MvnDependencyListLocalRepositoryResolver implements Resolver {

    private static final Artifact[] NO_ARTIFACT = new Artifact[0];

    private final MvnCoordinateToFileConverter coordinateConverter = new MvnCoordinateToFileConverter();

    private final String dependenciesListFile;

    private final Function<String, Path> artifactMapper;

    @Override
    public Stream<Artifact> resolve(final ClassLoader rootLoader, final String artifact) {
        return Stream
                .of(readDependencies(ofNullable(getJar(artifact))
                        .filter(Files::exists)
                        .map(this::findDependenciesFile)
                        .orElseGet(() -> {
                            boolean isNested;
                            try (final InputStream stream = rootLoader.getResourceAsStream(artifact)) {
                                isNested = stream != null;
                            } catch (final IOException e) {
                                log.debug(e.getMessage(), e);
                                return "";
                            }

                            if (isNested) {
                                try (final ConfigurableClassLoader ccl =
                                        new ConfigurableClassLoader("", new URL[0], rootLoader, name -> true,
                                                name -> true, new String[] { artifact }, new String[0])) {
                                    try (final InputStream deps = artifact.startsWith("nested:")
                                            ? ccl.getNestedResource(artifact + "!/" + dependenciesListFile)
                                            : ccl.getResourceAsStream(dependenciesListFile)) {
                                        return ofNullable(deps).map(s -> {
                                            try {
                                                return slurp(s);
                                            } catch (final IOException e) {
                                                log.debug(e.getMessage(), e);
                                                return "";
                                            }
                                        }).orElse("");
                                    }
                                } catch (final IOException e) {
                                    log.debug(e.getMessage(), e);
                                    return "";
                                }
                            }

                            return "";
                        })));
    }

    private Path getJar(final String artifact) {
        return of(PathFactory.get(artifact)).filter(Files::exists).orElseGet(() -> artifactMapper.apply(artifact));
    }

    public Stream<Artifact> resolveFromDescriptor(final InputStream descriptor) throws IOException {
        return Stream.of(readDependencies(slurp(descriptor)));
    }

    private Artifact[] readDependencies(final String content) {
        return content.isEmpty() ? NO_ARTIFACT
                : new MvnDependenciesTxtArtifactConverter(coordinateConverter).withContent(content).build();
    }

    /**
     * Grab the dependencies.txt from mvn dependency:list output, this can be
     * generated with:
     *
     * <pre>
     * {@code
     * <plugin>
     *   <groupId>org.apache.maven.plugins</groupId>
     *   <artifactId>maven-dependency-plugin</artifactId>
     *   <version>3.0.2</version>
     *   <executions>
     *     <execution>
     *       <id>generate-depends-file</id>
     *       <phase>generate-resources</phase>
     *       <goals>
     *         <goal>list</goal>
     *       </goals>
     *       <configuration>
     *         <outputFile>target/classes/${dependenciesPath}</outputFile>
     *       </configuration>
     *     </execution>
     *   </executions>
     * </plugin>
     * }
     * </pre>
     *
     * @param module
     * the component module currently loaded.
     * @return the dependencies.list (dependenciesPath) content.
     */
    private String findDependenciesFile(final Path module) {
        if (module.getFileName().toString().endsWith(".jar")) {
            try (final JarFile jar = new JarFile(module.toFile())) {
                final ZipEntry entry = jar.getEntry(dependenciesListFile);
                if (entry != null) {
                    return slurp(jar.getInputStream(entry));
                }
                return "";
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if (Files.isDirectory(module)) {
            final Path file = module.resolve(dependenciesListFile);
            if (Files.exists(file)) {
                try {
                    return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                } catch (final IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return "";
        }
        throw new IllegalArgumentException("Unsupported module " + module);
    }

    private String slurp(final InputStream in) throws IOException {
        try (final BufferedReader stream = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return stream.lines().collect(joining(System.lineSeparator()));
        }
    }

    // using the output of mvn dependency:list
    @RequiredArgsConstructor
    public static class MvnDependenciesTxtArtifactConverter {

        private static final Artifact[] EMPTY_ARTIFACTS = new Artifact[0];

        private String content;

        private Predicate<Artifact> filter = DefaultFilter.INSTANCE;

        private final MvnCoordinateToFileConverter coordinateConverter;

        public MvnDependenciesTxtArtifactConverter withContent(final String content) {
            this.content = content;
            return this;
        }

        public MvnDependenciesTxtArtifactConverter withFilter(final Predicate<Artifact> filter) {
            this.filter = filter;
            return this;
        }

        // reads dependencies.txt based on
        // org/apache/maven/plugins/dependency/utils/DependencyStatusSets.java logic
        public Artifact[] build() {
            if (content == null) {
                throw new IllegalArgumentException("No stream passed");
            }

            final Collection<Artifact> artifacts = new ArrayList<>();
            try (final BufferedReader reader = new BufferedReader(new StringReader(content))) {
                String line;

                boolean started = false;
                do {
                    line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    if (!started) {
                        if ("The following files have been resolved:".equals(line)) {
                            started = true;
                            continue;
                        }
                        if (line.split(":").length >= 3) {
                            started = true;
                        } else {
                            continue;
                        }
                    }

                    final Artifact artifact = coordinateConverter.toArtifact(line);
                    if (filter.test(artifact)) {
                        artifacts.add(artifact);
                    }
                } while (true);

                return artifacts.toArray(EMPTY_ARTIFACTS);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static class DefaultFilter implements Predicate<Artifact> {

        static final Predicate<Artifact> INSTANCE = new DefaultFilter();

        private static final Collection<String> ACCEPTED_SCOPES = new HashSet<>(asList("compile", "runtime"));

        private static final Collection<String> ACCEPTED_TYPES = new HashSet<>(asList("jar", "bundle", "zip"));

        @Override
        public boolean test(final Artifact artifact) {
            return ACCEPTED_SCOPES.contains(artifact.getScope()) && ACCEPTED_TYPES.contains(artifact.getType());
        }
    }
}
