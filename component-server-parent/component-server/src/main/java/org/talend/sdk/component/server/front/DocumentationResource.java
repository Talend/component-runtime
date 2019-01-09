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
package org.talend.sdk.component.server.front;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.PATH;
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.QUERY;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.OBJECT;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.STRING;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.dao.ComponentDao;
import org.talend.sdk.component.server.front.model.DocumentationContent;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.service.LocaleMapper;

import lombok.extern.slf4j.Slf4j;

@Tag(name = "Documentation", description = "Endpoint to retrieve embedded component documentation.")
@Slf4j
@Path("documentation")
@ApplicationScoped
public class DocumentationResource {

    @Inject
    private LocaleMapper localeMapper;

    @Inject
    private ComponentDao componentDao;

    @Inject
    private ComponentManager manager;

    @Inject
    private Instance<Object> instance;

    @Inject
    private ComponentServerConfiguration configuration;

    private File i18nBase;

    @PostConstruct
    private void init() {
        i18nBase = new File(configuration
                .getDocumentationI18nTranslations()
                .replace("${home}", System.getProperty("meecrowave.home", "")));
    }

    @GET
    @Path("component/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            description = "Returns an asciidoctor version of the documentation for the component represented by its identifier `id`.")
    @APIResponse(responseCode = "200",
            description = "the list of available and storable configurations (datastore, dataset, ...).",
            content = @Content(mediaType = APPLICATION_JSON))
    @APIResponse(responseCode = "404",
            description = "If the component is missing, payload will be an ErrorPayload with the code PLUGIN_MISSING.",
            content = @Content(mediaType = APPLICATION_JSON,
                    schema = @Schema(type = OBJECT, implementation = ErrorPayload.class)))
    public DocumentationContent getDocumentation(
            @PathParam("id") @Parameter(name = "id", description = "the component identifier",
                    in = PATH) final String id,
            @QueryParam("language") @DefaultValue("en") @Parameter(name = "language",
                    description = "the language for display names.", in = QUERY,
                    schema = @Schema(type = STRING, defaultValue = "en")) final String language,
            @QueryParam("segment") @DefaultValue("ALL") @Parameter(name = "segment",
                    description = "the part of the documentation to extract.", in = QUERY,
                    schema = @Schema(type = STRING, defaultValue = "ALL")) final DocumentationSegment segment) {
        final Locale locale = localeMapper.mapLocale(language);
        final Container container = ofNullable(componentDao.findById(id))
                .map(meta -> manager
                        .findPlugin(meta.getParent().getPlugin())
                        .orElseThrow(() -> new WebApplicationException(Response
                                .status(NOT_FOUND)
                                .entity(new ErrorPayload(ErrorDictionary.PLUGIN_MISSING,
                                        "No plugin '" + meta.getParent().getPlugin() + "'"))
                                .build())))
                .orElseThrow(() -> new WebApplicationException(Response
                        .status(NOT_FOUND)
                        .entity(new ErrorPayload(ErrorDictionary.COMPONENT_MISSING, "No component '" + id + "'"))
                        .build()));

        // rendering to html can be slow so do it lazily and once
        DocumentationCache cache = container.get(DocumentationCache.class);
        if (cache == null) {
            synchronized (container) {
                cache = container.get(DocumentationCache.class);
                if (cache == null) {
                    cache = new DocumentationCache();
                    container.set(DocumentationCache.class, cache);
                }
            }
        }

        return cache.documentations.computeIfAbsent(new DocKey(id, language, segment), key -> {
            // todo: handle i18n properly, for now just fallback on not suffixed version and assume the dev put it
            // in the comp
            final String content = Stream
                    .of("documentation_" + locale.getLanguage() + ".adoc", "documentation_" + language + ".adoc",
                            "documentation.adoc")
                    .map(name -> ofNullable(container.getLoader().getResource("TALEND-INF/" + name))
                            .orElseGet(() -> findLocalI18n(locale, container)))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(url -> {
                        try (final BufferedReader stream =
                                new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                            return stream.lines().collect(joining("\n"));
                        } catch (final IOException e) {
                            throw new WebApplicationException(Response
                                    .status(INTERNAL_SERVER_ERROR)
                                    .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED, e.getMessage()))
                                    .build());
                        }
                    })
                    .map(value -> ofNullable(container.get(ContainerComponentRegistry.class))
                            .flatMap(r -> r
                                    .getComponents()
                                    .values()
                                    .stream()
                                    .flatMap(f -> Stream
                                            .concat(f.getPartitionMappers().values().stream(),
                                                    f.getProcessors().values().stream()))
                                    .filter(c -> c.getId().equals(id))
                                    .findFirst()
                                    .map(c -> selectById(c.getName(), value, segment)))
                            .orElse(value))
                    .orElseThrow(() -> new WebApplicationException(Response
                            .status(NOT_FOUND)
                            .entity(new ErrorPayload(ErrorDictionary.COMPONENT_MISSING, "No component '" + id + "'"))
                            .build()));
            return new DocumentationContent("asciidoc", content);
        });
    }

    private URL findLocalI18n(final Locale locale, final Container container) {
        if (!i18nBase.exists()) {
            return null;
        }
        final File file =
                new File(i18nBase, "documentation_" + container.getId() + "_" + locale.getLanguage() + ".adoc");
        if (file.exists()) {
            try {
                return file.toURI().toURL();
            } catch (final MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }

    private static class DocKey {

        private final String id;

        private final String language;

        private final DocumentationSegment segment;

        private final int hash;

        private DocKey(final String id, final String language, final DocumentationSegment segment) {
            this.id = id;
            this.language = language;
            this.segment = segment;
            this.hash = Objects.hash(id, language, segment);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final DocKey docKey = DocKey.class.cast(o);
            return id.equals(docKey.id) && language.equals(docKey.language) && segment == docKey.segment;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static class DocumentationCache {

        private final ConcurrentMap<DocKey, DocumentationContent> documentations = new ConcurrentHashMap<>();
    }

    // see org.talend.sdk.component.tools.AsciidocDocumentationGenerator.toAsciidoc
    private String selectById(final String name, final String value, final DocumentationSegment segment) {
        final List<String> lines;
        try (final BufferedReader reader = new BufferedReader(new StringReader(value))) {
            lines = reader.lines().collect(toList());
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }

        // first try to find configuration level, default is 2 (==)
        final TreeMap<Integer, List<Integer>> configurationLevels = lines
                .stream()
                .filter(it -> it.endsWith("= Configuration"))
                .map(it -> it.indexOf(' '))
                .collect(groupingBy(it -> it, TreeMap::new, toList()));
        if (configurationLevels.isEmpty()) {
            // no standard configuration, just return it all
            return value;
        }

        final int titleLevels = Math.max(1, configurationLevels.lastKey() - 1);
        final String prefixTitle = IntStream.range(0, titleLevels).mapToObj(i -> "=").collect(joining()) + " ";
        final int titleIndex = lines.indexOf(prefixTitle + name);
        if (titleIndex < 0) {
            return value;
        }

        List<String> endOfLines = lines.subList(titleIndex, lines.size());
        int lineIdx = 0;
        for (final String line : endOfLines) {
            if (lineIdx > 0 && line.startsWith(prefixTitle)) {
                endOfLines = lines.subList(0, lineIdx);
                break;
            }
            lineIdx++;
        }
        if (!endOfLines.isEmpty()) {
            switch (segment) {
            case DESCRIPTION: {
                final String configTitle = getConfigTitle(prefixTitle);
                final int configIndex = endOfLines.indexOf(configTitle);
                final boolean skipFirst = endOfLines.get(0).startsWith(prefixTitle);
                final int lastIndex = configIndex < 0 ? endOfLines.size() : configIndex;
                final int firstIndex = skipFirst ? 1 : 0;
                if (lastIndex - firstIndex <= 0) {
                    return "";
                }
                return String.join("\n", endOfLines.subList(firstIndex, lastIndex));
            }
            case CONFIGURATION: {
                final String configTitle = getConfigTitle(prefixTitle);
                final int configIndex = endOfLines.indexOf(configTitle);
                if (configIndex < 0 || configIndex + 1 >= endOfLines.size()) {
                    return "";
                }
                return String.join("\n", endOfLines.subList(configIndex + 1, endOfLines.size()));
            }
            case ALL:
            default:
                return String.join("\n", endOfLines);
            }
        }

        // if not found just return all the doc
        return value;
    }

    private String getConfigTitle(final String prefixTitle) {
        return '=' + prefixTitle + "Configuration";
    }

    public enum DocumentationSegment {
        ALL,
        DESCRIPTION,
        CONFIGURATION
    }
}
