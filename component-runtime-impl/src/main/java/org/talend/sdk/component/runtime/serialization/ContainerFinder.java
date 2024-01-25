/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.serialization;

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

        // really a classloader (JVM most of the time) singleton since it is intended to
        // be used by serialization
        public static void set(final Supplier<ContainerFinder> provider) {
            // todo: do we want some safety here or not? normally shouldnt be needed, then
            // Supplier would be needed
            FINDER.set(provider.get());
        }

        public static ContainerFinder get() {
            if (!isInitialized()) {
                synchronized (FINDER) {
                    if (!isInitialized()) {
                        final Iterator<ContainerFinder> loader = ServiceLoader
                                .load(ContainerFinder.class, ContainerFinder.class.getClassLoader())
                                .iterator();
                        if (loader.hasNext()) {
                            FINDER.set(loader.next());
                        }
                    }
                }
            }

            // fallback on TCCL, depending the packaging it can work or not safer to not
            // return null here
            return ofNullable(FINDER.get()).orElseGet(TCCLContainerFinder::new);
        }
    }
}
