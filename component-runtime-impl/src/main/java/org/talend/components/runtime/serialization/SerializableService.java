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

import java.io.ObjectStreamException;
import java.io.Serializable;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SerializableService implements Serializable {

    private final String plugin;

    private final String type;

    public Object readResolve() throws ObjectStreamException {
        final ContainerFinder containerFinder = ContainerFinder.Instance.get();
        final LightContainer container = containerFinder.find(plugin);
        try {
            return ofNullable(container.findService(container.classloader().loadClass(type)))
                    .orElseThrow(() -> new IllegalStateException(
                            "Didn't find service " + type + ", ensure a ContainerFinder is setup (current=" + containerFinder
                                    + ") and " + type + " is registered as a @Service in " + plugin));
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException(type + " not found");
        }
    }
}
