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
package org.talend.sdk.component.sample.feature.actionsinforms;

import java.io.Serializable;
import java.util.Iterator;

import javax.annotation.PostConstruct;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.record.Record;

@Version
@Icon(value = Icon.IconType.CUSTOM, custom = "icon")
@Emitter(name = "Input")
@Documentation("A simple source connector that doesn't generate any record.")
public class Input implements Serializable {

    private static final long serialVersionUID = 1L;

    private final InputConfiguration configuration;

    private final SimpleService service;

    private Iterator<org.talend.sdk.component.api.record.Record> dataIterator;

    public Input(@Option("configuration") final InputConfiguration configuration,
            final SimpleService service) {
        this.configuration = configuration;
        this.service = service;
    }

    @PostConstruct
    public void postConstruct() {
        this.dataIterator = service.getDataIterator(configuration);
    }

    @Producer
    public Record next() {
        if (dataIterator == null || !dataIterator.hasNext()) {
            return null; // indicates end of data
        }
        return dataIterator.next();
    }
}