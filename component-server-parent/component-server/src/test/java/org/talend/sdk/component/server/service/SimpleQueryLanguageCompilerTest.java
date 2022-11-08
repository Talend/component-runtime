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
package org.talend.sdk.component.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.function.Function;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SimpleQueryLanguageCompilerTest {

    private final SimpleQueryLanguageCompiler compiler = new SimpleQueryLanguageCompiler();

    @ParameterizedTest
    @CsvSource({ "id = 5,true", "id != 5,false", "(id = 5),true", "(id != 5),false", "(id = 5),true",
            "(id = 5) AND (metadata = foo::bar),true", "(id = 5) AND (metadata = wrong),false",
            "(id = 5) OR (metadata = wrong),true", "(id = 4) AND (metadata = foo::bar),false" })
    void valid(final String input, final boolean result) {
        assertEquals(result, compiler.compile(input, new HashMap<String, Function<Object, Object>>() {

            {
                put("id", it -> "5");
                put("metadata", it -> "foo::bar");
                put("map", it -> new HashMap<String, String>() {

                    {
                        put("foo", "bar");
                        put("com::plex", "yes/it_is");
                    }
                });
            }
        }).test(null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "id", "id =", "(id = 5", "(id != 5", "id = 5)", "missing = 5",
            "(id = 5) AND (missing = foo::bar)" })
    void invalid(final String input) {
        assertThrows(IllegalArgumentException.class,
                () -> compiler.compile(input, new HashMap<String, Function<String, Object>>() {

                    {
                        put("id", it -> "5");
                        put("metadata", it -> "foo::bar");
                    }
                }));
    }
}
