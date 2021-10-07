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
package org.talend.sdk.component.runtime.serialization;

import static java.util.Optional.ofNullable;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.talend.sdk.component.api.service.serialization.Serial;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SerializableService implements Serializable, Serial {

    private final String plugin;

    private final String type;

    @Override
    public Object readResolve() throws ObjectStreamException {
        final ContainerFinder containerFinder = ContainerFinder.Instance.get();
        final LightContainer container = containerFinder.find(plugin);
        try {
            return ofNullable(container.findService(container.classloader().loadClass(type)))
                    .orElseThrow(() -> new IllegalStateException(
                            "Didn't find service " + type + ", ensure a ContainerFinder is setup (current="
                                    + containerFinder + ") and " + type + " is registered as a @Service in " + plugin));
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException(type + " not found");
        }
    }
}
