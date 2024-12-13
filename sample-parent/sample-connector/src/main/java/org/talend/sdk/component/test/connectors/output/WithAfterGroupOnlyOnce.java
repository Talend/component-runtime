/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.test.connectors.output;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.LastGroup;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * This output connector should consume all input record but have
 * only one call to @AfterGroup method after all input records are consumed.
 */

@Slf4j
@Version(1)
@Icon(value = Icon.IconType.CUSTOM, custom = "output")
@Processor(name = "WithAfterGroupOnlyOnce")
@Documentation("Consume all input records and should have only 1 call to @AfterGroup.")
public class WithAfterGroupOnlyOnce implements Serializable {

    private final RecordBuilderFactory recordBuilderFactory;

    private final WithAfterGroupOnlyOnceConfig config;

    private int nbConsumedRecords;

    private boolean afterGroupCalled;

    public WithAfterGroupOnlyOnce(final @Option("configuration") WithAfterGroupOnlyOnceConfig config,
            final RecordBuilderFactory recordBuilderFactory) {
        this.recordBuilderFactory = recordBuilderFactory;
        this.config = config;
    }

    @PostConstruct
    public void init() {
        this.nbConsumedRecords = 0;
    }

    @PreDestroy
    public void release() {
        if (!this.afterGroupCalled) {
            throw new RuntimeException("The @AfterGroup method has not been called.");
        }
    }

    @ElementListener
    public void onNext(@Input final Record record) {
        this.nbConsumedRecords++;
    }

//    @AfterGroup
//    public void afterGroup(@Output("REJECT") final OutputEmitter<Record> rejected) {
//        log.error("after group -- no Last" + ", record count = " + nbConsumedRecords);
//        this.afterGroupCalled = true;
//    }

    @AfterGroup
    public void afterGroup(@Output("REJECT") final OutputEmitter<Record> rejected, @LastGroup final boolean isLast) {
        log.error("--------last = " + isLast + ", record count = " + nbConsumedRecords);
        this.afterGroupCalled = true;
    }

    @Data
    @GridLayout({ @GridLayout.Row({ "expectedNumberOfRecords" }) })
    public static class WithAfterGroupOnlyOnceConfig implements Serializable {

        @Option
        @Documentation("The number of expected record processed when @AfterGroup is called.")
        private int expectedNumberOfRecords;

    }

}
