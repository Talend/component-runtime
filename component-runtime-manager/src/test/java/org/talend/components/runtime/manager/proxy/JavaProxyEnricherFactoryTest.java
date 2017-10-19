// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
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
