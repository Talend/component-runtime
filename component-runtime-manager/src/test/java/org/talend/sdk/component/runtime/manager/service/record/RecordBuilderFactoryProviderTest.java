/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service.record;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

public class RecordBuilderFactoryProviderTest {

    private Function<String, RecordBuilderFactory> provider =
            ComponentManager.instance().getRecordBuilderFactoryProvider();

    @Test
    void defaultProvider() {
        RecordBuilderFactory factory = provider.apply("test");
        assertEquals(RecordBuilderFactoryImpl.class, factory.getClass());
    }

    @Test
    void defaultProviderWithPresentLoadClass() {
        RecordBuilderFactory factory = provider.apply("service-meta");
        assertEquals(FakeRecordBuilderFactory.class, factory.getClass());
    }

    @Test
    void memoryProvider() {
        System.setProperty("talend.component.beam.record.factory.impl", "memory");
        RecordBuilderFactory factory = provider.apply("memory");
        assertEquals(RecordBuilderFactoryImpl.class, factory.getClass());
        System.clearProperty("talend.component.beam.record.factory.impl");
    }

    @Test
    void avroProvider() {
        System.setProperty("talend.component.beam.record.factory.impl", "avro");
        RecordBuilderFactory factory = provider.apply("avro");
        assertEquals(FakeRecordBuilderFactory.class, factory.getClass());
        System.clearProperty("talend.component.beam.record.factory.impl");
    }
}
