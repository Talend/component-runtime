/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.talend.sdk.component.runtime.manager.test.Serializer.roundTrip;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.cache.LocalCache;
import org.talend.sdk.component.api.service.injector.Injector;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;

class InjectorImplTest {

    private Injector injector;

    @BeforeEach
    void init() {
        injector = new InjectorImpl("LocalCacheServiceTest",
                singletonMap(LocalCache.class, new LocalCacheService("LocalCacheServiceTest")));
        DynamicContainerFinder.LOADERS.put("LocalCacheServiceTest", Thread.currentThread().getContextClassLoader());
        DynamicContainerFinder.SERVICES.put(Injector.class, injector);
    }

    @AfterEach
    void destroy() {
        DynamicContainerFinder.LOADERS.remove("LocalCacheServiceTest");
        DynamicContainerFinder.SERVICES.remove(Injector.class);
    }

    @Test
    void serialize() throws IOException, ClassNotFoundException {
        assertNotNull(roundTrip(injector));
    }

    @Test
    void inject() {
        final Injected instance = new Injected();
        injector.inject(instance);
        assertNotNull(instance.cache);
    }

    public static class Injected {

        @Service
        private LocalCache cache;
    }
}
