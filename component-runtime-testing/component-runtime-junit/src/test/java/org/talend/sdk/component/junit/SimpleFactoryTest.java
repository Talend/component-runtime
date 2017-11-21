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

    public static class Flat {

        @Option
        private String name;

        @Option
        private int age;
    }

    public static class WithNested {

        @Option("nested")
        private Flat flat;
    }

    public static class WithList {

        @Option("array")
        private List<String> list;
    }
}
