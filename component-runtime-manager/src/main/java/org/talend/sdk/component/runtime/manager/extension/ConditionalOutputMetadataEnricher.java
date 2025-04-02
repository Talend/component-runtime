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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.talend.sdk.component.api.meta.ConditionalOutput;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.spi.component.ComponentMetadataEnricher;

/**
 * Search annotation {@link ConditionalOutput} and add a new meta information about name of related
 * AvailableOutputsFlow.
 * NOTE. This functionality is used only in Studio.
 */
public class ConditionalOutputMetadataEnricher implements ComponentMetadataEnricher {

    private static final Set<Class<? extends Annotation>> SUPPORTED_ANNOTATIONS =
            new HashSet<>(Collections.singletonList(Processor.class));

    public static final String META_KEY_RETURN_VARIABLE = "conditional_output::value";

    @Override
    public Map<String, String> onComponent(final Type type, final Annotation[] annotations) {
        final boolean noneMatch =
                Stream.of(annotations).map(Annotation::annotationType).noneMatch(SUPPORTED_ANNOTATIONS::contains);
        if (noneMatch) {
            return Collections.emptyMap();
        }

        final Optional<ConditionalOutput> metaValue = Arrays.stream(annotations)
                .filter(a -> a.annotationType().equals(ConditionalOutput.class))
                .findFirst()
                .map(ConditionalOutput.class::cast);

        return metaValue
                .map(conditionalOutput -> Collections.singletonMap(META_KEY_RETURN_VARIABLE, conditionalOutput.value()))
                .orElse(Collections.emptyMap());

    }

}
