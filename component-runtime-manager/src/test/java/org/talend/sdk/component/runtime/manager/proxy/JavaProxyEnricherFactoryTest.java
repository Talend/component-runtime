/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.internationalization.Internationalized;
import org.talend.sdk.component.runtime.internationalization.InternationalizationServiceFactory;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;
import org.talend.sdk.component.runtime.manager.test.Serializer;

class JavaProxyEnricherFactoryTest {

    private final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    private final JavaProxyEnricherFactory proxyFactory = new JavaProxyEnricherFactory();

    private final InternationalizationServiceFactory builder =
            new InternationalizationServiceFactory(Locale::getDefault);

    @Test
    void serialization() throws IOException, ClassNotFoundException {

        final Translator translator = builder.create(Translator.class, loader);
        final Translator proxyBased = Translator.class
                .cast(proxyFactory
                        .asSerializable(loader, getClass().getSimpleName(), Translator.class.getName(),
                                translator, true));
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

    @Test
    void serializationOnSerial() throws IOException, ClassNotFoundException {

        final SerialTranslator translatorInstance = builder.create(SerialTranslator.class, loader);
        final SerialTranslator proxyBased = SerialTranslator.class
                .cast(proxyFactory
                        .asSerializable(loader, getClass().getSimpleName(), SerialTranslator.class.getName(),
                                translatorInstance, true));
        assertEquals("serial ok", proxyBased.message());

        DynamicContainerFinder.SERVICES.put(SerialTranslator.class, proxyBased);
        DynamicContainerFinder.LOADERS.put(getClass().getSimpleName(), Thread.currentThread().getContextClassLoader());
        try {
            final SerialTranslator fromApi = Serializer.roundTrip(proxyBased);
            assertEquals(fromApi, proxyBased);
            assertSame(Proxy.getInvocationHandler(fromApi), Proxy.getInvocationHandler(proxyBased));
        } finally {
            DynamicContainerFinder.LOADERS.clear();
            DynamicContainerFinder.SERVICES.remove(Translator.class);
        }
    }

    @Test
    void defaultMethod() {
        final SomeDefault proxyBased = SomeDefault.class
                .cast(proxyFactory
                        .asSerializable(loader, getClass().getSimpleName(), SomeDefault.class.getName(),
                                new SomeDefault() {

                                    @Override
                                    public String get() {
                                        return "ok";
                                    }
                                }));
        assertEquals("ok", proxyBased.get());
    }

    public interface SomeDefault {

        default String get() {
            throw new UnsupportedOperationException();
        }
    }

    @Internationalized
    public interface Translator {

        String message();
    }

    @Internationalized
    public interface SerialTranslator extends Serializable {

        String message();
    }
}
