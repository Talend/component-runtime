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
package org.talend.sdk.component.singer.kitap;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.StringReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.singer.java.Singer;
import org.talend.sdk.component.singer.java.SingerArgs;

import lombok.RequiredArgsConstructor;

public final class Kitap implements Runnable {

    private final SingerArgs args;

    private final Singer singer;

    private final JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(emptyMap());

    private final RecordJsonMapper recordJsonMapper;

    public Kitap(final String... args) {
        this(new SingerArgs(args), new Singer());
    }

    public Kitap(final SingerArgs args, final Singer singer) {
        this.args = args;
        this.singer = singer;
        this.recordJsonMapper = new RecordJsonMapper(jsonBuilderFactory, singer);
    }

    @Override
    public void run() {
        final JsonObject componentConfig = ofNullable(args.getConfig().getJsonObject("component"))
                .orElseGet(() -> ofNullable(args.getConfig().getJsonString("component_config")).map(jsonString -> {
                    try (final JsonReader reader = Json.createReader(new StringReader(jsonString.getString()))) {
                        return reader.readObject();
                    }
                })
                        .map(json -> json.containsKey("component") ? json.getJsonObject("component") : json)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No 'component' or 'component_config' entry in config.json")));
        final JsonObject stream = args
                .getCatalog()
                .flatMap(this::extractSelectedStream)
                .orElseGet(() -> jsonBuilderFactory.createObjectBuilder().add("stream", "default").build());
        final String streamName = stream.getString("stream");

        final ComponentManager mgr = ComponentManager.instance();
        if (mgr.find(Stream::of).count() == 0) { // note: normally TALEND-INF/plugins.properties is enough
            mgr.autoDiscoverPlugins(false, true);
        }

        final List<String> missingConfigs = Stream
                .concat(args.getComponentFamily().isPresent() ? Stream.of("family") : Stream.empty(), Stream.of("name"))
                .filter(key -> !componentConfig.containsKey(key))
                .collect(toList());
        if (!missingConfigs.isEmpty()) {
            throw new IllegalArgumentException("Missing component configuration entries: " + missingConfigs);
        }

        final String family = args.getComponentFamily().orElseGet(() -> componentConfig.getString("family"));
        final String name = componentConfig.getString("name");
        final int version = componentConfig.getInt("version", 0);
        final Map<String, String> configuration = ofNullable(componentConfig.getJsonObject("configuration"))
                .map(this::toConfig)
                .orElseGet(Collections::emptyMap);

        final Mapper mapper = mgr
                .findMapper(family, name, version, configuration)
                .orElseThrow(
                        () -> new IllegalArgumentException("Didn't find the component: '" + family + '#' + name + "'"));

        if (args.isDiscover()) {
            discover(mapper);
        } else {
            readAll(args.getConfig().getJsonObject("schemaCustomization"), stream, streamName, mapper);
        }
    }

    private void discover(final Mapper mapper) {
        final JsonObject schema = records(mapper)
                .findFirst()
                .map(record -> new JsonSchemaGenerator(record.getSchema().getEntries(), jsonBuilderFactory).get())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No record found for " + mapper.rootName() + '#' + mapper.name()));
        final JsonArray metadata = schema
                .getJsonObject("properties")
                .entrySet()
                .stream()
                .collect(jsonBuilderFactory::createArrayBuilder, (a, p) -> {
                    a
                            .add(jsonBuilderFactory
                                    .createObjectBuilder()
                                    .add("metadata",
                                            jsonBuilderFactory
                                                    .createObjectBuilder()
                                                    .add("inclusion", "automatic")
                                                    .add("selected-by-default", true))
                                    .add("breadcrumb",
                                            jsonBuilderFactory.createArrayBuilder().add("properties").add(p.getKey())));
                }, JsonArrayBuilder::addAll)
                .build();
        final String streams = jsonBuilderFactory
                .createObjectBuilder()
                .add("streams",
                        jsonBuilderFactory
                                .createArrayBuilder()
                                .add(jsonBuilderFactory
                                        .createObjectBuilder()
                                        .add("tap_stream_id", "default")
                                        .add("stream", "default")
                                        .add("schema", schema)
                                        .add("metadata", metadata)))
                .build()
                .toString();
        singer.stdout(streams);
    }

    private void readAll(final JsonObject config, final JsonObject stream, final String streamName,
            final Mapper mapper) {
        final AtomicReference<Schema> lastSchema = new AtomicReference<>();
        records(mapper).peek(record -> {
            final Schema newSchema = record.getSchema();
            if (!newSchema.equals(lastSchema.get())) {
                final JsonArray keys = config != null && config.containsKey("keys") ? config.getJsonArray("keys")
                        : jsonBuilderFactory.createArrayBuilder().build();
                final JsonArray bookmarks =
                        config != null && config.containsKey("bookmarks") ? config.getJsonArray("bookmarks")
                                : jsonBuilderFactory.createArrayBuilder().build();
                final JsonObject schema = ofNullable(stream.getJsonObject("schema"))
                        .orElseGet(() -> new JsonSchemaGenerator(newSchema.getEntries(), jsonBuilderFactory).get());
                singer.writeSchema(streamName, schema, keys, bookmarks);
                lastSchema.set(newSchema);
            }
        }).forEach(record -> singer.writeRecord(streamName, recordJsonMapper.apply(record)));
    }

    private Stream<Record> records(final Mapper mapper) {
        return createPartitions(mapper)
                .stream() // note: can be parallelized at some point if needed but NOT parallelStream() please
                .flatMap(m -> {
                    final Input input = m.create();
                    input.start();
                    return StreamSupport
                            .stream(spliteratorUnknownSize(new InputIterator(input), Spliterator.IMMUTABLE), false);
                });
    }

    private Map<String, String> toConfig(final JsonObject object) {
        return object
                .entrySet()
                .stream()
                .filter(e -> !JsonValue.NULL.equals(e.getValue()))
                .collect(toMap(Map.Entry::getKey, e -> {
                    switch (e.getValue().getValueType()) {
                    case STRING:
                        return JsonString.class.cast(e.getValue()).getString();
                    case NUMBER:
                        return String.valueOf(JsonNumber.class.cast(e.getValue()).doubleValue());
                    case TRUE:
                    case FALSE:
                        return String.valueOf(JsonValue.TRUE.equals(e.getValue()));
                    default:
                        throw new IllegalArgumentException("Unsupported json entry: " + e);
                    }
                }));
    }

    private Optional<JsonObject> extractSelectedStream(final JsonObject jsonObject) {
        return ofNullable(jsonObject.getJsonArray("streams"))
                .flatMap(streams -> streams
                        .stream()
                        .filter(stream -> stream.getValueType() == JsonValue.ValueType.OBJECT)
                        .map(JsonValue::asJsonObject)
                        .filter(this::hasSelectedProperty)
                        .findFirst());
    }

    private boolean hasSelectedProperty(final JsonObject stream) {
        return ofNullable(stream.getJsonObject("schema"))
                .map(schema -> schema.getJsonObject("properties"))
                .map(properties -> properties
                        .values()
                        .stream()
                        .filter(prop -> prop.getValueType() == JsonValue.ValueType.OBJECT)
                        .anyMatch(prop -> prop.asJsonObject().getBoolean("selected", false)))
                .orElse(false);
    }

    private List<Mapper> createPartitions(final Mapper mapper) {
        final List<Mapper> partitions;
        mapper.start();
        try {
            partitions = requireNonNull(mapper.split(mapper.assess()), "No mapper created after splitting the source");
        } finally {
            mapper.stop();
        }
        return partitions;
    }

    public static void main(final String... args) {
        EnvironmentSetup.init();
        new Kitap(args).run();
    }

    @RequiredArgsConstructor
    private static final class InputIterator implements Iterator<Record> {

        private final Input input;

        private Object next;

        @Override
        public boolean hasNext() {
            next = input.next();
            final boolean hasNext = next != null;
            if (!hasNext) {
                input.stop();
            }
            return hasNext;
        }

        @Override
        public Record next() {
            return Record.class.cast(next);
        }
    }
}
