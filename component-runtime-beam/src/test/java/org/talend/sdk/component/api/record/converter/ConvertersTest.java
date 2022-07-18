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
package org.talend.sdk.component.api.record.converter;

import java.util.Arrays;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider;

/**
 * Test schema to json & json to schema for SchemaAvro implementation
 */
public class ConvertersTest {

    private static RecordBuilderFactory factory;

    private static String factoryProp;

    @BeforeAll
    static void init() {
        AvroRecordBuilderFactoryProvider provider = new AvroRecordBuilderFactoryProvider();
        factoryProp = System.getProperty("talend.component.beam.record.factory.impl");
        System.setProperty("talend.component.beam.record.factory.impl", "avro");
        factory = provider.apply("test");
    }

    @AfterAll
    static void end() {
        if (factoryProp == null) {
            System.clearProperty("talend.component.beam.record.factory.impl");
        } else {
            System.setProperty("talend.component.beam.record.factory.impl", factoryProp);
        }
    }

    private final SchemaToJson toJson = new SchemaToJson();

    private final JsonToSchema toSchema = new JsonToSchema(factory);

    @ParameterizedTest
    @MethodSource("inputSchemas")
    void schemaJsonTest(final Schema schema) {
        final JsonObject jsonObject = toJson.toJson(schema);
        final Schema result = toSchema.toSchema(jsonObject);
        final JsonObject jsonCopy = toJson.toJson(result);

        Assertions.assertEquals(jsonObject, jsonCopy);
    }

    static Stream<Arguments> inputSchemas() {
        Schema.Entry f1 = field("f1", Schema.Type.STRING).build();
        Schema.Entry f2 = field("f2", Schema.Type.LONG)
                .withDefaultValue(23l)
                .withComment("Comment Long Type")
                .build();

        Schema.Entry f3 = field("f3", Schema.Type.ARRAY)
                .withComment("Comment Long Type")
                .withElementSchema(factory.newSchemaBuilder(Schema.Type.STRING).build())
                .withRawName("TheRawName")
                .withMetadata(true)
                .build();

        Schema innerSchema = newSchema(f2, f3);
        Schema.Entry f4 = field("f4", Schema.Type.RECORD)
                .withElementSchema(innerSchema)
                .withNullable(true)
                .build();

        Schema.Entry f5 = field("f5", Schema.Type.ARRAY)
                .withElementSchema(innerSchema)
                .withNullable(true)
                .build();
        Schema arrayOfString = factory.newSchemaBuilder(Schema.Type.ARRAY)
                .withElementSchema(factory.newSchemaBuilder(Schema.Type.STRING).build())
                .build();
        Schema.Entry f6 = field("f6", Schema.Type.ARRAY)
                .withElementSchema(arrayOfString)
                .withNullable(true)
                .build();

        Schema.Entry f7 = field("f7", Schema.Type.STRING)
                .withDefaultValue("defaultString")
                .build();

        Schema.Entry f8 = field("f8", Schema.Type.DATETIME)
                .build();

        return Stream.of(
                Arguments.of(newSchema(f1, f2)),
                Arguments.of(newSchema(f3)),
                Arguments.of(newSchema(f4)),
                Arguments.of(newSchema(f3, f1, f4)),
                Arguments.of(newSchema(f1, f2, f5)),
                Arguments.of(newSchema(f8, f6, f7)));
    }

    private static Schema.Builder newBuilder() {
        return factory.newSchemaBuilder(Schema.Type.RECORD);
    }

    private static Schema newSchema(Schema.Entry... fields) {
        Schema.Builder builder = newBuilder();
        Arrays.stream(fields).forEach(builder::withEntry);
        builder.withProp("p1", "schema1");
        builder.withProp("p2", "schema2");
        return builder.build();
    }

    private static Schema.Entry.Builder field(String name, Schema.Type type) {
        return factory.newEntryBuilder()
                .withType(type)
                .withName(name);
    }

}
