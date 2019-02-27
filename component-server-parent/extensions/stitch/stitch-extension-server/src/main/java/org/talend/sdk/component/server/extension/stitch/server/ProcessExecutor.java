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
package org.talend.sdk.component.server.extension.stitch.server;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;

import org.talend.sdk.component.server.extension.stitch.server.configuration.App;
import org.talend.sdk.component.server.extension.stitch.server.configuration.StitchExecutorConfiguration;
import org.talend.sdk.component.server.extension.stitch.server.configuration.Threads;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ProcessExecutor {

    @Inject
    @Threads(Threads.Type.EXECUTOR)
    private ExecutorService executor;

    @Inject
    @Threads(Threads.Type.STREAMS)
    private ExecutorService streamsExecutor;

    @Inject
    private StitchExecutorConfiguration configuration;

    @App
    @Inject
    private JsonReaderFactory jsonReaderFactory;

    @App
    @Inject
    private JsonBuilderFactory jsonBuilderFactory;

    @App
    @Inject
    private JsonWriterFactory jsonWriterFactory;

    @Inject
    private ProcessCommandMapper commandMapper;

    public void execute(final String tap, final JsonObject properties, final BooleanSupplier isRunning,
            final BiConsumer<String, JsonObject> onData) {
        final CountDownLatch streamPumped = new CountDownLatch(2);
        try {
            CompletableFuture
                    .supplyAsync(() -> doExecute(tap, properties, isRunning, onData, streamPumped), executor)
                    .handle((process, error) -> {
                        if (error != null) {
                            log.error(error.getMessage(), error);
                        }
                        try {
                            streamPumped.await(configuration.getExecutionTimeout(), MILLISECONDS);
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            if (process != null) {
                                process.close();
                            }
                        }
                        return process;
                    })
                    .get(configuration.getExecutionTimeout(), MILLISECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final ExecutionException | TimeoutException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    // note: we can switch to https://github.com/brettwooldridge/NuProcess if we start to rely on too much threads
    // or writing to a file to read it through an NIO.
    // That said it also means multiple processes and we want to control our resources so probably overkill.
    private CloseableProcess doExecute(final String tap, final JsonObject properties, final BooleanSupplier isRunning,
            final BiConsumer<String, JsonObject> onData, final CountDownLatch streamPumped) {
        final String id = UUID.randomUUID().toString();
        final Path configFile = createConfigFile(properties);
        final AutoCloseable onClose = () -> {
            try {
                Files.deleteIfExists(configFile);
            } catch (final IOException e) {
                log.warn("Can't delete {} ({})", configFile, e.getMessage());
            }
        };
        try {
            final ProcessBuilder builder = new ProcessBuilder();
            builder.command(createCommandFor(tap, configFile));

            final String marker = tap + " (" + id + ")";
            final Process process = builder.start();
            streamsExecutor.submit(() -> {
                try {
                    redirect(marker, "stdout", isRunning, onData, process.getInputStream());
                } finally {
                    streamPumped.countDown();
                }
            });
            streamsExecutor.submit(() -> {
                try {
                    redirect(marker, "stderr", isRunning, onData, process.getErrorStream());
                } finally {
                    streamPumped.countDown();
                }
            });
            return new CloseableProcess(process, onClose);
        } catch (final IOException e) {
            try {
                streamPumped.countDown();
                streamPumped.countDown();
                onClose.close();
            } catch (final Exception ignored) {
                // no-op: will not happen here
            }
            throw new IllegalStateException(e);
        }
    }

    private void redirect(final String name, final String streamName, final BooleanSupplier isRunning,
            final BiConsumer<String, JsonObject> onData, final InputStream stream) {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line = reader.readLine();
            do {
                if (line == null) {
                    log.info("Stream empty for '{}'", name);
                    return;
                }
                if (!isRunning.getAsBoolean()) {
                    log.info("Exiting {} from '{}' because it is no more running", streamName, name);
                    break;
                }

                final Optional<OutputMatcher> matcher = OutputMatcher.matches(line);
                if (!matcher.isPresent()) {
                    log.warn("Unknown {} line: '{}'", streamName, line);
                    line = reader.readLine();
                    continue;
                }

                final OutputMatcher.Data data = matcher.orElseThrow(IllegalArgumentException::new).read(line, () -> {
                    try {
                        return reader.readLine();
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                }, jsonReaderFactory, jsonBuilderFactory.createObjectBuilder());
                if (data.getObject() != null && data.getType() != null) {
                    onData.accept(data.getType(), data.getObject());
                }
                line = data.getNextLine();
            } while (line != null);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> createCommandFor(final String tap, final Path configFile) {
        return commandMapper.toCommand(tap, configFile.toAbsolutePath().toString());
    }

    private Path createConfigFile(final JsonObject properties) {
        final Path configFileFolder = Paths
                .get(configuration
                        .getWorkDirectory()
                        .replace("${base}",
                                System.getProperty("catalina.base", System.getProperty("meecrowave.base", ""))));
        if (!Files.exists(configFileFolder)) {
            try {
                Files.createDirectories(configFileFolder);
            } catch (IOException e) {
                throw new IllegalStateException("Can't create directory " + configFileFolder);
            }
        }
        final Path configFile = configFileFolder.resolve(UUID.randomUUID().toString() + ".json");
        try (final JsonWriter writer = jsonWriterFactory
                .createWriter(
                        Files.newBufferedWriter(configFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
            writer.write(properties);
        } catch (final IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
        return configFile;
    }

    @RequiredArgsConstructor
    private static class CloseableProcess implements AutoCloseable {

        @Delegate
        private final Process process;

        private final AutoCloseable cleanup;

        @Override
        public void close() {
            try {
                cleanup.close();
            } catch (final Exception e) {
                // no-op: will not happen here since we handle it higher level
            }
        }
    }
}
