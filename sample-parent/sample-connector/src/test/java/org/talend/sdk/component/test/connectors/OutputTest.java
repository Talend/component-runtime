/*
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.talend.sdk.component.test.connectors;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.junit.BaseComponentsHandler;
import org.talend.sdk.component.junit5.Injected;
import org.talend.sdk.component.junit5.WithComponents;
import org.talend.sdk.component.runtime.manager.chain.Job;

@WithComponents("org.talend.sdk.component.test.connectors")
public class OutputTest {

    @Injected
    protected BaseComponentsHandler componentsHandler;

    private final String testStringValue = "test";

    private final boolean testBooleanValue = true;

    @Test
    void testOutput() {
        final int recordSize = 15;

        Record testRecord = componentsHandler
                .findService(RecordBuilderFactory.class)
                .newRecordBuilder()
                .withString("stringValue", testStringValue)
                .withBoolean("booleanValue", testBooleanValue)
                .build();

        List<Record> testRecords = new ArrayList<>();
        for (int i = 0; i < recordSize; i++) {
            testRecords.add(testRecord);
        }
        componentsHandler.setInputData(testRecords);

        Job
                .components()
                .component("inputFlow", "test://emitter")
                .component("outputComponent", "Sample://WithAfterGroupOnlyOnce?$maxBatchSize=10")
                .connections()
                .from("inputFlow")
                .to("outputComponent")
                .build()
                .run();

        Assert.assertTrue(true);
    }
}
