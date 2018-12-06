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

import static org.apache.beam.sdk.util.SerializableUtils.ensureSerializableByCoder;
import static org.junit.Assert.assertArrayEquals;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;

class AvroRecordTest {

    @Test
    void bytes() {
        final byte[] array = { 0, 1, 2, 3, 4 };
        final Record record = new AvroRecordBuilder().withBytes("bytes", array).build();
        assertArrayEquals(array, record.getBytes("bytes"));

        final Record copy = ensureSerializableByCoder(SchemaRegistryCoder.of(), record, "test");
        assertArrayEquals(array, copy.getBytes("bytes"));
    }
}
