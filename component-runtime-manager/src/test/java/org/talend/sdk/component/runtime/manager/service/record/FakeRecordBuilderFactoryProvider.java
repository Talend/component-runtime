/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import static java.util.Optional.ofNullable;

import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

public class FakeRecordBuilderFactoryProvider implements RecordBuilderFactoryProvider {

    @Override
    public RecordBuilderFactory apply(final String containerId) {
        final String loadClazz =
                containerId.equals("service-meta") ? "org.talend.sdk.component.runtime.manager.ServiceMeta"
                        : "org.talend.sdk.component.runtime.beam.TalendIO";

        switch (System.getProperty("talend.component.beam.record.factory.impl", "auto")) {
            case "memory":
            case "default":
                return new RecordBuilderFactoryImpl(containerId);
            case "avro":
                return new FakeRecordBuilderFactory(containerId);
            default:
                try {
                    ofNullable(Thread.currentThread().getContextClassLoader())
                            .orElseGet(ClassLoader::getSystemClassLoader)
                            .loadClass(loadClazz);
                    return new FakeRecordBuilderFactory(containerId);
                } catch (final ClassNotFoundException | NoClassDefFoundError cnfe) {
                    return new RecordBuilderFactoryImpl(containerId);
                }
        }
    }
}
