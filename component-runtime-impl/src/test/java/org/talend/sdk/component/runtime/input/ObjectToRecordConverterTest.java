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
package org.talend.sdk.component.runtime.input;

import static org.junit.jupiter.api.Assertions.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

class ObjectToRecordConverterTest {

    @Test
    void convert() {
        final ObjectToRecordConverter converter = new ObjectToRecordConverter();
        Assertions.assertEquals("Hello", converter.convert(this::findService, "Hello"));
        Assertions.assertEquals(24, converter.convert(this::findService, 24));

        Assertions.assertEquals(null, converter.convert(this::findService, null));

        final JsonObject jsonObject = Json
                .createObjectBuilder() //
                .add("field1", 24) //
                .add("field2", "Hello") //
                .build();
        final Object record = converter.convert(this::findService, jsonObject);
        Assertions.assertTrue(Record.class.isInstance(record));
    }

    private <T> T findService(Class<T> type) {
        if (Jsonb.class.isAssignableFrom(type)) {
            return (T) JsonbBuilder.create();
        }
        if (RecordBuilderFactory.class.isAssignableFrom(type)) {
            return (T) new RecordBuilderFactoryImpl("test");
        }
        return null;
    }
}