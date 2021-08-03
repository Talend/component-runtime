/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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

import java.io.Serializable;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.BaseService;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.runtime.manager.asm.ProxyGenerator;
import org.talend.sdk.component.runtime.serialization.SerializableService;

class ServiceHelperTest {

    @Test
    void generate() throws NoSuchMethodException {

        final ServiceHelper helper = new ServiceHelper(new ProxyGenerator(), Collections.emptyMap());
        final Object service1 =
                helper.createServiceInstance(ServiceHelperTest.class.getClassLoader(), "id", Service1.class);
        Assertions.assertTrue(service1 instanceof Serializable);
        Assertions.assertNotNull(service1.getClass().getDeclaredMethod("writeReplace"));

        final Object service2 =
                helper.createServiceInstance(ServiceHelperTest.class.getClassLoader(), "id", Service2.class);
        Assertions.assertTrue(service2 instanceof Service2);
        Service2 s2 = (Service2) service2;
        Assertions.assertTrue(s2.getSerializationHelper() instanceof SerializableService);
    }

    @Service
    static public class Service1 {
    }

    @Service
    static public class Service2 extends BaseService {
    }
}