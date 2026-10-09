/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.di;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

class InputsHandlerTest {

    private Jsonb jsonb;

    private RecordBuilderFactory factory;

    private InputsHandler handler;

    @BeforeEach
    void setUp() {
        jsonb = JsonbBuilder.create();
        factory = new RecordBuilderFactoryImpl("test");
        final Map<Class<?>, Object> services = new HashMap<>();
        services.put(RecordBuilderFactory.class, factory);
        handler = new InputsHandler(jsonb, services);
    }

    @AfterEach
    void tearDown() throws Exception {
        jsonb.close();
    }

    @Test
    void unknownOrEmptyConnectionReturnsNull() {
        final InputFactory input = handler.asInputFactory();
        assertNull(input.read("missing"));
        handler.addConnection("FLOW", Object.class);
        assertNull(input.read("FLOW"));
    }

    @Test
    void recordIsPassedThrough() {
        final Record rec = factory.newRecordBuilder().withString("name", "n").build();
        handler.addConnection("FLOW", Record.class);
        handler.setInputValue("FLOW", rec);
        assertSame(rec, handler.asInputFactory().read("FLOW"));
    }

    @Test
    void setInputValueOnUnknownConnectionIsIgnored() {
        handler.setInputValue("nope", "value");
        assertNull(handler.asInputFactory().read("nope"));
    }

    @Test
    void initInputValueRegistersConnectionFromSimpleClassName() {
        handler.initInputValue("FLOW", "x");
        assertNull(handler.asInputFactory().read("FLOW"));
    }

    @Test
    void jsonNullIsConvertedToNull() {
        handler.addConnection("FLOW", JsonValue.class);
        handler.setInputValue("FLOW", JsonValue.NULL);
        assertNull(handler.asInputFactory().read("FLOW"));
    }

    @Test
    void linearRowStructIsConvertedWithoutJsonb() {
        final RowStruct row = new RowStruct();
        row.name = "Alice";
        handler.addConnection("FLOW", RowStruct.class);
        handler.setInputValue("FLOW", row);
        final Record rec = Record.class.cast(handler.asInputFactory().read("FLOW"));
        assertNotNull(rec);
    }

    @Test
    void nonLinearValueGoesThroughJsonbSerialization() {
        // the legacy behaviour is preserved: the value is serialized then re-read as a json object,
        // a json string can't be read as such so it fails the same way as before the refactoring
        handler.addConnection("FLOW", JsonObject.class);
        handler.setInputValue("FLOW", Json.createObjectBuilder().add("name", "Alice").build());
        final InputFactory input = handler.asInputFactory();
        assertThrows(ClassCastException.class, () -> input.read("FLOW"));

        handler.addConnection("POJO", Pojo.class);
        handler.setInputValue("POJO", new Pojo());
        assertThrows(ClassCastException.class, () -> input.read("POJO"));
    }

    public static class RowStruct implements routines.system.IPersistableRow {

        public String name;

        @Override
        public void writeData(final java.io.ObjectOutputStream out) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void readData(final java.io.ObjectInputStream in) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Pojo {

        public String name;

        public int age;
    }
}
