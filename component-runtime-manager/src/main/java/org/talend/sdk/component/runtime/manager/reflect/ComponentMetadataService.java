/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.reflect;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import org.talend.sdk.component.api.component.Metadatas;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.spi.component.ComponentMetadataEnricher;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ComponentMetadataService {

    private List<ComponentMetadataEnricher> enrichers;

    private static final String DOCUMENTATION_KEY = "documentation::value";

    public static final String MAPPER_INFINITE = "mapper::infinite";

    public static final String MAPPER_OPTIONAL_ROW = "mapper::optionalRow";

    public ComponentMetadataService() {
        this.enrichers = StreamSupport
                .stream(ServiceLoader.load(ComponentMetadataEnricher.class).spliterator(), false)
                .collect(toList());
    }

    public Map<String, String> getMetadata(final Class<?> clazz) {
        final Map<String, String> metas = new HashMap<>();
        ofNullable(clazz.getAnnotation(Documentation.class)).ifPresent(d -> metas.put(DOCUMENTATION_KEY, d.value()));
        ofNullable(clazz.getAnnotation(Metadatas.class))
                .ifPresent(m -> Arrays.stream(m.value()).forEach(meta -> metas.put(meta.key(), meta.value())));
        ofNullable(clazz.getAnnotation(PartitionMapper.class))
                .ifPresent(pm -> {
                    metas.put(MAPPER_INFINITE, Boolean.toString(pm.infinite()));
                    metas.put(MAPPER_OPTIONAL_ROW, Boolean.toString(pm.optionalRow()));
                });
        ofNullable(clazz.getAnnotation(Emitter.class)).ifPresent(e -> {
            metas.put(MAPPER_INFINITE, "false");
            metas.put(MAPPER_OPTIONAL_ROW, Boolean.toString(e.optionalRow()));
        });
        // user defined spi
        enrichers
                .stream()
                .sorted(comparing(ComponentMetadataEnricher::order))
                .forEach(enricher -> enricher.onComponent(clazz, clazz.getAnnotations()).forEach((k, v) -> {
                    if (metas.containsKey(k)) {
                        log
                                .warn("SPI {} (order: {}) overrides metadata {}.", enricher.getClass().getName(),
                                        enricher.order(), k);
                    }
                    metas.put(k, v);
                }));

        return metas;
    }

}
