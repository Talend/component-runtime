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
import static lombok.AccessLevel.PRIVATE;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import lombok.NoArgsConstructor;

@FunctionalInterface
public interface ContainerFinder {

    LightContainer find(String plugin);

    @NoArgsConstructor(access = PRIVATE)
    final class Instance {

        private static final AtomicReference<ContainerFinder> FINDER = new AtomicReference<>();

        public static boolean isInitialized() {
            return FINDER.get() != null;
        }

        // really a classloader (JVM most of the time) singleton since it is intended to be used by serialization
        public static void set(final Supplier<ContainerFinder> provider) {
            // todo: do we want some safety here or not? normally shouldnt be needed, then Supplier would be needed
            FINDER.set(provider.get());
        }

        public static ContainerFinder get() {
            if (!isInitialized()) {
                synchronized (FINDER) {
                    if (!isInitialized()) {
                        final Iterator<ContainerFinder> loader = ServiceLoader
                                .load(ContainerFinder.class, ContainerFinder.class.getClassLoader()).iterator();
                        if (loader.hasNext()) {
                            FINDER.set(loader.next());
                        }
                    }
                }
            }

            // fallback on TCCL, depending the packaging it can work or not safer to not return null here
            return ofNullable(FINDER.get()).orElseGet(TCCLContainerFinder::new);
        }
    }
}
