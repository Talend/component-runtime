/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LocalConfigurationService implements LocalConfiguration, Serializable {

    private final Collection<LocalConfiguration> rawDelegates;

    private final String plugin;

    @Override
    public String get(final String key) {
        return rawDelegates
                .stream()
                .map(d -> ofNullable(d.get(plugin + "." + key))
                        .orElseGet(() -> d.get(plugin + "_" + key.replace('.', '_'))))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Set<String> keys() {
        return rawDelegates
                .stream()
                .flatMap(d -> d.keys().stream())
                .filter(k -> k.startsWith(plugin + '.'))
                .map(k -> k.substring(plugin.length() + 1))
                .collect(toSet());
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, LocalConfiguration.class.getName());
    }
}
