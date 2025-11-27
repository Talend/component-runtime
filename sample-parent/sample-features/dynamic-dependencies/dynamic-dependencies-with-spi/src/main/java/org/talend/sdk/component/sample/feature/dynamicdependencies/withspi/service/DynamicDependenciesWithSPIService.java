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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.dependency.DynamicDependencies;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary.StringMapTransformer;
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
        String dep = "org.talend.sdk.samplefeature.dynamicdependencies:classloader-test-spi:"
                + loadVersion();
        System.out.println("Dynamic dependency to load: " + dep);
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
        String contentFromResourceDependency;
        try (InputStream resourceStreamFromDependency = DynamicDependenciesWithSPIService.class.getClassLoader()
                .getResourceAsStream("CLASSLOADER-TEST-LIBRARY/resource.properties")) {
            contentFromResourceDependency =
                    new String(resourceStreamFromDependency.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ComponentException("Can't retrieve resource from a dependency.", e);
        }

        String contentFromResourceDynamicDependency;
        try (InputStream resourceStreamFromDynamicDependency = DynamicDependenciesWithSPIService.class.getClassLoader()
                .getResourceAsStream("CLASSLOADER-TEST-SPI/resource.properties")) {
            contentFromResourceDynamicDependency =
                    new String(resourceStreamFromDynamicDependency.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ComponentException("Can't retrieve resource from a dependency.", e);
        }

        String contentFromMultipleResources;
        try {
            boolean isFirst = true;
            Enumeration<URL> resources = DynamicDependenciesWithSPIService.class.getClassLoader()
                    .getResources("MULTIPLE_RESOURCE/common.properties");

            StringBuilder stringBuilder = new StringBuilder();
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();

                try (InputStream is = url.openStream()) {
                    stringBuilder.append(isFirst ? "" : System.lineSeparator());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    reader.lines()
                            .filter(line -> !line.trim().startsWith("#"))
                            .filter(line -> !line.trim().isEmpty())
                            .forEach(stringBuilder::append);
                    reader.close();
                    isFirst = false;
                }
            }
            contentFromMultipleResources = stringBuilder.toString();
        } catch (IOException e) {
            throw new ComponentException("Can't retrieve multiple resources at once.", e);
        }

        StringMapTransformer<Record> stringMapTransformer = new StringMapTransformer<>(true);
        List<Record> records = stringMapTransformer
                .transform(s -> recordBuilderFactory.newRecordBuilder()
                        .withString("value", s)
                        .withString("contentFromResourceDependency", contentFromResourceDependency)
                        .withString("contentFromResourceDynamicDependency", contentFromResourceDynamicDependency)
                        .withString("contentFromMultipleResources", contentFromMultipleResources)
                        .build());
        return records.iterator();
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

}