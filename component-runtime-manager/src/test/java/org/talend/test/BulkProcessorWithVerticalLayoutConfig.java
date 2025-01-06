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
package org.talend.test;

import java.io.Serializable;

import javax.json.JsonObject;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.VerticalLayout;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;

import lombok.Data;

@Processor(family = "bulkProcessor", name = "BulkProcessorWithVerticalLayoutConfig")
public class BulkProcessorWithVerticalLayoutConfig implements Serializable {

    public BulkProcessorWithVerticalLayoutConfig(@Option("configuration") final Config config) {
    }

    @AfterGroup
    public void afterGroup(@Output final OutputEmitter<JsonObject> out) {
    }

    @ElementListener
    public void process(@Input("in") final JsonObject in, @Output final OutputEmitter<JsonObject> out) {
        if (in == null) {
            return;
        }
        out.emit(in);
    }

    @Data
    @VerticalLayout
    public static class Config {

        private int config;
    }
}
