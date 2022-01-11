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
package org.talend.sdk.component.junit;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

public class RecordAssertsTest {

    @Test(expected = IllegalArgumentException.class)
    public void missingOutput() {
        new RecordAsserts().withAsserts("missing", v -> {
        }).apply(singleton(new HashMap<>()));
    }

    @Test
    public void ok() {
        new RecordAsserts()
                .withAsserts("__default__", v -> assertTrue(v.isEmpty()))
                .apply(singleton(new HashMap<String, List<Serializable>>() {

                    {
                        put("__default__", emptyList());
                    }
                }));
    }

    @Test(expected = AssertionError.class)
    public void ko() {
        new RecordAsserts()
                .withAsserts("__default__", v -> assertTrue(v.isEmpty()))
                .apply(singleton(new HashMap<String, List<Serializable>>() {

                    {
                        put("__default__", singletonList("a"));
                    }
                }));
    }
}
