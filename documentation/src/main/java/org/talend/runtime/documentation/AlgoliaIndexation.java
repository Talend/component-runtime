/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import static com.algolia.search.Defaults.ALGOLIANET_COM;
import static com.algolia.search.Defaults.ALGOLIA_NET;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.algolia.search.APIClient;
import com.algolia.search.ApacheAPIClientBuilder;
import com.algolia.search.Index;
import com.algolia.search.exceptions.AlgoliaException;
import com.algolia.search.objects.IndexSettings;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import crawlercommons.sitemaps.SiteMap;
import crawlercommons.sitemaps.SiteMapParser;

import org.apache.commons.codec.digest.DigestUtils;
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
public class AlgoliaIndexation {

    /**
     * Inputs:
     * <ul>
     * <li>0: sitemap.xml location</li>
     * <li>1: applicationId</li>
     * <li>2: apiKey</li>
     * </ul>
     *
     * @param args the inputs.
     */
    public static void main(final String[] args) throws Exception {
        System.setProperty("networkaddress.cache.ttl", "60");

        final JsonBuilderFactory factory = Json.createBuilderFactory(emptyMap());

        final SiteMap siteMap = SiteMap.class.cast(
                new SiteMapParser(false /* we index a local file with remote urls */).parseSiteMap(new URL(args[0])));
        final ExecutorService pool = Executors.newFixedThreadPool(Integer.getInteger("talend.algolia.indexation", 256));
        final List<Future<List<JsonObject>>> updates = siteMap.getSiteMapUrls().stream().filter(url -> {
            // filter not indexed pages
            final String externalForm = url.getUrl().toExternalForm();
            return !externalForm.contains("/all-in-one") && !externalForm.contains("/index-");
        }).map(url -> pool.submit(() -> {
            final URL target = url.getUrl();
            log.debug("Indexing {}", target);
            try {
                final String urlStr = target.toExternalForm();
                final Document document = Jsoup.connect(urlStr).get();
                final JsonObjectBuilder builder = factory.createObjectBuilder();
                builder
                        .add("lang", "en")
                        .add("title",
                                ofNullable(document.title()).filter(t -> !t.isEmpty()).orElseGet(
                                        () -> document.getElementsByTag("h1").text()))
                        .add("version", document.select("div.navigation" + "-container").attr("data-version"))
                        .add("url", urlStr)
                        .add("date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") {

                            {
                                setTimeZone(TimeZone.getTimeZone("UTC"));
                            }
                        }.format(url.getLastModified()))
                        .add("timestamp", url.getLastModified().getTime());
                IntStream.rangeClosed(0, 3).forEach(i -> select(document, factory, ".doc " + "h" + (i + 1))
                        .ifPresent(value -> builder.add("lvl" + i, value)));
                builder.add("objectID", Base64.getEncoder().encodeToString(DigestUtils.sha1(urlStr)));

                final JsonObject base = builder.build();

                final Elements texts = document.select(".doc p, .doc td.content, .doc th.tableblock");
                if (texts.isEmpty()) {
                    return singletonList(base);
                }

                return texts
                        .stream()
                        .map(Element::text)
                        .map(v -> factory.createObjectBuilder(base).add("text", v).build())
                        .collect(toList());
            } catch (final IOException e) {
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

        final ObjectMapper mapper = new ObjectMapper().enable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT).disable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        final APIClient client = new ApacheAPIClientBuilder(args[1], args[2])
                .setObjectMapper(mapper)
                .setBuildHosts(((Supplier<List<String>>) () -> { // bug in the lib
                    final List<String> hosts = new ArrayList<>(4);
                    hosts.addAll(IntStream.rangeClosed(1, 3).mapToObj(i -> args[1] + "-3." + ALGOLIANET_COM).collect(
                            toList()));
                    Collections.shuffle(hosts);
                    hosts.add(0, args[1] + "." + ALGOLIA_NET);
                    return hosts;
                }).get())
                .build();
        final Index<JsonNode> index = client.initIndex("githubantora", JsonNode.class);
        index.setSettings(new IndexSettings()
                .setSearchableAttributes(asList("title", "unordered(lvl0)", "unordered(lvl1)", "unordered(lvl2)",
                        "unordered(lvl3)", "unordered(text)"))
                .setAttributesForFaceting(asList("lang", "version")));
        try {
            final List<JsonNode> collect = updates.stream().map(future -> {
                try {
                    return future.get();
                } catch (final InterruptedException | ExecutionException e) {
                    return null;
                }
            })
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(JsonValue::asJsonObject)
                    .map(JsonObject::toString)
                    .map(json -> {
                        try {
                            return mapper.readTree(json);
                        } catch (final IOException e) {
                            throw new IllegalArgumentException(e);
                        }
                    })
                    .collect(toList());
            if (collect.size() == 0) {
                throw new IllegalStateException("No data to index!");
            }
            log.info("{} records to index", collect.size());
            final int batchSize = Integer.getInteger("talend.algolia.batchSize", 1000);
            final AtomicInteger counter = new AtomicInteger();
            collect.stream().collect(groupingBy(i -> counter.getAndIncrement() / batchSize)).values().forEach(chunk -> {
                try {
                    index.saveObjects(chunk);
                } catch (final AlgoliaException e) {
                    throw new IllegalStateException(e);
                }
            });
            log.info("Index updated, {} #records", collect.size());
        } finally {
            client.close();
        }
    }

    private static Optional<JsonArrayBuilder> select(final Document document, final JsonBuilderFactory factory,
            final String selector) {
        final Elements select = document.select(selector);
        if (select.isEmpty()) {
            return Optional.empty();
        }
        return of(select.stream().map(Element::text).map(Json::createValue).collect(factory::createArrayBuilder,
                JsonArrayBuilder::add, JsonArrayBuilder::addAll));
    }
}
