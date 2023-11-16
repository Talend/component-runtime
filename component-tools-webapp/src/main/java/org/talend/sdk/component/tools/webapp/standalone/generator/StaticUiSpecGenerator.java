/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools.webapp.standalone.generator;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.talend.sdk.component.tools.webapp.standalone.generator.StaticResourceGenerator.OutputFormatter.JSON;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.path.PathFactory;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.tools.webapp.standalone.Route;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Run env example:
 * -Dtalend.component.server.maven.repository=target/talend-component-kit/maven
 * -Dtalend.component.server.component.registry=target/talend-component-kit/maven/component-registry.properties
 * -Dtalend.component.server.component.extend.dependencies=false
 */
@Slf4j
@RequiredArgsConstructor
public class StaticUiSpecGenerator implements Runnable {

    private final Map<String, String> systemPropertyVariables;

    private final Collection<String> languages;

    private final Path output;

    public StaticUiSpecGenerator(final String[] args) {
        this(emptyMap(), Stream.of(args[1].split(",")).collect(toList()), PathFactory.get(args[0]));
    }

    @Override
    public void run() {
        new StaticResourceGenerator(systemPropertyVariables, null, null, languages, JSON, true).generate(routes -> {
            final UiSpecService<Object> service = new UiSpecService<>(new Client() {

                @Override // for dynamic_values, just return an empty schema
                public CompletionStage<Map<String, Object>> action(final String family, final String type,
                        final String action, final String lang, final Map params, final Object context) {
                    final Map<String, Object> result = new HashMap<>();
                    result.put("items", emptyList());
                    return CompletableFuture.completedFuture(result);
                }

                @Override
                public void close() {
                    // no-op
                }
            });
            if (!Files.exists(output.getParent())) {
                try {
                    Files.createDirectories(output.getParent());
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            try (final Jsonb jsonb = JsonbBuilder.create();
                    final JarOutputStream jar = new JarOutputStream(Files.newOutputStream(output))) {

                jar.putNextEntry(new JarEntry("configuration/"));
                jar.closeEntry();

                jar.putNextEntry(new JarEntry("component/"));
                jar.closeEntry();

                final BiConsumer<String, String> onFile = (name, content) -> {
                    try {
                        jar.putNextEntry(new JarEntry(name));
                        jar.write(content.getBytes(StandardCharsets.UTF_8));
                        jar.closeEntry();
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                };

                routes
                        .stream()
                        .filter(it -> it.getId().startsWith("component_server_component_details_en_"))
                        .forEach(r -> {
                            try {
                                visitComponentRoute(service, jsonb, r, onFile);
                            } catch (final IOException e) {
                                throw new IllegalStateException(e);
                            }
                        });
                routes
                        .stream()
                        .filter(it -> it.getId().startsWith("component_server_configuration_details_en_"))
                        .forEach(r -> {
                            try {
                                visitConfigurationRoute(service, jsonb, r, onFile);
                            } catch (final IOException e) {
                                throw new IllegalStateException(e);
                            }
                        });
                log.info("Created '{}'", output);
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private void visitConfigurationRoute(final UiSpecService<Object> service, final Jsonb jsonb, final Route route,
            final BiConsumer<String, String> onFile) throws IOException {
        final String configId = route.getId().substring("component_server_configuration_details_en_".length());
        // todo: resolve the parent since we have it in the route instead of using that hack
        final String id = new String(Base64.getDecoder().decode(configId), StandardCharsets.UTF_8);
        final String family = id.split("#")[1];
        try (final InputStream stream = new ByteArrayInputStream(route.getContent())) {
            try {
                final ConfigTypeNodes nodes = jsonb.fromJson(stream, ConfigTypeNodes.class);
                final Ui ui = service
                        .convert(family, "en", nodes.getNodes().values().iterator().next(), null)
                        .toCompletableFuture()
                        .get();
                onFile.accept("configuration/" + configId, jsonb.toJson(ui));
            } catch (final InterruptedException | ExecutionException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void visitComponentRoute(final UiSpecService<Object> service, final Jsonb jsonb, final Route route,
            final BiConsumer<String, String> onFile) throws IOException {
        try (final InputStream stream = new ByteArrayInputStream(route.getContent())) {
            try {
                final ComponentDetailList list = jsonb.fromJson(stream, ComponentDetailList.class);
                final Ui ui =
                        service.convert(list.getDetails().iterator().next(), "en", null).toCompletableFuture().get();
                onFile
                        .accept("component/"
                                + route.getId().substring("component_server_component_details_en_".length()),
                                jsonb.toJson(ui));
            } catch (final InterruptedException | ExecutionException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static void main(final String[] args) {
        if (args.length < 2) {
            System.err
                    .println("Usage:\n" + "args: <where to output the zip> <which langs to use for the generation>"
                            + "  ex:  app output_root_dir lang1,lang2,...");
            return;
        }
        new StaticUiSpecGenerator(args).run();
    }
}
