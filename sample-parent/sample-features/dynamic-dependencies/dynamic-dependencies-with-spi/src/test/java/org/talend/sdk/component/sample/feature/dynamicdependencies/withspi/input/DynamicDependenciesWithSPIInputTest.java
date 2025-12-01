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
package org.talend.sdk.component.sample.feature.dynamicdependencies.withspi.input;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.junit.BaseComponentsHandler;
import org.talend.sdk.component.junit.SimpleFactory;
import org.talend.sdk.component.junit5.Injected;
import org.talend.sdk.component.junit5.WithComponents;
import org.talend.sdk.component.runtime.manager.chain.Job;
import org.talend.sdk.component.sample.feature.dynamicdependencies.withspi.config.Config;

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

        List<Record> collectedData = handler.getCollectedData(Record.class);
        Assertions.assertEquals(9, collectedData.size());

        int i = 0;
        for (; i < 3; i++) {
            Assertions.assertEquals("ServiceProviderFromDependency_" + (i + 1),
                    collectedData.get(i).getString("value"));
        }

        for (; i < 6; i++) {
            Assertions.assertEquals("ServiceProviderFromDynamicDependency_" + (i - 2),
                    collectedData.get(i).getString("value"));
        }

        for (; i < 9; i++) {
            Assertions.assertEquals("ServiceProviderFromExternalDependency_" + (i - 5),
                    collectedData.get(i).getString("value"));
        }

    }

}