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
package org.talend.sdk.component.server.front;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static java.util.Collections.list;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheResult;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.path.PathFactory;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.server.api.DocumentationResource;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.dao.ComponentDao;
import org.talend.sdk.component.server.front.model.DocumentationContent;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.service.ExtensionComponentMetadataManager;
import org.talend.sdk.component.server.service.LocaleMapper;
import org.talend.sdk.component.server.service.jcache.FrontCacheKeyGenerator;
import org.talend.sdk.component.server.service.jcache.FrontCacheResolver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@CacheDefaults(cacheResolverFactory = FrontCacheResolver.class, cacheKeyGenerator = FrontCacheKeyGenerator.class)
public class DocumentationResourceImpl implements DocumentationResource {

    private static final DocumentationContent NO_DOC = new DocumentationContent("asciidoc", "");

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

    @Inject
    private ExtensionComponentMetadataManager virtualComponents;

    private Path i18nBase;

    @PostConstruct
    private void init() {
        i18nBase = PathFactory
                .get(configuration
                        .getDocumentationI18nTranslations()
                        .replace("${home}", System.getProperty("meecrowave.home", "")));
    }

    @Override
    @CacheResult
    public DocumentationContent getDocumentation(final String id, final String language,
            final DocumentationSegment segment) {
        if (virtualComponents.isExtensionEntity(id)) {
            return NO_DOC;
        }

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
            final String content = Stream
                    .of("documentation_" + locale.getLanguage() + ".adoc", "documentation_" + language + ".adoc",
                            "documentation.adoc")
                    .flatMap(name -> {
                        try {
                            return ofNullable(container.getLoader().getResources("TALEND-INF/" + name))
                                    .filter(Enumeration::hasMoreElements)
                                    .map(e -> list(e).stream())
                                    .orElseGet(() -> ofNullable(findLocalI18n(locale, container))
                                            .map(Stream::of)
                                            .orElseGet(Stream::empty));
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .filter(Objects::nonNull)
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
                                            .of(f.getPartitionMappers().values().stream(),
                                                    f.getProcessors().values().stream(),
                                                    f.getDriverRunners().values().stream())
                                            .flatMap(t -> t))
                                    .filter(c -> c.getId().equals(id))
                                    .findFirst()
                                    .map(c -> selectById(c.getName(), value, segment)))
                            .orElse(value))
                    .map(String::trim)
                    .filter(it -> !it.isEmpty())
                    .findFirst()
                    .orElseThrow(() -> new WebApplicationException(Response
                            .status(NOT_FOUND)
                            .entity(new ErrorPayload(ErrorDictionary.COMPONENT_MISSING, "No component '" + id + "'"))
                            .build()));
            return new DocumentationContent("asciidoc", content);
        });
    }

    private URL findLocalI18n(final Locale locale, final Container container) {
        if (!Files.exists(i18nBase)) {
            return null;
        }
        final Path file = i18nBase.resolve("documentation_" + container.getId() + "_" + locale.getLanguage() + ".adoc");
        if (Files.exists(file)) {
            try {
                return file.toUri().toURL();
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
            hash = Objects.hash(id, language, segment);
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
    String selectById(final String name, final String value, final DocumentationSegment segment) {
        final List<String> lines;
        try (final BufferedReader reader = new BufferedReader(new StringReader(value))) {
            lines = reader.lines().collect(toList());
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }

        return extractUsingComments(name, lines, segment)
                .orElseGet(() -> noMarkingCommentFallbackExtraction(name, lines, segment, value));
    }

    private Optional<String> extractUsingComments(final String name, final List<String> lines,
            final DocumentationSegment segment) {
        final Map<String, List<String>> linesPerComponents = new HashMap<>();
        List<String> currentCapture = null;
        for (final String line : lines) {
            if (line.startsWith("//component_start:")) {
                currentCapture = new ArrayList<>();
                linesPerComponents.put(line.substring("//component_start:".length()), currentCapture);
            } else if (line.startsWith("//component_end:")) {
                currentCapture = null;
            } else if (currentCapture != null && (!line.isEmpty() || !currentCapture.isEmpty())) {
                currentCapture.add(line);
            }
        }
        final List<String> componentDoc = linesPerComponents.get(name);
        return ofNullable(componentDoc)
                .filter(componentLines -> componentLines.stream().filter(it -> !it.isEmpty()).count() > 1)
                .map(componentLines -> extractSegmentFromComments(segment, componentDoc))
                .filter(it -> !it.trim().isEmpty());
    }

    private String noMarkingCommentFallbackExtraction(final String name, final List<String> lines,
            final DocumentationSegment segment, final String fallback) {
        // first try to find configuration level, default is 2 (==)
        final TreeMap<Integer, List<Integer>> configurationLevels = lines
                .stream()
                .filter(it -> it.endsWith("= Configuration"))
                .map(it -> it.indexOf(' '))
                .collect(groupingBy(it -> it, TreeMap::new, toList()));
        if (configurationLevels.isEmpty()) {
            // no standard configuration, just return it all
            return fallback;
        }

        final int titleLevels = Math.max(1, configurationLevels.lastKey() - 1);
        final String prefixTitle = IntStream.range(0, titleLevels).mapToObj(i -> "=").collect(joining()) + " ";
        final int titleIndex = lines.indexOf(prefixTitle + name);
        if (titleIndex < 0) {
            return fallback;
        }

        List<String> endOfLines = lines.subList(titleIndex, lines.size());
        int lineIdx = 0;
        for (final String line : endOfLines) {
            if (lineIdx > 0 && line.startsWith(prefixTitle)) {
                endOfLines = endOfLines.subList(0, lineIdx);
                break;
            }
            lineIdx++;
        }
        if (!endOfLines.isEmpty()) {
            return extractSegmentFromTitles(segment, prefixTitle, endOfLines);
        }

        // if not found just return all the doc
        return fallback;
    }

    private String extractSegmentFromTitles(final DocumentationSegment segment, final String prefixTitle,
            final List<String> endOfLines) {
        if (endOfLines.isEmpty()) {
            return "";
        }
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
            default:
                return String.join("\n", endOfLines);
        }
    }

    private String extractSegmentFromComments(final DocumentationSegment segment, final List<String> lines) {
        if (lines.isEmpty()) {
            return "";
        }
        switch (segment) {
            case DESCRIPTION: {
                final int configStartIndex = lines.indexOf("//configuration_start");
                final int start = lines.get(0).startsWith("=") ? 1 : 0;
                if (configStartIndex > start) {
                    return String.join("\n", lines.subList(start, configStartIndex)).trim();
                }
                if (lines.get(0).startsWith("=")) {
                    return String.join("\n", lines.subList(1, lines.size()));
                }
                return String.join("\n", lines);
            }
            case CONFIGURATION: {
                int configStartIndex = lines.indexOf("//configuration_start");
                if (configStartIndex > 0) {
                    configStartIndex++;
                    final int configEndIndex = lines.indexOf("//configuration_end");
                    if (configEndIndex > configStartIndex) {
                        while (configStartIndex > 0 && configStartIndex < configEndIndex
                                && (lines.get(configStartIndex).isEmpty()
                                        || lines.get(configStartIndex).startsWith("="))) {
                            configStartIndex++;
                        }
                        if (configStartIndex > 0 && configEndIndex > configStartIndex + 2) {
                            return String.join("\n", lines.subList(configStartIndex, configEndIndex)).trim();
                        }
                    }
                }
                return "";
            }
            default:
                return String.join("\n", lines);
        }
    }

    private String getConfigTitle(final String prefixTitle) {
        return '=' + prefixTitle + "Configuration";
    }
}
