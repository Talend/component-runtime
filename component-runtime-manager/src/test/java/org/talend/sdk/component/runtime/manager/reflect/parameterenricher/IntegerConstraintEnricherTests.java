package org.talend.sdk.component.runtime.manager.reflect.parameterenricher;

import java.util.Collections;
import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IntegerConstraintEnricherTests {

    private final IntegerConstraintEnricher enricher = new IntegerConstraintEnricher();

    @Test
    void constraintOnPrimitive() {
        Assertions.assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::validation::max", String.valueOf(Integer.MAX_VALUE));
                put("tcomp::validation::min", String.valueOf(Integer.MIN_VALUE));

            }
        }, enricher.onParameterAnnotation("bla", int.class, null));
    }

    @Test
    void constraintOnBox() {
        Assertions.assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::validation::max", String.valueOf(Integer.MAX_VALUE));
                put("tcomp::validation::min", String.valueOf(Integer.MIN_VALUE));

            }
        }, enricher.onParameterAnnotation("bla", Integer.class, null));
    }

    @Test
    void noConstraintOnObject() {
        Assertions.assertEquals(Collections.emptyMap(), enricher.onParameterAnnotation("bla", Object.class, null));
    }

}
