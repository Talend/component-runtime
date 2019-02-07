/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.runtime.server.vault.proxy.service.DecryptedValue;
import org.talend.sdk.component.runtime.server.vault.proxy.service.VaultService;
import org.talend.sdk.component.runtime.server.vault.proxy.service.http.Http;
import org.talend.sdk.component.server.front.model.ActionList;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.Environment;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.Data;

@ApplicationScoped
public class TalendComponentKitService {

    private final Map<String, Collection<SimplePropertyDefinition>> componentSpecs = new ConcurrentHashMap<>();

    private final Map<String, Collection<SimplePropertyDefinition>> configurationSpecs = new ConcurrentHashMap<>();

    private final Map<ActionKey, Collection<SimplePropertyDefinition>> actionSpecs = new ConcurrentHashMap<>();

    @Inject
    @Http(Http.Type.TALEND_COMPONENT_KIT)
    private WebTarget client;

    @Inject
    @ConfigProperty(name = "talend.vault.cache.talendcomponentkit.cacheCheckDelay", defaultValue = "30000")
    private Long cacheCheckDelay;

    @Inject
    private VaultService vault;

    @Inject
    private Clock clock;

    private volatile long lastUpdated = System.currentTimeMillis();

    private ScheduledExecutorService executor;

    private ScheduledFuture<?> refreshTask;

    @PostConstruct
    private void init() {
        executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            private final ThreadGroup group = ofNullable(System.getSecurityManager())
                    .map(SecurityManager::getThreadGroup)
                    .orElseGet(() -> Thread.currentThread().getThreadGroup());

            @Override
            public Thread newThread(final Runnable r) {
                final Thread t = new Thread(group, r, "talend-vault-talendcomponentkit-refresh", 0);
                t.setDaemon(false);
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        });
        refreshTask = executor.scheduleWithFixedDelay(this::ensureCacheIsUpToDate, 0, cacheCheckDelay, MILLISECONDS);
    }

    @PreDestroy
    private void destroy() {
        ofNullable(refreshTask).ifPresent(s -> s.cancel(true));
        ofNullable(executor).ifPresent(ExecutorService::shutdownNow); // we don't care much
    }

    public CompletionStage<Map<String, String>> decrypt(final Collection<SimplePropertyDefinition> properties,
            final Map<String, String> original) {
        final List<String> cipheredKeys = findCipheredKeys(properties, original);
        return vault
                .get(cipheredKeys.stream().map(original::get).collect(Collectors.toList()), clock.millis())
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

    public CompletionStage<Collection<SimplePropertyDefinition>> getComponentSpec(final String id) {
        return ofNullable(componentSpecs.get(id))
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> client
                        .path("component/details")
                        .queryParam("identifiers", id)
                        .queryParam("language", "en")
                        .request(APPLICATION_JSON_TYPE)
                        .rx()
                        .get(ComponentDetailList.class)
                        .thenApply(list -> {
                            final Collection<SimplePropertyDefinition> props =
                                    list.getDetails().iterator().next().getProperties();
                            componentSpecs.putIfAbsent(id, props);
                            return props;
                        })
                        .toCompletableFuture());
    }

    public CompletionStage<Collection<SimplePropertyDefinition>> getConfigurationSpec(final String id) {
        return ofNullable(configurationSpecs.get(id))
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> client
                        .path("configurationtype/details")
                        .queryParam("identifiers", id)
                        .queryParam("language", "en")
                        .request(APPLICATION_JSON_TYPE)
                        .rx()
                        .get(ConfigTypeNodes.class)
                        .thenApply(list -> {
                            final Collection<SimplePropertyDefinition> props =
                                    list.getNodes().values().iterator().next().getProperties();
                            configurationSpecs.putIfAbsent(id, props);
                            return props;
                        })
                        .toCompletableFuture());
    }

    public CompletionStage<Collection<SimplePropertyDefinition>> getActionSpec(final String family, final String type,
            final String action) {
        final ActionKey key = new ActionKey(family, type, action);
        return ofNullable(actionSpecs.get(key))
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> client
                        .path("action/index")
                        .queryParam("language", "en")
                        .request(APPLICATION_JSON_TYPE)
                        .rx()
                        .get(ActionList.class)
                        .thenApply(list -> {
                            list
                                    .getItems()
                                    .forEach(item -> actionSpecs
                                            .put(new ActionKey(item.getComponent(), item.getType(), item.getName()),
                                                    item.getProperties()));
                            return actionSpecs.get(key);
                        })
                        .toCompletableFuture());
    }

    private void ensureCacheIsUpToDate() { // no need of sync, see init()
        final Environment environment =
                client.path("environment").request(APPLICATION_JSON_TYPE).get(Environment.class);
        // assumes time are synch-ed but not a high assumption
        if (lastUpdated < environment.getLastUpdated().getTime()) {
            Stream.of(componentSpecs, configurationSpecs, actionSpecs).forEach(Map::clear);
            lastUpdated = System.currentTimeMillis();
        }
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

    @Data
    private static class ActionKey {

        private final String family;

        private final String type;

        private final String action;

        private final int hash;

        private ActionKey(final String family, final String type, final String action) {
            this.family = family;
            this.type = type;
            this.action = action;
            this.hash = Objects.hash(family, type, action);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ActionKey actionKey = ActionKey.class.cast(o);
            return Objects.equals(family, actionKey.family) && Objects.equals(type, actionKey.type)
                    && Objects.equals(action, actionKey.action);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
