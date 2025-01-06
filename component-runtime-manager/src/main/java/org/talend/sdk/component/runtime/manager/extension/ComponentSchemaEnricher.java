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
package org.talend.sdk.component.runtime.manager.extension;

import static java.util.Collections.emptyMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.service.schema.FixedSchema;
import org.talend.sdk.component.api.standalone.DriverRunner;
import org.talend.sdk.component.spi.component.ComponentMetadataEnricher;

public class ComponentSchemaEnricher implements ComponentMetadataEnricher {

    public static final String FIXED_SCHEMA_META_PREFIX = "tcomp::ui::schema::fixed";

    public static final String FIXED_SCHEMA_FLOWS_META_PREFIX = "tcomp::ui::schema::flows::fixed";

    private static final Set<Class<? extends Annotation>> SUPPORTED_ANNOTATIONS =
            new HashSet<>(Arrays.asList(PartitionMapper.class, Emitter.class, Processor.class, DriverRunner.class));

    @Override
    public Map<String, String> onComponent(final Type type, final Annotation[] annotations) {
        if (Stream.of(annotations).map(Annotation::annotationType).noneMatch(SUPPORTED_ANNOTATIONS::contains)) {
            return emptyMap();
        }

        final Optional<FixedSchema> fixed = Arrays.stream(annotations)
                .filter(a -> a.annotationType().equals(FixedSchema.class))
                .findFirst()
                .map(FixedSchema.class::cast);

        if (!fixed.isPresent()) {
            return emptyMap();
        }

        final FixedSchema fixedSchema = fixed.get();
        final Map<String, String> metadata = new HashMap<>();
        metadata.put(FIXED_SCHEMA_META_PREFIX, fixedSchema.value());
        if (fixedSchema.flows().length > 0) {
            metadata.put(FIXED_SCHEMA_FLOWS_META_PREFIX, String.join(",", fixedSchema.flows()));
        }

        return metadata;
    }
}
