/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.server.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.talend.sdk.component.dependencies.maven.MvnCoordinateToFileConverter;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.ServiceMeta;
import org.talend.sdk.component.runtime.manager.util.IdGenerator;
import org.talend.sdk.component.runtime.output.data.AccessorCache;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@ApplicationScoped
public class ComponentManagerService {

    @Inject
    private ComponentServerConfiguration configuration;

    private ComponentManager instance;

    // for now we ignore reloading but we should add it if it starts to be used (through a listener)
    private Map<String, ComponentFamilyMeta.BaseMeta<?>> idMapping;

    private Map<String, ComponentFamilyMeta> familyIdMapping;

    private Map<ActionKey, ServiceMeta.ActionMeta> actionIndex;

    private ScheduledExecutorService cacheEvictorPool;

    private ScheduledFuture<?> evictor;

    void startupLoad(@Observes @Initialized(ApplicationScoped.class) final Object start) {
        // we just want it to be touched
    }

    @PostConstruct
    private void loadComponents() {
        final String mvnRepo = StrSubstitutor.replaceSystemProperties(configuration.mavenRepository());
        // because we will use the default instance
        ofNullable(mvnRepo).ifPresent(repo -> System.setProperty("talend.component.manager.m2.repository", repo));

        instance = ComponentManager.instance();
        final MvnCoordinateToFileConverter mvnCoordinateToFileConverter = new MvnCoordinateToFileConverter();
        // note: we don't want to download anything from the manager, if we need to download any artifact we need
        // to ensure it is controlled (secured) and allowed so don't make it implicit but enforce a first phase
        // where it is cached locally (provisioning solution)
        ofNullable(configuration.componentCoordinates()).orElse(emptySet()).stream().map(mvnCoordinateToFileConverter::toArtifact)
                                                        .map(artifact -> new File(StrSubstitutor.replaceSystemProperties(mvnRepo),
                                                                mvnCoordinateToFileConverter.toPath(artifact)))
                                                        .filter(Objects::nonNull)
                                                        .forEach(plugin -> instance.addPlugin(plugin.getAbsolutePath()));
        ofNullable(configuration.componentRegistry()).map(File::new).filter(File::exists).ifPresent(registry -> {
            final Properties properties = new Properties();
            try (final InputStream is = new FileInputStream(registry)) {
                properties.load(is);
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
            properties.stringPropertyNames()
                      .forEach(name -> instance.addPlugin(new File(mvnRepo,
                              mvnCoordinateToFileConverter
                                      .toPath(mvnCoordinateToFileConverter.toArtifact(properties.getProperty(name))))
                              .getAbsolutePath()));
        });

        cacheEvictorPool = Executors.newScheduledThreadPool(1, new ThreadFactory() {

            @Override
            public Thread newThread(final Runnable r) {
                final Thread thread = new Thread(r);
                thread.setName(getClass().getName() + "-evictor");
                thread.setDaemon(true);
                return thread;
            }
        });
        // trivial mecanism for now, just reset all accessor caches
        evictor = cacheEvictorPool
                .schedule(() -> instance.find(c -> Stream.of(c.get(ComponentManager.AllServices.class).getServices()))
                                        .filter(Objects::nonNull).map(s -> AccessorCache.class.cast(s.get(AccessorCache.class)))
                                        .filter(Objects::nonNull).peek(AccessorCache::reset).count(), 1, MINUTES);

        idMapping = instance.find(c -> c.get(ContainerComponentRegistry.class).getComponents().values().stream())
                            .flatMap(c -> Stream
                                    .concat(c.getPartitionMappers().values().stream(), c.getProcessors().values().stream()))
                            .collect(toMap(ComponentFamilyMeta.BaseMeta::getId, identity()));
        actionIndex = instance.find(c -> c.get(ContainerComponentRegistry.class).getServices().stream())
                              .flatMap(c -> c.getActions().stream())
                              .collect(toMap(e -> new ActionKey(e.getFamily(), e.getType(), e.getAction()), identity()));

        familyIdMapping = instance.find(c -> c.get(ContainerComponentRegistry.class).getComponents().values().stream())
                                  .collect(toMap(f -> IdGenerator.get(f.getName()), identity()));
    }

    public ServiceMeta.ActionMeta findActionById(final String component, final String type, final String action) {
        return actionIndex.get(new ActionKey(component, type, action));
    }

    public <T> ComponentFamilyMeta.BaseMeta<T> findMetaById(final String id) {
        return (ComponentFamilyMeta.BaseMeta<T>) idMapping.get(id);
    }

    public ComponentFamilyMeta findFamilyMetaById(final String id) {
        return familyIdMapping.get(id);
    }

    @PreDestroy
    private void stopEviction() {
        evictor.cancel(true);
        cacheEvictorPool.shutdownNow();
    }

    @Produces
    public ComponentManager manager() {
        return instance;
    }

    private static final class ActionKey {

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
