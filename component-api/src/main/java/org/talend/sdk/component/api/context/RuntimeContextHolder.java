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
package org.talend.sdk.component.api.context;

import java.io.Serializable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RuntimeContextHolder implements Serializable {

    private final String connectorId;

    private final Map<String, Object> map;

    public Object getGlobal(final String key) {
        return map.get(key);
    }

    public void setGlobal(final String key, final Object value) {
        map.put(key, value);
    }

    public Object get(final String key) {
        return map.get(connectorId + "_" + key);
    }

    public void set(final String key, final Object value) {
        map.put(connectorId + "_" + key, value);
    }

}
