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
package org.talend.sdk.component.server.dao;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import org.talend.sdk.component.runtime.manager.ServiceMeta;

@ApplicationScoped
public class ComponentActionDao {

    private Map<ActionKey, ServiceMeta.ActionMeta> data = new ConcurrentHashMap<>();

    public ActionKey createOrUpdate(final ServiceMeta.ActionMeta meta) {
        final ActionKey key = new ActionKey(meta.getFamily(), meta.getType(), meta.getAction());
        data.put(key, meta);
        return key;
    }

    public ServiceMeta.ActionMeta findBy(final String component, final String type, final String action) {
        return data.get(new ActionKey(component, type, action));
    }

    public void removeById(final ActionKey key) {
        data.remove(key);
    }

    public static class ActionKey {

        private final String component;

        private final String type;

        private final String name;

        private final int hash;

        private ActionKey(final String component, final String type, final String name) {
            this.component = component;
            this.name = name;
            this.type = type;
            this.hash = Objects.hash(component, type, name);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ActionKey other = ActionKey.class.cast(o);
            return Objects.equals(component, other.component) && Objects.equals(type, other.type)
                    && Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return hash;
        }

    }

}
