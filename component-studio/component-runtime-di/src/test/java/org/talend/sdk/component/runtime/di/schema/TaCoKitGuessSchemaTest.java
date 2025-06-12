/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
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
import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.exception.ComponentException.ErrorOrigin;
import org.talend.sdk.component.api.exception.DiscoverSchemaException;
import org.talend.sdk.component.api.exception.DiscoverSchemaException.HandleErrorWith;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.SchemaProperty;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.schema.DiscoverSchemaExtended;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

class TaCoKitGuessSchemaTest {

    private static final String EXPECTED_ERROR_MESSAGE = "Should not be invoked";

    private static RecordBuilderFactory factory;

    private final Pattern errorPattern =
            Pattern.compile("(\\{\"localizedMessage\":\".*?\"possibleHandleErrorWith\":\"\\w+\"})");

    private final Pattern schemaPattern = Pattern.compile("(\\[\\{.*\"talendType\".*\\}])");

    private final Pattern logPattern = Pattern.compile("^\\[\\s*(INFO|WARN|ERROR|DEBUG|TRACE)\\s*]");

    private final static java.io.PrintStream stdout = System.out;

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
            assertEquals(HandleErrorWith.EXCEPTION, exception.getPossibleHandleErrorWith());
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
                    .withProp(SchemaProperty.ORIGIN_TYPE, "VARCHAR")
                    .build();
            final Entry f2 = factory.newEntryBuilder()
                    .withName("f2")
                    .withType(Schema.Type.LONG)
                    .withDefaultValue(11l)
                    .withProp(SchemaProperty.ORIGIN_TYPE, "LONGINT")
                    .build();
            final Entry f3 = factory.newEntryBuilder()
                    .withName("f3")
                    .withType(Schema.Type.BOOLEAN)
                    .withProp(SchemaProperty.ORIGIN_TYPE, "BOOLEAN")
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
            redirectStdout(out);
            guessSchema.guessComponentSchema(schema, "out", false);
            guessSchema.close();
            restoreStdout();
            final String flattened = flatten(byteArrayOutputStream);
            final String expected =
                    "[{\"label\":\"f1\",\"nullable\":false,\"originalDbColumnName\":\"f1\",\"sourceType\":\"VARCHAR\",\"talendType\":\"id_String\"},{\"default\":\"11\",\"defaut\":\"11\",\"label\":\"f2\",\"nullable\":false,\"originalDbColumnName\":\"f2\",\"sourceType\":\"LONGINT\",\"talendType\":\"id_Long\"},{\"label\":\"f3\",\"nullable\":false,\"originalDbColumnName\":\"f3\",\"sourceType\":\"BOOLEAN\",\"talendType\":\"id_Boolean\"},{\"comment\":\"branch name\",\"label\":\"out\",\"nullable\":false,\"originalDbColumnName\":\"out\",\"talendType\":\"id_String\"}]";
            final Matcher schemaMatcher = schemaPattern.matcher(flattened);
            assertFalse(errorPattern.matcher(flattened).find());
            assertTrue(schemaMatcher.find());
            assertEquals(expected, schemaMatcher.group());
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
            config.put("configuration.failWith", "EXECUTE_LIFECYCLE");
            final TaCoKitGuessSchema guessSchema = new TaCoKitGuessSchema(out, config, "test-classes",
                    "TaCoKitGuessSchemaTest", "outputDi", null, "1");
            // guess schema action will fail and as start of job is true, it should use processor lifecycle
            redirectStdout(out);
            guessSchema.guessComponentSchema(schema, "out", true);
            guessSchema.close();
            restoreStdout();
            final String expected =
                    "[{\"label\":\"entry\",\"nullable\":true,\"originalDbColumnName\":\"entry\",\"talendType\":\"id_String\"}]";
            final String flattened = flatten(byteArrayOutputStream);
            final Matcher schemaMatcher = schemaPattern.matcher(flattened);
            assertFalse(errorPattern.matcher(flattened).find());
            assertTrue(schemaMatcher.find());
            assertEquals(expected, schemaMatcher.group());
        }
    }

    @Test
    void guessProcessorSchemaInStartWithMockExecution() throws Exception {
        final Schema sin = new RecordBuilderFactoryImpl("test-classes").newSchemaBuilder(Schema.Type.RECORD).build();
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(byteArrayOutputStream)) {
            Map<String, String> config = new HashMap<>();
            config.put("configuration.shouldActionFail", "true");
            final TaCoKitGuessSchema guessSchema = new TaCoKitGuessSchema(out, config, "test-classes",
                    "TaCoKitGuessSchemaTest", "outputDi", null, "1");
            try {
                redirectStdout(out);
                guessSchema.guessComponentSchema(sin, "out", true);
            } catch (Exception e) {
                guessSchema.close();
            }
            restoreStdout();
            // same transformations as in Studio
            final String flattened = flatten(byteArrayOutputStream);
            final Matcher errorMatcher = errorPattern.matcher(flattened);
            assertFalse(schemaPattern.matcher(flattened).find());
            assertTrue(errorMatcher.find());
            final DiscoverSchemaException de = jsonToException(errorMatcher.group());
            assertNotNull(de);
            assertEquals("Cannot execute action.", de.getMessage());
            assertEquals(HandleErrorWith.EXECUTE_MOCK_JOB, de.getPossibleHandleErrorWith());
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
            redirectStdout(out);
            guessSchema.guessComponentSchema(schema, "out", false);
            guessSchema.close();
            restoreStdout();

            final String flattened = flatten(byteArrayOutputStream);
            final String expected =
                    "[{\"label\":\"f1\",\"nullable\":false,\"originalDbColumnName\":\"f1\",\"talendType\":\"id_String\"},{\"default\":\"11\",\"defaut\":\"11\",\"label\":\"f2\",\"nullable\":false,\"originalDbColumnName\":\"f2\",\"talendType\":\"id_Long\"},{\"label\":\"f3\",\"nullable\":false,\"originalDbColumnName\":\"f3\",\"talendType\":\"id_Boolean\"},{\"comment\":\"hjk;ljkkj\",\"key\":true,\"label\":\"id\",\"length\":10,\"nullable\":false,\"originalDbColumnName\":\"id\",\"precision\":0,\"talendType\":\"id_Integer\"},{\"comment\":\"hljkjhlk\",\"default\":\"toto\",\"defaut\":\"toto\",\"label\":\"name\",\"length\":20,\"nullable\":true,\"originalDbColumnName\":\"name\",\"precision\":0,\"talendType\":\"id_String\"},{\"label\":\"flag\",\"length\":4,\"nullable\":true,\"originalDbColumnName\":\"flag\",\"precision\":0,\"talendType\":\"id_Character\"},{\"label\":\"female\",\"length\":1,\"nullable\":true,\"originalDbColumnName\":\"female\",\"precision\":0,\"talendType\":\"id_Boolean\"},{\"comment\":\"hhhh\",\"label\":\"num1\",\"length\":3,\"nullable\":true,\"originalDbColumnName\":\"num1\",\"precision\":0,\"talendType\":\"id_Byte\"},{\"label\":\"num2\",\"length\":5,\"nullable\":true,\"originalDbColumnName\":\"num2\",\"precision\":0,\"talendType\":\"id_Short\"},{\"label\":\"age\",\"length\":19,\"nullable\":true,\"originalDbColumnName\":\"age\",\"precision\":0,\"talendType\":\"id_Long\"},{\"label\":\"bonus\",\"length\":12,\"nullable\":true,\"originalDbColumnName\":\"bonus\",\"precision\":2,\"talendType\":\"id_Float\"},{\"label\":\"salary\",\"length\":22,\"nullable\":true,\"originalDbColumnName\":\"salary\",\"precision\":2,\"talendType\":\"id_Double\"},{\"label\":\"play\",\"length\":10,\"nullable\":true,\"originalDbColumnName\":\"play\",\"precision\":2,\"talendType\":\"id_String\"},{\"label\":\"startdate\",\"nullable\":true,\"originalDbColumnName\":\"startdate\",\"pattern\":\"\\\"yyyy-MM-dd\\\"\",\"talendType\":\"id_Date\"},{\"comment\":\"branch name\",\"label\":\"out\",\"nullable\":false,\"originalDbColumnName\":\"out\",\"talendType\":\"id_String\"}]";
            final Matcher schemaMatcher = schemaPattern.matcher(flattened);
            assertFalse(errorPattern.matcher(flattened).find());
            assertTrue(schemaMatcher.find());
            assertEquals(expected, schemaMatcher.group());
        }
    }

    @Test
    void testFromSchema() throws Exception {
        final RecordBuilderFactory factory = new RecordBuilderFactoryImpl("test-classes");
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(byteArrayOutputStream)) {
            Schema schema = factory.newSchemaBuilder(Schema.Type.RECORD)
                    .withEntry(factory.newEntryBuilder()
                            .withName("name")
                            .withType(Schema.Type.STRING)
                            .withProp(STUDIO_TYPE, StudioTypes.STRING)
                            .build())
                    .withEntry(factory.newEntryBuilder()
                            .withName("bit")
                            .withType(Schema.Type.BYTES)
                            .withProp(STUDIO_TYPE, StudioTypes.BYTE)
                            .build())
                    .withEntry(factory.newEntryBuilder()
                            .withName("dynamic")
                            .withType(Schema.Type.RECORD)
                            .withProp(STUDIO_TYPE, StudioTypes.DYNAMIC)
                            .withProp(PATTERN, "dd/MM/YYYY")
                            .withElementSchema(factory.newSchemaBuilder(Schema.Type.RECORD).build())
                            .build())
                    .withEntry(factory.newEntryBuilder()
                            .withName("document")
                            .withType(Schema.Type.RECORD)
                            .withProp(STUDIO_TYPE, StudioTypes.DOCUMENT)
                            .withElementSchema(factory.newSchemaBuilder(Schema.Type.RECORD).build())
                            .build())
                    .build();

            Map<String, String> config = new HashMap<>();
            config.put("configuration.skipAssertions", "true");
            final TaCoKitGuessSchema guessSchema = new TaCoKitGuessSchema(
                    out, config, "test-classes", "TaCoKitGuessSchemaTest",
                    "outputDi", null, "1");
            redirectStdout(out);
            guessSchema.guessComponentSchema(schema, "out", false);
            guessSchema.close();
            restoreStdout();

            final String flattened = flatten(byteArrayOutputStream);
            final String expected =
                    "[{\"label\":\"name\",\"nullable\":false,\"originalDbColumnName\":\"name\",\"talendType\":\"id_String\"},{\"label\":\"bit\",\"nullable\":false,\"originalDbColumnName\":\"bit\",\"talendType\":\"id_Byte\"},{\"label\":\"dynamic\",\"nullable\":true,\"originalDbColumnName\":\"dynamic\",\"pattern\":\"\\\"dd/MM/YYYY\\\"\",\"talendType\":\"id_Dynamic\"},{\"label\":\"document\",\"nullable\":true,\"originalDbColumnName\":\"document\",\"talendType\":\"id_Document\"},{\"comment\":\"branch name\",\"label\":\"out\",\"nullable\":false,\"originalDbColumnName\":\"out\",\"talendType\":\"id_String\"}]";
            assertTrue(byteArrayOutputStream.size() > 0);
            final Matcher schemaMatcher = schemaPattern.matcher(flattened);
            assertFalse(errorPattern.matcher(flattened).find());
            assertTrue(schemaMatcher.find());
            assertEquals(expected, schemaMatcher.group());
        }
    }

    @Test
    void serializeDiscoverSchemaException() throws Exception {
        final DiscoverSchemaException de = new DiscoverSchemaException(
                new ComponentException(ErrorOrigin.BACKEND, "Unknown error. Retry!", new NullPointerException()),
                HandleErrorWith.RETRY);
        final String msg = "{\"localizedMessage\":\"Unknown error. Retry!\"";
        final String hdl = "\"possibleHandleErrorWith\":\"RETRY\"}";
        final String json = exceptionToJsonString(de);
        System.out.println(json);
        assertTrue(json.contains(msg));
        assertTrue(json.startsWith(msg));
        assertTrue(json.contains(hdl));
        assertTrue(json.endsWith(hdl));
    }

    @Test
    void deserializeDiscoverSchemaException() throws Exception {
        final String flattened =
                "{\"message\":\"Not allowed to execute the HTTP call to retrieve the schema.\",\"stackTrace\":[],\"suppressed\":[],\"possibleHandleErrorWith\":\"EXCEPTION\"}";
        final String serialized =
                "{\"localizedMessage\":\"Unknown error. Retry!\",\"message\":\"Unknown error. Retry!\",\"stackTrace\":[],\"suppressed\":[],\"possibleHandleErrorWith\":\"RETRY\"}";
        DiscoverSchemaException e = jsonToException(flattened);
        assertNotEquals("EXECUTE_MOCK_JOB", e.getPossibleHandleErrorWith().name());
        assertEquals("EXCEPTION", e.getPossibleHandleErrorWith().name());
        assertEquals("Not allowed to execute the HTTP call to retrieve the schema.", e.getMessage());
        //
        e = jsonToException(serialized);
        assertNotEquals("EXCEPTION", e.getPossibleHandleErrorWith().name());
        assertEquals("RETRY", e.getPossibleHandleErrorWith().name());
        assertEquals("Unknown error. Retry!", e.getMessage());
    }

    private DiscoverSchemaException jsonToException(final String json) throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.fromJson(json, DiscoverSchemaException.class);
        }
    }

    private String exceptionToJsonString(final DiscoverSchemaException e) throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.toJson(e, DiscoverSchemaException.class);
        }
    }

    /**
     * Mimic Mock_job_for_Guess_schema.java :
     * Deactivate System.out for guess schema
     * This stream is used to transfer the schema between process
     */
    void redirectStdout(final PrintStream out) {
        System.setOut(out);
    }

    void restoreStdout() {
        System.setOut(stdout);
    }

    private String flatten(ByteArrayOutputStream out) {
        return out.toString().replaceAll("\n", "");
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

        @Option
        HandleErrorWith failWith = HandleErrorWith.EXECUTE_MOCK_JOB;

        @Option
        Boolean skipAssertions = false;
    }

    @Slf4j
    @Service
    public static class StudioProcessorService implements Serializable {

        @DiscoverSchemaExtended("outputDi")
        public Schema discoverProcessorSchema(final Schema incomingSchema,
                @Option("configuration") final ProcessorConfiguration conf, final String branch) {
            log.info("[discoverProcessorSchema] calling guess schema action.");
            log.info("[discoverProcessorSchema] trash values coming...");
            log.warn(
                    "[discoverProcessorSchema] trash values collected [WARN ] 14:38:00 com.couchbase.endpoint- [com.couchbase.endpoint][EndpointConnectionFailedEvent][905us] Connect attempt 5 failed because of UnknownHostException: <AnyIPwithoutcouchbaseservice> {\"circuitBreaker\":\"DISABLED\",\"coreId\":\"0x2620ac1e00000001\",\"remote\":\"<AnyIPwithoutcouchbaseservice>:11210\",\"type\":\"KV\"}\n"
                            +
                            "java.net.UnknownHostException: <AnyIPwithoutcouchbaseservice>\n" +
                            "        at java.net.InetAddress$CachedAddresses.get(InetAddress.java:801) ~[?:?]\n" +
                            "        at java.net.InetAddress.getAllByName0(InetAddress.java:1533) ~[?:?]\n" +
                            "        at java.net.InetAddress.getAllByName(InetAddress.java:1385) ~[?:?]\n" +
                            "        at java.net.InetAddress.getAllByName(InetAddress.java:1306) ~[?:?]\n" +
                            "        at java.net.InetAddress.getByName(InetAddress.java:1256) ~[?:?]\n" +
                            "        at java.security.AccessController.doPrivileged(AccessController.java:569) ~[?:?]\n"
                            +
                            "        at java.lang.Thread.run(Thread.java:840) [?:?]\n");
            if (conf.shouldActionFail) {
                log.error("[discoverProcessorSchema] Action will fail!");
                throw new DiscoverSchemaException("Cannot execute action.", conf.failWith);
            }
            if (!conf.skipAssertions) {
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
            }
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