/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit;

import static org.junit.Assert.assertEquals;
import static org.talend.sdk.component.junit.SimpleFactory.configurationByExample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.talend.sdk.component.api.configuration.Option;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

public class SimpleFactoryTest {

    @Test
    public void instantiate() {
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
    public void noValue() {
        final Flat flat = new Flat();
        assertEquals(new HashMap<String, String>() {

            {
                put("configuration.age", "0");
            }
        }, configurationByExample(flat));
    }

    @Test
    public void nestedObject() {
        final WithNested root = new WithNested();
        root.flat = new Flat();
        root.flat.name = "foo";

        assertEquals(new HashMap<String, String>() {

            {
                put("configuration.flat.name", "foo");
                put("configuration.flat.age", "0");
            }
        }, configurationByExample(root));
    }

    @Test
    public void nestedList() {
        final WithList root = new WithList();
        root.list = new ArrayList<>();
        root.list.add("a");
        root.list.add("b");

        assertEquals(new HashMap<String, String>() {

            {
                put("configuration.list[0]", "a");
                put("configuration.list[1]", "b");
            }
        }, configurationByExample(root));
    }

    @Test
    public void listOfObject() {
        final ListOfObjects root = new ListOfObjects();
        root.list = new ArrayList<>();
        root.list.add(new Flat("a", 1));
        root.list.add(new Flat("b", 2));

        assertEquals(new HashMap<String, String>() {

            {
                put("configuration.list[0].name", "a");
                put("configuration.list[0].age", "1");

                put("configuration.list[1].name", "b");
                put("configuration.list[1].age", "2");
            }
        }, configurationByExample(root));
    }

    @Test
    public void listOfObjectWithNested() {
        final ListOfObjectWithNested root = new ListOfObjectWithNested();
        root.list = new ArrayList<>();
        root.list.add(new WithNested(new Flat("a", 1)));
        root.list.add(new WithNested(new Flat("b", 2)));

        assertEquals(new HashMap<String, String>() {

            {
                put("configuration.list[0].flat.name", "a");
                put("configuration.list[0].flat.age", "1");

                put("configuration.list[1].flat.name", "b");
                put("configuration.list[1].flat.age", "2");
            }
        }, configurationByExample(root));
    }

    @Test
    public void listOfListObjectWithNested() {
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
                put("configuration.list[0].list[0].flat.name", "a");
                put("configuration.list[0].list[0].flat.age", "1");

                put("configuration.list[0].list[1].flat.name", "b");
                put("configuration.list[0].list[1].flat.age", "2");

                put("configuration.list[1].list[0].flat.name", "c");
                put("configuration.list[1].list[0].flat.age", "3");

                put("configuration.list[1].list[1].flat.name", "d");
                put("configuration.list[1].list[1].flat.age", "4");
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
}
