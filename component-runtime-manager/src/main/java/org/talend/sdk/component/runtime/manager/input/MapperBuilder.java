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
package org.talend.sdk.component.runtime.manager.input;

import static java.util.Optional.of;

import java.io.Serializable;
import java.util.Optional;

import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.runtime.input.LocalPartitionMapper;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.input.ObjectConverter;
import org.talend.sdk.component.runtime.input.PartitionMapperImpl;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;

/**
 * Interface to build mapper.
 * Allow ComponentManager users to inject specific builder to get specific Mapper
 */
public class MapperBuilder {

    private final ObjectConverter converter;

    public MapperBuilder(final ObjectConverter converter) {
        this.converter = converter;
    }

    public Mapper forEmitter(final Class<?> type, final Emitter emitter, final ComponentFamilyMeta component,
            final String plugin, final Serializable instance) {
        final String name = Optional
                .of(emitter.name()) //
                .filter((String n) -> !n.isEmpty()) //
                .orElseGet(type::getName);
        return new LocalPartitionMapper(component.getName(), name, plugin, instance, this.converter);
    }

    public Mapper forPartitionMapper(final Class<?> type, final PartitionMapper partitionMapper,
            final ComponentFamilyMeta component, final String plugin, final Serializable instance) {
        final String name = of(partitionMapper.name()) //
                .filter(n -> !n.isEmpty()) //
                .orElseGet(type::getName);
        final boolean infinite = partitionMapper.infinite();

        return new PartitionMapperImpl(component.getName(), name, null, plugin, infinite, instance, this.converter);
    }
}
