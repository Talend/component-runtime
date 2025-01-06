/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager;

import org.talend.sdk.component.spi.component.ComponentExtension;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ComponentInstanceImpl implements ComponentExtension.ComponentInstance {

    private final Object instance;

    private final String plugin;

    private final String family;

    private final String name;

    @Override
    public Object instance() {
        return instance;
    }

    @Override
    public String plugin() {
        return plugin;
    }

    @Override
    public String family() {
        return family;
    }

    @Override
    public String name() {
        return name;
    }
}
