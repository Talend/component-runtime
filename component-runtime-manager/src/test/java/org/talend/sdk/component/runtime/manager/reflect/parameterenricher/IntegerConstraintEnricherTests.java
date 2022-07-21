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
