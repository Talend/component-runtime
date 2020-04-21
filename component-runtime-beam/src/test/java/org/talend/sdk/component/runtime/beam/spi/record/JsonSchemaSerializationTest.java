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
package org.talend.sdk.component.runtime.beam.spi.record;

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
                .withEntry(new SchemaImpl.EntryImpl("array", "array", Schema.Type.ARRAY, true, null,
                        new AvroSchemaBuilder().withType(STRING).build(), null))
                .build();
        try (final Jsonb jsonb = JsonbBuilder
                .create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL))) {
            assertEquals(
                    "{\"entries\":[{\"elementSchema\":{\"entries\":[],\"type\":\"STRING\"},\"name\":\"array\",\"label\":\"array\"\"nullable\":true,\"type\":\"ARRAY\"}],\"type\":\"RECORD\"}",
                    jsonb.toJson(schema));
        }
    }
}
