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

import org.talend.sdk.component.api.component.ReturnVariables;
import org.talend.sdk.component.api.component.ReturnVariables.ReturnVariable;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.standalone.DriverRunner;
import org.talend.sdk.component.spi.component.ComponentMetadataEnricher;

/**
 * Search annotation {@link ReturnVariable} and add a new meta information about description of return variables.
 * NOTE. This functionality is used only in Studio.
 */
public class ReturnVariableMetadataEnricher implements ComponentMetadataEnricher {

    private static final Set<Class<? extends Annotation>> SUPPORTED_ANNOTATIONS =
            new HashSet<>(Arrays.asList(PartitionMapper.class, Emitter.class, Processor.class, DriverRunner.class));

    public static final String META_KEY_RETURN_VARIABLE = "variables::return::value";

    private static final String VALUE_DELIMITER = "\\:";

    private static final String LINE_DELIMITER = "\\;";

    @Override
    public Map<String, String> onComponent(final Type type, final Annotation[] annotations) {
        boolean noneMatch =
                Stream.of(annotations).map(Annotation::annotationType).noneMatch(SUPPORTED_ANNOTATIONS::contains);
        if (noneMatch) {
            return Collections.emptyMap();
        }

        String returnVariableMetaValue = Stream
                .concat(Stream.of(annotations),
                        Stream
                                .of(annotations)
                                .filter(a -> a.annotationType().equals(ReturnVariables.class))
                                .map(ReturnVariables.class::cast)
                                .map(ReturnVariables::value)
                                .flatMap(Stream::of))
                .filter(a -> a.annotationType().equals(ReturnVariable.class))
                .map(ReturnVariable.class::cast)
                .map(ReturnVariableMetadataEnricher::makeReturnVariableString)
                .collect(Collectors.joining(LINE_DELIMITER));

        if (returnVariableMetaValue.isEmpty()) {
            return Collections.emptyMap();
        }

        return Collections.singletonMap(META_KEY_RETURN_VARIABLE, returnVariableMetaValue);
    }

    private static String makeReturnVariableString(final ReturnVariable var) {
        return var.value() + VALUE_DELIMITER + var.type().getCanonicalName() + VALUE_DELIMITER
                + var.availability().getKey() + VALUE_DELIMITER + var.description();
    }
}
