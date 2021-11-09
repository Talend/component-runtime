/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordService;
import org.talend.sdk.component.api.service.record.RecordVisitor;
import org.talend.sdk.component.runtime.manager.service.DefaultServiceProvider;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.singer.java.Singer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor // todo: simplify with recordconverters
public class RecordJsonMapper implements Function<Record, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    private final Singer singer;

    private final RecordService service = RecordService.class
            .cast(new DefaultServiceProvider(null, JsonProvider.provider(), Json.createGeneratorFactory(emptyMap()),
                    Json.createReaderFactory(emptyMap()), Json.createBuilderFactory(emptyMap()),
                    Json.createParserFactory(emptyMap()), Json.createWriterFactory(emptyMap()), new JsonbConfig(),
                    JsonbProvider.provider(), null, null, emptyList(), t -> new RecordBuilderFactoryImpl("kitap"), null)
                            .lookup(null, Thread.currentThread().getContextClassLoader(), null, null,
                                    RecordService.class, null, null));

    @Override
    public JsonObject apply(final Record record) {
        return service.visit(new JsonVisitor(singer, service, jsonBuilderFactory), record);
    }

    private static class JsonVisitor implements RecordVisitor<JsonObject> {

        private final Singer singer;

        private final RecordService service;

        private final JsonBuilderFactory factory;

        private final JsonObjectBuilder builder;

        private final LinkedList<BiFunction<JsonObject, JsonObject, JsonObject>> stack = new LinkedList<>();

        private JsonVisitor(final Singer singer, final RecordService service,
                final JsonBuilderFactory jsonBuilderFactory) {
            this.singer = singer;
            this.service = service;
            this.factory = jsonBuilderFactory;
            this.builder = jsonBuilderFactory.createObjectBuilder();
        }

        @Override
        public JsonObject get() {
            return builder.build();
        }

        @Override
        public JsonObject apply(final JsonObject t1, final JsonObject t2) {
            return stack.removeLast().apply(t1, t2);
        }

        @Override
        public void onInt(final Schema.Entry entry, final OptionalInt optionalInt) {
            optionalInt.ifPresent(v -> builder.add(entry.getName(), v));
        }

        @Override
        public void onLong(final Schema.Entry entry, final OptionalLong optionalLong) {
            optionalLong.ifPresent(v -> builder.add(entry.getName(), v));
        }

        @Override
        public void onFloat(final Schema.Entry entry, final OptionalDouble optionalFloat) {
            optionalFloat.ifPresent(v -> builder.add(entry.getName(), v));
        }

        @Override
        public void onDouble(final Schema.Entry entry, final OptionalDouble optionalDouble) {
            optionalDouble.ifPresent(v -> builder.add(entry.getName(), v));
        }

        @Override
        public void onBoolean(final Schema.Entry entry, final Optional<Boolean> optionalBoolean) {
            optionalBoolean.ifPresent(v -> builder.add(entry.getName(), v));
        }

        @Override
        public void onString(final Schema.Entry entry, final Optional<String> string) {
            string.ifPresent(v -> builder.add(entry.getName(), v));
        }

        @Override
        public void onDatetime(final Schema.Entry entry, final Optional<ZonedDateTime> dateTime) {
            dateTime.ifPresent(v -> builder.add(entry.getName(), singer.formatDate(v)));
        }

        @Override
        public void onBytes(final Schema.Entry entry, final Optional<byte[]> bytes) {
            bytes.ifPresent(v -> builder.add(entry.getName(), Base64.getEncoder().encodeToString(v)));
        }

        @Override
        public RecordVisitor<JsonObject> onRecord(final Schema.Entry entry, final Optional<Record> record) {
            stack
                    .add((o1, o2) -> factory
                            .createObjectBuilder(o2)
                            .add(entry.getName(), JsonObject.class.cast(o1))
                            .build());
            return new JsonVisitor(singer, service, factory);
        }

        @Override
        public void onIntArray(final Schema.Entry entry, final Optional<Collection<Integer>> array) {
            array
                    .ifPresent(vs -> builder
                            .add(entry.getName(),
                                    vs
                                            .stream()
                                            .collect(factory::createArrayBuilder, JsonArrayBuilder::add,
                                                    JsonArrayBuilder::addAll)
                                            .build()));
        }

        @Override
        public void onLongArray(final Schema.Entry entry, final Optional<Collection<Long>> array) {
            array
                    .ifPresent(vs -> builder
                            .add(entry.getName(),
                                    vs
                                            .stream()
                                            .collect(factory::createArrayBuilder, JsonArrayBuilder::add,
                                                    JsonArrayBuilder::addAll)
                                            .build()));
        }

        @Override
        public void onFloatArray(final Schema.Entry entry, final Optional<Collection<Float>> array) {
            array
                    .ifPresent(vs -> builder
                            .add(entry.getName(),
                                    vs
                                            .stream()
                                            .collect(factory::createArrayBuilder, JsonArrayBuilder::add,
                                                    JsonArrayBuilder::addAll)
                                            .build()));
        }

        @Override
        public void onDoubleArray(final Schema.Entry entry, final Optional<Collection<Double>> array) {
            array
                    .ifPresent(vs -> builder
                            .add(entry.getName(),
                                    vs
                                            .stream()
                                            .collect(factory::createArrayBuilder, JsonArrayBuilder::add,
                                                    JsonArrayBuilder::addAll)
                                            .build()));
        }

        @Override
        public void onBooleanArray(final Schema.Entry entry, final Optional<Collection<Boolean>> array) {
            array
                    .ifPresent(vs -> builder
                            .add(entry.getName(),
                                    vs
                                            .stream()
                                            .collect(factory::createArrayBuilder, JsonArrayBuilder::add,
                                                    JsonArrayBuilder::addAll)
                                            .build()));
        }

        @Override
        public void onStringArray(final Schema.Entry entry, final Optional<Collection<String>> array) {
            array
                    .ifPresent(vs -> builder
                            .add(entry.getName(),
                                    vs
                                            .stream()
                                            .collect(factory::createArrayBuilder, JsonArrayBuilder::add,
                                                    JsonArrayBuilder::addAll)
                                            .build()));
        }

        @Override
        public void onDatetimeArray(final Schema.Entry entry, final Optional<Collection<ZonedDateTime>> array) {
            array
                    .ifPresent(vs -> builder
                            .add(entry.getName(),
                                    vs
                                            .stream()
                                            .map(singer::formatDate)
                                            .collect(factory::createArrayBuilder, JsonArrayBuilder::add,
                                                    JsonArrayBuilder::addAll)
                                            .build()));
        }

        @Override
        public void onBytesArray(final Schema.Entry entry, final Optional<Collection<byte[]>> array) {
            array
                    .ifPresent(vs -> builder
                            .add(entry.getName(),
                                    vs
                                            .stream()
                                            .map(it -> Base64.getEncoder().encodeToString(it))
                                            .collect(factory::createArrayBuilder, JsonArrayBuilder::add,
                                                    JsonArrayBuilder::addAll)
                                            .build()));
        }

        @Override
        public RecordVisitor<JsonObject> onRecordArray(final Schema.Entry entry,
                final Optional<Collection<Record>> array) {
            array
                    .ifPresent(vs -> builder
                            .add(entry.getName(),
                                    vs
                                            .stream()
                                            .map(it -> service.visit(new JsonVisitor(singer, service, factory), it))
                                            .collect(factory::createArrayBuilder, JsonArrayBuilder::add,
                                                    JsonArrayBuilder::addAll)
                                            .build()));
            return new RecordVisitor<JsonObject>() {
            }; // no-op since we already handled it
        }
    }
}
