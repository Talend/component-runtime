/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

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
import org.talend.sdk.component.dependencies.maven.Artifact;
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
import static java.util.stream.Stream.empty;

@Slf4j
@ApplicationScoped
public class ComponentManagerService {

    @Inject
    private ComponentServerConfiguration configuration;

    private ComponentsCache componentsCache;

    private ComponentManager instance;

    private String mvnRepo;

    private MvnCoordinateToFileConverter mvnCoordinateToFileConverter;

    void startupLoad(@Observes @Initialized(ApplicationScoped.class) final Object start) {
        // we just want it to be touched
    }

    @PostConstruct
    private void init() {
        mvnRepo = StrSubstitutor.replaceSystemProperties(configuration.mavenRepository());
        System.setProperty("talend.component.manager.m2.repository", mvnRepo);// we will use the default instance
        mvnCoordinateToFileConverter = new MvnCoordinateToFileConverter();
        instance = ComponentManager.instance();
        componentsCache = new ComponentsCache(manager());
        loadComponents();
    }

    @PreDestroy
    private void close() {
        componentsCache.destroy();
    }

    private void loadComponents() {
        // note: we don't want to download anything from the manager, if we need to download any artifact we need
        // to ensure it is controlled (secured) and allowed so don't make it implicit but enforce a first phase
        // where it is cached locally (provisioning solution)
        ofNullable(configuration.componentCoordinates()).orElse(emptySet()).forEach(this::deploy);
        ofNullable(configuration.componentRegistry())
                .map(StrSubstitutor::replaceSystemProperties)
                .map(File::new)
                .filter(File::exists)
                .ifPresent(registry -> {
                    final Properties properties = new Properties();
                    try (final InputStream is = new FileInputStream(registry)) {
                        properties.load(is);
                    } catch (final IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                    properties.stringPropertyNames().stream().map(properties::getProperty).forEach(this::deploy);
                });

        componentsCache.update();
    }

    public String deploy(final String pluginGAV) {
        String pluginPath = ofNullable(pluginGAV)
                .map(gav -> mvnCoordinateToFileConverter.toArtifact(gav))
                .map(Artifact::toPath)
                .orElseThrow(() -> new IllegalArgumentException("Plugin GAV can't be empty"));

        return instance.addWithLocationPlugin(pluginGAV, new File(mvnRepo, pluginPath).getAbsolutePath());
    }

    public void undeploy(final String pluginGAV) {
        if (pluginGAV == null || pluginGAV.isEmpty()) {
            throw new IllegalArgumentException("plugin maven GAV are required to undeploy a plugin");
        }

        String pluginID = instance
                .find(c -> pluginGAV.equals(c.get(ComponentManager.OriginalId.class).getValue()) ? Stream.of(c.getId())
                        : empty())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No plugin found using maven GAV: " + pluginGAV));

        instance.removePlugin(pluginID);
        componentsCache.update();
    }

    public ServiceMeta.ActionMeta findActionById(final String component, final String type, final String action) {
        return componentsCache.actionIndex.get(new ActionKey(component, type, action));
    }

    public <T> ComponentFamilyMeta.BaseMeta<T> findMetaById(final String id) {
        return (ComponentFamilyMeta.BaseMeta<T>) componentsCache.idMapping.get(id);
    }

    public ComponentFamilyMeta findFamilyMetaById(final String id) {
        return componentsCache.familyIdMapping.get(id);
    }

    @Produces
    public ComponentManager manager() {
        return instance;
    }

    @Data
    private static class ComponentsCache {

        // for now we ignore reloading but we should add it if it starts to be used (through a listener)
        private Map<String, ComponentFamilyMeta.BaseMeta<?>> idMapping;

        private Map<String, ComponentFamilyMeta> familyIdMapping;

        private Map<ActionKey, ServiceMeta.ActionMeta> actionIndex;

        private ScheduledExecutorService cacheEvictorPool;

        private ScheduledFuture<?> evictor;

        private ComponentManager componentManager;

        private ComponentsCache(final ComponentManager manager) {
            this.componentManager = manager;
            cacheEvictorPool = Executors.newScheduledThreadPool(1, new ThreadFactory() {

                @Override
                public Thread newThread(final Runnable r) {
                    final Thread thread = new Thread(r);
                    thread.setName(getClass().getName() + "-evictor");
                    thread.setDaemon(true);
                    return thread;
                }
            });
        }

        public void update() {
            // trivial mecanism for now, just reset all accessor caches
            evictor = cacheEvictorPool.schedule(() -> componentManager
                    .find(c -> Stream.of(c.get(ComponentManager.AllServices.class).getServices()))
                    .filter(Objects::nonNull)
                    .map(s -> AccessorCache.class.cast(s.get(AccessorCache.class)))
                    .filter(Objects::nonNull)
                    .peek(AccessorCache::reset)
                    .count(), 1, MINUTES);

            idMapping = componentManager
                    .find(c -> c.get(ContainerComponentRegistry.class).getComponents().values().stream())
                    .flatMap(c -> Stream.concat(c.getPartitionMappers().values().stream(),
                            c.getProcessors().values().stream()))
                    .collect(toMap(ComponentFamilyMeta.BaseMeta::getId, identity()));
            actionIndex = componentManager
                    .find(c -> c.get(ContainerComponentRegistry.class).getServices().stream())
                    .flatMap(c -> c.getActions().stream())
                    .collect(toMap(e -> new ActionKey(e.getFamily(), e.getType(), e.getAction()), identity()));

            familyIdMapping =
                    componentManager.find(
                            c -> c.get(ContainerComponentRegistry.class).getComponents().values().stream())
                            .collect(toMap(f -> IdGenerator.get(f.getName()), identity()));
        }

        public void destroy() {
            evictor.cancel(true);
            cacheEvictorPool.shutdownNow();
        }
    }

    public static final class ActionKey {

        private final String component;

        private final String type;

        private final String name;

        private final int hash;

        public ActionKey(final String component, final String type, final String name) {
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
