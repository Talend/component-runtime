/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.transform.service;

import static lombok.AccessLevel.PRIVATE;

import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.service.DefaultServices;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class ServiceLookup {

    public static <T> T lookup(final ComponentManager manager, final String plugin, final Class<T> type) {
        if (plugin == null) {
            return type.cast(DefaultServices.lookup(type.getName()));
        }
        return type
                .cast(manager.findPlugin(plugin).get().get(ComponentManager.AllServices.class).getServices().get(type));
    }
}
