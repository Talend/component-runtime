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
package org.talend.sdk.component.runtime.beam.spi.record;

import static org.junit.Assert.assertEquals;
import static org.talend.sdk.component.api.record.Schema.Type.DATETIME;
import static org.talend.sdk.component.api.record.Schema.Type.RECORD;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.record.SchemaImpl;

class AvroSchemaTest {

    @Test
    void checkDateConversion() {
        final org.apache.avro.Schema avro = AvroSchema.class
                .cast(new AvroSchemaBuilder()
                        .withType(RECORD)
                        .withEntry(new SchemaImpl.EntryImpl.BuilderImpl().withType(DATETIME).withName("date").build())
                        .build())
                .getDelegate();
        assertEquals(DATETIME, new AvroSchema(avro).getEntries().iterator().next().getType());
    }
}
