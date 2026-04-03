/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.sample.feature.loadinganalysis.specificisolation.service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.dependency.DynamicDependencies;
import org.talend.sdk.component.api.service.dependency.Resolver;
import org.talend.sdk.component.api.service.dependency.Resolver.ClassLoaderDescriptor;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.container.ContainerManager.ClassLoaderConfiguration;
import org.talend.sdk.component.sample.feature.loadinganalysis.specificisolation.config.Config;
import org.talend.sdk.component.sample.feature.loadinganalysis.specificisolation.config.Datastore;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpecificIsolationService {

    @Service
    private RecordBuilderFactory recordBuilderFactory;

    @Service
    private Resolver resolver;

    @DynamicDependencies
    public List<String> getDynamicDependencies(final @Option("configuration") Datastore datastore) {
        if (datastore.isNoDynamicDependency()) {
            log.info("No dynamic dependency");
            return Collections.emptyList();
        }

        String dependency = getDependency(datastore);
        log.info("Dynamic dependency: " + dependency);
        return Collections.singletonList(dependency);
    }

    private static String getDependency(final Datastore datastore) {
        return datastore.getGroup() + ":" + datastore.getArtifact() + ":" + datastore.getVersion();
    }

    public Record buildRecord(final Config config) {

        final Predicate<String> classfilterPredicate = config.getDse().getDso().isClassesFilterAcceptAll() ? s -> true
                : predicateFromString(config.getDse().getDso().getClassesFilterOption());

        final Predicate<String> parentClassfilterPredicate =
                config.getDse().getDso().isParentClassesFilterAcceptAll() ? s -> true
                        : predicateFromString(config.getDse().getDso().getParentClassesFilterOption());

        final Predicate<String> parentResourcesfilterPredicate =
                config.getDse().getDso().isParentResourcesFilterAcceptAll() ? s -> true
                        : predicateFromString(config.getDse().getDso().getParentResourcesFilterOption());

        final ClassLoader loader = getClass().getClassLoader();
        final ClassLoaderConfiguration clConfiguration = ClassLoaderConfiguration
                .builder()
                .parent(loader)
                .classesFilter(classfilterPredicate)
                .parentClassesFilter(parentClassfilterPredicate)
                .supportsResourceDependencies(config.getDse().getDso().isJarAsResource())
                .parentResourcesFilter(parentResourcesfilterPredicate)
                .create();

        String dependency = getDependency(config.getDse().getDso());
        List<String> deps = Collections.singletonList(dependency);
        try (ClassLoaderDescriptor classLoaderDescriptor = resolver.mapDescriptorToClassLoader(deps, clConfiguration)) {
            ClassLoader specificClassLoader = classLoaderDescriptor.asClassLoader();

            Class<?> aClass = specificClassLoader.loadClass(config.getDse().getDso().getClazz());

            String content = "Can't read resource " + config.getDse().getDso().getResource();
            try (InputStream resourceAsStream =
                    specificClassLoader.getResourceAsStream(config.getDse().getDso().getResource())) {
                if (resourceAsStream != null) {
                    content = new String(resourceAsStream.readAllBytes());
                } else {
                    log.warn("Resource not found: " + config.getDse().getDso().getResource());
                }
            }

            return recordBuilderFactory.newRecordBuilder()
                    .withString("connector_classloader", this.getClass().getClassLoader().toString())
                    .withString("loaded_class_classloader", aClass.getClassLoader().toString())
                    .withString("loaded_resource_content", content)
                    .build();

        } catch (Exception e) {
            throw new ComponentException("Failed to load class or resource with specific isolation", e);
        }
    }

    private Predicate<String> predicateFromString(final String filterAsString) {
        final List<String> filters = List.of(filterAsString.split("\\n"));
        return s -> filters.stream().anyMatch(s::startsWith);
    }

}