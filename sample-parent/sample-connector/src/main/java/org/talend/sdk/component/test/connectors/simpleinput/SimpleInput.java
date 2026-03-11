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
package org.talend.sdk.component.test.connectors.simpleinput;

import java.io.Serializable;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.test.connectors.config.SimpleInputConfiguration;

@Version
@Icon(value = Icon.IconType.CUSTOM, custom = "mapper")
@Emitter(name = "simple-input")
@Documentation("A simple emitter that doesn't expect dynamic dependencies and generated few records.")
public class SimpleInput implements Serializable {

    private RecordBuilderFactory recordBuilderFactory;

    private final SimpleInputConfiguration configuration;

    private boolean done = false;

    public SimpleInput(@Option("configuration") final SimpleInputConfiguration configuration,
            final RecordBuilderFactory recordBuilderFactory) {
        this.configuration = configuration;
        this.recordBuilderFactory = recordBuilderFactory;
    }

    @Producer
    public Record next() {
        if (done) {
            return null;
        }

        done = true;

        return recordBuilderFactory.newRecordBuilder()
                .withString("url", configuration.getDataset().getDatastore().getUrl())
                .withString("token", configuration.getDataset().getDatastore().getToken())
                .withString("resource", configuration.getDataset().getResource())
                .withBoolean("debug", configuration.isDebug())
                .build();
    }

}