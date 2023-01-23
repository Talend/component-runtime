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
package org.talend.sdk.component.singer.kitap;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.singer.java.IO;
import org.talend.sdk.component.singer.java.Singer;

class RecordJsonMapperTest {

    private final RecordBuilderFactory factory = new RecordBuilderFactoryImpl(null);

    @Test
    void map() {
        final JsonObject object =
                new RecordJsonMapper(Json.createBuilderFactory(emptyMap()), new Singer(new IO(), ZonedDateTime::now))
                        .apply(factory
                                .newRecordBuilder()
                                .withString("name", "hello")
                                .withInt("age", 1)
                                .withBoolean("toggle", true)
                                .withDateTime("date", ZonedDateTime.of(2019, 8, 23, 16, 31, 0, 0, ZoneId.of("UTC")))
                                .withLong("lg", 2L)
                                .withBytes("bytes", "test".getBytes(StandardCharsets.UTF_8))
                                .withRecord("nested",
                                        factory
                                                .newRecordBuilder()
                                                .withString("value", "set")
                                                .withRecord("nested2",
                                                        factory.newRecordBuilder().withInt("l2", 2).build())
                                                .build())
                                .withArray(factory
                                        .newEntryBuilder()
                                        .withType(Schema.Type.ARRAY)
                                        .withName("array")
                                        .withElementSchema(factory.newSchemaBuilder(Schema.Type.STRING).build())
                                        .build(), singleton("value-from-array"))
                                .build());
        assertEquals("{" + "\"name\":\"hello\"," + "\"age\":1," + "\"toggle\":true,"
                + "\"date\":\"2019-08-23T16:31:00.000Z\"," + "\"lg\":2," + "\"bytes\":\"dGVzdA==\","
                + "\"array\":[\"value-from-array\"]," + "\"nested\":{\"value\":\"set\",\"nested2\":{\"l2\":2}}}",
                object.toString());
    }
}
