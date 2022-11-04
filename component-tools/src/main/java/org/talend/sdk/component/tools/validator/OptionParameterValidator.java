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
package org.talend.sdk.component.tools.validator;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;

/**
 * Check option parameters on {@link PostConstruct} methods in {@link Emitter} class.
 */
public class OptionParameterValidator implements Validator {

    private static final Set<String> ALLOWED_OPTION_PARAMETERS = new HashSet<>();

    static {
        ALLOWED_OPTION_PARAMETERS.add(Option.MAX_DURATION_PARAMETER);
        ALLOWED_OPTION_PARAMETERS.add(Option.MAX_RECORDS_PARAMETER);
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final Set<Class<?>> emitterClassesOfPartition = finder.findAnnotatedMethods(Emitter.class)
                .stream()
                .filter(m -> m.getDeclaringClass().isAnnotationPresent(PartitionMapper.class))
                .map(Method::getReturnType)
                .collect(Collectors.toSet());

        return finder.findAnnotatedMethods(PostConstruct.class)
                .stream()
                .filter(m -> m.getParameterCount() != 0)
                .filter(m -> emitterClassesOfPartition.contains(m.getDeclaringClass())
                        || m.getDeclaringClass().isAnnotationPresent(Emitter.class))
                .flatMap(m -> Stream.concat(
                        Arrays.stream(m.getParameters())
                                .filter(p -> !p.isAnnotationPresent(Option.class))
                                .map(p -> "Parameter '" + p.getName()
                                        + "' should be either annotated with @Option or removed"),
                        Arrays.stream(m.getParameters())
                                .filter(p -> p.isAnnotationPresent(Option.class))
                                .filter(p -> !ALLOWED_OPTION_PARAMETERS.contains(p.getAnnotation(Option.class).value()))
                                .map(p -> "Option value on the parameter '" + p.getName() + "' is not acceptable. "
                                        + "Acceptable values: " + acceptableOptionValues())))
                .sorted();
    }

    private static String acceptableOptionValues() {
        return ALLOWED_OPTION_PARAMETERS.stream()
                .sorted()
                .collect(Collectors.joining(",", "[", "]"));
    }
}
