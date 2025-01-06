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
package org.talend.sdk.component.server.test.jdbc;

import java.io.Serializable;

import javax.json.JsonObject;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Icon(Icon.IconType.DB_INPUT)
@Processor(family = "jdbc", name = "output")
public class JdbcOutput implements Serializable {

    private final ConfigWrapper dataset;

    public JdbcOutput(@Option("configuration") final ConfigWrapper dataset) {
        this.dataset = dataset;
    }

    @ElementListener
    public void onElement(final JsonObject object) {
        // no-op
    }

    @AfterGroup
    public void afterGroup() {
        // just to get $maxBatchSize
    }

    @Data
    public static class ConfigWrapper {

        @Option
        private JdbcDataSet dataSet;

        @Option
        private Type type;
    }

    public enum Type {
        FAST,
        PRECISE
    }
}
