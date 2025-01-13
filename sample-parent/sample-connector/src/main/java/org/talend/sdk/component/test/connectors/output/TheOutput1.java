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
package org.talend.sdk.component.test.connectors.output;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.test.connectors.config.OutputConfig;
import org.talend.sdk.component.test.connectors.migration.AbstractMigrationHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Version(value = OutputConfig.OUTPUT_CONFIG_VERSION,
        migrationHandler = AbstractMigrationHandler.OutputMigrationHandler.class)
@Icon(value = Icon.IconType.CUSTOM, custom = "output")
@Processor(name = "TheOutput1")
@Documentation("Doc: default TheOutput1 documentation without Internationalization.")
public class TheOutput1 implements Serializable {

    /*
     * An Output is a Processor that does not return any data.
     * Conceptually, an output is a data listener. It matches the concept of processor. Being the
     * last component of the execution chain or returning no data makes your processor an
     * output component.
     */

    private OutputConfig config;

    public TheOutput1(final @Option("configout") OutputConfig config) {

        this.config = config;
    }

    @PostConstruct
    public void init() {
    }

    @PreDestroy
    public void release() {
    }

    @ElementListener
    public void onNext(@Input final Record record) {
        // skip empty record
        if (record != null && record.getSchema().getEntries().isEmpty()) {
            log.info("[onNext] Skipping empty record.");
            return;
        }
        log.info("[onNext] manage record.");
    }

}
