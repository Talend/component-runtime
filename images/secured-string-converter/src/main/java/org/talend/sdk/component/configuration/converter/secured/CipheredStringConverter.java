/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.configuration.converter.secured;

import static java.util.Optional.ofNullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.Priority;
import javax.enterprise.inject.Vetoed;

import org.eclipse.microprofile.config.spi.Converter;

@Vetoed // loaded by SPI - almost the exact same impl than maven, see org.sonatype.plexus.components.cipher.PBECipher
@Priority(100)
public class CipheredStringConverter implements Converter<String> {

    private static final String SECURE_PREFIX = "secure:";

    private final byte[] masterPassword;

    public CipheredStringConverter() {
        this(readMasterPassword());
    }

    protected CipheredStringConverter(final byte[] pass) {
        masterPassword = pass;
    }

    @Override
    public String convert(final String value) {
        if (value == null || !value.startsWith(SECURE_PREFIX) || !isActive()) {
            return value;
        }
        return new PBECipher().decrypt64(value.substring(SECURE_PREFIX.length()), masterPassword);
    }

    public String cipher(final String value) {
        try {
            return SECURE_PREFIX + new PBECipher().encrypt64(value, masterPassword);
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private boolean isActive() {
        return masterPassword != null;
    }

    private static byte[] readMasterPassword() {
        return ofNullable(System
                .getProperty("talend.component.server.configuration.master_key.location",
                        new File(System.getProperty("meecrowave.base", ""), "conf/master_key").getAbsolutePath()))
                                .map(path -> Paths.get(path))
                                .filter(Files::exists)
                                .map(it -> MasterKey.read(it.toAbsolutePath().toString()))
                                .orElse(null);
    }
}
