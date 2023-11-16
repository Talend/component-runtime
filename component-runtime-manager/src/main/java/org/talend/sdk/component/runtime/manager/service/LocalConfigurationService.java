/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
import java.util.stream.Stream;

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
                .map(d -> read(d, plugin + "." + key))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> rawDelegates // fallback on direct keys
                        .stream()
                        .map(d -> read(d, key))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null));
    }

    @Override
    public Set<String> keys() {
        return rawDelegates
                .stream()
                .flatMap(d -> d.keys().stream())
                .flatMap(k -> !k.startsWith(plugin + '.') ? Stream.of(k)
                        : Stream.of(k, k.substring(plugin.length() + 1)))
                .collect(toSet());
    }

    private String read(final LocalConfiguration d, final String entryKey) {
        return ofNullable(d.get(entryKey)).orElseGet(() -> d.get(entryKey.replace('.', '_')));
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, LocalConfiguration.class.getName());
    }
}
