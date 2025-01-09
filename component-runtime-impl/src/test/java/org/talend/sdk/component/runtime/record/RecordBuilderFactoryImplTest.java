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
package org.talend.sdk.component.runtime.record;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.talend.sdk.component.api.record.Schema.Type.INT;
import static org.talend.sdk.component.api.record.Schema.Type.RECORD;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.serialization.DynamicContainerFinder;

@TestInstance(PER_CLASS)
class RecordBuilderFactoryImplTest {

    private final RecordBuilderFactory factory = new RecordBuilderFactoryImpl(null);

    private final Schema address = factory
            .newSchemaBuilder(RECORD)
            .withEntry(
                    factory.newEntryBuilder().withName("street").withRawName("current street").withType(STRING).build())
            .withEntry(factory.newEntryBuilder().withName("number").withType(INT).build())
            .build();

    private final Schema baseSchema = factory
            .newSchemaBuilder(RECORD)
            .withEntry(factory.newEntryBuilder().withName("name").withRawName("current name").withType(STRING).build())
            .withEntry(factory.newEntryBuilder().withName("age").withType(INT).build())
            .withEntry(factory
                    .newEntryBuilder()
                    .withName("current address")
                    .withType(RECORD)
                    .withElementSchema(address)
                    .build())
            .build();

    @Test
    void copySchema() {
        final Schema custom = factory
                .newSchemaBuilder(baseSchema)
                .withEntry(factory.newEntryBuilder().withName("custom").withType(STRING).build())
                .build();
        assertEquals("name/STRING/current name,age/INT/null,current_address/RECORD/current address,custom/STRING/null",
                custom
                        .getEntries()
                        .stream()
                        .map(it -> it.getName() + '/' + it.getType() + '/' + it.getRawName())
                        .collect(joining(",")));
    }

    @Test
    void copyRecord() {
        final Schema customSchema = factory
                .newSchemaBuilder(baseSchema)
                .withEntry(factory.newEntryBuilder().withName("custom").withType(STRING).build())
                .build();
        final Record baseRecord = factory
                .newRecordBuilder(baseSchema)
                .withString("name", "Test")
                .withInt("age", 33)
                .withRecord("current_address",
                        factory.newRecordBuilder(address).withString("street", "here").withInt("number", 1).build())
                .build();
        final Record output = factory.newRecordBuilder(customSchema, baseRecord).withString("custom", "added").build();
        assertEquals(
                "{\"name\":\"Test\",\"age\":33,\"current_address\":{\"street\":\"here\",\"number\":1},\"custom\":\"added\"}",
                output.toString());
    }

    @Test
    void serial() throws IOException, ClassNotFoundException {
        DynamicContainerFinder.LOADERS.put("test", Thread.currentThread().getContextClassLoader());
        final RecordBuilderFactory factory = DynamicContainerFinder.Instance.get()
                .find("test")
                .findService(RecordBuilderFactory.class);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(factory);
        }
        try (final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            final RecordBuilderFactory factory2 = (RecordBuilderFactory) ois.readObject();
            Assertions.assertNotNull(factory2);
        }
    }
}
