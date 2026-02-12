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
package org.talend.sdk.component.sample.feature.loadinganalysis;

import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_CLASS;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_CLAZZ_CLASSLOADER;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_CONNECTOR_CLASSLOADER;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_FROM_LOCATION;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_IS_LOADED;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_IS_TCK_CONTAINER;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_MAVEN;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_ROOT_REPOSITORY;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_RUNTIME_CLASSPATH;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_WORKING_DIRECTORY;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.sample.feature.loadinganalysis.config.Dependency;
import org.talend.sdk.component.sample.feature.loadinganalysis.config.DynamicDependencyConfig;
import org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService;

import lombok.Getter;

public abstract class AbstractDynamicDependenciesServiceTest<C extends DynamicDependencyConfig, S extends AbstractDynamicDependenciesService> {

    @Getter
    private C config;

    protected abstract C buildConfig();

    protected abstract S getService();

    @BeforeEach
    void setUp() {
        this.config = this.buildConfig();
    }

    @Test
    void testLoadIterator() {
        System.setProperty("talend.component.manager.m2.repository", "./lib/");

        final Iterator<Record> result = getService().loadIterator(config);

        List<Record> records = new ArrayList<>();
        result.forEachRemaining(records::add);
        Assertions.assertEquals(4, records.size());

        Record fourth = records.get(3);
        ResultDetails expected = new ResultDetails(
                false,
                "./lib/",
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "org.apache.commons:commons-numbers-primes:1.2",
                System.getProperty("user.dir"),
                true,
                "org/apache/commons/commons-numbers-primes/1.2/commons-numbers-primes-1.2.jar!/org/apache/commons/numbers/primes/SmallPrimes.class",
                null,
                "org.apache.commons.numbers.primes.SmallPrimes",
                "commons-numbers-primes-1.2.jar");
        this.assertRecord(fourth, expected);
    }

    protected List<Dependency> getDependList() {
        List<Dependency> depends = new ArrayList<>();
        Dependency depend = new Dependency();
        depend.setArtifactId("commons-numbers-primes");
        depend.setVersion("1.2");
        depend.setGroupId("org.apache.commons");
        depend.setClazz("org.apache.commons.numbers.primes.SmallPrimes");
        depends.add(depend);
        return depends;
    }

    private void assertRecord(Record record, ResultDetails expected) {
        Assertions.assertNotNull(record);

        Assertions.assertEquals(expected.isTckContainer(), record.getBoolean(ENTRY_IS_TCK_CONTAINER));
        Assertions.assertEquals(expected.rootRepository(), record.getString(ENTRY_ROOT_REPOSITORY));
        Assertions.assertTrue(record.getString(ENTRY_CONNECTOR_CLASSLOADER)
                .startsWith(expected.connectorClassloader()));
        Assertions.assertTrue(record.getString(ENTRY_CLAZZ_CLASSLOADER).startsWith(expected.clazzClassloader()));
        Assertions.assertEquals(expected.maven(), record.getString(ENTRY_MAVEN));
        Assertions.assertEquals(expected.workingDirectory(), record.getString(ENTRY_WORKING_DIRECTORY));
        Assertions.assertEquals(expected.isLoaded(), record.getBoolean(ENTRY_IS_LOADED));
        Assertions.assertTrue(record.getString(ENTRY_FROM_LOCATION).endsWith(expected.fromLocation()));
        Assertions.assertEquals(expected.clazz(), record.getString(ENTRY_CLASS));
        Assertions.assertTrue(record.getString(ENTRY_RUNTIME_CLASSPATH).contains(expected.runtimeClasspath()));

    }

    public record ResultDetails(
            boolean isTckContainer,
            String rootRepository,
            String connectorClassloader,
            String clazzClassloader,
            String maven,
            String workingDirectory,
            boolean isLoaded,
            String fromLocation,
            String comment,
            String clazz,
            String runtimeClasspath
    ) {
    }
}