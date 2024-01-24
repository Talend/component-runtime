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
package org.talend.sdk.component.runtime.record.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.json.bind.Jsonb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.record.PluralRecordExtension;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

class RecordJsonGeneratorTest {

    @RegisterExtension
    public final PluralRecordExtension pluralRecordExtension = new PluralRecordExtension();

    @Test
    void flatRecord(final Jsonb jsonb) {
        final RecordBuilderFactoryImpl factory = new RecordBuilderFactoryImpl("test");
        final OutputRecordHolder out = new OutputRecordHolder();
        final RecordJsonGenerator generator = new RecordJsonGenerator(factory, jsonb, out);
        generator.writeStartObject();
        generator.write("a", 1);
        generator.write("b", "s");
        generator.write("c", 1.0);
        generator.writeEnd();
        generator.close();
        assertEquals("{\"a\":1,\"b\":\"s\",\"c\":1.0}", out.getRecord().toString());
    }

    @Test
    void arrayOfDouble(final Jsonb jsonb) {
        final RecordBuilderFactoryImpl factory = new RecordBuilderFactoryImpl("test");
        final OutputRecordHolder out = new OutputRecordHolder();
        final RecordJsonGenerator generator = new RecordJsonGenerator(factory, jsonb, out);
        generator.writeStartObject();
        generator.writeStartArray("a");
        generator.write(1.0);
        generator.write(2.0);
        generator.writeEnd();
        generator.writeEnd();
        generator.close();
        assertEquals("{\"a\":[1.0,2.0]}", out.getRecord().toString());
    }

    @Test
    void arrayRecords(final Jsonb jsonb) {
        final RecordBuilderFactoryImpl factory = new RecordBuilderFactoryImpl("test");
        final OutputRecordHolder out = new OutputRecordHolder();
        final RecordJsonGenerator generator = new RecordJsonGenerator(factory, jsonb, out);
        generator.writeStartObject();
        generator.writeStartArray("a");
        generator.writeStartObject();
        generator.write("b", "1");
        generator.writeEnd();
        generator.writeStartObject();
        generator.write("c", "2");
        generator.writeEnd();
        generator.writeEnd();
        generator.writeEnd();
        generator.close();

        final Record record = out.getRecord();
        assertEquals("{\"a\":[{\"b\":\"1\"},{\"c\":\"2\"}]}", record.toString());
    }

    @Test
    void objectOfObject(final Jsonb jsonb) {
        final RecordBuilderFactoryImpl factory = new RecordBuilderFactoryImpl("test");
        final OutputRecordHolder out = new OutputRecordHolder();
        final RecordJsonGenerator generator = new RecordJsonGenerator(factory, jsonb, out);
        generator.writeStartObject();
        generator.writeStartObject("a");
        generator.write("b", "1");
        generator.writeEnd();
        generator.writeEnd();
        generator.close();

        final Record record = out.getRecord();
        assertEquals("{\"a\":{\"b\":\"1\"}}", record.toString());
    }
}
