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
package org.talend.sdk.component.sample.feature.loadinganalysis.withspi.input;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.junit.BaseComponentsHandler;
import org.talend.sdk.component.junit.SimpleFactory;
import org.talend.sdk.component.junit5.Injected;
import org.talend.sdk.component.junit5.WithComponents;
import org.talend.sdk.component.runtime.manager.chain.Job;
import org.talend.sdk.component.sample.feature.loadinganalysis.withspi.config.Config;

@WithComponents("org.talend.sdk.component.sample.feature.dynamicdependencies.withspi")
class DynamicDependenciesWithSPIInputTest {

    @Injected
    protected BaseComponentsHandler handler;

    @Test
    public void testGeneratedRecord() {
        Config config = new Config();
        String queryString = SimpleFactory.configurationByExample().forInstance(config).configured().toQueryString();

        Job.components()
                .component("input", "DynamicDependenciesWithSPI://Input?" + queryString)
                .component("collector", "test://collector")
                .connections()
                .from("input")
                .to("collector")
                .build()
                .run();

        List<Record> records = handler.getCollectedData(Record.class);
        Assertions.assertEquals(4, records.size());

        Result expected0 = new Result(
                "interface org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary.serviceInterfaces.StringProviderSPIAsDependency",
                "class org.talend.sdk.component.sample.feature.dynamicdependencies.serviceproviderfromdependency.ServiceProviderFromDependency",
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "SPI implementation loaded from a dependency.",
                "ServiceProviderFromDependency value");
        validate(expected0, records.get(0));

        Result expected1 = new Result(
                "interface org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary.serviceInterfaces.StringProviderSPIAsDynamicDependency",
                "class org.talend.sdk.component.sample.feature.dynamicdependencies.serviceproviderfromdynamicdependency.ServiceProviderFromDynamicDependency",
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "SPI implementation loaded from a dynamic dependency.",
                "ServiceProviderFromDynamicDependency value");
        validate(expected1, records.get(1));

        Result expected2 = new Result(
                "interface org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary.serviceInterfaces.StringProviderFromExternalSPI",
                "class org.talend.sdk.component.sample.feature.dynamicdependencies.serviceproviderfromexternaldependency.ServiceProviderFromExternalDependency",
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "jdk.internal.loader.ClassLoaders$AppClassLoader",
                "SPI implementation loaded from a runtime/provided dependency.",
                "ServiceProviderFromExternalDependency value");
        validate(expected2, records.get(2));

        Result expected3 = new Result(
                "N/A",
                "N/A",
                "N/A",
                "N/A",
                "Resources loading.",
                "{\"contentFromResourceDependency\":\"Message from a dependency resource.\",\"contentFromResourceDynamicDependency\":\"Message from a dynamic dependency resource.\",\"contentFromResourceExternalDependency\":\"Message from an external dependency resource.\",\"contentFromMultipleResources\":\"There should be 3 different values:\\nContent from static dependency\\nContent from dynamic dependency\\nContent from external dependency\"}");
        validate(expected3, records.get(3));
    }

    private void validate(Result expected, Record record) {
        Assertions.assertEquals(expected.spiInterface(), record.getString("SPI_Interface"));
        Assertions.assertEquals(expected.spiImpl(), record.getString("SPI_Impl"));
        Assertions.assertTrue(
                record.getString("SPI_Interface_classloader").startsWith(expected.spiInterfaceClassloader()));
        Assertions.assertTrue(record.getString("SPI_Impl_classloader").startsWith(expected.spiImplClassloader()));
        Assertions.assertEquals(expected.comment(), record.getString("comment"));
        Assertions.assertEquals(expected.value(), record.getString("value"));
    }

    private record Result(
            String spiInterface,
            String spiImpl,
            String spiInterfaceClassloader,
            String spiImplClassloader,
            String comment,
            String value
    ) {
    }

}