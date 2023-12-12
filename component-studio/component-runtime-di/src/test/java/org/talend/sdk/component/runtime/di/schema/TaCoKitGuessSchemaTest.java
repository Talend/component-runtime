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
package org.talend.sdk.component.runtime.di.schema;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.api.exception.DiscoverSchemaException.HandleErrorWith.EXCEPTION;
import static org.talend.sdk.component.api.exception.DiscoverSchemaException.HandleErrorWith.EXECUTE_MOCK_JOB;
import static org.talend.sdk.component.api.record.SchemaProperty.IS_KEY;
import static org.talend.sdk.component.api.record.SchemaProperty.PATTERN;
import static org.talend.sdk.component.api.record.SchemaProperty.SCALE;
import static org.talend.sdk.component.api.record.SchemaProperty.SIZE;
import static org.talend.sdk.component.api.record.SchemaProperty.STUDIO_TYPE;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

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
import org.talend.sdk.component.api.exception.DiscoverSchemaException;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.schema.DiscoverSchemaExtended;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

import lombok.Data;

class TaCoKitGuessSchemaTest {

    private static final String EXPECTED_ERROR_MESSAGE = "Should not be invoked";

    private static RecordBuilderFactory factory;

    @BeforeAll
    static void forceManagerInit() {
        final ComponentManager manager = ComponentManager.instance();
        if (!manager.find(Stream::of).findAny().isPresent()) {
            manager.addPlugin(new File("target/test-classes").getAbsolutePath());
        }
        factory = manager.getRecordBuilderFactoryProvider().apply("default");
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
                    "TaCoKitGuessSchemaTest",
                    "inputDi",
                    null,
                    version) {

                @Override
                public boolean guessSchemaThroughAction(final Schema schema) {
                    // stub to invoke: guessInputComponentSchemaThroughResult
                    return false;
                }

            };
            guessSchema.guessInputComponentSchema(null);
            guessSchema.close();

            assertTrue(byteArrayOutputStream.size() > 0);

            final String content = byteArrayOutputStream.toString();
            assertTrue(content.contains("\"length\":10"));
            assertTrue(content.contains("\"precision\":2"));
            assertTrue(!content.contains("\"length\":0"));
            assertTrue(!content.contains("\"precision\":0"));
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
                    "TaCoKitGuessSchemaTest",
                    "inputDi",
                    null,
                    version) {

                @Override
                public boolean guessSchemaThroughAction(final Schema schema) {
                    // stub to invoke: guessInputComponentSchemaThroughResult
                    return false;
                }

            };
            final DiscoverSchemaException exception =
                    Assertions.assertThrows(DiscoverSchemaException.class,
                            () -> guessSchema.guessInputComponentSchema(null));
            assertEquals(EXPECTED_ERROR_MESSAGE, exception.getMessage());
            assertEquals(EXCEPTION, exception.getPossibleHandleErrorWith());
        }
    }

    final Entry f1 = factory.newEntryBuilder()
            .withName("f1")
            .withType(Schema.Type.STRING)
            .build();

    final Entry f2 = factory.newEntryBuilder()
            .withName("f2")
            .withType(Schema.Type.LONG)
            .withDefaultValue(11l)
            .build();

    final Entry f3 = factory.newEntryBuilder()
            .withName("f3")
            .withType(Schema.Type.BOOLEAN)
            .build();

    @Test
    void guessProcessorSchema() throws Exception {
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(byteArrayOutputStream)) {
            final Entry f1 = factory.newEntryBuilder()
                    .withName("f1")
                    .withType(Schema.Type.STRING)
                    .build();
            final Entry f2 = factory.newEntryBuilder()
                    .withName("f2")
                    .withType(Schema.Type.LONG)
                    .withDefaultValue(11l)
                    .build();
            final Entry f3 = factory.newEntryBuilder()
                    .withName("f3")
                    .withType(Schema.Type.BOOLEAN)
                    .build();
            final Schema schema = factory.newSchemaBuilder(Schema.Type.RECORD)
                    .withProp("aprop", "a property!")
                    .withEntry(f1)
                    .withEntry(f2)
                    .withEntry(f3)
                    .build();
            Map<String, String> config = new HashMap<>();
            config.put("configuration.param1", "parameter one");
            config.put("configuration.param2", "parameter two");
            final TaCoKitGuessSchema guessSchema =
                    new TaCoKitGuessSchema(out, config, "test-classes", "TaCoKitGuessSchemaTest", "outputDi", null,
                            "1");
            guessSchema.guessComponentSchema(schema, "out", false);
            guessSchema.close();
            final Pattern pattern = Pattern.compile("^\\[\\s*(INFO|WARN|ERROR|DEBUG|TRACE)\\s*]");
            final String lines = Arrays.stream(byteArrayOutputStream.toString().split("\n"))
                    .filter(l -> !pattern.matcher(l).find()) // filter out logs
                    .filter(l -> l.startsWith("[") || l.startsWith("{")) // ignore line with non json data
                    .collect(joining("\n"));
            final String expected =
                    "[{\"label\":\"f1\",\"nullable\":false,\"originalDbColumnName\":\"f1\",\"talendType\":\"id_String\"},{\"default\":\"11\",\"defaut\":\"11\",\"label\":\"f2\",\"nullable\":false,\"originalDbColumnName\":\"f2\",\"talendType\":\"id_Long\"},{\"label\":\"f3\",\"nullable\":false,\"originalDbColumnName\":\"f3\",\"talendType\":\"id_Boolean\"},{\"comment\":\"branch name\",\"label\":\"out\",\"nullable\":false,\"originalDbColumnName\":\"out\",\"talendType\":\"id_String\"}]";
            assertEquals(expected, lines);
            assertTrue(byteArrayOutputStream.size() > 0);
        }
    }

    @Test
    void guessProcessorSchemaRecordBuilderFactoryImpl() throws Exception {
        guessProcessorSchemaWithRecordBuilderFactory(null);
    }

    @Test
    void guessProcessorSchemaAvroRecordBuilderFactory() throws Exception {
        guessProcessorSchemaWithRecordBuilderFactory(factory);
    }

    @Test
    void guessProcessorSchemaInStartOfJob() throws Exception {
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(byteArrayOutputStream)) {
            final Schema schema = factory.newSchemaBuilder(Schema.Type.RECORD).build();
            Map<String, String> config = new HashMap<>();
            config.put("configuration.shouldActionFail", "true");
            final TaCoKitGuessSchema guessSchema =
                    new TaCoKitGuessSchema(out, config, "test-classes", "TaCoKitGuessSchemaTest",
                            "outputDi", null, "1");
            guessSchema.guessComponentSchema(schema, "out", true);
            guessSchema.close();
            final String expected =
                    "[{\"label\":\"entry\",\"nullable\":true,\"originalDbColumnName\":\"entry\",\"talendType\":\"id_String\"}]";
            assertTrue(byteArrayOutputStream.size() > 0);
            assertTrue(byteArrayOutputStream.toString().contains(expected));
        }
    }

    private void guessProcessorSchemaWithRecordBuilderFactory(RecordBuilderFactory facto) throws Exception {
        final RecordBuilderFactory factory = facto == null ? new RecordBuilderFactoryImpl("test") : facto;
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(byteArrayOutputStream)) {
            Schema schema = factory.newSchemaBuilder(Schema.Type.RECORD)
                    .withProp("aprop", "a property!")
                    .withEntry(f1)
                    .withEntry(f2)
                    .withEntry(f3)
                    .withEntry(factory.newEntryBuilder()
                            .withName("id")
                            .withType(Schema.Type.INT)
                            .withNullable(false)
                            .withRawName("id")
                            .withComment("hjk;ljkkj")
                            .withProp(STUDIO_TYPE, "id_Integer")
                            .withProp(IS_KEY, "true")
                            .withProp(SCALE, "0")
                            .withProp(SIZE, "10")
                            .build())
                    .withEntry(factory.newEntryBuilder()
                            .withName("name")
                            .withType(Schema.Type.STRING)
                            .withNullable(true)
                            .withRawName("name")
                            .withComment("hljkjhlk")
                            .withDefaultValue("toto")
                            .withProp(STUDIO_TYPE, "id_String")
                            .withProp(SCALE, "0")
                            .withProp(SIZE, "20")
                            .build())
                    .withEntry(factory.newEntryBuilder()
                            .withName("flag")
                            .withType(Schema.Type.STRING)
                            .withNullable(true)
                            .withRawName("flag")
                            .withProp(STUDIO_TYPE, "id_Character")
                            .withProp(SCALE, "0")
                            .withProp(SIZE, "4")
                            .build())
                    .withEntry(factory.newEntryBuilder()
                            .withName("female")
                            .withType(Schema.Type.BOOLEAN)
                            .withNullable(true)
                            .withRawName("female")
                            .withProp(STUDIO_TYPE, "id_Boolean")
                            .withProp(SCALE, "0")
                            .withProp(SIZE, "1")
                            .build())
                    .withEntry(factory.newEntryBuilder()
                            .withName("num1")
                            .withType(Schema.Type.BYTES)
                            .withNullable(true)
                            .withRawName("num1")
                            .withComment("hhhh")
                            .withProp(STUDIO_TYPE, "id_Byte")
                            .withProp(SCALE, "0")
                            .withProp(SIZE, "3")
                            .build())
                    .withEntry(factory.newEntryBuilder()
                            .withName("num2")
                            .withType(Schema.Type.INT)
                            .withNullable(true)
                            .withRawName("num2")
                            .withProp(STUDIO_TYPE, "id_Short")
                            .withProp(SCALE, "0")
                            .withProp(SIZE, "5")
                            .build())
                    .withEntry(factory.newEntryBuilder()
                            .withName("age")
                            .withType(Schema.Type.LONG)
                            .withNullable(true)
                            .withRawName("age")
                            .withProp(STUDIO_TYPE, "id_Long")
                            .withProp(SCALE, "0")
                            .withProp(SIZE, "19")
                            .build())
                    .withEntry(factory.newEntryBuilder()
                            .withName("bonus")
                            .withType(Schema.Type.FLOAT)
                            .withNullable(true)
                            .withRawName("bonus")
                            .withProp(STUDIO_TYPE, "id_Float")
                            .withProp(SCALE, "2")
                            .withProp(SIZE, "12")
                            .build())
                    .withEntry(factory.newEntryBuilder()
                            .withName("salary")
                            .withType(Schema.Type.DOUBLE)
                            .withNullable(true)
                            .withRawName("salary")
                            .withProp(STUDIO_TYPE, "id_Double")
                            .withProp(SCALE, "2")
                            .withProp(SIZE, "22")
                            .build())
                    .withEntry(factory.newEntryBuilder()
                            .withName("play")
                            .withType(Schema.Type.STRING)
                            .withNullable(true)
                            .withRawName("play")
                            .withProp(STUDIO_TYPE, "id_String")
                            .withProp(SCALE, "2")
                            .withProp(SIZE, "10")
                            .build())
                    .withEntry(factory.newEntryBuilder()
                            .withName("startdate")
                            .withType(Schema.Type.DATETIME)
                            .withNullable(true)
                            .withRawName("startdate")
                            .withProp(STUDIO_TYPE, "id_Date")
                            .withProp(PATTERN, "yyyy-MM-dd")
                            .build())
                    .build();

            Map<String, String> config = new HashMap<>();
            config.put("configuration.param1", "parameter one");
            config.put("configuration.param2", "parameter two");
            final TaCoKitGuessSchema guessSchema = new TaCoKitGuessSchema(
                    out, config, "test-classes", "TaCoKitGuessSchemaTest",
                    "outputDi", null, "1");
            guessSchema.guessComponentSchema(schema, "out", false);
            guessSchema.close();
            final Pattern pattern = Pattern.compile("^\\[\\s*(INFO|WARN|ERROR|DEBUG|TRACE)\\s*]");
            final String lines = Arrays.stream(byteArrayOutputStream.toString().split("\n"))
                    .filter(l -> !pattern.matcher(l).find()) // filter out logs
                    .filter(l -> l.startsWith("[") || l.startsWith("{")) // ignore line with non json data
                    .collect(joining("\n"));
            final String expected =
                    "[{\"label\":\"f1\",\"nullable\":false,\"originalDbColumnName\":\"f1\",\"talendType\":\"id_String\"},{\"default\":\"11\",\"defaut\":\"11\",\"label\":\"f2\",\"nullable\":false,\"originalDbColumnName\":\"f2\",\"talendType\":\"id_Long\"},{\"label\":\"f3\",\"nullable\":false,\"originalDbColumnName\":\"f3\",\"talendType\":\"id_Boolean\"},{\"comment\":\"hjk;ljkkj\",\"key\":true,\"label\":\"id\",\"length\":10,\"nullable\":false,\"originalDbColumnName\":\"id\",\"precision\":0,\"talendType\":\"id_Integer\"},{\"comment\":\"hljkjhlk\",\"default\":\"toto\",\"defaut\":\"toto\",\"label\":\"name\",\"length\":20,\"nullable\":true,\"originalDbColumnName\":\"name\",\"precision\":0,\"talendType\":\"id_String\"},{\"label\":\"flag\",\"length\":4,\"nullable\":true,\"originalDbColumnName\":\"flag\",\"precision\":0,\"talendType\":\"id_Character\"},{\"label\":\"female\",\"length\":1,\"nullable\":true,\"originalDbColumnName\":\"female\",\"precision\":0,\"talendType\":\"id_Boolean\"},{\"comment\":\"hhhh\",\"label\":\"num1\",\"length\":3,\"nullable\":true,\"originalDbColumnName\":\"num1\",\"precision\":0,\"talendType\":\"id_byte[]\"},{\"label\":\"num2\",\"length\":5,\"nullable\":true,\"originalDbColumnName\":\"num2\",\"precision\":0,\"talendType\":\"id_Short\"},{\"label\":\"age\",\"length\":19,\"nullable\":true,\"originalDbColumnName\":\"age\",\"precision\":0,\"talendType\":\"id_Long\"},{\"label\":\"bonus\",\"length\":12,\"nullable\":true,\"originalDbColumnName\":\"bonus\",\"precision\":2,\"talendType\":\"id_Float\"},{\"label\":\"salary\",\"length\":22,\"nullable\":true,\"originalDbColumnName\":\"salary\",\"precision\":2,\"talendType\":\"id_Double\"},{\"label\":\"play\",\"length\":10,\"nullable\":true,\"originalDbColumnName\":\"play\",\"precision\":2,\"talendType\":\"id_String\"},{\"label\":\"startdate\",\"nullable\":true,\"originalDbColumnName\":\"startdate\",\"pattern\":\"\\\"yyyy-MM-dd\\\"\",\"talendType\":\"id_Date\"},{\"comment\":\"branch name\",\"label\":\"out\",\"nullable\":false,\"originalDbColumnName\":\"out\",\"talendType\":\"id_String\"}]";
            assertEquals(expected, lines);
            assertTrue(byteArrayOutputStream.size() > 0);
        }
    }

    @Test
    void serializeDiscoverSchemaException() throws Exception {
        final String serialized =
                "{\"localizedMessage\":\"Unknown error. Retry!\",\"message\":\"Unknown error. Retry!\",\"stackTrace\":[],\"suppressed\":[],\"possibleHandleErrorWith\":\"RETRY\"}";
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            DiscoverSchemaException e =
                    new DiscoverSchemaException("Unknown error. Retry!", DiscoverSchemaException.HandleErrorWith.RETRY);
            String json = jsonb.toJson(e);
            assertTrue(json.contains("\"message\":\"Unknown error. Retry!\""));
            assertTrue(json.contains("\"possibleHandleErrorWith\":\"RETRY\""));
        }
    }

    @Test
    void deserializeDiscoverSchemaException() throws Exception {
        final String flattened =
                "{\"message\":\"Not allowed to execute the HTTP call to retrieve the schema.\",\"stackTrace\":[],\"suppressed\":[],\"possibleHandleErrorWith\":\"EXCEPTION\"}";
        final String serialized =
                "{\"localizedMessage\":\"Unknown error. Retry!\",\"message\":\"Unknown error. Retry!\",\"stackTrace\":[],\"suppressed\":[],\"possibleHandleErrorWith\":\"RETRY\"}";
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            DiscoverSchemaException e = jsonb.fromJson(flattened, DiscoverSchemaException.class);
            assertFalse("EXECUTE_MOCK_JOB".equals(e.getPossibleHandleErrorWith().name()));
            assertEquals("EXCEPTION", e.getPossibleHandleErrorWith().name());
            assertEquals("Not allowed to execute the HTTP call to retrieve the schema.", e.getMessage());
            //
            e = jsonb.fromJson(serialized, DiscoverSchemaException.class);
            assertFalse("EXCEPTION".equals(e.getPossibleHandleErrorWith()));
            assertEquals("RETRY", e.getPossibleHandleErrorWith().name());
            assertEquals("Unknown error. Retry!", e.getMessage());
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
    @Emitter(name = "inputDi", family = "TaCoKitGuessSchemaTest")
    public static class InputComponentDi implements Serializable {

        private RecordBuilderFactory factory = new RecordBuilderFactoryImpl("test-classes");

        // TODO : in future, will always use action result instead of mock job result, so will remove this
        @Producer
        public Record next() {
            final Schema.Entry entry1 = factory.newEntryBuilder()
                    .withName("c1")
                    .withRawName("the c1")
                    .withType(Schema.Type.STRING)
                    .withNullable(true)
                    .withProp(IS_KEY, "true")
                    .withProp(SIZE, "10")
                    .build();

            final Schema.Entry entry2 = factory.newEntryBuilder()
                    .withName("c2")
                    .withRawName("the c2")
                    .withType(Schema.Type.DECIMAL)
                    .withNullable(true)
                    .withProp(IS_KEY, "false")
                    .withProp(SIZE, "10")
                    .withProp(SCALE, "2")
                    .build();

            final Schema.Entry entry3 = factory.newEntryBuilder()
                    .withName("c3")
                    .withRawName("the c3")
                    .withType(Schema.Type.BOOLEAN)
                    .withNullable(true)
                    .withProp(IS_KEY, "false")
                    .build();

            return factory.newRecordBuilder()
                    .with(entry1, "testvalue")
                    .with(entry2, new BigDecimal("123.123"))
                    .with(entry3, true)
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
    @Processor(name = "outputDi", family = "TaCoKitGuessSchemaTest")
    public static class StudioProcessor implements Serializable {

        @Option
        private ProcessorConfiguration configuration;

        private RecordBuilderFactory factory = new RecordBuilderFactoryImpl("test-classes");

        @ElementListener
        public Record next(Record in) {
            return factory.newRecordBuilder().withString("entry", "test").build();
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

        @Option
        Boolean shouldActionFail = false;
    }

    @Service
    public static class StudioProcessorService implements Serializable {

        @DiscoverSchemaExtended("outputDi")
        public Schema discoverProcessorSchema(final Schema incomingSchema,
                @Option("configuration") final ProcessorConfiguration conf, final String branch) {
            if (conf.shouldActionFail) {
                throw new DiscoverSchemaException("Cannot execute action.", EXECUTE_MOCK_JOB);
            }
            assertEquals("out", branch);
            assertEquals("parameter one", conf.param1);
            assertEquals("parameter two", conf.param2);
            assertNull(conf.param3);
            assertNotNull(incomingSchema);
            assertEquals(Schema.Type.RECORD, incomingSchema.getType());
            assertEquals("a property!", incomingSchema.getProp("aprop"));
            assertNotNull(incomingSchema.getEntry("f1"));
            assertEquals(Schema.Type.STRING, incomingSchema.getEntry("f1").getType());
            assertNotNull(incomingSchema.getEntry("f2"));
            assertEquals(Schema.Type.LONG, incomingSchema.getEntry("f2").getType());
            assertEquals(11L, (Long) incomingSchema.getEntry("f2").getDefaultValue());
            assertNotNull(incomingSchema.getEntry("f3"));
            assertEquals(Schema.Type.BOOLEAN, incomingSchema.getEntry("f3").getType());

            return factory.newSchemaBuilder(incomingSchema)
                    .withEntry(factory.newEntryBuilder()
                            .withName(branch)
                            .withType(Schema.Type.STRING)
                            .withComment("branch name")
                            .withProp("branch", branch)
                            .build())
                    .build();
        }
    }

}