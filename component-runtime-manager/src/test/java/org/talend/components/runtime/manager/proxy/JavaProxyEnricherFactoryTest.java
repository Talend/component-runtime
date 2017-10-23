/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.components.runtime.manager.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.lang.reflect.Proxy;

import org.junit.Test;
import org.talend.component.api.internationalization.Internationalized;
import org.talend.components.runtime.manager.serialization.DynamicContainerFinder;
import org.talend.components.runtime.manager.test.Serializer;
import org.talend.components.runtime.internationalization.InternationalizationServiceFactory;

public class JavaProxyEnricherFactoryTest {

    @Test
    public void serialization() throws IOException, ClassNotFoundException {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final JavaProxyEnricherFactory factory = new JavaProxyEnricherFactory();
        final Translator proxyBased = Translator.class.cast(factory.asSerializable(loader, getClass().getSimpleName(),
                Translator.class.getName(), new InternationalizationServiceFactory().create(Translator.class, loader)));
        assertEquals("ok", proxyBased.message());

        DynamicContainerFinder.SERVICES.put(Translator.class, proxyBased);
        DynamicContainerFinder.LOADERS.put(getClass().getSimpleName(), Thread.currentThread().getContextClassLoader());
        try {
            final Translator fromApi = Serializer.roundTrip(proxyBased);
            assertEquals(fromApi, proxyBased);
            assertSame(Proxy.getInvocationHandler(fromApi), Proxy.getInvocationHandler(proxyBased));
        } finally {
            DynamicContainerFinder.LOADERS.clear();
            DynamicContainerFinder.SERVICES.remove(Translator.class);
        }
    }

    @Internationalized
    public interface Translator {

        String message();
    }
}
