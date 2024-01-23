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
package org.talend.sdk.component.runtime.manager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.talend.sdk.component.runtime.manager.test.Serializer.roundTrip;

import java.io.IOException;
import java.util.HashMap;
import java.util.function.Supplier;

import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.factory.ObjectFactory;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;

class ObjectFactoryImplTest {

    private ObjectFactory factory;

    @BeforeEach
    void init() {
        factory = new ObjectFactoryImpl("ObjectFactoryImplTest", new PropertyEditorRegistry());
        DynamicContainerFinder.LOADERS.put("ObjectFactoryImplTest", Thread.currentThread().getContextClassLoader());
        DynamicContainerFinder.SERVICES.put(ObjectFactory.class, factory);
    }

    @AfterEach
    void destroy() {
        DynamicContainerFinder.LOADERS.remove("ObjectFactoryImplTest");
        DynamicContainerFinder.SERVICES.remove(ObjectFactory.class);
    }

    @Test
    void serialize() throws IOException, ClassNotFoundException {
        assertEquals(factory, roundTrip(factory));
    }

    @Test
    void createDefaults() {
        assertEquals("test(setter)/0", factory
                .createInstance("org.talend.sdk.component.runtime.manager.service.ObjectFactoryImplTest$Created")
                .ignoreUnknownProperties()
                .withProperties(new HashMap<String, String>() {

                    {
                        put("name", "test");
                        put("age", "30");
                    }
                })
                .create(Supplier.class)
                .get());
    }

    @Test
    void createFields() {
        assertEquals("test/30", factory
                .createInstance("org.talend.sdk.component.runtime.manager.service.ObjectFactoryImplTest$Created")
                .withFieldInjection()
                .withProperties(new HashMap<String, String>() {

                    {
                        put("name", "test");
                        put("age", "30");
                    }
                })
                .create(Supplier.class)
                .get());
    }

    @Test
    void failOnUnknown() {
        assertThrows(IllegalArgumentException.class, () -> factory
                .createInstance("org.talend.sdk.component.runtime.manager.service.ObjectFactoryImplTest$Created")
                .withProperties(new HashMap<String, String>() {

                    {
                        put("name", "test");
                        put("age", "30");
                    }
                })
                .create(Supplier.class));
    }

    public static class Created implements Supplier<String> {

        private String name;

        private int age;

        public void setName(final String name) {
            this.name = name + "(setter)";
        }

        @Override
        public String get() {
            return name + "/" + age;
        }
    }
}
