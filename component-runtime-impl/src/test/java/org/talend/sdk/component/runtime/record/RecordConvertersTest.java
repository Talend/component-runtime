/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.record;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;

import javax.json.Json;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;

class RecordConvertersTest {

    private final RecordConverters converter = new RecordConverters();

    @Test
    void convertListString() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final Record record = converter.toRecord(Json
                    .createObjectBuilder()
                    .add("list",
                            Json.createArrayBuilder().add(Json.createValue("a")).add(Json.createValue("b")).build())
                    .build(), () -> jsonb, () -> new RecordBuilderFactoryImpl("test"));
            final Collection<String> list = record.getArray(String.class, "list");
            assertEquals(asList("a", "b"), list);
        }
    }

    @Test
    void convertListObject() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final Record record = converter.toRecord(Json
                    .createObjectBuilder()
                    .add("list",
                            Json
                                    .createArrayBuilder()
                                    .add(Json.createObjectBuilder().add("name", "a").build())
                                    .add(Json.createObjectBuilder().add("name", "b").build())
                                    .build())
                    .build(), () -> jsonb, () -> new RecordBuilderFactoryImpl("test"));
            final Collection<Record> list = record.getArray(Record.class, "list");
            assertEquals(asList("a", "b"), list.stream().map(it -> it.getString("name")).collect(toList()));
        }
    }
}
