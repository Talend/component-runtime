/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.proxy;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Proxy;

import org.talend.sdk.component.runtime.manager.service.DefaultServices;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SerializationHandlerReplacer implements Serializable {

    private final String plugin;

    private final String type;

    public Object readResolve() throws ObjectStreamException {
        return Proxy.getInvocationHandler(findProxy());
    }

    private Object findProxy() throws ObjectStreamException {
        if (plugin == null) {
            return DefaultServices.lookup(type);
        }
        return new SerializableService(plugin, type).readResolve();
    }
}
