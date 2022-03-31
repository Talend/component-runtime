/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.runtime.documentation;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyOrderStrategy;

import crawlercommons.sitemaps.SiteMap;
import crawlercommons.sitemaps.SiteMapParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// js crawler are cool but not multithreaded.
// this impl is not sexy but way faster!
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class SearchIndexation {

    /**
     * Input:
     * <ul>
     * <li>0: sitemap.xml location</li>
     * </ul>
     *
     * @param args the inputs.
     * @throws Exception if there is an error.
     */
    public static void main(final String[] args) throws Exception {
        final JsonBuilderFactory factory = Json.createBuilderFactory(emptyMap());

        final File siteMapFile = new File(args[0]);
        final String latest = args[1];
        log.info("[main] sitemap: {}; latest: {}.", siteMapFile.toString(), latest);
        final String urlMarker = "/component-runtime/";
        final SiteMap siteMap = SiteMap.class
                .cast(new SiteMapParser(false /* we index a local file with remote urls */)
                        .parseSiteMap(siteMapFile.toURI().toURL()));
        final ExecutorService pool = Executors.newFixedThreadPool(Integer.getInteger("talend.algolia.indexation", 256));
        final List<Future<List<JsonObject>>> updates = siteMap.getSiteMapUrls().stream().filter(url -> {
            // filter not indexed pages
            final String externalForm = url.getUrl().toExternalForm();
            return !externalForm.contains("/all-in-one.html") && !externalForm.contains("/pdf-")
                    && !externalForm.contains("/landing.html") && !externalForm.contains("/index.html");
        }).filter(it -> it.getUrl().toExternalForm().contains(urlMarker)).map(url -> pool.submit(() -> {
            final String target = url.getUrl().toExternalForm();
            final String path = target.substring(target.indexOf(urlMarker) + urlMarker.length());
            final File adocSource = new File(siteMapFile.getParentFile().getParentFile().getParentFile(),
                    "src/main/antora/modules/ROOT/pages"
                            + path.substring(path.lastIndexOf('/'), path.length() - ".html".length()) + ".adoc");
            if (!adocSource.exists() || Files
                    .readAllLines(adocSource.toPath())
                    .stream()
                    .anyMatch(line -> line.trim().startsWith(":page-talend_skipindexation:"))) {
                return Collections.<JsonObject> emptyList();
            }
            log.debug("Indexing {}", target);
            final File relativeHtml = new File(siteMapFile.getParentFile(), path);
            try {
                final Document document = Jsoup.parse(String.join("\n", Files.readAllLines(relativeHtml.toPath())));
                if (!document.select("div > div.sect1.relatedlinks").isEmpty()) {
                    document.select("div > div.sect1.relatedlinks").first().html("");
                }
                final JsonObjectBuilder builder = factory.createObjectBuilder();
                builder
                        .add("lang", "en")
                        .add("title", extractTitle(document))
                        .add("version", document.select("div.navigation" + "-container").attr("data-version"))
                        .add("url", target)
                        .add("date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") {

                            {
                                setTimeZone(TimeZone.getTimeZone("UTC"));
                            }
                        }.format(url.getLastModified()))
                        .add("timestamp", url.getLastModified().getTime());
                IntStream
                        .rangeClosed(0, 3)
                        .forEach(i -> select(document, factory, ".doc " + "h" + (i + 1))
                                .ifPresent(value -> builder.add("lvl" + i, value)));

                selectMeta(document, "description").ifPresent(description -> builder.add("description", description));
                selectMeta(document, "keywords").ifPresent(keywords -> builder.add("keywords", keywords));

                final Elements texts = document.select(".doc p, .doc td.content, .doc th.tableblock");
                if (texts.isEmpty()) {
                    return singletonList(builder.build());
                }

                /*
                 * was for algolia but no more needed, keeping in case we want to resplit for the same reason
                 * return texts
                 * .stream()
                 * .map(Element::text)
                 * .map(v -> factory.createObjectBuilder(base).add("text", v).build())
                 * .collect(toList());
                 */
                return singletonList(builder
                        .add("text",
                                texts
                                        .stream()
                                        .map(Element::text)
                                        .collect(Collector
                                                .of(factory::createArrayBuilder, JsonArrayBuilder::add,
                                                        JsonArrayBuilder::addAll)))
                        .build());
            } catch (final Exception e) {
                log.warn(target + ": " + e.getMessage());
                return Collections.<JsonObject> emptyList();
            }
        })).collect(toList());
        pool.shutdown();

        // await
        updates.forEach(f -> {
            try {
                f.get();
            } catch (final InterruptedException | ExecutionException e) {
                log.error(e.getMessage());
            }
        });

        try (final Jsonb jsonb = JsonbBuilder
                .create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL))) {
            final Map<String, List<JsonObject>> byVersion = updates.stream().map(f -> {
                try {
                    return f.get();
                } catch (final InterruptedException | ExecutionException e) {
                    throw new IllegalStateException(e);
                }
            })
                    .flatMap(Collection::stream)
                    .sorted(comparing(o -> o.getString("title")))
                    .collect(groupingBy(
                            o -> (o.getString("version").equals(latest) ? "latest" : o.getString("version"))));
            byVersion.forEach((version, records) -> {
                final File file = new File(siteMapFile.getParentFile(), "main/" + version + "/search-index.json");
                try (final OutputStream output = new WriteIfDifferentStream(file)) {
                    jsonb.toJson(records, output);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
                log.info("Created {}", file);
            });
        }
    }

    private static String extractTitle(final Document document) {
        final String title = ofNullable(document.title())
                .filter(t -> !t.isEmpty())
                .orElseGet(() -> document.getElementsByTag("h1").text());
        if (title.contains(":: ")) {
            return title.substring(title.lastIndexOf(":: ") + 2).trim();
        }
        return title;
    }

    private static Optional<String> selectMeta(final Document document, final String metaName) {
        return document
                .select("meta")
                .stream()
                .filter(it -> it.hasAttr("name") && it.hasAttr("content") && metaName.equals(it.attr("name")))
                .findFirst()
                .map(it -> it.attr("content"));
    }

    private static Optional<JsonArrayBuilder> select(final Document document, final JsonBuilderFactory factory,
            final String selector) {
        final Elements select = document.select(selector);
        if (select.isEmpty()) {
            return Optional.empty();
        }
        return of(select
                .stream()
                .map(Element::text)
                .filter(s -> !s.matches("In this article|Related articles"))
                .filter(s -> !s.isEmpty())
                .distinct()
                .map(Json::createValue)
                .collect(factory::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::addAll));
    }
}
