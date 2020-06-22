/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.di.studio;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.talend.sdk.component.api.record.dynamic.DynamicColumns.DYNAMIC_COLUMN_MARKER;

import routines.system.Dynamic;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.spi.JsonProvider;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.record.dynamic.DynamicColumns;
import org.talend.sdk.component.api.record.dynamic.DynamicColumnsHelper;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.di.AutoChunkProcessor;
import org.talend.sdk.component.runtime.di.InputsHandler;
import org.talend.sdk.component.runtime.di.JobStateAware;
import org.talend.sdk.component.runtime.di.OutputsHandler;
import org.talend.sdk.component.runtime.di.record.DiMappingMetaRegistry;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ComponentManager.AllServices;
import org.talend.sdk.component.runtime.manager.chain.ChainedMapper;
import org.talend.sdk.component.runtime.output.Branches;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.record.RecordConverters;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DynamicColumnsTest {

    protected static RecordBuilderFactory builderFactory;

    @BeforeAll
    static void forceManagerInit() {
        final ComponentManager manager = ComponentManager.instance();
        if (manager.find(Stream::of).count() == 0) {
            manager.addPlugin(new File("target/test-classes").getAbsolutePath());
        }
    }

    @Test
    void fromRecordToRowStructToRecord() {
        final ComponentManager manager = ComponentManager.instance();
        final Collection<Object> sourceData = new ArrayList<>();
        final Collection<Object> processorData = new ArrayList<>();
        doDi(manager, sourceData, processorData, manager.findProcessor("DynamicColumnsTest", "output", 1, emptyMap()),
                manager.findMapper("DynamicColumnsTest", "input", 1, singletonMap("count", "10")));
        assertEquals(10, sourceData.size());
        assertEquals(10, processorData.size());
    }

    private void doDi(final ComponentManager manager, final Collection<Object> sourceData,
            final Collection<Object> processorData, final Optional<Processor> proc, final Optional<Mapper> mapper) {
        final Map<String, Object> globalMap = new HashMap<>();
        try {
            final Processor processor = proc.orElseThrow(() -> new IllegalStateException("scanning failed"));
            JobStateAware.init(processor, globalMap);
            final Jsonb jsonbProcessor = Jsonb.class
                    .cast(manager
                            .findPlugin(processor.plugin())
                            .get()
                            .get(AllServices.class)
                            .getServices()
                            .get(Jsonb.class));
            final AutoChunkProcessor processorProcessor = new AutoChunkProcessor(100, processor);

            processorProcessor.start();
            globalMap.put("processorProcessor", processorProcessor);

            final java.util.Map<Class<?>, Object> servicesMapper =
                    manager.findPlugin(proc.get().plugin()).get().get(AllServices.class).getServices();

            final InputsHandler inputsHandlerProcessor = new InputsHandler(jsonbProcessor, servicesMapper);
            inputsHandlerProcessor.addConnection("FLOW", row1Struct.class);

            final OutputsHandler outputHandlerProcessor = new OutputsHandler(jsonbProcessor, servicesMapper);

            final InputFactory inputsProcessor = inputsHandlerProcessor.asInputFactory();
            final OutputFactory outputsProcessor = outputHandlerProcessor.asOutputFactory();

            final Mapper tempMapperMapper = mapper.orElseThrow(() -> new IllegalStateException("scanning failed"));
            JobStateAware.init(tempMapperMapper, globalMap);

            doRun(manager, sourceData, processorData, globalMap, processorProcessor, inputsHandlerProcessor,
                    outputHandlerProcessor, inputsProcessor, outputsProcessor, tempMapperMapper);
        } finally {
            doClose(globalMap);
        }
    }

    private void doRun(final ComponentManager manager, final Collection<Object> sourceData,
            final Collection<Object> processorData, final Map<String, Object> globalMap,
            final AutoChunkProcessor processorProcessor, final InputsHandler inputsHandlerProcessor,
            final OutputsHandler outputHandlerProcessor, final InputFactory inputsProcessor,
            final OutputFactory outputsProcessor, final Mapper tempMapperMapper) {
        row1Struct row1;
        tempMapperMapper.start();
        final ChainedMapper mapperMapper;
        try {
            final List<Mapper> splitMappersMapper = tempMapperMapper.split(tempMapperMapper.assess());
            mapperMapper = new ChainedMapper(tempMapperMapper, splitMappersMapper.iterator());
            mapperMapper.start();
            globalMap.put("mapperMapper", mapperMapper);
        } finally {
            try {
                tempMapperMapper.stop();
            } catch (final RuntimeException re) {
                re.printStackTrace();
            }
        }

        final Input inputMapper = mapperMapper.create();
        inputMapper.start();
        globalMap.put("inputMapper", inputMapper);

        final Map<Class<?>, Object> servicesMapper =
                manager.findPlugin(mapperMapper.plugin()).get().get(AllServices.class).getServices();
        final Jsonb jsonbMapper = Jsonb.class.cast(servicesMapper.get(Jsonb.class));
        final JsonProvider jsonProvider = JsonProvider.class.cast(servicesMapper.get(JsonProvider.class));
        final JsonBuilderFactory jsonBuilderFactory =
                JsonBuilderFactory.class.cast(servicesMapper.get(JsonBuilderFactory.class));
        final RecordBuilderFactory recordBuilderMapper =
                RecordBuilderFactory.class.cast(servicesMapper.get(RecordBuilderFactory.class));
        builderFactory = recordBuilderMapper;
        final RecordConverters converters = new RecordConverters();
        final DiMappingMetaRegistry registry = new DiMappingMetaRegistry();

        Object dataMapper;
        while ((dataMapper = inputMapper.next()) != null) {
            row1 = row1Struct.class
                    .cast(converters
                            .toType(registry, dataMapper, row1Struct.class, () -> jsonBuilderFactory,
                                    () -> jsonProvider, () -> jsonbMapper, () -> recordBuilderMapper));

            assertTrue(row1Struct.class.isInstance(row1));

            sourceData.add(row1);
            inputsHandlerProcessor.reset();
            inputsHandlerProcessor.setInputValue("FLOW", row1);
            outputHandlerProcessor.reset();
            processorProcessor.onElement(name -> {
                assertEquals(Branches.DEFAULT_BRANCH, name);
                final Object read = inputsProcessor.read(name);
                processorData.add(read);
                return read;
            }, outputsProcessor);
        }
    }

    private void doClose(final Map<String, Object> globalMap) {
        final Mapper mapperMapper = Mapper.class.cast(globalMap.remove("mapperMapper"));
        final Input inputMapper = Input.class.cast(globalMap.remove("inputMapper"));
        try {
            if (inputMapper != null) {
                inputMapper.stop();
            }
        } catch (final RuntimeException re) {
            fail(re.getMessage());
        } finally {
            try {
                if (mapperMapper != null) {
                    mapperMapper.stop();
                }
            } catch (final RuntimeException re) {
                fail(re.getMessage());
            }
        }

        final AutoChunkProcessor processorProcessor =
                AutoChunkProcessor.class.cast(globalMap.remove("processorProcessor"));
        try {
            if (processorProcessor != null) {
                processorProcessor.stop();
            }
        } catch (final RuntimeException re) {
            fail(re.getMessage());
        }
    }

    @org.talend.sdk.component.api.processor.Processor(name = "output", family = "DynamicColumnsTest")
    @DynamicColumns
    public static class OutputComponent implements Serializable {

        int counter;

        @ElementListener
        public void onElement(final Record record) {
            log.debug("[onElement] iteration#{} {}.", counter, record);
            assertNotNull(record);
            assertNotNull(record.getString("id"));
            assertNotNull(record.getString("name"));
            assertTrue(record.getString("name").startsWith("record"));
            Collection columns = record.getSchema().getEntries().stream().map(e -> e.getName()).collect(toList());
            assertTrue(DynamicColumnsHelper.hasDynamicColumn(columns));
            assertEquals("dynamic", DynamicColumnsHelper.getDynamicRealColumnName(columns));
            assertEquals("dynamic" + DYNAMIC_COLUMN_MARKER, DynamicColumnsHelper.getDynamicColumnName(columns));
            Record dynamic = record.getRecord(DynamicColumnsHelper.getDynamicColumnName(record.getSchema()));
            assertNotNull(dynamic);
            assertEquals(9, dynamic.getSchema().getEntries().size());
            assertEquals("value" + counter, dynamic.getString("string0"));
            assertEquals((counter % 2 == 0), dynamic.getBoolean("bool0"));
            assertEquals(counter, dynamic.getInt("int0"));
            assertEquals(counter, dynamic.getLong("long0"));
            assertEquals(1.23f * counter, dynamic.getFloat("float0"));
            assertEquals(12345.6789 * counter, dynamic.getDouble("double0"));
            assertEquals(String.format("zorglub-is-still-alive-%05d", counter), new String(dynamic.getBytes("bytes0")));
            assertEquals(IntStream.range(0, counter + 1).boxed().collect(toList()),
                    dynamic.getArray(Integer.class, "array0"));
            assertTrue(ZonedDateTime.now().toEpochSecond() >= dynamic.getDateTime("date0").toEpochSecond());

            counter++;
        }
    }

    @Emitter(name = "input", family = "DynamicColumnsTest")
    @DynamicColumns
    public static class InputComponent implements Serializable {

        private final PrimitiveIterator.OfInt stream;

        public InputComponent(@Option("count") final int count) {
            this.stream = IntStream.range(0, count).iterator();
        }

        @Producer
        public Record next() {
            if (!stream.hasNext()) {
                return null;
            }
            final Integer i = stream.next();
            final Record dyn = builderFactory
                    .newRecordBuilder()
                    .withString("string0", "value" + i)
                    .withBoolean("bool0", (i % 2 == 0))
                    .withInt("int0", i)
                    .withLong("long0", (long) i)
                    .withFloat("float0", 1.23f * i)
                    .withDouble("double0", 12345.6789 * i)
                    .withBytes("bytes0", String.format("zorglub-is-still-alive-%05d", i).getBytes())
                    .withArray(builderFactory
                            .newEntryBuilder()
                            .withName("array0")
                            .withType(Schema.Type.ARRAY)
                            .withElementSchema(builderFactory.newSchemaBuilder(Type.INT).build())
                            .build(), IntStream.range(0, i + 1).boxed().collect(toList()))
                    .withDateTime("date0", ZonedDateTime.now())
                    .build();
            Record record = builderFactory
                    .newRecordBuilder()
                    .withString("id", String.valueOf(i))
                    .withString("name", "record" + i)
                    .withRecord("dynamic", dyn)
                    .build();
            log.debug("[next] iteration#{} {}.", i, record);
            return record;
        }
    }

    @Getter
    @ToString
    public static class row1Struct implements routines.system.IPersistableRow {

        public String id;

        public String name;

        public Dynamic dynamic;

        @Override
        public void writeData(final ObjectOutputStream objectOutputStream) {
            throw new UnsupportedOperationException("#writeData()");
        }

        @Override
        public void readData(final ObjectInputStream objectInputStream) {
            throw new UnsupportedOperationException("#readData()");
        }
    }

}
