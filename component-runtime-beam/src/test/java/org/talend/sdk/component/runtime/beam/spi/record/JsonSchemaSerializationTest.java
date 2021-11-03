/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.spi.record;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.talend.sdk.component.api.record.Schema.Type.RECORD;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyOrderStrategy;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.record.SchemaImpl;

// used by ui actions
class JsonSchemaSerializationTest {

    @Test
    void toJson() throws Exception {
        final Schema schema = new AvroSchemaBuilder()
                .withType(RECORD)
                .withEntry(new Schema.Entry.Builder()
                        .withName("array")
                        .withRawName("array")
                        .withType(Schema.Type.ARRAY)
                        .withNullable(true)
                        .withElementSchema(new AvroSchemaBuilder().withType(STRING).build())
                        .withProps(emptyMap())
                        .build())
                .build();
        try (final Jsonb jsonb = JsonbBuilder
                .create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL))) {
            final String json = jsonb.toJson(schema);
            assertEquals(
                    "{\"entries\":[{\"elementSchema\":{\"entries\":[],\"metadata\":[],\"props\":{},\"type\":\"STRING\"},\"metadata\":false,\"name\":\"array\",\"nullable\":true,\"props\":{\"talend.component.label\":\"array\"},\"rawName\":\"array\",\"type\":\"ARRAY\"}],\"metadata\":[],\"props\":{},\"type\":\"RECORD\"}",
                    json);
        }
    }

    @Test
    void toJsonWithMeta() throws Exception {
        final Schema schema = new AvroSchemaBuilder()
                .withType(RECORD)
                .withEntry(new Schema.Entry.Builder()
                        .withName("array")
                        .withRawName("array")
                        .withType(Schema.Type.ARRAY)
                        .withNullable(true)
                        .withMetadata(true)
                        .withElementSchema(new AvroSchemaBuilder().withType(STRING).build())
                        .withProps(emptyMap())
                        .build())
                .build();
        try (final Jsonb jsonb = JsonbBuilder
                .create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL))) {
            assertEquals(
                    "{\"entries\":[],\"metadata\":[{\"elementSchema\":{\"entries\":[],\"metadata\":[],\"props\":{},\"type\":\"STRING\"},\"metadata\":true,\"name\":\"array\",\"nullable\":true,\"props\":{\"talend.component.label\":\"array\"},\"rawName\":\"array\",\"type\":\"ARRAY\"}],\"props\":{},\"type\":\"RECORD\"}",
                    jsonb.toJson(schema));

        }
    }
}
