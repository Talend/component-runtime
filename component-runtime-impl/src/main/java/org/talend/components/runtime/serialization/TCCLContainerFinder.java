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
package org.talend.components.runtime.serialization;

import static java.util.Optional.ofNullable;

public class TCCLContainerFinder implements ContainerFinder {

    private static final LightContainer DELEGATE = new LightContainer() {

        @Override
        public ClassLoader classloader() {
            return ofNullable(Thread.currentThread().getContextClassLoader()).orElseGet(ClassLoader::getSystemClassLoader);
        }

        @Override
        public <T> T findService(final Class<T> key) {
            return null;
        }
    };

    @Override
    public LightContainer find(final String plugin) {
        return DELEGATE;
    }
}
