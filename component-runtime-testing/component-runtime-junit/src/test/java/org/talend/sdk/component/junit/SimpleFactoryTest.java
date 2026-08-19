/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.talend.sdk.component.junit.SimpleFactory.configurationByExample;

import java.net.URI;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

class SimpleFactoryTest {

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
    void toQuery() {
        final Flat flat = new Flat();
        flat.age = 31;
        flat.name = "http://u:p@foo.com?test=value";
        final String queryString = configurationByExample().forInstance(flat).configured().toQueryString();
        assertEquals("configuration.age=31&configuration.name=http%3A%2F%2Fu%3Ap%40foo.com%3Ftest%3Dvalue",
                queryString);
        assertEquals("configuration.age=31&configuration.name=http://u:p@foo.com?test=value",
                URI.create("test://foo?" + queryString).getQuery());
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class Flat {

        @Option
        private String name;

        @Option
        private int age;
    }
}
