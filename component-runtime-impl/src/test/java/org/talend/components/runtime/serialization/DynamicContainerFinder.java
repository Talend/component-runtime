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

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicContainerFinder implements ContainerFinder {

    public static final Map<String, ClassLoader> LOADERS = new ConcurrentHashMap<>();

    @Override
    public LightContainer find(final String plugin) {
        return new LightContainer() {

            @Override
            public ClassLoader classloader() {
                return plugin == null ? Thread.currentThread().getContextClassLoader() : LOADERS.get(plugin);
            }

            @Override
            public <T> T findService(final Class<T> key) {
                try {
                    return key.isInterface() ? null : key.getConstructor().newInstance();
                } catch (final InstantiationException | IllegalAccessException | InvocationTargetException
                        | NoSuchMethodException e) {
                    return null; // if there is a constructor let the default be handled in the caller
                }
            }
        };
    }
}
