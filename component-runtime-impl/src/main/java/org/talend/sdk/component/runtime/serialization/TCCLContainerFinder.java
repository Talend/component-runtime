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

public class TCCLContainerFinder implements ContainerFinder {

    private static final LightContainer DELEGATE = new LightContainer() {

        @Override
        public ClassLoader classloader() {
            return ofNullable(Thread.currentThread().getContextClassLoader())
                    .orElseGet(ClassLoader::getSystemClassLoader);
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
