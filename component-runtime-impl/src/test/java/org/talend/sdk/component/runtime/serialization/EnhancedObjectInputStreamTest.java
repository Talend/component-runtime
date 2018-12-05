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
package org.talend.sdk.component.runtime.serialization;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

class EnhancedObjectInputStreamTest {

    @Test
    void allSupportedByDefault() {
        assertTrue(EnhancedObjectInputStream.Defaults.SECURITY_FILTER_WHITELIST.test("org.foo.Bar"));
        assertTrue(EnhancedObjectInputStream.Defaults.SECURITY_FILTER_WHITELIST.test("com.foo.Bar"));
        assertTrue(EnhancedObjectInputStream.Defaults.SECURITY_FILTER_WHITELIST.test("sun.foo.Bar"));
        assertTrue(EnhancedObjectInputStream.Defaults.SECURITY_FILTER_WHITELIST.test("jdk.foo.Bar"));
    }

    @Test
    void whitelist() throws Exception {
        final byte[] serializationHeader = new byte[] { (byte) (((short) 0xaced >>> 8) & 0xFF),
                (byte) (((short) 0xaced) & 0xFF), (byte) (((short) 5 >>> 8) & 0xFF), (byte) (((short) 5) & 0xFF) };
        final ObjectStreamClass desc = ObjectStreamClass.lookup(Ser.class);
        assertNotNull(new EnhancedObjectInputStream(new ByteArrayInputStream(serializationHeader),
                Thread.currentThread().getContextClassLoader(), name -> name
                        .equals("org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStreamTest$Ser")) {
        }.resolveClass(desc));
        assertThrows(SecurityException.class, () -> new EnhancedObjectInputStream(
                new ByteArrayInputStream(serializationHeader), Thread.currentThread().getContextClassLoader(),
                name -> name.equals("org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream")) {
        }.resolveClass(desc));
    }

    public static class Ser implements Serializable {
    }
}
