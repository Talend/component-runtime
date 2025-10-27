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

import static org.talend.sdk.component.sample.feature.dynamicdependencies.service.DynamicDependenciesService.ENTRY_CLASS;
import static org.talend.sdk.component.sample.feature.dynamicdependencies.service.DynamicDependenciesService.ENTRY_FROM_LOCATION;
import static org.talend.sdk.component.sample.feature.dynamicdependencies.service.DynamicDependenciesService.ENTRY_MAVEN;
import static org.talend.sdk.component.sample.feature.dynamicdependencies.service.DynamicDependenciesService.ENTRY_ROOT_REPOSITORY;
import static org.talend.sdk.component.sample.feature.dynamicdependencies.service.DynamicDependenciesService.ENTRY_RUNTIME_CLASSPATH;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.junit5.WithComponents;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Config;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Dataset;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Datastore;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithComponents(value = "org.talend.sdk.component.sample.feature.dynamicdependencies")
public class DynamicDependenciesServiceTest {

    @Service
    DynamicDependenciesService dynamicDependenciesServiceService;

    private Config config;

    @BeforeEach
    void setUp() {
        // Any setup required before each test can be done here
        config = new Config();
        Dataset dse = new Dataset();
        Datastore dso = new Datastore();
        List<Dataset.Dependency> depends = new ArrayList<>();
        Dataset.Dependency depend = new Dataset.Dependency();
        depend.setArtifactId("commons-numbers-primes");
        depend.setVersion("1.2");
        depend.setGroupId("org.apache.commons");
        depend.setClazz("org.apache.commons.numbers.primes.SmallPrimes");
        depends.add(depend);

        dse.setDependencies(depends);
        dse.setDso(dso);
        config.setDse(dse);
    }

    @Test
    void testloadIterator() {
        final Iterator<Record> result = dynamicDependenciesServiceService.loadIterator(config);

        Assertions.assertTrue(result.hasNext());
        final Record record = result.next();
        Assertions.assertNotNull(record);
        // Assertions.assertEquals(" ", record.getString(ENTRY_CLASSLOADER));
        Assertions.assertEquals(" ", record.getString(ENTRY_MAVEN));
        Assertions.assertEquals(" ", record.getString(ENTRY_CLASS));
        Assertions.assertEquals(" ", record.getString(ENTRY_FROM_LOCATION));
        Assertions.assertEquals(" ", record.getString(ENTRY_ROOT_REPOSITORY));
        Assertions.assertEquals(" ", record.getString(ENTRY_RUNTIME_CLASSPATH));
        Assertions.assertFalse(result.hasNext());
    }
}
