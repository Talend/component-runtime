/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.manager.util;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.runtime.manager.ParameterMeta.Type.ARRAY;
import static org.talend.sdk.component.runtime.manager.ParameterMeta.Type.OBJECT;
import static org.talend.sdk.component.runtime.manager.ParameterMeta.Type.STRING;

import java.util.Collection;
import java.util.List;

import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.manager.ParameterMeta;

class DefaultValueInspectorTest {

    @Test
    void createListInstance() {
        final DefaultValueInspector inspector = new DefaultValueInspector();
        final DefaultValueInspector.Instance demoInstance = inspector
                .createDemoInstance(new Wrapper(), new ParameterMeta(null,
                        new JohnzonParameterizedType(List.class, Foo.class), ARRAY, "foos", "foos", null,
                        singletonList(new ParameterMeta(null, Foo.class, OBJECT, "foos[${index}]", "foos[${index}]",
                                null,
                                singletonList(new ParameterMeta(null, Foo.class, STRING, "foos[${index}].name", "name",
                                        null, emptyList(), null, emptyMap(), false)),
                                null, emptyMap(), false)),
                        null, emptyMap(), false));
        assertNotNull(demoInstance.getValue());
        assertTrue(demoInstance.isCreated());
        assertTrue(Collection.class.isInstance(demoInstance.getValue()));
        final Collection<?> list = Collection.class.cast(demoInstance.getValue());
        assertEquals(1, list.size());
        final Object first = list.iterator().next();
        assertNotNull(first);
        assertTrue(Foo.class.isInstance(first));
        assertNull(Foo.class.cast(first).name);
    }

    public static class Foo {

        private String name;
    }

    public static class Wrapper {

        private List<Foo> foos;
    }
}
