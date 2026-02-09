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
package org.talend.sdk.component.sample.feature.loadinganalysis.withspi.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.dependency.DynamicDependencies;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.sample.feature.loadinganalysis.classloadertestlibrary.serviceInterfaces.StringProviderFromExternalSPI;
import org.talend.sdk.component.sample.feature.loadinganalysis.classloadertestlibrary.serviceInterfaces.StringProviderSPIAsDependency;
import org.talend.sdk.component.sample.feature.loadinganalysis.classloadertestlibrary.serviceInterfaces.StringProviderSPIAsDynamicDependency;
import org.talend.sdk.component.sample.feature.loadinganalysis.classloadertestlibrary.spiConsumers.DependencySPIConsumer;
import org.talend.sdk.component.sample.feature.loadinganalysis.classloadertestlibrary.spiConsumers.DynamicDependencySPIConsumer;
import org.talend.sdk.component.sample.feature.loadinganalysis.classloadertestlibrary.spiConsumers.ExternalDependencySPIConsumer;
import org.talend.sdk.component.sample.feature.loadinganalysis.withspi.config.Dataset;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DynamicDependenciesWithSPIService implements Serializable {

    private static String version;

    @Service
    private RecordBuilderFactory recordBuilderFactory;

    @Service
    private JsonBuilderFactory jsonBuilderFactory;

    @DynamicDependencies
    public List<String> getDynamicDependencies(@Option("theDataset") final Dataset dataset) {
        String dep = "org.talend.sdk.samplefeature.dynamicdependencies:service-provider-from-dynamic-dependency:"
                + loadVersion();
        List<String> strings = Collections.singletonList(dep);
        log.info("Dynamic dependencies with SPI: {}", strings.stream().collect(Collectors.joining(";")));
        return strings;
    }

    @DiscoverSchema("dyndepsdse")
    public Schema guessSchema4Input(final @Option("configuration") Dataset dse) {
        return recordBuilderFactory.newSchemaBuilder(Type.RECORD)
                .withEntry(recordBuilderFactory.newEntryBuilder().withType(Type.STRING).withName("value").build())
                .withEntry(
                        recordBuilderFactory.newEntryBuilder().withType(Type.STRING).withName("SPI_Interface").build())
                .withEntry(recordBuilderFactory.newEntryBuilder()
                        .withType(Type.STRING)
                        .withName("SPI_Interface_classloader")
                        .build())
                .withEntry(recordBuilderFactory.newEntryBuilder().withType(Type.STRING).withName("SPI_Impl").build())
                .withEntry(recordBuilderFactory.newEntryBuilder()
                        .withType(Type.STRING)
                        .withName("SPI_Impl_classloader")
                        .build())
                .withEntry(recordBuilderFactory.newEntryBuilder().withType(Type.STRING).withName("comment").build())
                .withEntry(
                        recordBuilderFactory.newEntryBuilder().withType(Type.STRING).withName("rootRepository").build())
                .withEntry(recordBuilderFactory.newEntryBuilder().withType(Type.STRING).withName("classpath").build())
                .withEntry(recordBuilderFactory.newEntryBuilder()
                        .withType(Type.STRING)
                        .withName("workingDirectory")
                        .build())
                .build();
    }

    public Iterator<Record> getRecordIterator() {
        String rootRepository = System.getProperty("talend.component.manager.m2.repository");
        String runtimeClasspath = System.getProperty("java.class.path");
        String workDirectory = System.getProperty("user.dir");

        String contentFromResourceDependency = loadAPropertyFromResource("FROM_DEPENDENCY/resource.properties",
                "ServiceProviderFromDependency.message");

        String contentFromResourceDynamicDependency = loadAPropertyFromResource(
                "FROM_DYNAMIC_DEPENDENCY/resource.properties",
                "ServiceProviderFromDynamicDependency.message");

        String contentFromResourceExternalDependency = loadAPropertyFromResource(
                "FROM_EXTERNAL_DEPENDENCY/resource.properties",
                "ServiceProviderFromExternalDependency.message");

        String contentFromMultipleResources;
        try {
            Enumeration<URL> resources = DynamicDependenciesWithSPIService.class.getClassLoader()
                    .getResources("MULTIPLE_RESOURCE/content.txt");

            StringBuilder stringBuilder = new StringBuilder("There should be 3 different values:");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();

                try (InputStream is = url.openStream()) {
                    String content = filterComments(is);
                    stringBuilder.append("\n");
                    stringBuilder.append(content);
                }
            }
            contentFromMultipleResources = stringBuilder.toString();
        } catch (IOException e) {
            throw new ComponentException("Can't retrieve multiple resources at once.", e);
        }

        DependencySPIConsumer<Record> dependencySPIConsumer = new DependencySPIConsumer<>();
        Record recordsFromDependencySPI = dependencySPIConsumer
                .transform(s -> recordBuilderFactory.newRecordBuilder()
                        .withString("value", s)
                        .withString("SPI_Interface", String.valueOf(StringProviderSPIAsDependency.class))
                        .withString("SPI_Impl",
                                dependencySPIConsumer.getSPIImpl().isPresent()
                                        ? String.valueOf(dependencySPIConsumer.getSPIImpl().get().getClass())
                                        : "Not found")
                        .withString("SPI_Interface_classloader",
                                String.valueOf(StringProviderSPIAsDependency.class.getClassLoader()))
                        .withString("SPI_Impl_classloader",
                                dependencySPIConsumer.getSPIImpl().isPresent()
                                        ? String.valueOf(
                                                dependencySPIConsumer.getSPIImpl().get().getClass().getClassLoader())
                                        : "Not found")
                        .withString("comment", "SPI implementation loaded from a dependency.")
                        .withString("rootRepository", rootRepository)
                        .withString("classpath", runtimeClasspath)
                        .withString("workingDirectory", workDirectory)
                        .build());

        DynamicDependencySPIConsumer<Record> dynamicDependencySPIConsumer = new DynamicDependencySPIConsumer<>();
        Record recordsFromDynamicDependencySPI = dynamicDependencySPIConsumer
                .transform(s -> recordBuilderFactory.newRecordBuilder()
                        .withString("value", s)
                        .withString("SPI_Interface", String.valueOf(StringProviderSPIAsDynamicDependency.class))
                        .withString("SPI_Impl",
                                dynamicDependencySPIConsumer.getSPIImpl().isPresent()
                                        ? String.valueOf(dynamicDependencySPIConsumer.getSPIImpl().get().getClass())
                                        : "Not found")
                        .withString("SPI_Interface_classloader",
                                String.valueOf(StringProviderSPIAsDynamicDependency.class.getClassLoader()))
                        .withString("SPI_Impl_classloader",
                                dynamicDependencySPIConsumer.getSPIImpl().isPresent() ? String.valueOf(
                                        dynamicDependencySPIConsumer.getSPIImpl().get().getClass().getClassLoader())
                                        : "Not found")
                        .withString("comment", "SPI implementation loaded from a dynamic dependency.")
                        .withString("rootRepository", rootRepository)
                        .withString("classpath", runtimeClasspath)
                        .withString("workingDirectory", workDirectory)
                        .build());

        ExternalDependencySPIConsumer<Record> externalDependencySPI = new ExternalDependencySPIConsumer<>();
        Record recordsFromExternalSPI = externalDependencySPI
                .transform(s -> recordBuilderFactory.newRecordBuilder()
                        .withString("value", s)
                        .withString("SPI_Interface", String.valueOf(StringProviderFromExternalSPI.class))
                        .withString("SPI_Impl",
                                externalDependencySPI.getSPIImpl().isPresent()
                                        ? String.valueOf(externalDependencySPI.getSPIImpl().get().getClass())
                                        : "Not found")
                        .withString("SPI_Interface_classloader",
                                String.valueOf(StringProviderFromExternalSPI.class.getClassLoader()))
                        .withString("SPI_Impl_classloader",
                                externalDependencySPI.getSPIImpl().isPresent()
                                        ? String.valueOf(
                                                externalDependencySPI.getSPIImpl().get().getClass().getClassLoader())
                                        : "Not found")
                        .withString("comment", "SPI implementation loaded from a runtime/provided dependency.")
                        .withString("rootRepository", rootRepository)
                        .withString("classpath", runtimeClasspath)
                        .withString("workingDirectory", workDirectory)
                        .build());

        JsonObject contentFromResources = jsonBuilderFactory.createObjectBuilder()
                .add("contentFromResourceDependency", contentFromResourceDependency)
                .add("contentFromResourceDynamicDependency", contentFromResourceDynamicDependency)
                .add("contentFromResourceExternalDependency", contentFromResourceExternalDependency)
                .add("contentFromMultipleResources", contentFromMultipleResources)
                .build();
        Record recordWithContentFromResources = recordBuilderFactory.newRecordBuilder()
                .withString("value", contentFromResources.toString())
                .withString("SPI_Interface", "N/A")
                .withString("SPI_Impl", "N/A")
                .withString("SPI_Interface_classloader", "N/A")
                .withString("SPI_Impl_classloader", "N/A")
                .withString("comment", "Resources loading.")
                .withString("rootRepository", rootRepository)
                .withString("classpath", runtimeClasspath)
                .withString("workingDirectory", workDirectory)
                .build();

        List<Record> values = new ArrayList<>();
        values.add(recordsFromDependencySPI);
        values.add(recordsFromDynamicDependencySPI);
        values.add(recordsFromExternalSPI);
        values.add(recordWithContentFromResources);

        return values.iterator();
    }

    private static String loadVersion() {
        if (version == null) {
            try (InputStream is = DynamicDependenciesWithSPIService.class.getClassLoader()
                    .getResourceAsStream("version.properties")) {
                Properties props = new Properties();
                props.load(is);
                version = props.getProperty("version");
            } catch (IOException e) {
                throw new ComponentException("Unable to load project version", e);
            }
        }
        return version;
    }

    private String filterComments(final InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

        String collect = reader.lines()
                .filter(line -> !line.trim().startsWith("#"))
                .filter(line -> !line.trim().isEmpty())
                .collect(Collectors.joining(System.lineSeparator()));
        try {
            reader.close();
        } catch (IOException e) {
            throw new ComponentException("Can't close a resource reader.", e);
        }

        return collect;
    }

    private String loadAPropertyFromResource(final String resource, final String property) {
        try (InputStream resourceStreamFromDependency = DynamicDependenciesWithSPIService.class.getClassLoader()
                .getResourceAsStream(resource)) {

            if (resourceStreamFromDependency == null) {
                return "The resource '%s' has not been found, it can't retrieve the '%s' property value."
                        .formatted(resource, property);
            }

            Properties prop = new Properties();
            prop.load(resourceStreamFromDependency);
            return prop.getProperty(property);
        } catch (IOException e) {
            throw new ComponentException("Can't retrieve resource from a dependency.", e);
        }
    }

}