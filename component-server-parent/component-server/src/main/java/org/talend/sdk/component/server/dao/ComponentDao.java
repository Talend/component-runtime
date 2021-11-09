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
package org.talend.sdk.component.server.dao;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;

@ApplicationScoped
public class ComponentDao {

    // for now we ignore reloading but we should add it if it starts to be used (through a listener)
    private Map<String, ComponentFamilyMeta.BaseMeta<?>> data = new ConcurrentHashMap<>();

    public String createOrUpdate(final ComponentFamilyMeta.BaseMeta<?> meta) {
        data.put(meta.getId(), meta);
        return meta.getId();
    }

    public <T extends Lifecycle> ComponentFamilyMeta.BaseMeta<T> findById(final String id) {
        return (ComponentFamilyMeta.BaseMeta<T>) data.get(id);
    }

    public void removeById(final String id) {
        data.remove(id);
    }
}
