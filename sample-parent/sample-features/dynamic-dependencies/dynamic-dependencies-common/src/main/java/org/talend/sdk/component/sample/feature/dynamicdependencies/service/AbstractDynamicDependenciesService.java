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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Record.Builder;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.dependency.Resolver;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.source.ProducerFinder;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Connector;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Dependency;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.DynamicDependencyConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractDynamicDependenciesService implements Serializable {

    public static final String ENTRY_MAVEN = "maven";

    public static final String ENTRY_CLASS = "clazz";

    public static final String ENTRY_IS_LOADED = "is_loaded";

    public static final String ENTRY_CONNECTOR_CLASSLOADER = "connector_classloader";

    public static final String ENTRY_CLAZZ_CLASSLOADER = "clazz_classloader";

    public static final String ENTRY_FROM_LOCATION = "from_location";

    public static final String ENTRY_IS_TCK_CONTAINER = "is_tck_container";

    public static final String ENTRY_FIRST_RECORD = "first_record";

    public static final String ENTRY_ROOT_REPOSITORY = "root_repository";

    public static final String ENTRY_RUNTIME_CLASSPATH = "runtime_classpath";

    public static final String ENTRY_WORKING_DIRECTORY = "Working_directory";

    @Service
    private RecordBuilderFactory factory;

    @Service
    private ProducerFinder finder;

    @Service
    private Resolver resolver;

    public Iterator<Record> loadIterator(final DynamicDependencyConfig dynamicDependencyConfig) {
        Schema schema = buildSchema(dynamicDependencyConfig);

        List<Record> standardDependencies = loadStandardDependencies(dynamicDependencyConfig, schema);
        List<Record> additionalConnectors = loadConnectors(dynamicDependencyConfig, schema);

        return Stream.concat(standardDependencies.stream(), additionalConnectors.stream()).iterator();
    }

    private List<Record> loadStandardDependencies(final DynamicDependencyConfig dynamicDependencyConfig,
            final Schema schema) {
        List<Record> records = new ArrayList<>();

        List<Dependency> dependencies = new ArrayList<>();
        // Add a class that should be imported by a 'standard' dependency (not a dynamic one)
        // to have an example from which classloaded it is loaded
        // In that case the version doesn't matter.
        dependencies.add(new Dependency("org.talend.sdk.samplefeature.dynamicdependencies",
                "dynamic-dependencies-common",
                "N/A",
                "org.talend.sdk.component.sample.feature.dynamicdependencies.config.Dependency"));
        dependencies.addAll(dynamicDependencyConfig.getDependencies());
        for (Dependency dependency : dependencies) {

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

            Record record = buildRecord(schema,
                    dynamicDependencyConfig,
                    maven,
                    dependency.getClazz(),
                    isLoaded,
                    connectorClassLoaderId,
                    clazzClassLoaderId,
                    fromLocation,
                    false,
                    Optional.empty());
            records.add(record);
        }

        return records;
    }

    private List<Record> loadConnectors(final DynamicDependencyConfig dynamicDependencyConfig, final Schema schema) {
        List<Record> records = new ArrayList<>();
        for (Connector connector : dynamicDependencyConfig.getConnectors()) {

            String maven = String.format("%s:%s:%s", connector.getGroupId(), connector.getArtifactId(),
                    connector.getVersion());

            String connectorClassLoaderId = this.getClass().getClassLoader().toString();
            String clazzClassLoaderId = "N/A";
            String fromLocation = "N/A";
            Optional<Record> optionalRecord = testLoadingData(connector);
            boolean isLoaded = optionalRecord.isPresent();

            Record record = buildRecord(schema,
                    dynamicDependencyConfig,
                    maven,
                    "N/A",
                    isLoaded,
                    connectorClassLoaderId,
                    clazzClassLoaderId,
                    fromLocation,
                    true,
                    optionalRecord);
            records.add(record);
        }

        return records;

    }

    private Record buildRecord(final Schema schema,
            final DynamicDependencyConfig dynamicDependencyConfig,
            final String maven,
            final String clazz,
            final boolean isLoaded,
            final String connectorClassLoaderId,
            final String clazzClassLoaderId,
            final String fromLocation,
            final boolean isTckContainer,
            final Optional<Record> firstRecord) {
        Builder builder = factory.newRecordBuilder(schema);
        Builder recordBuilder = builder
                .withString(ENTRY_MAVEN, maven)
                .withString(ENTRY_CLASS, clazz)
                .withBoolean(ENTRY_IS_LOADED, isLoaded)
                .withString(ENTRY_CONNECTOR_CLASSLOADER, connectorClassLoaderId)
                .withString(ENTRY_CLAZZ_CLASSLOADER, clazzClassLoaderId)
                .withString(ENTRY_FROM_LOCATION, fromLocation)
                .withBoolean(ENTRY_IS_TCK_CONTAINER, isTckContainer);

        firstRecord.ifPresent(record -> builder.withRecord(ENTRY_FIRST_RECORD, record));

        if (dynamicDependencyConfig.isEnvironmentInformation()) {
            String rootRepository = System.getProperty("talend.component.manager.m2.repository");
            String runtimeClasspath = System.getProperty("java.class.path");
            String workDirectory = System.getProperty("user.dir");

            recordBuilder = recordBuilder
                    .withString(ENTRY_ROOT_REPOSITORY, rootRepository)
                    .withString(ENTRY_RUNTIME_CLASSPATH, runtimeClasspath)
                    .withString(ENTRY_WORKING_DIRECTORY, workDirectory);
        }

        return recordBuilder.build();
    }

    private Optional<Record> testLoadingData(final Connector connector) {
        Iterator<Record> recordIterator = this.loadData(connector.getConnectorFamily(), connector.getConnectorName(),
                connector.getConnectorVersion(), json2Map(connector.getConnectorConfiguration()));
        return Optional.ofNullable(
                recordIterator.hasNext() ? recordIterator.next() : null);
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
                .withEntry(factory.newEntryBuilder().withName(ENTRY_IS_TCK_CONTAINER).withType(Type.BOOLEAN).build());

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

    protected Iterator<Record> loadData(final String family, final String name, final int version,
            final Map<String, String> parameters) {
        return finder.find(family, name, version, parameters);
    }

    private void manageException(final boolean dieOnError, final String message, final Exception e) {
        String msg = "Dynamic dependencies connector raised an exception: %s : %s".formatted(message, e.getMessage());
        log.error(msg, e);
        if (dieOnError) {
            throw new ComponentException(msg, e);
        }
    }

    protected List<String> getDynamicDependencies(final List<Dependency> dependencies,
            final List<Connector> connectors) {
        List<String> standardDependencies = dependencies
                .stream()
                .map(d -> String.format("%s:%s:%s", d.getGroupId(), d.getArtifactId(), d.getVersion()))
                .toList();

        List<String> additionalConnectors = connectors
                .stream()
                .map(c -> String.format("%s:%s:%s", c.getGroupId(), c.getArtifactId(), c.getVersion()))
                .toList();

        List<String> connectorsDependencies = connectors
                .stream()
                .flatMap(this::getConnectorDependencies)
                .toList();
        List<String> all = Stream.of(standardDependencies, additionalConnectors, connectorsDependencies)
                .flatMap(Collection::stream)
                .toList();

        if (log.isInfoEnabled()) {
            String collect = all.stream().collect(Collectors.joining("\n- ", "- ", ""));
            log.info("All identified dependencies:\n" + collect);
        }
        return all;
    }

    private Stream<String> getConnectorDependencies(final Connector connector) {
        if (!connector.isLoadTransitiveDependencies()) {
            return Stream.empty();
        }

        List<String> result;

        String gav = String.format("%s:%s:%s", connector.getGroupId(),
                connector.getArtifactId(),
                connector.getVersion());
        Collection<File> jarFiles = resolver.resolveFromDescriptor(
                Collections.singletonList(gav));

        if (jarFiles == null || jarFiles.size() <= 0) {
            throw new ComponentException("Can't find additional connector '%s'.".formatted(gav));
        }
        if (jarFiles.size() > 1) {
            String join = jarFiles.stream().map(File::getAbsolutePath).collect(Collectors.joining(","));
            throw new ComponentException("Several files have been found to resolve '%s': %s".formatted(gav, join));
        }

        File jarFile = jarFiles.iterator().next();

        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry("TALEND-INF/dependencies.txt");
            if (entry == null) {
                throw new ComponentException("TALEND-INF/dependencies.txt not found in JAR");
            }

            try (InputStream is = jar.getInputStream(entry);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                result = reader.lines()
                        .filter(line -> !line.isBlank()) // skip empty lines
                        .map(line -> line.substring(0, line.lastIndexOf(":"))) // remove last ':xxx'
                        .collect(Collectors.toList());
            }

        } catch (IOException e) {
            throw new ComponentException("Can't load dependencies for %s: %s".formatted(gav, e.getMessage()), e);
        }
        return result.stream();
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