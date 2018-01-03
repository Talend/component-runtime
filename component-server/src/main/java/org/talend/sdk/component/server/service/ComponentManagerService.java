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
package org.talend.sdk.component.server.service;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Stream.empty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.dependencies.maven.MvnCoordinateToFileConverter;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.util.IdGenerator;
import org.talend.sdk.component.runtime.output.data.AccessorCache;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.dao.ComponentActionDao;
import org.talend.sdk.component.server.dao.ComponentDao;
import org.talend.sdk.component.server.dao.ComponentFamilyDao;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ComponentManagerService {

    @Inject
    private ComponentServerConfiguration configuration;

    @Inject
    private ComponentDao componentDao;

    @Inject
    private ComponentFamilyDao componentFamilyDao;

    @Inject
    private ComponentActionDao actionDao;

    private ComponentManager instance;

    private MvnCoordinateToFileConverter mvnCoordinateToFileConverter;

    private ScheduledExecutorService cacheEvictorPool;

    private ScheduledFuture<?> evictor;

    void startupLoad(@Observes @Initialized(ApplicationScoped.class) final Object start) {
        // we just want it to be touched
    }

    @PostConstruct
    private void init() {
        System.setProperty("talend.component.manager.m2.repository", configuration.mavenRepository());
        mvnCoordinateToFileConverter = new MvnCoordinateToFileConverter();
        instance = ComponentManager.instance();

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

        // trivial mecanism for now, just reset all accessor caches
        cacheEvictorPool = Executors.newScheduledThreadPool(1, new ThreadFactory() {

            @Override
            public Thread newThread(final Runnable r) {
                final Thread thread = new Thread(r);
                thread.setName(getClass().getName() + "-evictor");
                thread.setDaemon(true);
                return thread;
            }
        });

        evictor = cacheEvictorPool.schedule(() -> instance
                .find(c -> Stream.of(c.get(ComponentManager.AllServices.class).getServices()))
                .filter(Objects::nonNull)
                .map(s -> AccessorCache.class.cast(s.get(AccessorCache.class)))
                .filter(Objects::nonNull)
                .peek(AccessorCache::reset)
                .count(), 1, MINUTES);
    }

    @PreDestroy
    private void close() {
        evictor.cancel(true);
        cacheEvictorPool.shutdownNow();
    }

    public String deploy(final String pluginGAV) {
        String pluginPath = ofNullable(pluginGAV)
                .map(gav -> mvnCoordinateToFileConverter.toArtifact(gav))
                .map(Artifact::toPath)
                .orElseThrow(() -> new IllegalArgumentException("Plugin GAV can't be empty"));

        final String pluginID = instance.addWithLocationPlugin(pluginGAV,
                new File(configuration.mavenRepository(), pluginPath).getAbsolutePath());
        final Container plugin = instance.findPlugin(pluginID).get();

        plugin
                .get(ContainerComponentRegistry.class)
                .getComponents()
                .values()
                .stream()
                .flatMap(c -> Stream.concat(c.getPartitionMappers().values().stream(),
                        c.getProcessors().values().stream()))
                .forEach(meta -> componentDao.createOrUpdate(meta));

        plugin
                .get(ContainerComponentRegistry.class)
                .getServices()
                .stream()
                .flatMap(c -> c.getActions().stream())
                .forEach(e -> actionDao.createOrUpdate(e));

        plugin.get(ContainerComponentRegistry.class).getComponents().values().forEach(
                meta -> componentFamilyDao.createOrUpdate(meta));

        return pluginID;
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

        final Container plugin = instance.findPlugin(pluginID).orElseThrow(
                () -> new IllegalArgumentException("No plugin found using maven GAV: " + pluginGAV));

        final ContainerComponentRegistry containerComponentRegistry = plugin.get(ContainerComponentRegistry.class);
        containerComponentRegistry
                .getComponents()
                .values()
                .stream()
                .flatMap(c -> Stream.concat(c.getPartitionMappers().values().stream(),
                        c.getProcessors().values().stream()))
                .forEach(meta -> componentDao.removeById(meta.getId()));

        containerComponentRegistry.getServices().stream().flatMap(c -> c.getActions().stream()).forEach(
                e -> actionDao.remove(e));

        containerComponentRegistry.getComponents().values().forEach(
                meta -> componentFamilyDao.removeById(IdGenerator.get(meta.getName())));

        instance.removePlugin(pluginID);
    }

    @Produces
    public ComponentManager manager() {
        return instance;
    }

}
