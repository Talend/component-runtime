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
package org.talend.components.runtime.manager.xbean;

import java.io.IOException;
import java.io.InputStream;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class KeepingOpenInputStream extends InputStream {

    private final InputStream delegate;

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return delegate.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return delegate.read(b, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
        return delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public void mark(final int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    @Override
    public void close() throws IOException {
        // no-op
    }
}
