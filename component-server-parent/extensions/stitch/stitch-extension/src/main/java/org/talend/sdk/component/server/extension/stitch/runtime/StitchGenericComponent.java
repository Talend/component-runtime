/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.stitch.runtime;

import java.util.Map;

import javax.json.JsonBuilderFactory;
import javax.json.JsonReaderFactory;

import org.talend.sdk.component.api.service.http.HttpClientFactory;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.spi.component.GenericComponentExtension;

public class StitchGenericComponent implements GenericComponentExtension {

    @Override
    public boolean canHandle(final Class<?> expectedType, final String plugin, final String name) {
        return Mapper.class == expectedType && "stitch".equals(plugin);
    }

    @Override
    public <T> T createInstance(final Class<T> type, final String plugin, final String name, final int version,
            final Map<String, String> configuration, final Map<Class<?>, Object> services) {
        return type
                .cast(new StitchMapper(plugin, name, configuration,
                        JsonReaderFactory.class.cast(services.get(JsonReaderFactory.class)),
                        JsonBuilderFactory.class.cast(services.get(JsonBuilderFactory.class)),
                        RecordBuilderFactory.class.cast(services.get(RecordBuilderFactory.class)),
                        HttpClientFactory.class.cast(services.get(HttpClientFactory.class))));
    }
}
