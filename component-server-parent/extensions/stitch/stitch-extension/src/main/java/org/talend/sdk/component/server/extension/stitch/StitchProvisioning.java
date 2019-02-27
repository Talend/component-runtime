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
package org.talend.sdk.component.server.extension.stitch;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toMap;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.talend.sdk.component.server.extension.api.ExtensionRegistrar;
import org.talend.sdk.component.server.extension.stitch.runtime.StitchGenericComponent;
import org.talend.sdk.component.server.extension.stitch.runtime.StitchInput;
import org.talend.sdk.component.server.extension.stitch.runtime.StitchMapper;
import org.talend.sdk.component.server.front.model.DependencyDefinition;
import org.talend.sdk.component.spi.component.GenericComponentExtension;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class StitchProvisioning {

    void register(@ObservesAsync final ExtensionRegistrar event, final StitchConfiguration configuration) {
        if (!configuration.getToken().isPresent()) {
            log.info("Skipping stitch extension since token is not set");
            return;
        }

        // ensure we can create our runtime first
        final DependencyDefinition dependencyDefinition = getRuntimeDependency(event);

        log.info("Loading stitch data, this is an experimental extension");

        final int threads = configuration.getParallelism() <= 0 ? Runtime.getRuntime().availableProcessors()
                : configuration.getParallelism();
        final ExecutorService stitchExecutor = new ThreadPoolExecutor(threads, threads, 1, MINUTES,
                new LinkedBlockingQueue<>(), new StitchThreadFactory());
        final ClientBuilder builder = ClientBuilder.newBuilder();
        builder.connectTimeout(configuration.getConnectTimeout(), MILLISECONDS);
        builder.readTimeout(configuration.getReadTimeout(), MILLISECONDS);
        builder.executorService(stitchExecutor);
        final Client stitchClient = builder.build();

        final CountDownLatch latch = new CountDownLatch(1);
        final Runnable whenDone = () -> {
            latch.countDown();
            stitchClient.close();
            stitchExecutor.shutdownNow();
            try {
                stitchExecutor.awaitTermination(1, MINUTES);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        event.registerAwait(() -> {
            try {
                latch.await(configuration.getInitTimeout(), MILLISECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        new ModelLoader(stitchClient, configuration.getBase(),
                configuration.getToken().orElseThrow(IllegalArgumentException::new), configuration.getRetries())
                        .load()
                        .thenApply(model -> {
                            event.registerComponents(model.getComponents());
                            event.registerConfigurations(model.getConfigurations());
                            event
                                    .registerDependencies(model
                                            .getComponents()
                                            .stream()
                                            .collect(toMap(it -> it.getId().getId(), it -> dependencyDefinition)));
                            return model;
                        })
                        .whenComplete((model, error) -> {
                            if (error != null) {
                                log.error(error.getMessage(), error);
                            } else {
                                log.info("Loaded {} Stitch components", model.getComponents().size());
                            }
                            whenDone.run();
                        });
    }

    private DependencyDefinition getRuntimeDependency(final ExtensionRegistrar event) {
        final String groupId = "org.talend.stitch.generated";
        final String artifactId = "stitch"; // match the family for later lookups

        // for tests
        final String versionMarker = System.getProperty("talend.component.server.extension.stitch.versionMarker", "");
        final String version = "1.0.0" + (versionMarker.isEmpty() ? "" : ('-' + versionMarker));

        event.createExtensionJarIfNotExist(groupId, artifactId, version, jar -> {
            try {
                {
                    // write manifest.mf
                    final Manifest manifest = new Manifest();
                    final Attributes mainAttributes = manifest.getMainAttributes();
                    mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
                    mainAttributes.putValue("Created-By", "Talend Component Kit Server Stitch Extension");
                    mainAttributes.putValue("Talend-Time", Long.toString(System.currentTimeMillis()));
                    mainAttributes.putValue("Talend-Family-Name", "Stitch");

                    final ZipEntry e = new ZipEntry(JarFile.MANIFEST_NAME);
                    jar.putNextEntry(e);
                    manifest.write(new BufferedOutputStream(jar));
                    jar.closeEntry();
                }

                // create root package (folders)
                final StringBuilder pck = new StringBuilder();
                for (final String s : StitchGenericComponent.class.getPackage().getName().split("\\.")) {
                    pck.append(s).append('/');
                    jar.putNextEntry(new JarEntry(pck.toString()));
                    jar.closeEntry();
                }

                // now we add our classes in that package
                Stream
                        .of(StitchGenericComponent.class, StitchMapper.class, StitchInput.class,
                                org.talend.sdk.component.server.extension.stitch.runtime.StitchClient.class)
                        .forEach(clazz -> {
                            final String resource = clazz.getName().replace('.', '/') + ".class";
                            try (final InputStream stream =
                                    StitchProvisioning.class.getClassLoader().getResourceAsStream(resource)) {
                                if (stream == null) {
                                    throw new IllegalStateException("Can't find " + resource);
                                }

                                jar.putNextEntry(new JarEntry(resource));
                                final byte[] buffer = new byte[8192];
                                int read;
                                while ((read = stream.read(buffer)) >= 0) {
                                    jar.write(buffer, 0, read);
                                }
                                jar.closeEntry();
                            } catch (final IOException streamError) {
                                throw new IllegalStateException(streamError);
                            }
                        });
                // register our generic component
                jar.putNextEntry(new JarEntry("META-INF/services/" + GenericComponentExtension.class.getName()));
                jar.write((StitchGenericComponent.class.getName()).getBytes(StandardCharsets.UTF_8));
                jar.closeEntry();
            } catch (final IOException ioe) {
                throw new IllegalStateException(ioe);
            }
        });
        return new DependencyDefinition(singletonList(groupId + ':' + artifactId + ':' + version));
    }

    private static class StitchThreadFactory implements ThreadFactory {

        private final ThreadGroup group = ofNullable(System.getSecurityManager())
                .map(SecurityManager::getThreadGroup)
                .orElseGet(() -> Thread.currentThread().getThreadGroup());

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(final Runnable worker) {
            final Thread t =
                    new Thread(group, worker, "talend-server-stitch-extension-" + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
