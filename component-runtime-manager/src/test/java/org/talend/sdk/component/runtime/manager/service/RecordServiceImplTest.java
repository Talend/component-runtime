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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.talend.sdk.component.api.record.Schema.Type.INT;
import static org.talend.sdk.component.api.record.Schema.Type.RECORD;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.bind.JsonbConfig;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.record.RecordService;
import org.talend.sdk.component.api.service.record.RecordVisitor;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

class RecordServiceImplTest {

    private final RecordBuilderFactory factory = new RecordBuilderFactoryImpl(null);

    private final RecordService service = RecordService.class
            .cast(new DefaultServiceProvider(null, JsonProvider.provider(), Json.createGeneratorFactory(emptyMap()),
                    Json.createReaderFactory(emptyMap()), Json.createBuilderFactory(emptyMap()),
                    Json.createParserFactory(emptyMap()), Json.createWriterFactory(emptyMap()), new JsonbConfig(),
                    JsonbProvider.provider(), null, null, emptyList(), t -> factory, null)
                            .lookup(null, Thread.currentThread().getContextClassLoader(), null, null,
                                    RecordService.class, null, null));

    private final Schema address = factory
            .newSchemaBuilder(RECORD)
            .withEntry(factory.newEntryBuilder().withName("street").withType(STRING).build())
            .withEntry(factory.newEntryBuilder().withName("number").withType(INT).build())
            .build();

    private final Schema baseSchema = factory
            .newSchemaBuilder(RECORD)
            .withEntry(factory.newEntryBuilder().withName("name").withType(STRING).build())
            .withEntry(factory.newEntryBuilder().withName("age").withType(INT).build())
            .withEntry(
                    factory.newEntryBuilder().withName("address").withType(RECORD).withElementSchema(address).build())
            .build();

    private final Record baseRecord = factory
            .newRecordBuilder(baseSchema)
            .withString("name", "Test")
            .withInt("age", 33)
            .withRecord("address",
                    factory.newRecordBuilder(address).withString("street", "here").withInt("number", 1).build())
            .build();

    @Test
    void mappers() {
        final Pojo pojo = new Pojo();
        pojo.name = "now";
        pojo.obj.objName = "objName222";

        final Record record = service.toRecord(pojo);
        assertNotNull(record);
        assertEquals("{\"name\":\"now\",\"obj\":{\"magic\":1971,\"objName\":\"objName222\"}}", record.toString());

        final Pojo after = service.toObject(record, Pojo.class);
        assertNotSame(after, pojo);
        assertEquals(after.name, pojo.name);
        assertEquals(after.obj.objName, pojo.obj.objName);
    }

    @Test
    void visit() {
        final Collection<String> visited = new ArrayList<>();
        final AtomicInteger out = new AtomicInteger();
        assertEquals(3,
                service
                        .visit(RecordVisitor.class
                                .cast(Proxy
                                        .newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                                new Class<?>[] { RecordVisitor.class }, (proxy, method, args) -> {
                                                    visited
                                                            .add(method.getName() + "/"
                                                                    + (args == null ? "null"
                                                                            : Stream
                                                                                    .of(args)
                                                                                    .filter(it -> !Schema.Entry.class
                                                                                            .isInstance(it))
                                                                                    .collect(Collectors.toList())));
                                                    switch (method.getName()) {
                                                    case "get":
                                                        return out.incrementAndGet();
                                                    case "apply":
                                                        return asList(args)
                                                                .stream()
                                                                .mapToInt(Integer.class::cast)
                                                                .sum();
                                                    default:
                                                        return method.getReturnType() == RecordVisitor.class ? proxy
                                                                : null;
                                                    }
                                                })),
                                baseRecord));
        assertEquals(asList("onString/[Optional[Test]]", "onInt/[OptionalInt[33]]",
                "onRecord/[Optional[{\"street\":\"here\",\"number\":1}]]", "onString/[Optional[here]]",
                "onInt/[OptionalInt[1]]", "get/null", "get/null", "apply/[1, 2]"), visited);
    }

    @Test
    void buildRecord() {
        final Schema customSchema = factory
                .newSchemaBuilder(baseSchema)
                .withEntry(factory
                        .newEntryBuilder()
                        .withName("custom")
                        .withType(STRING)
                        .withNullable(true)
                        .withMetadata(true)
                        .build())
                .build();

        final List<Collection<String>> spy = asList(new LinkedList<>(), new LinkedList<>());
        final Record noCustomRecord = service.create(customSchema, baseRecord, (entry, builder) -> {
            spy.get(0).add("visited=" + entry.getName());
            if (entry.getName().equals("custom")) {
                builder.withString("custom", "yes");
                return true;
            }
            return false;
        }, (builder, done) -> {
            spy.get(0).add("done=" + done);
            if (!done) {
                builder.withString("custom", "yes");
            }
        });
        final Record customRecord = service.create(customSchema, noCustomRecord, (entry, builder) -> {
            spy.get(1).add("visited=" + entry.getName());
            if ("custom".equals(entry.getName())) {
                builder.withString("custom", "yes");
                return true;
            }
            return false;
        }, (builder, done) -> spy.get(1).add("done=" + done));
        Stream
                .of(noCustomRecord, customRecord)
                .forEach(record -> assertEquals(
                        "custom=yes,name=Test,age=33,address={\"street\":\"here\",\"number\":1}", toString(record)));
        assertEquals(asList("visited=name", "visited=age", "visited=address", "done=false"), spy.get(0));
        assertEquals(asList("visited=custom", "visited=name", "visited=age", "visited=address", "done=true"),
                spy.get(1));
    }

    private String toString(final Record record) {
        return record
                .getSchema()
                .getAllEntries()
                .map(e -> e.getName() + '=' + record.get(Object.class, e.getName()))
                .collect(joining(","));
    }

    public static class Pojo {

        public String name;

        public ObjConfiguration obj = new ObjConfiguration();
    }

    public static class ObjConfiguration {

        public String objName;

        public int magic = 1971;
    }
}
