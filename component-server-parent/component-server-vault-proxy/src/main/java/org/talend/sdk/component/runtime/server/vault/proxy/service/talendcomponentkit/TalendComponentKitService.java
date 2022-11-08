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
package org.talend.sdk.component.runtime.server.vault.proxy.service.talendcomponentkit;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheResult;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;

import org.talend.sdk.component.runtime.server.vault.proxy.service.DecryptedValue;
import org.talend.sdk.component.runtime.server.vault.proxy.service.VaultService;
import org.talend.sdk.component.runtime.server.vault.proxy.service.http.Http;
import org.talend.sdk.component.runtime.server.vault.proxy.service.jcache.VaultProxyCacheKeyGenerator;
import org.talend.sdk.component.runtime.server.vault.proxy.service.jcache.VaultProxyCacheResolver;
import org.talend.sdk.component.server.front.model.ActionItem;
import org.talend.sdk.component.server.front.model.ActionList;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

@ApplicationScoped
@CacheDefaults(cacheKeyGenerator = VaultProxyCacheKeyGenerator.class,
        cacheResolverFactory = VaultProxyCacheResolver.class)
public class TalendComponentKitService {

    @Inject
    private VaultService vault;

    @Inject
    private Clock clock;

    @Inject
    @Http(Http.Type.TALEND_COMPONENT_KIT)
    private WebTarget client;

    @Inject
    private TalendComponentKitService self;

    public CompletionStage<Map<String, String>> decrypt(final Collection<SimplePropertyDefinition> properties,
            final Map<String, String> original, final HttpHeaders headers) {
        final List<String> cipheredKeys = findCipheredKeys(properties, original);
        return vault
                .get(cipheredKeys.stream().map(original::get).collect(Collectors.toList()), clock.millis(), headers)
                .thenApply(decrypted -> original
                        .entrySet()
                        .stream()
                        .collect(toMap(Map.Entry::getKey,
                                e -> Optional
                                        .of(cipheredKeys.indexOf(e.getKey()))
                                        .filter(idx -> idx >= 0)
                                        .map(decrypted::get)
                                        .map(DecryptedValue::getValue)
                                        .orElseGet(() -> original.get(e.getKey())))));
    }

    private List<String> findCipheredKeys(final Collection<SimplePropertyDefinition> properties,
            final Map<String, String> original) {
        return original
                .keySet()
                .stream()
                .filter(key -> findDefinition(properties, key)
                        .map(SimplePropertyDefinition::getMetadata)
                        .map(m -> Boolean.parseBoolean(m.get("ui::credential")))
                        .orElse(false))
                .collect(toList());
    }

    @CacheResult
    public CompletionStage<Collection<SimplePropertyDefinition>> getComponentSpec(final String id) {
        return client
                .path("component/details")
                .queryParam("identifiers", id)
                .queryParam("language", "en")
                .request(APPLICATION_JSON_TYPE)
                .rx()
                .get(ComponentDetailList.class)
                .thenApply(list -> list.getDetails().iterator().next().getProperties());
    }

    public CompletionStage<Collection<SimplePropertyDefinition>> getConfigurationSpec(final String id) {
        return client
                .path("configurationtype/details")
                .queryParam("identifiers", id)
                .queryParam("language", "en")
                .request(APPLICATION_JSON_TYPE)
                .rx()
                .get(ConfigTypeNodes.class)
                .thenApply(list -> list.getNodes().values().iterator().next().getProperties());
    }

    @CacheResult
    public CompletionStage<Collection<SimplePropertyDefinition>> getActionSpec(final String family, final String type,
            final String action) {
        return self
                .getActions()
                .thenApply(it -> it
                        .getItems()
                        .stream()
                        .filter(item -> Objects.equals(item.getComponent(), family)
                                && Objects.equals(item.getName(), action) && Objects.equals(item.getType(), type))
                        .findFirst()
                        .map(ActionItem::getProperties)
                        .orElse(null));
    }

    @CacheResult
    CompletionStage<ActionList> getActions() {
        return client
                .path("action/index")
                .queryParam("language", "en")
                .request(APPLICATION_JSON_TYPE)
                .rx()
                .get(ActionList.class);
    }

    private Optional<SimplePropertyDefinition> findDefinition(final Collection<SimplePropertyDefinition> props,
            final String key) {
        final String lookupKey = getLookupKey(key);
        return props.stream().filter(it -> it.getPath().equals(lookupKey)).findFirst();
    }

    private String getLookupKey(final String key) {
        String lookupKey = key;
        while (true) {
            final int start = lookupKey.indexOf('[');
            final int end = lookupKey.indexOf(']');
            if (start < 0 || end < 0 || end < start) {
                break;
            }
            final String indexStr = lookupKey.substring(start + 1, end);
            try {
                Integer.parseInt(indexStr);
            } catch (final NumberFormatException nfe) {
                break;
            }
            lookupKey = lookupKey.substring(0, start + 1) + "${index}" + lookupKey.substring(end);
        }
        return lookupKey;
    }
}
