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
package org.talend.components.runtime.base;

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Serializer {

    public static byte[] toBytes(final Object delegate) {
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(ofNullable(delegate.getClass().getClassLoader()).orElse(old));
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(delegate);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        } finally {
            thread.setContextClassLoader(old);
        }
        return baos.toByteArray();
    }
}
