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
package org.talend.sdk.component.server.tomcat;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.meecrowave.Meecrowave;
import org.junit.jupiter.api.Test;

class GenerateCertificateAndActivateHttpsTest {

    @Test
    void generate() throws Exception {
        final File cert = new File(
                "target/test/GenerateCertificateAndActivateHttpsTest/cert_" + UUID.randomUUID().toString() + ".p12");
        final Map<String, String> systemPropsToSet = new HashMap<>();
        systemPropsToSet.put("talend.component.server.ssl.active", "true");
        systemPropsToSet.put("talend.component.server.ssl.keystore.location", cert.getPath());
        systemPropsToSet.put("talend.component.server.ssl.port", "1234");
        // ensure we don't init there the repo otherwise next tests will be broken (@MonoMeecrowave)
        systemPropsToSet.put("org.talend.sdk.component.server.test.InitTestInfra.skip", "true");

        final Collection<Runnable> reset = systemPropsToSet.entrySet().stream().map(it -> {
            final String property = System.getProperty(it.getKey());
            System.setProperty(it.getKey(), it.getValue());
            if (property == null) {
                return (Runnable) () -> System.clearProperty(it.getKey());
            }
            return (Runnable) () -> System.setProperty(it.getKey(), property);
        }).collect(toList());
        try {
            final Meecrowave.Builder builder = new Meecrowave.Builder();
            assertTrue(cert.exists());
            assertTrue(builder.isSsl());
            assertEquals(1234, builder.getHttpsPort());
            assertEquals("talend", builder.getKeyAlias());
            assertEquals("changeit", builder.getKeystorePass());
            assertEquals("PKCS12", builder.getKeystoreType());
            assertEquals(cert.getAbsolutePath(), builder.getKeystoreFile());
            assertCert(builder);
        } finally {
            reset.forEach(Runnable::run);
        }
    }

    private void assertCert(final Meecrowave.Builder builder) throws Exception {
        final KeyStore keyStore = KeyStore.getInstance(builder.getKeystoreType());
        try (final InputStream inputStream = new FileInputStream(builder.getKeystoreFile())) {
            keyStore.load(inputStream, builder.getKeystorePass().toCharArray());
        }
        final Key key = keyStore.getKey(builder.getKeyAlias(), builder.getKeystorePass().toCharArray());
        assertTrue(PrivateKey.class.isInstance(key));
        final Certificate certificate = keyStore.getCertificate(builder.getKeyAlias());
        assertTrue(X509Certificate.class.isInstance(certificate));
        final Certificate[] chain = keyStore.getCertificateChain(builder.getKeyAlias());
        assertEquals(1, chain.length);
        assertEquals(certificate, chain[0]);
    }
}
