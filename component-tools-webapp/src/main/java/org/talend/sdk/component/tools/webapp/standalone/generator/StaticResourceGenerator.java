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
package org.talend.sdk.component.tools.webapp.standalone.generator;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.BinaryDataStrategy;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.talend.sdk.component.path.PathFactory;
import org.talend.sdk.component.server.api.ActionResource;
import org.talend.sdk.component.server.api.ComponentResource;
import org.talend.sdk.component.server.api.ConfigurationTypeResource;
import org.talend.sdk.component.server.api.DocumentationResource;
import org.talend.sdk.component.server.api.EnvironmentResource;
import org.talend.sdk.component.server.front.model.ComponentIndex;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.tools.webapp.standalone.Route;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Run env example:
 * -Dtalend.component.server.maven.repository=target/talend-component-kit/maven
 * -Dtalend.component.server.component.registry=target/talend-component-kit/maven/component-registry.properties
 * -Dtalend.component.server.component.extend.dependencies=false
 */
@RequiredArgsConstructor
public class StaticResourceGenerator implements Runnable {

    public static final String THEME = "light";

    private final Map<String, String> systemPropertyVariables;

    private final Path outputRepository;

    private final Path outputDescriptor;

    private final Collection<String> languages;

    private final OutputFormatter formatter;

    private final boolean skipDependencies;

    public StaticResourceGenerator(final String[] args) {
        this(emptyMap(), PathFactory.get(args[0]).resolve("repository"),
                PathFactory.get(args[0]).resolve("routes.json"), Stream.of(args[1].split(",")).collect(toList()),
                OutputFormatter.JSON, args.length >= 3 && Boolean.parseBoolean(args[2]));
    }

    @Override
    public void run() {
        validate();
        generate(routes -> {
            try {
                if (!Files.exists(outputDescriptor.getParent())) {
                    Files.createDirectories(outputDescriptor.getParent());
                }

                if (formatter.needsFileIdMapping()) { // don't store the content
                    if (!Files.exists(outputRepository)) {
                        Files.createDirectories(outputRepository);
                    }
                    routes.forEach(r -> {
                        try {
                            Files
                                    .copy(new ByteArrayInputStream(r.getContent()), outputRepository.resolve(r.getId()),
                                            StandardCopyOption.REPLACE_EXISTING);
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    routes.forEach(r -> r.setContent(null));
                }

                try (final OutputStream stream = Files.newOutputStream(outputDescriptor)) {
                    formatter.format(routes, stream);
                }
            } catch (final IOException ioe) {
                throw new IllegalStateException(ioe);
            }
        });
    }

    public void generate(final Consumer<Collection<Route>> routesConsumer) {
        try (final AutoCloseable autoClosed = prepareEnv()) {
            routesConsumer.accept(collectResources());
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /*
     * Routes:
     * Service URI: /api/v1/action -> o.t.s.c.server.front.ActionResourceImpl
     * GET /api/v1/action/index -> // types and families params ignored
     * POST /api/v1/action/execute -> CompletionStage<Response> execute(String, String, String, String, Map)
     * Service URI: /api/v1/bulk -> o.t.s.c.server.front.BulkReadResourceImpl
     * POST /api/v1/bulk -> CompletionStage<BulkResponses> bulk(BulkRequests)
     * Service URI: /api/v1/component -> o.t.s.c.server.front.ComponentResourceImpl
     * GET /api/v1/component/dependency/{id} -> StreamingOutput getDependency(String)
     * GET /api/v1/component/details -> ComponentDetailList getDetail(String, String[])
     * GET /api/v1/component/icon/family/{id} -> Response familyIcon(String)
     * GET /api/v1/component/icon/{id} -> Response icon(String)
     * GET /api/v1/component/index -> // ignore query
     * POST /api/v1/component/migrate/{id}/{configurationVersion} -> Map<String, String> migrate(String,int,Map)
     * Service URI: /api/v1/configurationtype -> o.t.s.c.server.front.ConfigurationTypeResourceImpl
     * GET /api/v1/configurationtype/details -> ConfigTypeNodes getDetail(String, String[])
     * GET /api/v1/configurationtype/index -> ConfigTypeNodes getRepositoryModel(String,boolean,String) // q unsupported
     * POST /api/v1/configurationtype/migrate/{id}/{confVersion} -> Map<String,String> migrate(String, int, Map)
     * Service URI: /api/v1/documentation -> o.t.s.c.server.front.DocumentationResourceImpl
     * GET /api/v1/documentation/component/{id} -> DocumentationContent getDocumentation(String,String,DocSegment)
     * Service URI: /api/v1/environment -> o.t.s.c.server.front.EnvironmentResourceImpl
     * GET /api/v1/environment -> Environment get()
     */
    public Collection<Route> collectResources() {
        final Collection<Route> routes = new ArrayList<>(languages.size() * 11);
        try (final SeContainer container = SeContainerInitializer
                .newInstance()
                .addProperty("skipHttp", true) // this generator does not need to bind any port
                .addProperty("org.apache.webbeans.scanBeansXmlOnly", "true") // dont scan all the classpath
                .initialize(); final Jsonb jsonb = JsonbBuilder.create()) {
            final String[] emptyStringArray = new String[0];

            final EnvironmentResource environment = container.select(EnvironmentResource.class).get();
            routes
                    .add(route("component_server_environment", "/api/v1/environment", MapBuilder.map().done(),
                            emptyMap(), emptyMap(), jsonb.toJson(environment.get())));

            final ActionResource actions = container.select(ActionResource.class).get();
            routes
                    .addAll(languages
                            .stream()
                            .map(lang -> route("component_server_action_index_" + lang, "/api/v1/action/index",
                                    MapBuilder.map().with("language", lang).done(), emptyMap(), emptyMap(),
                                    jsonb.toJson(actions.getIndex(emptyStringArray, emptyStringArray, lang))))
                            .collect(toList()));

            final ComponentResource components = container.select(ComponentResource.class).get();
            routes
                    .addAll(Stream
                            .of(true, false)
                            .flatMap(
                                    includeIconContent -> languages
                                            .stream()
                                            .map(lang -> route(
                                                    "component_server_component_index_" + lang + '_'
                                                            + (includeIconContent ? "includeIconContent"
                                                                    : "noIncludeIconContent"),
                                                    "/api/v1/component/index",
                                                    MapBuilder
                                                            .map()
                                                            .with("language", lang)
                                                            .with("includeIconContent",
                                                                    Boolean.toString(includeIconContent))
                                                            .done(),
                                                    emptyMap(), emptyMap(),
                                                    jsonb.toJson(
                                                            components.getIndex(lang, includeIconContent, "",
                                                                    THEME)))))
                            .collect(toList()));

            final List<ComponentIndex> componentIndex = components.getIndex("en", false, "", THEME).getComponents();
            final List<String> componentIds =
                    componentIndex.stream().map(it -> it.getId().getId()).distinct().collect(toList());
            final List<String> componentFamilyIds =
                    componentIndex.stream().map(it -> it.getId().getFamilyId()).distinct().collect(toList());
            routes
                    .addAll(componentIds
                            .stream()
                            .flatMap(componentId -> languages
                                    .stream()
                                    .map(lang -> route(
                                            "component_server_component_dependencies_" + lang + '_' + componentId,
                                            "/api/v1/component/dependencies",
                                            MapBuilder.map().with("identifier", componentId).done(), emptyMap(),
                                            emptyMap(),
                                            jsonb.toJson(components.getDependencies(new String[] { componentId })))))
                            .collect(toList()));
            routes
                    .addAll(componentIds
                            .stream()
                            .flatMap(componentId -> languages
                                    .stream()
                                    .map(lang -> route("component_server_component_details_" + lang + '_' + componentId,
                                            "/api/v1/component/details",
                                            MapBuilder
                                                    .map()
                                                    .with("language", lang)
                                                    .with("identifiers", componentId)
                                                    .done(),
                                            emptyMap(), emptyMap(),
                                            jsonb.toJson(components.getDetail(lang, new String[] { componentId })))))
                            .collect(toList()));

            // this can become huge, maybe we should directly go on the FS for the binary ones
            if (!skipDependencies) {
                routes
                        .addAll(componentIds
                                .stream()
                                .flatMap(componentId -> components
                                        .getDependencies(new String[] { componentId })
                                        .getDependencies()
                                        .values()
                                        .stream())
                                .flatMap(dep -> dep.getDependencies().stream())
                                .distinct()
                                .filter(it -> !it.startsWith("org.talend.sdk.component:"))
                                .map(dep -> new Route(
                                        "component_server_component_dependency_"
                                                + dep.replace(':', '_').replace('.', '_'),
                                        Response.Status.OK.getStatusCode(), "/api/v1/component/dependency/" + dep,
                                        MapBuilder.map().done(),
                                        singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM),
                                        emptyMap(), swallow(() -> components.getDependency(dep))))
                                .collect(toList()));
            }
            routes.addAll(componentIds.stream().map(componentId -> {
                final Response response = components.icon(componentId, THEME);
                return route("component_server_component_icon_" + componentId, "/api/v1/component/icon/" + componentId,
                        MapBuilder.map().done(),
                        singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM), emptyMap(),
                        response.getStatus(), response::readEntity, jsonb);
            }).collect(toList()));
            routes.addAll(componentFamilyIds.stream().map(familyId -> {
                final Response response = components.familyIcon(familyId, THEME);
                return route("component_server_component_family_icon_" + familyId,
                        "/api/v1/component/icon/family/" + familyId, MapBuilder.map().done(),
                        singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM), emptyMap(),
                        response.getStatus(), response::readEntity, jsonb);
            }).collect(toList()));

            final ConfigurationTypeResource configurations = container.select(ConfigurationTypeResource.class).get();
            final ConfigTypeNodes configurationTypes = configurations.getRepositoryModel("en", true, "");
            routes
                    .addAll(Stream
                            .of(true, false)
                            .flatMap(lightPayload -> languages
                                    .stream()
                                    .map(lang -> route(
                                            "component_server_configuration_index_" + lang + '_'
                                                    + (lightPayload ? "lightPayload" : "noLightPayload"),
                                            "/api/v1/configurationtype/index",
                                            MapBuilder
                                                    .map()
                                                    .with("language", lang)
                                                    .with("lightPayload", Boolean.toString(lightPayload))
                                                    .done(),
                                            emptyMap(), emptyMap(),
                                            jsonb.toJson(configurations.getRepositoryModel(lang, lightPayload, "")))))
                            .collect(toList()));
            routes
                    .addAll(configurationTypes
                            .getNodes()
                            .keySet()
                            .stream()
                            .flatMap(id -> languages
                                    .stream()
                                    .map(lang -> route("component_server_configuration_details_" + lang + "_" + id,
                                            "/api/v1/configurationtype/details",
                                            MapBuilder.map().with("language", lang).with("identifiers", id).done(),
                                            emptyMap(), emptyMap(),
                                            jsonb.toJson(configurations.getDetail(lang, new String[] { id })))))
                            .collect(toList()));

            final DocumentationResource documentations = container.select(DocumentationResource.class).get();
            routes
                    .addAll(componentIds
                            .stream()
                            .flatMap(componentId -> languages
                                    .stream()
                                    .flatMap(lang -> Stream
                                            .of(DocumentationResource.DocumentationSegment.values())
                                            .map(segment -> route("component_server_documentation_" + lang + "_"
                                                    + segment.name().toLowerCase(Locale.ROOT) + '_' + componentId,
                                                    "/api/v1/documentation/component/" + componentId,
                                                    MapBuilder
                                                            .map()
                                                            .with("language", lang)
                                                            .with("segment", segment.name())
                                                            .done(),
                                                    emptyMap(), emptyMap(),
                                                    jsonb
                                                            .toJson(documentations
                                                                    .getDocumentation(componentId, lang, segment))))))
                            .collect(toList()));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        return routes;
    }

    private void validate() {
        if (outputDescriptor == null) {
            throw new IllegalArgumentException("No otuput set");
        }
        if (languages.isEmpty()) {
            throw new IllegalArgumentException("No language set, nothing to generate");
        }
    }

    private AutoCloseable prepareEnv() {
        final List<Runnable> cleanupTasks =
                ofNullable(systemPropertyVariables).orElseGet(Collections::emptyMap).entrySet().stream().map(e -> {
                    final String existing = System.getProperty(e.getKey());
                    System.setProperty(e.getKey(), e.getValue());
                    return (Runnable) () -> {
                        if (existing == null) {
                            System.clearProperty(e.getKey());
                        } else {
                            System.setProperty(e.getKey(), existing);
                        }
                    };
                }).collect(toList());

        final Locale oldLocale = Locale.getDefault();
        cleanupTasks.add(() -> Locale.setDefault(oldLocale));
        return () -> cleanupTasks.forEach(Runnable::run);
    }

    private byte[] swallow(final Supplier<StreamingOutput> dependency) {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            dependency.get().write(baos);
            baos.flush();
            return baos.toByteArray();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        } catch (final WebApplicationException wae) {
            throw new IllegalStateException(wae.getResponse().readEntity(Object.class).toString(), wae);
        }
    }

    private static Route route(final String id, final String path, final SortedMap<String, String> queries,
            final Map<String, String> requestHeaders, final Map<String, String> responseHeaders, final String content) {
        return new Route(id, 200, path, queries,
                responseHeaders.isEmpty()
                        ? singletonMap("Accept", responseHeaders.getOrDefault("Content-Type", "application/json"))
                        : requestHeaders,
                responseHeaders.isEmpty() ? singletonMap("Content-Type", "application/json") : responseHeaders,
                content.getBytes(StandardCharsets.UTF_8));
    }

    private static Route route(final String id, final String path, final SortedMap<String, String> queries,
            final Map<String, String> requestHeaders, final Map<String, String> responseHeaders, final int status,
            final Function<Class<?>, Object> responseProvider, final Jsonb jsonb) {
        return new Route(id, status, path, queries, requestHeaders, responseHeaders,
                status == 200 ? byte[].class.cast(responseProvider.apply(byte[].class))
                        : jsonb.toJson(responseProvider.apply(ErrorPayload.class)).getBytes(StandardCharsets.UTF_8));
    }

    public static void main(final String[] args) {
        if (args.length < 2) {
            System.err
                    .println("Usage:\n"
                            + "args: <where to output the dump> <which langs to use for the generation> <ignore dependency endpoint>"
                            + "  ex:  app output_root_dir lang1,lang2,... [true|false]");
            return;
        }
        new StaticResourceGenerator(args).run();
    }

    @Data
    private static class MapBuilder {

        private final SortedMap<String, String> queries = new TreeMap<>();

        public static MapBuilder map() {
            return new MapBuilder();
        }

        public MapBuilder with(final String k, final String v) {
            queries.put(k, v);
            return this;
        }

        public SortedMap<String, String> done() {
            return queries;
        }
    }

    public enum OutputFormatter {

        JSON {

            @Override
            public void format(final Collection<Route> routes, final OutputStream outputStream) {
                try (final Jsonb jsonb = JsonbBuilder
                        .create(new JsonbConfig()
                                .withFormatting(true)
                                .withBinaryDataStrategy(BinaryDataStrategy.BASE_64)
                                .setProperty("johnzon.cdi.activated", false))) {
                    jsonb.toJson(routes, outputStream);
                } catch (final Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        };

        public boolean needsFileIdMapping() {
            return true;
        }

        public abstract void format(Collection<Route> routes, OutputStream stream);
    }
}
