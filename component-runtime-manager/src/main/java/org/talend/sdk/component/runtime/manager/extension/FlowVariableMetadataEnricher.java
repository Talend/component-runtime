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
package org.talend.sdk.component.runtime.manager.extension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.talend.sdk.component.api.component.FlowVariables;
import org.talend.sdk.component.api.component.FlowVariables.FlowVariable;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.standalone.DriverRunner;
import org.talend.sdk.component.spi.component.ComponentMetadataEnricher;

/**
 * Search annotation {@link FlowVariable} and add a new meta information about description of flow variables.
 * NOTE. This functionality is used only in Studio.
 */
public class FlowVariableMetadataEnricher implements ComponentMetadataEnricher {

    private static final Set<Class<? extends Annotation>> SUPPORTED_ANNOTATIONS =
            new HashSet<>(Arrays.asList(PartitionMapper.class, Emitter.class, Processor.class, DriverRunner.class));

    public static final String META_KEY_FLOW_VARIABLE = "variables::flow::value";

    private static final String VALUE_DELIMITER = "\\:";

    private static final String LINE_DELIMITER = "\\;";

    @Override
    public Map<String, String> onComponent(final Type type, final Annotation[] annotations) {
        boolean noneMatch =
                Stream.of(annotations).map(Annotation::annotationType).noneMatch(SUPPORTED_ANNOTATIONS::contains);
        if (noneMatch) {
            return Collections.emptyMap();
        }

        String flowVariableMetaValue = Stream
                .concat(Stream.of(annotations),
                        Stream
                                .of(annotations)
                                .filter(a -> a.annotationType().equals(FlowVariables.class))
                                .map(FlowVariables.class::cast)
                                .map(FlowVariables::value)
                                .flatMap(Stream::of))
                .filter(a -> a.annotationType().equals(FlowVariable.class))
                .map(FlowVariable.class::cast)
                .map(FlowVariableMetadataEnricher::makeFlowVariableString)
                .collect(Collectors.joining(LINE_DELIMITER));

        if (flowVariableMetaValue.isEmpty()) {
            return Collections.emptyMap();
        }

        return Collections.singletonMap(META_KEY_FLOW_VARIABLE, flowVariableMetaValue);
    }

    private static String makeFlowVariableString(final FlowVariable a) {
        return a.value() + VALUE_DELIMITER + a.type().getCanonicalName() + VALUE_DELIMITER + a.description();
    }
}
