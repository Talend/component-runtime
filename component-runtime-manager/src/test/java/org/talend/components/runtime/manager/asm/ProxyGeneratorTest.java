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
package org.talend.components.runtime.manager.asm;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import org.junit.Test;
import org.talend.component.api.service.Service;
import org.talend.components.runtime.manager.serialization.DynamicContainerFinder;

public class ProxyGeneratorTest {

    @Test
    public void serialization() throws Exception {
        try {
            assertFalse(Serializable.class.isInstance(new CatService())); // if this fails the whole test is pointless

            final ProxyGenerator generator = new ProxyGenerator();
            final Class<?> proxyType = generator.generateProxy(Thread.currentThread().getContextClassLoader(), CatService.class,
                    "test", CatService.class.getName());
            assertNotNull(proxyType);

            final Object proxy = proxyType.getConstructor().newInstance();
            assertProxy(proxy);
            DynamicContainerFinder.SERVICES.put(CatService.class, proxy);
            DynamicContainerFinder.LOADERS.put("test", Thread.currentThread().getContextClassLoader());

            // now we did all the sanity checks let's do a round trip
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (final ObjectOutputStream oos = new ObjectOutputStream(out)) {
                oos.writeObject(proxy);
            }
            try (final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
                final Object deserialized = ois.readObject();
                assertProxy(deserialized);
                assertSame(deserialized, proxy);
            }
        } finally {
            DynamicContainerFinder.LOADERS.clear();
            DynamicContainerFinder.SERVICES.remove(CatService.class);
        }
    }

    private void assertProxy(final Object proxy) throws Exception {
        assertThat(proxy, instanceOf(CatService.class));
        assertEquals("123[4]", CatService.class.cast(proxy).cat(1, "2", "3", singletonList("4")));
        assertEquals(CatService.class.getName() + "$$TalendServiceProxy", proxy.getClass().getName());

        assertTrue(Serializable.class.isInstance(proxy));
        try {
            proxy.getClass().getMethod("writeReplace");
        } catch (final NoSuchMethodException nsm) {
            fail("proxy should be serializable");
        }
    }

    @Service
    public static class CatService {

        public String cat(final int v1, final String v2, final Object v3, final List<String> v4) {
            return v1 + v2 + v3 + v4;
        }
    }
}
