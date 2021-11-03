/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString
public class ContainerComponentRegistry {

    private final Map<String, ComponentFamilyMeta> components = new ConcurrentHashMap<>();

    private final Collection<ServiceMeta> services = new ArrayList<>();

    public ComponentFamilyMeta findComponentFamily(final String pluginId) {
        if (!this.components.containsKey(pluginId)) {
            log.error("Search for a plugin {} that is not registered", pluginId);
        }
        return this.components.get(pluginId);
    }
}
