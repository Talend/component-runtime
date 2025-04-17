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
package org.talend.sdk.component.server.service;

import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.NotificationOptions;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.talend.sdk.component.path.PathFactory;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.extension.api.ExtensionRegistrar;
import org.talend.sdk.component.server.extension.api.action.Action;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentId;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.DependencyDefinition;
import org.talend.sdk.component.server.front.model.Link;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ExtensionComponentMetadataManager {

    private static final String EXTENSION_MARKER = "extension::";

    @Inject
    private Event<ExtensionRegistrar> extensionsEvent;

    private final Collection<Runnable> waiters = new ArrayList<>();

    private final Map<String, ComponentDetail> details = new LinkedHashMap<>();

    private final Map<String, ConfigTypeNode> configurations = new LinkedHashMap<>();

    private final Map<String, DependencyDefinition> dependencies = new LinkedHashMap<>();

    private final Map<ActionKey, Action> actions = new LinkedHashMap<>();

    public void startupLoad(@Observes @Initialized(ApplicationScoped.class) final Object start,
            final ComponentServerConfiguration configuration) {
        try {
            extensionsEvent.fireAsync(new ExtensionRegistrar() {

                @Override
                public void registerAwait(final Runnable waiter) {
                    synchronized (waiters) {
                        waiters.add(waiter);
                    }
                }

                @Override
                public void registerActions(final Collection<Action> userActions) {
                    final Map<ActionKey, Action> actionMap =
                            userActions
                                    .stream()
                                    .collect(toMap(
                                            it -> new ActionKey(it.getReference().getFamily(),
                                                    it.getReference().getType(), it.getReference().getName()),
                                            identity()));
                    synchronized (actions) {
                        actions.putAll(actionMap);
                    }
                }

                @Override
                public void registerComponents(final Collection<ComponentDetail> components) {
                    final Map<String, ComponentDetail> mapped = components
                            .stream()
                            .map(it -> new ComponentDetail(
                                    new ComponentId(it.getId().getId(), EXTENSION_MARKER + it.getId().getFamilyId(),
                                            EXTENSION_MARKER + it.getId().getPlugin(),
                                            EXTENSION_MARKER + it.getId().getPluginLocation(), it.getId().getFamily(),
                                            it.getId().getName(), it.getId().getDatabaseMapping()),
                                    it.getDisplayName(), it.getIcon(), it.getType(), it.getVersion(),
                                    it.getProperties(), it.getActions(), it.getInputFlows(), it.getOutputFlows(),
                                    Stream
                                            .concat(createBuiltInLinks(it),
                                                    it.getLinks() == null ? Stream.empty() : it.getLinks().stream())
                                            .distinct()
                                            .collect(toList()),
                                    singletonMap("mapper::infinite", "false")))
                            .collect(toMap(it -> it.getId().getId(), identity(), (a, b) -> {
                                throw new IllegalArgumentException(a + " and " + b + " are conflicting");
                            }, LinkedHashMap::new));
                    synchronized (details) {
                        details.putAll(mapped);
                    }
                }

                @Override
                public void registerConfigurations(final Collection<ConfigTypeNode> configs) {
                    final Map<String, ConfigTypeNode> mapped =
                            configs.stream().collect(toMap(ConfigTypeNode::getId, identity(), (a, b) -> {
                                throw new IllegalArgumentException(a + " and " + b + " are conflicting");
                            }, LinkedHashMap::new));
                    synchronized (configurations) {
                        configurations.putAll(mapped);
                    }
                }

                @Override
                public void registerDependencies(final Map<String, DependencyDefinition> deps) {
                    synchronized (dependencies) {
                        dependencies.putAll(deps);
                    }
                }

                @Override
                public void createExtensionJarIfNotExist(final String groupId, final String artifactId,
                        final String version, final Consumer<JarOutputStream> creator) {
                    final String m2 = configuration
                            .getExtensionMavenRepository()
                            .orElseThrow(
                                    () -> new IllegalArgumentException("No extension maven repository configured"));
                    final Path path = PathFactory.get(m2);
                    final Path jar = path
                            .resolve(groupId.replace('.', '/') + '/' + artifactId + '/' + version + '/' + artifactId
                                    + '-' + version + ".jar");
                    if (Files.exists(jar)) {
                        return;
                    }
                    try {
                        if (!Files.isDirectory(jar.getParent())) {
                            Files.createDirectories(jar.getParent());
                        }
                    } catch (final IOException e) {
                        throw new IllegalArgumentException(
                                "Can't create extension artifact " + groupId + ':' + artifactId + ':' + version, e);
                    }
                    try (final JarOutputStream stream = new JarOutputStream(new BufferedOutputStream(
                            Files.newOutputStream(jar, StandardOpenOption.CREATE, StandardOpenOption.WRITE)))) {
                        creator.accept(stream);
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }, NotificationOptions.ofExecutor(r -> {
                final Thread thread = new Thread(r,
                        ExtensionComponentMetadataManager.this.getClass().getName() + "-extension-registrar");
                thread.start();
            })).toCompletableFuture().get(configuration.getExtensionsStartupTimeout(), MILLISECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        } catch (final TimeoutException e) {
            throw new IllegalStateException("Can't initialize extensions withing "
                    + MILLISECONDS.toSeconds(configuration.getExtensionsStartupTimeout()) + "s");
        }
    }

    private Stream<Link> createBuiltInLinks(final ComponentDetail componentDetail) {
        return Stream
                .of(new Link("Detail", "/component/details?identifiers=" + componentDetail.getId().getId(),
                        MediaType.APPLICATION_JSON));
    }

    public String getFamilyIconFor(final String familyId) {
        if (!isExtensionEntity(familyId)) {
            throw new IllegalArgumentException(familyId + " is not a virtual family");
        }
        return familyId.replace("::", "_");
    }

    public boolean isExtensionEntity(final String id) {
        return details.containsKey(id) || id.startsWith(EXTENSION_MARKER);
    }

    public Optional<Action> getAction(final String family, final String type, final String name) {
        waitAndClearWaiters();
        return ofNullable(actions.get(new ActionKey(family, type, name)));
    }

    public Collection<Action> getActions() {
        waitAndClearWaiters();
        return actions.values();
    }

    public Collection<ConfigTypeNode> getConfigurations() {
        waitAndClearWaiters();
        return configurations.values();
    }

    public Collection<ComponentDetail> getDetails() {
        waitAndClearWaiters();
        return details.values();
    }

    public Optional<ComponentDetail> findComponentById(final String id) {
        waitAndClearWaiters();
        return ofNullable(details.get(id));
    }

    public DependencyDefinition getDependenciesFor(final String id) {
        waitAndClearWaiters();
        return dependencies.get(id);
    }

    private void waitAndClearWaiters() {
        if (waiters.isEmpty()) {
            return;
        }
        synchronized (waiters) {
            if (waiters.isEmpty()) {
                return;
            }
            waiters.forEach(Runnable::run);
            waiters.clear();
        }
    }

    /*
     * private ConfigTypeNode findFamily(final ConfigTypeNode node, final Map<String, ConfigTypeNode> configs) {
     * if (node.getParentId() == null) {
     * return node;
     * }
     * String parentId = node.getParentId();
     * while (parentId != null) {
     * final ConfigTypeNode parent = configs.get(parentId);
     * if (parent == null) {
     * return null; // error
     * }
     * parentId = parent.getParentId();
     * if (parentId == null) {
     * return parent;
     * }
     * }
     * return null; // error
     * }
     */

    private static class ActionKey {

        private final String family;

        private final String type;

        private final String name;

        private final int hash;

        private ActionKey(final String family, final String type, final String name) {
            this.family = family;
            this.type = type;
            this.name = name;
            this.hash = Objects.hash(family, type, name);
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
            return hash == actionKey.hash && family.equals(actionKey.family) && type.equals(actionKey.type)
                    && name.equals(actionKey.name);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
