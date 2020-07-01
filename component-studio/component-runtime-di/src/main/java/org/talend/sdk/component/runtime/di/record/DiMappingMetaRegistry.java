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
package org.talend.sdk.component.runtime.di.record;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.bind.JsonbConfig;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.record.RecordService;
import org.talend.sdk.component.runtime.manager.service.DefaultServiceProvider;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.runtime.record.RecordConverters.IMappingMeta;
import org.talend.sdk.component.runtime.record.RecordConverters.MappingMeta;
import org.talend.sdk.component.runtime.record.RecordConverters.MappingMetaRegistry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
/**
 * This class overides the component-runtime-impl and provides the needed provisioning for dynamic columns.
 * Its scope is runtime Studio only.
 */
public class DiMappingMetaRegistry extends MappingMetaRegistry {

    protected final Map<Class<?>, DiMappingMeta> diRegistry = new ConcurrentHashMap<>();

    public DiMappingMeta findDi(final Class<?> parameterType, final RecordBuilderFactory factorySupplier) {
        final DiMappingMeta meta = diRegistry.get(parameterType);
        if (meta != null) {
            return meta;
        }
        final DiMappingMeta mappingMeta = new DiMappingMeta(parameterType, factorySupplier);
        final DiMappingMeta existing = diRegistry.putIfAbsent(parameterType, mappingMeta);
        if (existing != null) {
            return existing;
        }
        return mappingMeta;
    }

    public static class DiMappingMeta implements IMappingMeta {

        private final RecordService recordService = RecordService.class
                .cast(new DefaultServiceProvider(null, JsonProvider.provider(), Json.createGeneratorFactory(emptyMap()),
                        Json.createReaderFactory(emptyMap()), Json.createBuilderFactory(emptyMap()),
                        Json.createParserFactory(emptyMap()), Json.createWriterFactory(emptyMap()), new JsonbConfig(),
                        JsonbProvider.provider(), null, null, emptyList(), t -> new RecordBuilderFactoryImpl("di"),
                        null)
                                .lookup(null, Thread.currentThread().getContextClassLoader(), null, null,
                                        RecordService.class, null));

        private final Class<?> rowStruct;

        DiMappingMeta(final Class<?> parameterType, final RecordBuilderFactory factorySupplier) {

            this.rowStruct = parameterType;
        }

        @Override
        public boolean isLinearMapping() {
            return Stream.of(rowStruct.getInterfaces()).anyMatch(it -> it.getName().startsWith("routines.system."));
        }

        @Override
        public Object newInstance(final Record record) {
            return recordService.visit(new DiRecordVisitor(rowStruct), record);
        }

        @Override
        public <T> Record newRecord(final T data, final RecordBuilderFactory factory) {
            DiRowStructVisitor visitor = new DiRowStructVisitor(factory);
            visitor.visit(data);
            return visitor.get();
        }
    }

}
