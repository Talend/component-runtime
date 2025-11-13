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
package org.talend.sdk.component.sample.feature.dynamicdependencies.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;

import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Record.Builder;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.source.ProducerFinder;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Dependency;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.DynamicDependencyConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractDynamicDependenciesService implements Serializable {

    public static final String DISCOVERSCHEMA_ACTION = "DISCOVERSCHEMA_ACTION";

    public static final String ENTRY_MAVEN = "maven";

    public static final String ENTRY_CLASS = "clazz";

    public static final String ENTRY_IS_LOADED = "is_loaded";

    public static final String ENTRY_CONNECTOR_CLASSLOADER = "connector_classloader";

    public static final String ENTRY_CLAZZ_CLASSLOADER = "clazz_classloader";

    public static final String ENTRY_FROM_LOCATION = "from_location";

    public static final String ENTRY_IS_TCK_CONTAINER = "is_tck_container";

    public static final String ENTRY_IS_LOADED_IN_TCK = "is_loaded_in_tck_manager";

    public static final String ENTRY_ROOT_REPOSITORY = "root_repository";

    public static final String ENTRY_RUNTIME_CLASSPATH = "runtime_classpath";

    public static final String ENTRY_WORKING_DIRECTORY = "Working_directory";

    @Service
    private RecordBuilderFactory factory;

    @Service
    protected ProducerFinder finder;

    public Iterator<Record> loadIterator(final DynamicDependencyConfig dynamicDependencyConfig) {
        Schema schema = buildSchema(dynamicDependencyConfig);

        List<Record> records = new ArrayList<>();
        for (Dependency dependency : dynamicDependencyConfig.getDependencies()) {
            Builder builder = factory.newRecordBuilder(schema);

            String maven = String.format("%s:%s:%s", dependency.getGroupId(), dependency.getArtifactId(),
                    dependency.getVersion());

            boolean isLoaded = false;
            String connectorClassLoaderId = this.getClass().getClassLoader().toString();
            String clazzClassLoaderId = "N/A";
            String fromLocation = "N/A";
            try {
                Class<?> clazz = Class.forName(dependency.getClazz());
                isLoaded = true;
                clazzClassLoaderId = clazz.getClassLoader().toString();

                // This way to retrieve the location works even if the jar from where clazz comes from
                // is nested into another jar (uber jar scenario)
                String classPath = clazz.getName().replace('.', '/') + ".class";
                URL url = clazz.getClassLoader().getResource(classPath);
                fromLocation = String.valueOf(url);
            } catch (ClassNotFoundException e) {
                manageException(dynamicDependencyConfig.isDieOnError(),
                        "Cannot load class %s from system classloader".formatted(dependency.getClazz()), e);
            }

            boolean isTckContainer = false;
            boolean isLoadedInTck = false;
            if (dependency.getConnectorFamily() != null && !dependency.getConnectorFamily().isEmpty()) {
                isTckContainer = isTCKContainer(fromLocation); // not now
                // package-info@Components
                isLoadedInTck = testLoadingData(dynamicDependencyConfig, dependency); // to improve
            }

            Builder recordBuilder = builder
                    .withString(ENTRY_MAVEN, maven)
                    .withString(ENTRY_CLASS, dependency.getClazz())
                    .withBoolean(ENTRY_IS_LOADED, isLoaded)
                    .withString(ENTRY_CONNECTOR_CLASSLOADER, connectorClassLoaderId)
                    .withString(ENTRY_CLAZZ_CLASSLOADER, clazzClassLoaderId)
                    .withString(ENTRY_FROM_LOCATION, fromLocation)
                    .withBoolean(ENTRY_IS_TCK_CONTAINER, isTckContainer)
                    .withBoolean(ENTRY_IS_LOADED_IN_TCK, isLoadedInTck);

            if (dynamicDependencyConfig.isEnvironmentInformation()) {
                String rootRepository = System.getProperty("talend.component.manager.m2.repository");
                String runtimeClasspath = System.getProperty("java.class.path");
                String workDirectory = System.getProperty("user.dir");

                recordBuilder = recordBuilder
                        .withString(ENTRY_ROOT_REPOSITORY, rootRepository)
                        .withString(ENTRY_RUNTIME_CLASSPATH, runtimeClasspath)
                        .withString(ENTRY_WORKING_DIRECTORY, workDirectory);
            }

            Record record = recordBuilder.build();
            records.add(record);
        }

        return records.iterator();
    }

    protected boolean testLoadingData(final DynamicDependencyConfig dynamicDependencyConfig, final Dependency dependency) {
        Iterator<Record> recordIterator = finder.find(dependency.getConnectorFamily(), dependency.getConnectorName(),
                dependency.getConnectorVersion(), Collections.emptyMap());
        return recordIterator.hasNext();
    }

    //If the dependency is a TCK connector, get its dependencies list
    protected Map<String, String> loadComponentDepends(final DynamicDependencyConfig dynamicDependencyConfig, final String jarPath) {
        Map<String, String> configMap = new HashMap<>();
        final Path archive = FileSystems.getDefault().getPath(jarPath);
        try (final JarFile file = new JarFile(archive.toFile());
            final InputStream stream = file.getInputStream(file.getEntry("TALEND-INF/dependencies.txt"))) {
            final Properties properties = new Properties();
            properties.load(stream);
            properties.stringPropertyNames().forEach(name -> {
                configMap.put(name, properties.getProperty(name));
            });
            return configMap;//empty if no dependencies
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, String> json2Map(final String json) {
        // Transform the given json to map
        return Collections.emptyMap();
    }

    protected Schema buildSchema(final DynamicDependencyConfig dynamicDependencyConfig) {
        Schema.Builder builder = factory.newSchemaBuilder(Type.RECORD)
                .withEntry(factory.newEntryBuilder().withName(ENTRY_MAVEN).withType(Type.STRING).build())
                .withEntry(factory.newEntryBuilder().withName(ENTRY_CLASS).withType(Type.STRING).build())
                .withEntry(factory.newEntryBuilder().withName(ENTRY_IS_LOADED).withType(Type.BOOLEAN).build())
                .withEntry(
                        factory.newEntryBuilder().withName(ENTRY_CONNECTOR_CLASSLOADER).withType(Type.STRING).build())
                .withEntry(factory.newEntryBuilder().withName(ENTRY_CLAZZ_CLASSLOADER).withType(Type.STRING).build())
                .withEntry(factory.newEntryBuilder().withName(ENTRY_FROM_LOCATION).withType(Type.STRING).build())
                .withEntry(factory.newEntryBuilder().withName(ENTRY_IS_TCK_CONTAINER).withType(Type.BOOLEAN).build())
                .withEntry(factory.newEntryBuilder().withName(ENTRY_IS_LOADED_IN_TCK).withType(Type.BOOLEAN).build());

        if (dynamicDependencyConfig.isEnvironmentInformation()) {
            builder = builder
                    .withEntry(factory.newEntryBuilder().withName(ENTRY_ROOT_REPOSITORY).withType(Type.STRING).build())
                    .withEntry(
                            factory.newEntryBuilder().withName(ENTRY_RUNTIME_CLASSPATH).withType(Type.STRING).build())
                    .withEntry(
                            factory.newEntryBuilder().withName(ENTRY_WORKING_DIRECTORY).withType(Type.STRING).build());
        }

        return builder.build();
    }


    private void manageException(final boolean dieOnError, final String message, final Exception e) {
        String msg = "Dynamic dependencies connector raised an exception: %s : %s".formatted(message, e.getMessage());
        log.error(msg, e);
        if (dieOnError) {
            throw new ComponentException(msg, e);
        }
    }

    protected List<String> getDynamicDependencies(final List<Dependency> dependencies) {
        return dependencies
                .stream()
                .map(d -> String.format("%s:%s:%s", d.getGroupId(), d.getArtifactId(), d.getVersion()))
                .toList();
    }

    /**
     * Return true if the given path correspond to a class that has been loaded from a jar that contains
     * a TALEND-INF/dependencies.txt file.
     *
     * @param path The clazz location
     * @return true if the given path correspond to a TCK container
     */
    private boolean isTCKContainer(final String path) {
        // TO DO
        return false;
    }
}