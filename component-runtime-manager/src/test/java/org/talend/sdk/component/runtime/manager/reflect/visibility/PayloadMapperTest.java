/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.reflect.visibility;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.json.JsonValue;

import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.runtime.manager.test.MethodsHolder;

class PayloadMapperTest {

    private final PayloadMapper extractorFactory = new PayloadMapper((a, b) -> {
    });

    private final ParameterModelService service = new ParameterModelService(new PropertyEditorRegistry());

    @Test
    void simpleDirectValue() throws NoSuchMethodException {
        final List<ParameterMeta> params = service
                .buildParameterMetas(MethodsHolder.class.getMethod("primitives", String.class, String.class, int.class),
                        "def", new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test")));

        final Map<String, String> payload = new HashMap<>();
        payload.put("url", "http://foo");

        final JsonValue value = extractorFactory.visitAndMap(params, payload);
        assertEquals("{\"url\":\"http://foo\"}", value.toString());
    }

    @Test
    void simpleListValue() throws NoSuchMethodException {
        final List<ParameterMeta> params = service
                .buildParameterMetas(MethodsHolder.class.getMethod("collections", List.class, List.class, Map.class),
                        "def", new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test")));

        final Map<String, String> payload = new HashMap<>();
        payload.put("ports[0]", "1");
        payload.put("ports[1]", "2");

        final JsonValue value = extractorFactory.visitAndMap(params, payload);
        assertEquals("{\"ports\":[1,2]}", value.toString());
    }

    @Test
    void complex() throws NoSuchMethodException {
        final List<ParameterMeta> params = service
                .buildParameterMetas(Model.class.getConstructor(Complex.class), "def",
                        new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test")));

        final Map<String, String> payload = new TreeMap<>();
        payload.put("configuration.objects[0].simple", "s");
        payload.put("configuration.objects[0].otherObjects[0].value", "o11");
        payload.put("configuration.objects[0].otherObjects[1].value", "o12");

        payload.put("configuration.objects[1].otherObjects[0].value", "o21");

        payload.put("configuration.objects[2].strings[0]", "s1");
        payload.put("configuration.objects[2].strings[1]", "s2");

        payload.put("configuration.simple", "rs");
        payload.put("configuration.strings[0]", "rs1");
        payload.put("configuration.other.value", "done");

        final JsonValue value = extractorFactory.visitAndMap(params, payload);
        assertEquals("{\"configuration\":{\"objects\":[{\"otherObjects\":[{\"value\":\"o11\"},{\"value\":\"o12\"}],"
                + "\"simple\":\"s\"},{\"otherObjects\":[{\"value\":\"o21\"}]},{\"strings\":[\"s1\",\"s2\"]}],"
                + "\"other\":{\"value\":\"done\"},\"simple\":\"rs\",\"strings\":[\"rs1\"]}}", value.toString());
    }

    @Test
    void filters() throws NoSuchMethodException {
        final List<ParameterMeta> params = service
                .buildParameterMetas(
                        MethodsHolder.class.getMethod("visibility", MethodsHolder.FilterConfiguration.class), "def",
                        new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test")));
        final Map<String, String> payload = new TreeMap<>();
        payload.put("configuration.logicalOpType", "ALL");
        payload.put("configuration.filters[0].columnName", "col0");
        payload.put("configuration.filters[0].operator", "IS_NULL");
        payload.put("configuration.filters[0].value", "");
        final JsonValue value = extractorFactory.visitAndMap(params, payload);
        System.out.println(value.toString());
        assertEquals(
                "{\"configuration\":{\"filters\":[{\"columnName\":\"col0\",\"operator\":\"IS_NULL\",\"value\":\"\"}],\"logicalOpType\":\"ALL\"}}",
                value.toString());
    }

    public static class OtherObject {

        @Option
        private String value;
    }

    public static class SomeObject {

        @Option
        private String simple;

        @Option
        private List<OtherObject> otherObjects;

        @Option
        private List<String> strings;
    }

    public static class Complex {

        @Option
        private String simple;

        @Option
        private List<SomeObject> objects;

        @Option
        private List<String> strings;

        @Option
        private OtherObject other;
    }

    public static class Model {

        public Model(@Option("configuration") final Complex complex) {
            // no-op
        }
    }
}
