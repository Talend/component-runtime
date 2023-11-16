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
package org.talend.sdk.component.server.front.security;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.components.vault.client.VaultClient;

@Dependent
public class SecurityUtils {

    public static final String CREDENTIAL = "tcomp::ui::credential";

    @Inject
    private VaultClient vault;

    public Map<String, String> decrypt(final Collection<ParameterMeta> spec, final Map<String, String> config,
            final String tenant) {
        if (!hasCipheredKeys(config)) {
            return config;
        }

        final List<String> toDecipher = spec.stream()
                .flatMap(s -> flatten(s)
                        .filter(p -> Boolean.parseBoolean(p.getMetadata().getOrDefault(CREDENTIAL, "false"))))
                .map(m -> m.getPath())
                .collect(toList());

        return Stream.concat(vault.decrypt(config.entrySet()
                .stream()
                .filter(e -> toDecipher.contains(e.getKey()))
                .collect(toMap(Entry::getKey, Entry::getValue)), tenant)
                .entrySet()
                .stream(), config.entrySet().stream().filter(e -> !toDecipher.contains(e.getKey())))
                .collect(toMap(Entry::getKey, Entry::getValue));
    }

    public boolean hasCipheredKeys(final Map<String, String> configuration) {
        return configuration.values().stream().anyMatch(v -> v.startsWith("vault:"));
    }

    public List<String> findCipheredKeys(final ParameterMeta meta, final Map<String, String> original) {
        return flatten(meta)
                .filter(p -> Boolean.parseBoolean(p.getMetadata().getOrDefault(CREDENTIAL, "false")))
                .map(m -> m.getPath())
                .collect(toList());
    }

    private Stream<ParameterMeta> flatten(final ParameterMeta meta) {
        if (meta.getNestedParameters() == null || meta.getNestedParameters().isEmpty()) {
            return Stream.of(meta);
        }
        return Stream.concat(meta.getNestedParameters().stream().flatMap(this::flatten), Stream.of(meta));
    }

}
