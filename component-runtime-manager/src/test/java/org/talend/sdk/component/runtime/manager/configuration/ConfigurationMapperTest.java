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
package org.talend.sdk.component.runtime.manager.configuration;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

class ConfigurationMapperTest {

    private final ConfigurationMapper mapper = new ConfigurationMapper();

    private Map<String, String> configurationByExample(final Object instance) {
        return mapper
                .map(new SimpleParameterModelService()
                        .build("configuration.", "configuration.", instance.getClass(), new Annotation[0],
                                new ArrayList<>(singletonList(instance.getClass().getPackage().getName())))
                        .getNestedParameters(), instance);
    }

    @Test
    void instantiate() {
        final Flat flat = new Flat();
        flat.age = 31;
        flat.name = "Tester";
        assertEquals(new HashMap<String, String>() {

            {
                put("configuration.age", "31");
                put("configuration.name", "Tester");
            }
        }, configurationByExample(flat));
    }

    @Test
    void noValue() {
        final Flat flat = new Flat();
        assertEquals(new HashMap<String, String>() {

            {
                put("configuration.age", "0");
            }
        }, configurationByExample(flat));
    }

    @Test
    void nestedObject() {
        final WithNested root = new WithNested();
        root.flat = new Flat();
        root.flat.name = "foo";

        assertEquals(new HashMap<String, String>() {

            {
                put("configuration.nested.name", "foo");
                put("configuration.nested.age", "0");
            }
        }, configurationByExample(root));
    }

    @Test
    void nestedList() {
        final WithList root = new WithList();
        root.list = new ArrayList<>();
        root.list.add("a");
        root.list.add("b");

        assertEquals(new HashMap<String, String>() {

            {
                put("configuration.array[0]", "a");
                put("configuration.array[1]", "b");
            }
        }, configurationByExample(root));
    }

    @Test
    void listOfObject() {
        final ListOfObjects root = new ListOfObjects();
        root.list = new ArrayList<>();
        root.list.add(new Flat("a", 1));
        root.list.add(new Flat("b", 2));

        assertEquals(new HashMap<String, String>() {

            {
                put("configuration.array[0].name", "a");
                put("configuration.array[0].age", "1");

                put("configuration.array[1].name", "b");
                put("configuration.array[1].age", "2");
            }
        }, configurationByExample(root));
    }

    @Test
    void listOfObjectWithNested() {
        final ListOfObjectWithNested root = new ListOfObjectWithNested();
        root.list = new ArrayList<>();
        root.list.add(new WithNested(new Flat("a", 1)));
        root.list.add(new WithNested(new Flat("b", 2)));

        assertEquals(new HashMap<String, String>() {

            {
                put("configuration.array[0].nested.name", "a");
                put("configuration.array[0].nested.age", "1");

                put("configuration.array[1].nested.name", "b");
                put("configuration.array[1].nested.age", "2");
            }
        }, configurationByExample(root));
    }

    @Test
    void listOfListObjectWithNested() {
        final ListOfListWithNested root = new ListOfListWithNested();

        final ListOfObjectWithNested child1 = new ListOfObjectWithNested();
        child1.list = new ArrayList<>();
        child1.list.add(new WithNested(new Flat("a", 1)));
        child1.list.add(new WithNested(new Flat("b", 2)));

        final ListOfObjectWithNested child2 = new ListOfObjectWithNested();
        child2.list = new ArrayList<>();
        child2.list.add(new WithNested(new Flat("c", 3)));
        child2.list.add(new WithNested(new Flat("d", 4)));

        root.list = new ArrayList<>();
        root.list.add(child1);
        root.list.add(child2);

        assertEquals(new HashMap<String, String>() {

            {
                put("configuration.array[0].array[0].nested.name", "a");
                put("configuration.array[0].array[0].nested.age", "1");

                put("configuration.array[0].array[1].nested.name", "b");
                put("configuration.array[0].array[1].nested.age", "2");

                put("configuration.array[1].array[0].nested.name", "c");
                put("configuration.array[1].array[0].nested.age", "3");

                put("configuration.array[1].array[1].nested.name", "d");
                put("configuration.array[1].array[1].nested.age", "4");
            }
        }, configurationByExample(root));
    }

    @Test
    void listOfListObjectWithParameter() {
        ListOfObjectOfListOfObjectWithNestedAndParam root = new ListOfObjectOfListOfObjectWithNestedAndParam();
        ObjectOfListOfObjectWithNestedAndParam child1Level1 = new ObjectOfListOfObjectWithNestedAndParam();

        root.list = new ArrayList<>();
        root.list.add(child1Level1);

        ListOfObjectWithNestedAndParam child1Level2 = new ListOfObjectWithNestedAndParam();
        child1Level2.list = new ArrayList<>();
        child1Level2.list.add(new WithNested(new Flat("a", 1)));
        child1Level2.list.add(new WithNested(new Flat("b", 2)));
        child1Level2.stringValue = "value1";

        child1Level1.object = child1Level2;

        assertEquals(new HashMap<String, String>() {

            {
                put("configuration.array[0].object.array[0].nested.name", "a");
                put("configuration.array[0].object.array[0].nested.age", "1");

                put("configuration.array[0].object.array[1].nested.name", "b");
                put("configuration.array[0].object.array[1].nested.age", "2");

                put("configuration.array[0].object.string", "value1");
            }
        }, configurationByExample(root));
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class Flat {

        @Option
        private String name;

        @Option
        private int age;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class WithNested {

        @Option("nested")
        private Flat flat;
    }

    public static class WithList {

        @Option("array")
        private List<String> list;
    }

    public static class ListOfObjects {

        @Option("array")
        private List<Flat> list;
    }

    public static class ListOfObjectWithNested {

        @Option("array")
        private List<WithNested> list;
    }

    public static class ListOfListWithNested {

        @Option("array")
        List<ListOfObjectWithNested> list;
    }

    public static class ListOfObjectWithNestedAndParam {

        @Option("array")
        private List<WithNested> list;

        @Option("string")
        private String stringValue;
    }

    public static class ObjectOfListOfObjectWithNestedAndParam {

        @Option("object")
        ListOfObjectWithNestedAndParam object;
    }

    public static class ListOfObjectOfListOfObjectWithNestedAndParam {

        @Option("array")
        List<ObjectOfListOfObjectWithNestedAndParam> list;
    }

    private static class SimpleParameterModelService extends ParameterModelService {

        public SimpleParameterModelService() {
            super(new PropertyEditorRegistry());
        }

        private ParameterMeta build(final String name, final String prefix, final Type genericType,
                final Annotation[] annotations, final Collection<String> i18nPackages) {
            return super.buildParameter(name, prefix, null, genericType, annotations, i18nPackages, false,
                    new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test")));
        }
    }
}
