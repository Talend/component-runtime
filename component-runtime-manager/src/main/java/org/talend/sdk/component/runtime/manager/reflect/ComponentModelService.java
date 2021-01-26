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
package org.talend.sdk.component.runtime.manager.reflect;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.talend.sdk.component.api.component.Metadatas;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ValidationParameterEnricher;
import org.talend.sdk.component.spi.component.ComponentMetadataEnricher;

public class ComponentModelService {

    private List<ComponentMetadataEnricher> enrichers;

    private static final String DOCUMENTATION_KEY = "documentation::value";

    private static final String METADATA_PREFIX = "meta::";

    public static final String MAPPER_INFINITE = "mapper::infinite";

    public ComponentModelService() {
        this.enrichers = StreamSupport
                .stream(Spliterators
                        .spliteratorUnknownSize(ServiceLoader.load(ComponentMetadataEnricher.class).iterator(),
                                Spliterator.IMMUTABLE),
                        false)
                .collect(toList());
    }

    public Map<String, String> getMetadata(final Class<?> clazz) {
        Map<String, String> metas = new HashMap<>();
        ofNullable(clazz.getAnnotation(Documentation.class)).ifPresent(d -> metas.put(DOCUMENTATION_KEY, d.value()));
        ofNullable(clazz.getAnnotation(Metadatas.class))
                .ifPresent(m -> Arrays
                        .stream(m.value())
                        .forEach(meta -> metas.put(METADATA_PREFIX + meta.key(), meta.value())));
        ofNullable(clazz.getAnnotation(PartitionMapper.class))
                .ifPresent(pm -> metas.put(MAPPER_INFINITE, Boolean.toString(pm.infinite())));
        ofNullable(clazz.getAnnotation(Emitter.class)).ifPresent(e -> metas.put(MAPPER_INFINITE, "false"));
        // user defined spi
        enrichers
                .stream()
                .forEach(enricher -> enricher
                        .onComponent(clazz, clazz.getAnnotations())
                        .forEach((k, v) -> metas.put(METADATA_PREFIX + k, v)));

        return metas;
    }

    public static Map<String, String> sanitizeMetadata(final Map<String, String> metadata) {
        return ofNullable(metadata)
                .map(m -> m
                        .entrySet()
                        .stream()
                        .filter(e -> !e.getKey().startsWith(ValidationParameterEnricher.META_PREFIX))
                        .collect(toMap(e -> e.getKey().replace("tcomp::", ""), Map.Entry::getValue)))
                .orElse(Collections.emptyMap());
    }
}
