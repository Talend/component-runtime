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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.talend.sdk.component.api.configuration.dependency.ConnectorReference;

class DependencyParameterEnricherTest {

    private final DependencyParameterEnricher enricher = new DependencyParameterEnricher();

    @Test
    void onParameterAnnotation() throws ReflectiveOperationException {
        final Field field = MyConfig.class.getDeclaredField("reference");
        final Map<String, String> value = enricher.onParameterAnnotation("value", field.getGenericType(), null);
        Assertions.assertNotNull(value);
        Assertions.assertNotNull(value.get("tcomp::dependencies::connector"));

        final Field fieldRef = MyConfig.class.getDeclaredField("references");
        final Map<String, String> value2 = enricher.onParameterAnnotation("value", fieldRef.getGenericType(), null);
        Assertions.assertNotNull(value2);
        Assertions.assertNotNull(value2.get("tcomp::dependencies::connector"));
    }

    @ParameterizedTest
    @ValueSource(strings = { /* "wrongColl", "wrongColl2", */ "wrongColl3" })
    void testWrong(final String args) throws ReflectiveOperationException {
        final Field fieldErr = MyConfig.class.getDeclaredField(args);
        final Map<String, String> value3 = enricher.onParameterAnnotation("value", fieldErr.getGenericType(), null);
        Assertions.assertNotNull(value3);
        Assertions.assertTrue(value3.isEmpty());
    }

    static class MyConfig {

        private ConnectorReference reference;

        private List<ConnectorReference> references;

        private List<String> wrongColl;

        private String wrongColl2;

        private Map<String, ConnectorReference> wrongColl3;
    }
}