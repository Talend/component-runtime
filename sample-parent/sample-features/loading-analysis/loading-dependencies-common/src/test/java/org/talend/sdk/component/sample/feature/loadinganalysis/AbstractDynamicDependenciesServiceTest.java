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
package org.talend.sdk.component.sample.feature.loadinganalysis;

import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_CLASS;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_CLAZZ_CLASSLOADER;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_CONNECTOR_CLASSLOADER;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_FROM_LOCATION;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_IS_LOADED;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_IS_TCK_CONTAINER;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_MAVEN;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_RUNTIME_CLASSPATH;
import static org.talend.sdk.component.sample.feature.loadinganalysis.service.AbstractDynamicDependenciesService.ENTRY_WORKING_DIRECTORY;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.sample.feature.loadinganalysis.config.Connector;
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
        final Iterator<Record> result = getService().loadIterator(config);

        List<Record> records = new ArrayList<>();
        result.forEachRemaining(records::add);
        Assertions.assertEquals(4, records.size());

        Record record = records.get(0);
        ResultDetails expected = new ResultDetails(
                false,
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "org.talend.sdk.component.loading-analysis:loading-dependencies-common:N/A",
                System.getProperty("user.dir"),
                true,
                "org/talend/sdk/component/sample/feature/loadinganalysis/config/Dependency.class",
                "Hardcoded 'static' dependency test.",
                "org.talend.sdk.component.sample.feature.loadinganalysis.config.Dependency",
                "commons-numbers-primes-1.2.jar");
        this.assertRecord(record, expected);

        record = records.get(1);
        expected = new ResultDetails(
                false,
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "org.talend.sdk.component:component-runtime:N/A",
                System.getProperty("user.dir"),
                true,
                "org/talend/sdk/component/api/service/asyncvalidation/ValidationResult.class",
                "Hardcoded provided dependency test.",
                "org.talend.sdk.component.api.service.asyncvalidation.ValidationResult",
                "component-runtime-impl");
        this.assertRecord(record, expected);

        record = records.get(2);
        expected = new ResultDetails(
                false,
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "org.apache.maven.resolver:maven-resolver-api:2.0.14",
                System.getProperty("user.dir"),
                true,
                "org/eclipse/aether/artifact/DefaultArtifact.class",
                "Hardcoded dynamic dependency test. The instantiated object has been assigned.",
                "org.eclipse.aether.artifact.DefaultArtifact",
                "maven-resolver-api-2.0.14.jar");
        this.assertRecord(record, expected);

        record = records.get(3);
        expected = new ResultDetails(
                false,
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "org.apache.commons:commons-numbers-primes:1.2",
                System.getProperty("user.dir"),
                true,
                "org/apache/commons/commons-numbers-primes/1.2/commons-numbers-primes-1.2.jar!/org/apache/commons/numbers/primes/SmallPrimes.class",
                null,
                "org.apache.commons.numbers.primes.SmallPrimes",
                "commons-numbers-primes-1.2.jar");
        this.assertRecord(record, expected);
    }

    @Test
    void testGuessSchema4Input() {
        Schema schema = this.getService().buildSchema();
        Map<String, Entry> entries = schema.getAllEntries().collect(Collectors.toMap(Entry::getName, e -> e));
        Assertions.assertEquals(12, entries.size());
        Assertions.assertEquals(Type.BOOLEAN, entries.get("is_tck_container").getType());
        Assertions.assertEquals(Type.STRING, entries.get("root_repository").getType());
        Assertions.assertEquals(Type.STRING, entries.get("clazz_classloader").getType());
        Assertions.assertEquals(Type.STRING, entries.get("maven").getType());
        Assertions.assertEquals(Type.RECORD, entries.get("first_record").getType());
        Assertions.assertEquals(Type.BOOLEAN, entries.get("is_loaded").getType());
        Assertions.assertEquals(Type.STRING, entries.get("from_location").getType());
        Assertions.assertEquals(Type.STRING, entries.get("working_directory").getType());
        Assertions.assertEquals(Type.STRING, entries.get("comment").getType());
        Assertions.assertEquals(Type.STRING, entries.get("connector_classloader").getType());
        Assertions.assertEquals(Type.STRING, entries.get("clazz").getType());
        Assertions.assertEquals(Type.STRING, entries.get("runtime_classpath").getType());
    }

    protected List<Dependency> getDynamicDependenciesConfigurationList() {
        // This dependency is added in dynamic dependencies list
        List<Dependency> depends = new ArrayList<>();
        Dependency depend = new Dependency();
        depend.setArtifactId("commons-numbers-primes");
        depend.setVersion("1.2");
        depend.setGroupId("org.apache.commons");
        depend.setClazz("org.apache.commons.numbers.primes.SmallPrimes");
        depends.add(depend);
        return depends;
    }

    protected List<Connector> getDynamicDependenciesConnectorsConfigurationList() {
        // Return an empty list since currently this is not supported at unit test time.
        // It is not possible to load another connector in unit test.
        return Collections.emptyList();
    }

    private void assertRecord(Record record, ResultDetails expected) {
        Assertions.assertNotNull(record);

        Assertions.assertEquals(expected.isTckContainer(), record.getBoolean(ENTRY_IS_TCK_CONTAINER));
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

    /**
     * rootRepository is present for debugging information, there is no need to check its value in unit test.
     * Moreover, its value can be different on different machines, so it is not possible to set a fixed value for it in
     * the expected result.
     */
    public record ResultDetails(
            boolean isTckContainer,
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

    protected static String getVersion() {
        String version;
        try (InputStream is = AbstractDynamicDependenciesServiceTest.class.getClassLoader()
                .getResourceAsStream("version.properties")) {
            if (is == null) {
                throw new ComponentException("Can't retrieve version.properties resource.");
            }
            Properties props = new Properties();
            props.load(is);
            version = props.getProperty("version");
        } catch (IOException e) {
            throw new ComponentException("Unable to load project version", e);
        }
        return version;
    }
}