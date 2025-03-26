package org.talend.sdk.component.runtime.manager.extension;

import java.lang.annotation.Annotation;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.meta.ConditionalOutput;
import org.talend.sdk.component.api.processor.Processor;

public class ConditionalOutputMetadataEnricherTest {

    @Test
    void oneAnnotation() {
        Map<String, String> metas = invokeMetaEnricher(OutputClass1.class);
        Assertions.assertFalse(metas.isEmpty());
        Assertions.assertTrue(metas.containsKey(ConditionalOutputMetadataEnricher.META_KEY_RETURN_VARIABLE));
        Assertions
                .assertEquals("output1",
                        metas.get(ConditionalOutputMetadataEnricher.META_KEY_RETURN_VARIABLE));
    }

    @Test
    void oneAnnotation2() {
        Map<String, String> metas = invokeMetaEnricher(OutputClass2.class);
        Assertions.assertFalse(metas.isEmpty());
        Assertions.assertTrue(metas.containsKey(ConditionalOutputMetadataEnricher.META_KEY_RETURN_VARIABLE));
        Assertions
                .assertEquals("output2",
                        metas.get(ConditionalOutputMetadataEnricher.META_KEY_RETURN_VARIABLE));
    }

    @Test
    void empty() {
        Map<String, String> metas = invokeMetaEnricher(EmptyOutputClass.class);
        Assertions.assertTrue(metas.isEmpty());
    }

    private Map<String, String> invokeMetaEnricher(Class<?> clazz) {
        Annotation[] annotations = clazz.getAnnotations();
        return new ConditionalOutputMetadataEnricher().onComponent(OutputClass1 .class, annotations);
    }

    @ConditionalOutput("output1")
    @Processor(name="test1")
    private static class OutputClass1 {

    }

    @ConditionalOutput("output2")
    @Processor(name="test2")
    private static class OutputClass2 {

    }

    @Processor(name="test2")
    private static class EmptyOutputClass {

    }
}
