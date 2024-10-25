/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.di.beam.components;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
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
import org.talend.sdk.component.api.configuration.constraint.Required;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.context.RuntimeContext;
import org.talend.sdk.component.api.context.RuntimeContextHolder;
import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.BeforeGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.connection.CloseConnection;
import org.talend.sdk.component.api.service.connection.CloseConnectionObject;
import org.talend.sdk.component.api.service.connection.Connection;
import org.talend.sdk.component.api.service.connection.CreateConnection;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.di.AutoChunkProcessor;
import org.talend.sdk.component.runtime.di.InputsHandler;
import org.talend.sdk.component.runtime.di.JobStateAware;
import org.talend.sdk.component.runtime.di.OutputsHandler;
import org.talend.sdk.component.runtime.di.studio.RuntimeContextInjector;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.chain.ChainedMapper;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.output.ProcessorImpl;
import org.talend.sdk.component.runtime.record.RecordConverters;
import org.talend.sdk.component.runtime.record.RecordImpl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

public class DIBulkAutoChunkTest {

    protected static RecordBuilderFactory builderFactory;

    // do the same thing with studio
    private static final Map<String, Object> globalMap = Collections.synchronizedMap(new HashMap<>());

    @BeforeAll
    static void forceManagerInit() {
        final ComponentManager manager = ComponentManager.instance();
        if (manager.find(Stream::of).count() == 0) {
            manager.addPlugin(new File("target" + File.separator + "test-classes").getAbsolutePath());
        }
    }

    @Test
    void fromRecordToRowStructToRecord() {
        final ComponentManager manager = ComponentManager.instance();
        final Collection<Object> sourceData = new ArrayList<>();
        final Collection<Object> processorData = new ArrayList<>();

        globalMap.put("key", "value");
        globalMap.put("outputDi_1_key", "value4Output");
        globalMap.put("inputDi_1_key", "value4Input");
        globalMap.put("connection_1_key", "value4Connection");
        globalMap.put("close_1_key", "value4Close");

        callConnectionComponent(manager);

        doDi(manager, sourceData, processorData,
                manager.findProcessor("DIBulkAutoChunkTest", "outputDi", 1, emptyMap()),
                manager.findMapper("DIBulkAutoChunkTest", "inputDi", 1, singletonMap("count", "1000")));
        assertEquals(1000, sourceData.size());
        assertEquals(1000, processorData.size());

        callCloseComponent(manager);
    }

    private void callCloseComponent(final ComponentManager manager) {
        String plugin = "test-classes";
        RuntimeContextInjector.injectService(manager, plugin, new RuntimeContextHolder("close_1", globalMap));

        manager
                .findPlugin(plugin)
                .get()
                .get(ContainerComponentRegistry.class)
                .getServices()
                .stream()
                .flatMap(c -> c.getActions().stream())
                .filter(actionMeta -> "close_connection".equals(actionMeta.getType()))
                .forEach(actionMeta -> {
                    Object result = actionMeta.getInvoker().apply(null);
                    CloseConnectionObject cco = (CloseConnectionObject) result;
                    Object conn = globalMap.get("conn_tS3Connection_1");

                    injectValue(cco, conn);

                    boolean r = cco.close();
                    assertEquals(true, r);
                });
    }

    private void callConnectionComponent(final ComponentManager manager) {
        final Map<String, String> runtimeParams = new HashMap<>();
        runtimeParams.put("conn.para1", "v1");
        runtimeParams.put("conn.para2", "200");

        String plugin = "test-classes";

        RuntimeContextInjector.injectService(manager, plugin, new RuntimeContextHolder("connection_1", globalMap));

        manager
                .findPlugin(plugin)
                .get()
                .get(ContainerComponentRegistry.class)
                .getServices()
                .stream()
                .flatMap(c -> c.getActions().stream())
                .filter(actionMeta -> "create_connection".equals(actionMeta.getType()))
                .forEach(actionMeta -> {
                    Object connnection = actionMeta.getInvoker().apply(runtimeParams);
//                    assertEquals("v1100connection_1value", connnection);

                    globalMap.put("conn_tS3Connection_1", connnection);
                });
    }

    private void doDi(final ComponentManager manager, final Collection<Object> sourceData,
            final Collection<Object> processorData, final Optional<Processor> proc, final Optional<Mapper> mapper) {
        try {
            final Processor processor = proc.orElseThrow(() -> new IllegalStateException("scanning failed"));

            RuntimeContextInjector.injectLifecycle(processor, new RuntimeContextHolder("outputDi_1", globalMap));

            try {
                Field field = processor.getClass().getSuperclass().getDeclaredField("delegate");
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                Object v = field.get(processor);
                Object conn = globalMap.get("conn_tS3Connection_1");

                injectValue(v, conn);

            } catch (Exception e) {
                System.out.println(e);
            }

            JobStateAware.init(processor, globalMap);
            final Jsonb jsonbProcessor = Jsonb.class
                    .cast(manager
                            .findPlugin(processor.plugin())
                            .get()
                            .get(ComponentManager.AllServices.class)
                            .getServices()
                            .get(Jsonb.class));

            final AutoChunkProcessor processorProcessor = new AutoChunkProcessor(-1, processor);

            processorProcessor.start();
            globalMap.put("processorProcessor", processorProcessor);

            final Map<Class<?>, Object> servicesMapper =
                    manager.findPlugin(proc.get().plugin()).get().get(ComponentManager.AllServices.class).getServices();

            final InputsHandler inputsHandlerProcessor = new InputsHandler(jsonbProcessor, servicesMapper);
            inputsHandlerProcessor.addConnection("FLOW", row1Struct.class);

            final OutputsHandler outputHandlerProcessor = new OutputsHandler(jsonbProcessor, servicesMapper);

            final InputFactory inputsProcessor = inputsHandlerProcessor.asInputFactory();
            final OutputFactory outputsProcessor = outputHandlerProcessor.asOutputFactory();

            final Mapper tempMapperMapper = mapper.orElseThrow(() -> new IllegalStateException("scanning failed"));
            JobStateAware.init(tempMapperMapper, globalMap);

            RuntimeContextInjector.injectLifecycle(tempMapperMapper, new RuntimeContextHolder("inputDi_1", globalMap));

            doRun(manager, sourceData, processorData, processorProcessor, inputsHandlerProcessor,
                    outputHandlerProcessor, inputsProcessor, outputsProcessor, tempMapperMapper);
        } finally {
            doClose(globalMap);
        }
    }

    private void injectValue(Object v, Object conn) {
        Class<?> current = v.getClass();
        while (current != null && current != Object.class) {
            Stream.of(current.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Connection.class)).forEach(f -> {
                if (!f.isAccessible()) {
                    f.setAccessible(true);
                }
                try {
                    f.set(v, conn);
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            });
            current = current.getSuperclass();
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

        final org.talend.sdk.component.runtime.input.Input inputMapper = mapperMapper.create();
        inputMapper.start();
        globalMap.put("inputMapper", inputMapper);

        final Map<Class<?>, Object> servicesMapper =
                manager.findPlugin(mapperMapper.plugin()).get().get(ComponentManager.AllServices.class).getServices();
        final Jsonb jsonbMapper = Jsonb.class.cast(servicesMapper.get(Jsonb.class));
        final JsonProvider jsonProvider = JsonProvider.class.cast(servicesMapper.get(JsonProvider.class));
        final JsonBuilderFactory jsonBuilderFactory =
                JsonBuilderFactory.class.cast(servicesMapper.get(JsonBuilderFactory.class));
        final RecordBuilderFactory recordBuilderMapper =
                RecordBuilderFactory.class.cast(servicesMapper.get(RecordBuilderFactory.class));
        builderFactory = recordBuilderMapper;
        final RecordConverters converters = new RecordConverters();
        final RecordConverters.MappingMetaRegistry registry = new RecordConverters.MappingMetaRegistry();

        Object dataMapper;
        while ((dataMapper = inputMapper.next()) != null) {
            row1 = row1Struct.class.cast(registry.find(row1Struct.class).newInstance(Record.class.cast(dataMapper)));

            sourceData.add(row1);

            inputsHandlerProcessor.reset();
            inputsHandlerProcessor.setInputValue("FLOW", row1);
            outputHandlerProcessor.reset();
            processorProcessor.onElement(name -> {
//                assertEquals(Branches.DEFAULT_BRANCH, name);

                final Object read = inputsProcessor.read(name);
                processorData.add(read);

                return read;
            }, outputsProcessor);
        }
    }

    private void doClose(final Map<String, Object> globalMap) {
        final Mapper mapperMapper = Mapper.class.cast(globalMap.remove("mapperMapper"));
        final org.talend.sdk.component.runtime.input.Input inputMapper =
                org.talend.sdk.component.runtime.input.Input.class.cast(globalMap.remove("inputMapper"));
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

    @org.talend.sdk.component.api.processor.Processor(name = "outputDi", family = "DIBulkAutoChunkTest")
    public static class OutputComponentDi implements Serializable {

        @RuntimeContext
        private transient RuntimeContextHolder context;

        int counter;

        int groupCounter;

        @Connection
        Object conn;

        @ElementListener
        public void onElement(final Record record) {
            // can get connection, if not null, can use it directly instead of creating again
            assertNotNull(conn);

            counter++;
//            if (counter % 100 == 0) {
//                System.err.println("--on element: " + counter);
//            }
        }

        @BeforeGroup
        public void beforeGroup() {
            if (counter % 100 == 0) {
                System.err.println("--before : " + counter);
            }
        }

        @AfterGroup
        public void afterGroup(@Output("reject") final OutputEmitter<Record> reject) {
            groupCounter++;
            if (groupCounter % 100 == 0) {
                System.err.println("--after group: " + groupCounter);
            }
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

        @Option
        @Required
        @Documentation("parameter 3")
        private int para3;
    }

    @Service
    public static class MyService implements Serializable {

        @RuntimeContext
        private transient RuntimeContextHolder context;

        @CreateConnection
        public Object createConn(@Option("conn") final TestDataStore dataStore) throws ComponentException {
            // create connection
            assertEquals("value4Connection", context.get("key"));
            return dataStore.getPara1() + dataStore.getPara2() + context.getConnectorId() + context.getGlobal("key");
        }

        @CloseConnection
        public CloseConnectionObject closeConn() {
            return new CloseConnectionObject() {

                public boolean close() throws ComponentException {
                    assertEquals("value4Close", context.get("key"));

                    return "v1100connection_1value".equals(this.getConnection())
                            && "value".equals(context.getGlobal("key"))
                            && "close_1".equals(context.getConnectorId());
                }

            };
        }
    }

    @Emitter(name = "inputDi", family = "DIBulkAutoChunkTest")
    public static class InputComponentDi implements Serializable {

        @RuntimeContext
        private transient RuntimeContextHolder context;

        private final PrimitiveIterator.OfInt stream;

        public InputComponentDi(@Option("count") final int count) {
            stream = IntStream.range(0, count).iterator();
        }

        @Connection
        Object conn;

        @Producer
        public Record next() {
            if (!stream.hasNext()) {
                return null;
            }

            final Integer i = stream.next();
            final Record record = builderFactory
                    .newRecordBuilder()
                    .withString("id", String.valueOf(i))
                    .withString("name", "record" + i)
                    .build();
            return record;
        }
    }

    @Getter
    @ToString
    public static class row1Struct implements routines.system.IPersistableRow {

        public String id;

        public String name;

        @Override
        public void writeData(final ObjectOutputStream objectOutputStream) {
            throw new UnsupportedOperationException("#writeData()");
        }

        @Override
        public void readData(final ObjectInputStream objectInputStream) {
            throw new UnsupportedOperationException("#readData()");
        }
    }

    //////////////////////////////////////////
    private static final OutputFactory NO_OUTPUT = name -> value -> {
        // no-op
    };

    //@Test
    void bulkGroup() {
        Bufferized.RECORDS = null;
        final org.talend.sdk.component.runtime.output.Processor processor =
                new ProcessorImpl("Root", "Test", "Plugin", emptyMap(), new Bufferized());
        final AutoChunkProcessor chunkProcessor = new AutoChunkProcessor(-1, processor);
        chunkProcessor.start();
        for (int i = 0; i < 5; i++) {
            final Collection<Record> data = IntStream
                    .rangeClosed(1, 9)
                    .mapToObj(idx -> new RecordImpl.BuilderImpl().withInt("value", idx).build())
                    .collect(toList());
            // processor.beforeGroup();

            data.forEach(it -> processor.onNext(n -> it, null));
            assertNull(Bufferized.RECORDS);
            // chunkProcessor.afterGroup(null);
            assertEquals(data, Bufferized.RECORDS);
            Bufferized.RECORDS = null;
        }
        processor.stop();
    }

    public static class SampleProcessor implements Serializable {

        final Collection<String> stack = new ArrayList<>();

        @ElementListener
        public void elementListener(@Input final Sample sample, @Output("reject") final OutputEmitter<Sample> reject) {
            stack.add("next{" + sample.data + "}");
            System.err.println("---" + sample.data);
        }

        @AfterGroup
        public void afterGroup(@Output("reject") final OutputEmitter<Sample> reject) {

        }
    }

    public static class Bufferized implements Serializable {

        private static Collection<Record> RECORDS;

        @AfterGroup
        public void onCommit(final Collection<Record> records) {
            RECORDS = records;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Sample implements Serializable {

        private int data;
    }
}
