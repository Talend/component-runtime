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
package org.talend.sdk.component.runtime.beam.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AvroRecordBuilderFactoryProviderTest {

    @ParameterizedTest
    @CsvSource({ "default,org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl",
            "memory,org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl",
            "avro,org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider$AvroRecordBuilderFactory",
            "auto,org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider$AvroRecordBuilderFactory" })
    void createFactory(final String mode, final Class<?> expected) {
        final String old = System.getProperty("talend.component.beam.record.factory.impl");
        System.setProperty("talend.component.beam.record.factory.impl", mode);
        try {
            assertEquals(expected, new AvroRecordBuilderFactoryProvider().apply("test").getClass());
        } finally {
            if (old == null) {
                System.clearProperty("talend.component.beam.record.factory.impl");
            } else {
                System.setProperty("talend.component.beam.record.factory.impl", old);
            }
        }
    }
}
