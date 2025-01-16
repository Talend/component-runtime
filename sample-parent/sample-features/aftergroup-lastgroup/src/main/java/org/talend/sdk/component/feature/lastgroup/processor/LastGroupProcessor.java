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
package org.talend.sdk.component.feature.lastgroup.processor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.LastGroup;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.feature.lastgroup.config.Config;

@Version
@Processor(name = "Processor")
@Icon(value = Icon.IconType.CUSTOM, custom = "icon")
@Documentation("AfterGroup / LastGroup sample processor connector.")
public class LastGroupProcessor implements Serializable {

    private final Config config;

    private List<Record> buffer;

    public LastGroupProcessor(final Config config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        buffer = new ArrayList<>();
    }

    @ElementListener
    public void process(@Input final Record input) {
        buffer.add(input);
    }

    @AfterGroup
    public void afterGroup(@Output final OutputEmitter<Record> main, @LastGroup final boolean lastGroup) {
        if (lastGroup) {
            if (buffer.size() != config.getExpectedNumberOfInputRecords()) {
                throw new IllegalStateException("Expected " +
                        config.getExpectedNumberOfInputRecords() + " records but got " + buffer.size());
            }
            buffer.stream().forEach(main::emit);
            buffer.clear();
        }
    }

}
