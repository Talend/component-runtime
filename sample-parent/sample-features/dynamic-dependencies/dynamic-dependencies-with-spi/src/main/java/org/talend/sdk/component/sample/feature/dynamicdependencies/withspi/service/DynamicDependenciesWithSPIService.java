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
package org.talend.sdk.component.sample.feature.dynamicdependencies.withspi.service;

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

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.dependency.DynamicDependencies;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary.spiConsumers.DependencySPIConsumer;
import org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary.spiConsumers.DynamicDependencySPIConsumer;
import org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary.spiConsumers.ExternalDependencySPIConsumer;
import org.talend.sdk.component.sample.feature.dynamicdependencies.withspi.config.Dataset;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DynamicDependenciesWithSPIService implements Serializable {

    private static String version;

    @Service
    private RecordBuilderFactory recordBuilderFactory;

    @DynamicDependencies
    public List<String> getDynamicDependencies(@Option("theDataset") final Dataset dataset) {
        String dep = "org.talend.sdk.samplefeature.dynamicdependencies:service-provider-from-dynamic-dependency:"
                + loadVersion();
        return Collections.singletonList(dep);
    }

    @DiscoverSchema("dyndepsdse")
    public Schema guessSchema4Input(final @Option("configuration") Dataset dse) {
        Iterator<Record> recordIterator = getRecordIterator();
        if (!recordIterator.hasNext()) {
            throw new ComponentException("No data loaded from StringMapTransformer.");
        }

        Record record = recordIterator.next();
        return record.getSchema();
    }

    public Iterator<Record> getRecordIterator() {
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
            boolean isFirst = true;
            Enumeration<URL> resources = DynamicDependenciesWithSPIService.class.getClassLoader()
                    .getResources("MULTIPLE_RESOURCE/common.properties");

            StringBuilder stringBuilder = new StringBuilder();
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();

                try (InputStream is = url.openStream()) {
                    String content = filterComments(is);
                    stringBuilder.append(content);
                    if (!isFirst) {
                        stringBuilder.append(System.lineSeparator());
                    }
                    isFirst = false;
                }
            }
            contentFromMultipleResources = stringBuilder.toString();
        } catch (IOException e) {
            throw new ComponentException("Can't retrieve multiple resources at once.", e);
        }

        DependencySPIConsumer<Record> dependencySPIConsumer = new DependencySPIConsumer<>(true);
        List<Record> recordsFromDependencySPI = dependencySPIConsumer
                .transform(s -> recordBuilderFactory.newRecordBuilder()
                        .withString("value", s)
                        .withString("contentFromResourceDependency", contentFromResourceDependency)
                        .withString("contentFromResourceDynamicDependency", contentFromResourceDynamicDependency)
                        .withString("contentFromResourceExternalDependency", contentFromResourceExternalDependency)
                        .withString("contentFromMultipleResources", contentFromMultipleResources)
                        .build());

        DynamicDependencySPIConsumer<Record> dynamicDependencySPIConsumer = new DynamicDependencySPIConsumer<>(true);
        List<Record> recordsFromDynamicDependencySPI = dynamicDependencySPIConsumer
                .transform(s -> recordBuilderFactory.newRecordBuilder()
                        .withString("value", s)
                        .withString("contentFromResourceDependency", contentFromResourceDependency)
                        .withString("contentFromResourceDynamicDependency", contentFromResourceDynamicDependency)
                        .withString("contentFromMultipleResources", contentFromMultipleResources)
                        .build());

        ExternalDependencySPIConsumer<Record> externalDependencySPI = new ExternalDependencySPIConsumer<>(true);
        List<Record> recordsFromExternalSPI = externalDependencySPI
                .transform(s -> recordBuilderFactory.newRecordBuilder()
                        .withString("value", s)
                        .withString("contentFromResourceDependency", contentFromResourceDependency)
                        .withString("contentFromResourceDynamicDependency", contentFromResourceDynamicDependency)
                        .withString("contentFromMultipleResources", contentFromMultipleResources)
                        .build());

        List<Record> values = new ArrayList<>();
        values.addAll(recordsFromDependencySPI);
        values.addAll(recordsFromDynamicDependencySPI);
        values.addAll(recordsFromExternalSPI);

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
            Properties prop = new Properties();
            prop.load(resourceStreamFromDependency);
            return prop.getProperty(property);
        } catch (IOException e) {
            throw new ComponentException("Can't retrieve resource from a dependency.", e);
        }
    }

}