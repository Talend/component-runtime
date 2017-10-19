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
package org.talend.components.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

public class LifecycleSupport implements AutoCloseable {

    private final AtomicBoolean closed = new AtomicBoolean(false);

    @Override
    public void close() throws Exception {
        closed.compareAndSet(false, true);
    }

    public boolean isClosed() {
        return closed.get();
    }

    public void closeIfNeeded(final Runnable task) {
        if (closed.compareAndSet(false, true)) {
            task.run();
        }
    }
}
