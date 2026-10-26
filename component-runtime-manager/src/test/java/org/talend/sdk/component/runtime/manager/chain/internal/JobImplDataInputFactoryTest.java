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
package org.talend.sdk.component.runtime.manager.chain.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.output.InputFactory;

import lombok.AllArgsConstructor;
import lombok.Data;

class JobImplDataInputFactoryTest {

    private static InputFactory newFactory(final String branch, final Object... values) throws Exception {
        final Class<?> type = Class.forName(JobImpl.class.getName() + "$DataInputFactory");
        final Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        final Object factory = constructor.newInstance();
        final Method withInput = type.getDeclaredMethod("withInput", String.class, java.util.Collection.class);
        withInput.setAccessible(true);
        withInput.invoke(factory, branch, java.util.Arrays.asList(values));
        return (InputFactory) factory;
    }

    @Test
    void pojoIsConvertedToRecord() throws Exception {
        final InputFactory factory = newFactory("__default__", new Person("Ada", 36));
        final Object read = factory.read("__default__");
        final Record record = (Record) read;
        assertEquals("Ada", record.getString("name"));
        assertEquals(36, record.getInt("age"));
        assertNull(factory.read("__default__"));
    }

    @Test
    void primitiveIsKept() throws Exception {
        assertEquals(5, newFactory("__default__", 5).read("__default__"));
    }

    @Data
    @AllArgsConstructor
    public static class Person {

        private String name;

        private int age;
    }
}
