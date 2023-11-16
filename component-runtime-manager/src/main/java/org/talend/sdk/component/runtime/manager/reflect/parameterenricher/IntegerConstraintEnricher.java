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
package org.talend.sdk.component.runtime.manager.reflect.parameterenricher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.talend.sdk.component.api.configuration.constraint.Max;
import org.talend.sdk.component.api.configuration.constraint.Min;

/**
 * @author Oleksand Zhelezniak
 */
public class IntegerConstraintEnricher extends BaseParameterEnricher {

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName, final Type parameterType,
            final Annotation annotation) {
        return Collections.emptyMap();
    }

    @Override
    public Map<Type, Collection<Annotation>> getImplicitAnnotationForTypes() {
        // we can re-use existed constraint enricher
        // also we can differentiate that those constraints are implicit
        // and they can be overwritten by explicit
        final List<Annotation> annotations = Arrays.asList(new PseudoMin(), new PseudoMax());
        return Stream.of(int.class, Integer.class)
                .collect(Collectors.toMap(Function.identity(), k -> annotations));
    }

    private static final class PseudoMin implements Min {

        @Override
        public double value() {
            return Integer.MIN_VALUE;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Min.class;
        }
    }

    private static final class PseudoMax implements Max {

        @Override
        public double value() {
            return Integer.MAX_VALUE;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Max.class;
        }
    }
}
