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
package org.talend.components.runtime.manager.serialization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.talend.components.runtime.serialization.ContainerFinder;
import org.talend.components.runtime.serialization.LightContainer;

public class DynamicContainerFinder implements ContainerFinder {

    public static final Map<String, ClassLoader> LOADERS = new ConcurrentHashMap<>();

    public static final Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();

    @Override
    public LightContainer find(final String plugin) {
        return new LightContainer() {

            @Override
            public ClassLoader classloader() {
                return LOADERS.get(plugin);
            }

            @Override
            public <T> T findService(final Class<T> key) {
                return key.cast(SERVICES.get(key));
            }
        };
    }
}
