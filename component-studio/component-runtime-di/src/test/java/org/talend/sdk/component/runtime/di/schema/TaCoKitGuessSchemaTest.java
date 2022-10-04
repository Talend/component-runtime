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
package org.talend.sdk.component.runtime.di.schema;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;

import org.apache.beam.sdk.options.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.schema.DiscoverProcessorSchema;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class TaCoKitGuessSchemaTest {

    private static final String EXPECTED_ERROR_MESSAGE = "Should not be invoked";

    @BeforeAll
    static void forceManagerInit() {
        final ComponentManager manager = ComponentManager.instance();
        if (!manager.find(Stream::of).findAny().isPresent()) {
            manager.addPlugin(new File("target/test-classes").getAbsolutePath());
        }
    }

    @Description("What are we testing here? " +
            "During guess schema, if we don't define action. " +
            "The migration handler can be invoked. " +
            "It should receive the correct version of component " +
            "otherwise we can execute migration each time when we want to guess schema.")
    @MethodSource("guessSchemaUseVersionSource")
    @ParameterizedTest
    void guessSchemaUseVersion(String version) throws Exception {

        // version is the same, higher than defined in the component or null
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(byteArrayOutputStream)) {
            final TaCoKitGuessSchema guessSchema = new TaCoKitGuessSchema(
                    out,
                    Collections.singletonMap("para1", "bla"),
                    "test-classes",
                    "TaCoKitGuessSchema",
                    "inputDi",
                    null,
                    version) {

                @Override
                public boolean guessSchemaThroughAction() {
                    // stub to invoke: guessInputComponentSchemaThroughResult
                    return false;
                }

            };
            guessSchema.guessInputComponentSchema();
            guessSchema.close();

            Assertions.assertTrue(byteArrayOutputStream.size() > 0);
        }
    }

    static Stream<String> guessSchemaUseVersionSource() {
        return Stream.of("2", "100500", null);
    }

    @ValueSource(strings = { "1", "-1" })
    @ParameterizedTest
    void guessSchemaUseVersionNOK(final String version) throws Exception {
        // version is lower than defined in the component

        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(byteArrayOutputStream)) {
            final TaCoKitGuessSchema guessSchema = new TaCoKitGuessSchema(
                    out,
                    Collections.singletonMap("para1", "bla"),
                    "test-classes",
                    "TaCoKitGuessSchema",
                    "inputDi",
                    null,
                    version) {

                @Override
                public boolean guessSchemaThroughAction() {
                    // stub to invoke: guessInputComponentSchemaThroughResult
                    return false;
                }

            };
            final IllegalStateException exception =
                    Assertions.assertThrows(IllegalStateException.class, guessSchema::guessInputComponentSchema);
            Assertions.assertEquals(EXPECTED_ERROR_MESSAGE, exception.getMessage());
        }
    }

    @Test
    void guessProcessorSchema() throws Exception {
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             PrintStream out = new PrintStream(byteArrayOutputStream)) {
            RecordBuilderFactory factory = ComponentManager.instance().getRecordBuilderFactoryProvider().apply("default");
            Schema.Entry f1 = factory.newEntryBuilder().withName("f1").withType(Schema.Type.STRING).build();
            Schema.Entry f2 = factory.newEntryBuilder().withName("f2").withType(Schema.Type.LONG).build();
            Schema.Entry f3 = factory.newEntryBuilder().withName("f3").withType(Schema.Type.BOOLEAN).build();
            Schema schema = factory.newSchemaBuilder(Schema.Type.RECORD)
                    .withEntry(f1)
                    .withEntry(f2)
                    .withEntry(f3)
                    .build();
            Jsonb jsonb = ComponentManager.instance().getJsonbProvider().create().build();
            Map<String, String> config = new HashMap<>();
//            config.put("incomingSchema.schema", jsonb.toJson(schema));
//            config.put("conf.param1", "parameter one");
//            config.put("conf.param2", "parameter two");
//            config.put("out.out", "main");
            //   config.put("incomingSchema", jsonb.toJson(schema));
            //config.put("branch", "main");
            config.put("configuration.param1", "parameter one");
            config.put("configuration.param2", "parameter two");
            final TaCoKitGuessSchema guessSchema = new TaCoKitGuessSchema(
                    out, config, "test-classes", "TaCoKitGuessSchema",
                    "outputDi", null, "1");
            guessSchema.guessProcessorComponentSchema(schema, "out");
            guessSchema.close();
            log.warn("[guessProcessorSchema] {}", out);
            Assertions.assertTrue(byteArrayOutputStream.size() > 0);
        }
    }

    @Data
    @DataStore("TestDataStore")
    public static class TestDataStore implements Serializable {

        @Option
        @Documentation("par 1")
        private String para1;

    }

    @Version(value = 2, migrationHandler = TestMigration.class)
    @Emitter(name = "inputDi", family = "TaCoKitGuessSchema")
    public static class InputComponentDi implements Serializable {

        private RecordBuilderFactory factory = new RecordBuilderFactoryImpl("test-classes");

        @Producer
        public Record next() {
            return factory.newRecordBuilder()
                    .withString("test", "test")
                    .build();
        }
    }

    public static class TestMigration implements MigrationHandler {

        @Override
        public Map<String, String> migrate(final int incomingVersion, final Map<String, String> incomingData) {
            if (incomingVersion < 2) {
                throw new IllegalStateException(EXPECTED_ERROR_MESSAGE);
            } else {
                return incomingData;
            }
        }
    }

    @Data
    @Processor(family = "TaCoKitGuessSchema", name = "outputDi")
    public static class StudioProcessor implements Serializable {

        @Option
        private ProcessorConfiguration configuration;

        @ElementListener
        public Object next(Record in, Record out) {
            return null;
        }
    }

    @Data
    public static class ProcessorConfiguration implements Serializable {
        @Option
        private String param1;
        @Option
        private String param2;
        @Option
        private String param3;
    }

    @Service
    public static class StudioProcessorService implements Serializable {
        @DiscoverProcessorSchema
        public Schema discoverProcessorSchema(
                final Schema incomingSchema,
                @Option("configuration") final ProcessorConfiguration conf,
                final String branch) {

            log.warn("[discoverProcessorSchema] configuration:{} branch: {};", conf, branch);
            incomingSchema.getAllEntries().forEach(s -> log.warn("[discoverProcessorSchema] {}", s));

            RecordBuilderFactory factory = ComponentManager.instance().getRecordBuilderFactoryProvider().apply("default");
            Schema.Entry f1 = factory.newEntryBuilder().withName("f1").withType(Schema.Type.STRING).build();
            Schema.Entry f2 = factory.newEntryBuilder().withName("f2").withType(Schema.Type.STRING).build();
            Schema.Entry f3 = factory.newEntryBuilder().withName("f3").withType(Schema.Type.STRING).build();
            return factory.newSchemaBuilder(Schema.Type.RECORD)
                    .withEntry(f1)
                    .withEntry(f2)
                    .withEntry(f3)
                    .build();
        }
    }

}