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
package org.talend.sdk.component.runtime.di.studio;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.JsonBuilderFactory;
import javax.json.bind.Jsonb;
import javax.json.spi.JsonProvider;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.context.RuntimeContext;
import org.talend.sdk.component.api.context.RuntimeContextInjector;
import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.connection.CloseConnection;
import org.talend.sdk.component.api.service.connection.CloseConnectionObject;
import org.talend.sdk.component.api.service.connection.CreateConnection;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.di.AutoChunkProcessor;
import org.talend.sdk.component.runtime.di.InputsHandler;
import org.talend.sdk.component.runtime.di.JobStateAware;
import org.talend.sdk.component.runtime.di.OutputsHandler;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ComponentManager.AllServices;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.chain.ChainedMapper;
import org.talend.sdk.component.runtime.output.Branches;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.record.RecordConverters;
import org.talend.sdk.component.runtime.record.RecordConverters.MappingMetaRegistry;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import routines.system.Dynamic;

@Slf4j
public class DynamicColumnsTest {

    protected static RecordBuilderFactory builderFactory;

    // do the same thing with studio
    private static final Map<String, Object> globalMap = Collections.synchronizedMap(new HashMap<>());

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

        callConnectionComponent(manager);

        doDi(manager, sourceData, processorData, manager.findProcessor("DynamicColumnsTest", "outputDi", 1, emptyMap()),
                manager.findMapper("DynamicColumnsTest", "inputDi", 1, singletonMap("count", "10")));
        assertEquals(10, sourceData.size());
        assertEquals(10, processorData.size());

        callCloseComponent(manager);
    }

    private void callCloseComponent(final ComponentManager manager) {
        manager
                .findPlugin("test-classes")
                .get()
                .get(ContainerComponentRegistry.class)
                .getServices()
                .stream()
                .flatMap(c -> c.getActions().stream())
                .filter(actionMeta -> "closeconnection".equals(actionMeta.getType()))
                .forEach(actionMeta -> {
                    Object result = actionMeta.getInvoker().apply(null);
                    CloseConnectionObject cco = (CloseConnectionObject) result;

                    RuntimeContext runtimeContext = new RuntimeContext() {

                        @Override
                        public Object getConnection() {
                            return globalMap.get("conn_tS3Connection_1");
                        }

                    };

                    cco.setRuntimeContext(runtimeContext);
                    boolean r = cco.close();
                    assertEquals(true, r);
                });
    }

    private void callConnectionComponent(final ComponentManager manager) {
        final Map<String, String> runtimeParams = new HashMap<>();
        // TODO how to match the path in client?
        runtimeParams.put("conn.para1", "v1");
        runtimeParams.put("conn.para2", "100");

        // TODO how to get the plugin id in client?
        manager
                .findPlugin("test-classes")
                .get()
                .get(ContainerComponentRegistry.class)
                .getServices()
                .stream()
                .flatMap(c -> c.getActions().stream())
                .filter(actionMeta -> "createconnection".equals(actionMeta.getType()))
                .forEach(actionMeta -> {
                    Object connnection = actionMeta.getInvoker().apply(runtimeParams);
                    assertEquals("v1100", connnection);

                    globalMap.put("conn_tS3Connection_1", connnection);
                });
    }

    private void doDi(final ComponentManager manager, final Collection<Object> sourceData,
            final Collection<Object> processorData, final Optional<Processor> proc, final Optional<Mapper> mapper) {
        try {
            final Processor processor = proc.orElseThrow(() -> new IllegalStateException("scanning failed"));

            try {
                Field field = processor.getClass().getSuperclass().getDeclaredField("delegate");
                field.setAccessible(true);
                Object v = field.get(processor);
                if (v instanceof RuntimeContextInjector) {
                    RuntimeContext runtimeContext = new RuntimeContext() {

                        @Override
                        public Object getConnection() {
                            return globalMap.get("conn_tS3Connection_1");
                        }

                    };

                    ((RuntimeContextInjector) v).setRuntimeContext(runtimeContext);
                }
                field.setAccessible(false);
            } catch (Exception e) {
                System.out.println(e);
            }

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

            final Map<Class<?>, Object> servicesMapper =
                    manager.findPlugin(proc.get().plugin()).get().get(AllServices.class).getServices();

            final InputsHandler inputsHandlerProcessor = new InputsHandler(jsonbProcessor, servicesMapper);
            inputsHandlerProcessor.addConnection("FLOW", row1Struct.class);

            final OutputsHandler outputHandlerProcessor = new OutputsHandler(jsonbProcessor, servicesMapper);

            final InputFactory inputsProcessor = inputsHandlerProcessor.asInputFactory();
            final OutputFactory outputsProcessor = outputHandlerProcessor.asOutputFactory();

            final Mapper tempMapperMapper = mapper.orElseThrow(() -> new IllegalStateException("scanning failed"));
            JobStateAware.init(tempMapperMapper, globalMap);

            doRun(manager, sourceData, processorData, processorProcessor, inputsHandlerProcessor,
                    outputHandlerProcessor, inputsProcessor, outputsProcessor, tempMapperMapper);
        } finally {
            doClose(globalMap);
        }
    }

    private void doRun(final ComponentManager manager, final Collection<Object> sourceData,
            final Collection<Object> processorData, final AutoChunkProcessor processorProcessor,
            final InputsHandler inputsHandlerProcessor, final OutputsHandler outputHandlerProcessor,
            final InputFactory inputsProcessor, final OutputFactory outputsProcessor, final Mapper tempMapperMapper) {
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
        final MappingMetaRegistry registry = new MappingMetaRegistry();

        Object dataMapper;
        while ((dataMapper = inputMapper.next()) != null) {
            row1 = row1Struct.class.cast(registry.find(row1Struct.class).newInstance(Record.class.cast(dataMapper)));

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

    @org.talend.sdk.component.api.processor.Processor(name = "outputDi", family = "DynamicColumnsTest")
    public static class OutputComponentDi extends RuntimeContextInjector implements Serializable {

        int counter;

        @ElementListener
        public void onElement(final Record record) {
            // can get connection, if not null, can use it directly instead of creating again
            assertNotNull(this.getRuntimeContext());
            assertNotNull(this.getRuntimeContext().getConnection());

            assertNotNull(record);
            assertNotNull(record.getString("id"));
            assertNotNull(record.getString("name"));
            assertTrue(record.getString("name").startsWith("record"));
            Collection columns = record.getSchema().getEntries().stream().map(e -> e.getName()).collect(toList());
            assertEquals("value" + counter, record.getString("string0"));
            assertEquals((counter % 2 == 0), record.getBoolean("bool0"));
            assertEquals(counter, record.getInt("int0"));
            assertEquals(counter, record.getLong("long0"));
            assertEquals(1.23f * counter, record.getFloat("float0"));
            assertEquals(new BigDecimal(12345.6789 * counter).setScale(7, RoundingMode.HALF_EVEN).doubleValue(),
                    record.getDouble("double0"));
            assertEquals(String.format("zorglub-is-still-alive-%05d", counter), new String(record.getBytes("bytes0")));
            assertEquals(IntStream.range(0, counter + 1).boxed().collect(toList()),
                    record.getArray(Integer.class, "array0"));
            assertTrue(ZonedDateTime.now().toEpochSecond() >= record.getDateTime("date0").toEpochSecond());

            counter++;
        }
    }

    @Data
    @DataStore("TestDataStore")
    public static class TestDataStore implements Serializable {

        @Option
        @Documentation("parameter 1")
        private String para1;

        @Option
        @Documentation("parameter 2")
        private int para2;
    }

    @Service
    public static class MyService implements Serializable {

        @CreateConnection
        public Object createConn(@Option("conn") final TestDataStore dataStore) {
            // create connection
            return dataStore.para1 + dataStore.para2;
        }

        @CloseConnection
        public CloseConnectionObject closeConn() {
            return new CloseConnectionObject() {

                public boolean close() throws ComponentException {
                    return "v1100".equals(this.getRuntimeContext().getConnection());
                }

            };
        }
    }

    @Emitter(name = "inputDi", family = "DynamicColumnsTest")
    public static class InputComponentDi extends RuntimeContextInjector implements Serializable {

        private final PrimitiveIterator.OfInt stream;

        public InputComponentDi(@Option("count") final int count) {
            this.stream = IntStream.range(0, count).iterator();
        }

        @Producer
        public Record next() {
            if (!stream.hasNext()) {
                return null;
            }
            final Integer i = stream.next();
            Record record = builderFactory
                    .newRecordBuilder()
                    .withString("id", String.valueOf(i))
                    .withString("name", "record" + i)
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
                            .withType(Type.ARRAY)
                            .withElementSchema(builderFactory.newSchemaBuilder(Type.INT).build())
                            .build(), IntStream.range(0, i + 1).boxed().collect(toList()))
                    .withDateTime("date0", ZonedDateTime.now())
                    .build();
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
