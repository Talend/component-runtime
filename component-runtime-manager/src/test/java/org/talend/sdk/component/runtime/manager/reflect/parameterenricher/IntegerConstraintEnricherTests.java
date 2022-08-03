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
package org.talend.sdk.component.runtime.manager.reflect.parameterenricher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.constraint.Max;
import org.talend.sdk.component.api.configuration.constraint.Min;

public class IntegerConstraintEnricherTests {

    private final IntegerConstraintEnricher enricher = new IntegerConstraintEnricher();

    @Test
    void checkConstraints() {
        assertTrue(enricher.onParameterAnnotation("bla", int.class, null).isEmpty());
        assertTrue(enricher.onParameterAnnotation("bla", Integer.class, null).isEmpty());
    }

    @Test
    void checkImplicitAnnotations() {
        final Map<Type, Collection<Annotation>> map = enricher.getImplicitAnnotationForTypes();

        assertEquals(2, map.size());
        final Collection<Annotation> intAnnotations = map.get(int.class);
        assertEquals(2, intAnnotations.size());
        final Collection<Annotation> integerAnnotations = map.get(Integer.class);
        assertEquals(2, integerAnnotations.size());

        Set<Class<? extends Annotation>> annotations = new HashSet<>(Arrays.asList(Min.class, Max.class));
        assertTrue(intAnnotations.stream().allMatch(it -> annotations.contains(it.annotationType())));
        assertTrue(integerAnnotations.stream().allMatch(it -> annotations.contains(it.annotationType())));
    }
}
