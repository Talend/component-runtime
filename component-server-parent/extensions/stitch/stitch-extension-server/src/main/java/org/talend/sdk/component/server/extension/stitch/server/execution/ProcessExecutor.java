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
package org.talend.sdk.component.server.extension.stitch.server.execution;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonBuilderFactory;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
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
    @Threads(Threads.Type.EXECUTOR_TIMEOUT)
    private ScheduledExecutorService timeoutExecutor;

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

    public enum ProcessOutputMode {
        LINE,
        JSON_OBJECT
    }

    public CompletionStage<Integer> execute(final String tap, final JsonObject properties,
            final BooleanSupplier isRunning, final BiConsumer<String, JsonObject> onData,
            final ProcessOutputMode outputMode, final String... args) {
        final CountDownLatch streamPumped = new CountDownLatch(2);
        final AtomicReference<CloseableProcess> closeableProcess = new AtomicReference<>();
        final ScheduledFuture<?> timeout = timeoutExecutor.schedule(() -> {
            final CloseableProcess process = closeableProcess.get();
            if (process == null) {
                log.warn("No process to cancel > {}", tap);
                return;
            }
            process.close();
        }, configuration.getExecutionTimeout(), MILLISECONDS);
        return CompletableFuture.supplyAsync(() -> {
            final CloseableProcess process =
                    doExecute(tap, properties, isRunning, onData, streamPumped, outputMode, args);
            closeableProcess.set(process);
            return process;
        }, executor).thenApply(p -> {
            timeout.cancel(true);
            return p;
        }).handle((process, error) -> {
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
        }).thenApply(process -> {
            if (timeout.isCancelled()) {
                try {
                    return process.exitValue();
                } catch (final IllegalStateException ise) {
                    if (process.isAlive()) {
                        process.close();
                    }
                    return -1;
                }
            }
            return -2; // timeout
        });
    }

    // note: we can switch to https://github.com/brettwooldridge/NuProcess if we start to rely on too much threads
    // or writing to a file to read it through an NIO.
    // That said it also means multiple processes and we want to control our resources so probably overkill.
    private CloseableProcess doExecute(final String tap, final JsonObject properties, final BooleanSupplier isRunning,
            final BiConsumer<String, JsonObject> onData, final CountDownLatch streamPumped,
            final ProcessOutputMode outputMode, final String... args) {
        final String id = UUID.randomUUID().toString();
        final Path configFile = createConfigFile(properties);
        final AutoCloseable cleanTempFiles = () -> {
            if (Files.exists(configFile)) {
                try {
                    Files.deleteIfExists(configFile);
                } catch (final IOException e) {
                    log.warn("Can't delete {} ({})", configFile, e.getMessage());
                }
            }
        };
        try {
            final ProcessBuilder builder = new ProcessBuilder();
            builder.command(createCommandFor(tap, configFile, args));

            final String marker = tap + " (" + id + ")";
            final Process process = builder.start();
            streamsExecutor.submit(() -> {
                try {
                    switch (outputMode) {
                    case LINE:
                        redirect(marker, "stdout", isRunning, onData, process.getInputStream());
                        break;
                    case JSON_OBJECT:
                        try (final JsonReader reader = jsonReaderFactory
                                .createReader(new BufferedReader(new InputStreamReader(process.getInputStream())))) {
                            onData
                                    .accept("data",
                                            jsonBuilderFactory
                                                    .createObjectBuilder()
                                                    .add("data", reader.readObject())
                                                    .add("success", true)
                                                    .build());
                        } catch (final JsonException ex) {
                            onData
                                    .accept("data",
                                            jsonBuilderFactory
                                                    .createObjectBuilder()
                                                    .add("error", ex.getMessage())
                                                    .add("success", false)
                                                    .build());
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported output mode: " + outputMode);
                    }
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
            return new CloseableProcess(process, () -> {
                try {
                    cleanTempFiles.close();
                } finally {
                    if (process.isAlive()) {
                        process.destroyForcibly().waitFor(5, SECONDS);
                    }
                }
            });
        } catch (final IOException e) {
            try {
                streamPumped.countDown();
                streamPumped.countDown();
                cleanTempFiles.close();
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
                    log.info("Stream {} empty for '{}'", streamName, name);
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

    private List<String> createCommandFor(final String tap, final Path configFile, final String... args) {
        return Stream
                .concat(commandMapper.toCommand(tap, configFile.toAbsolutePath().toString()).stream(),
                        args == null ? Stream.empty() : Stream.of(args))
                .collect(toList());
    }

    private Path createConfigFile(final JsonObject properties) {
        final Path configurationsWorkingDirectory = configuration.getConfigurationsWorkingDirectory();
        if (!Files.exists(configurationsWorkingDirectory)) {
            try {
                Files.createDirectories(configurationsWorkingDirectory);
            } catch (IOException e) {
                throw new IllegalStateException("Can't create directory " + configurationsWorkingDirectory);
            }
        }
        final Path configFile = configurationsWorkingDirectory.resolve(UUID.randomUUID().toString() + ".json");
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
