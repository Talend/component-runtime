/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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
import java.util.Map;

import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;

import lombok.Data;

@Processor(family = "metadata", name = "MetadataMigrationProcessor")
public class MetadataMigrationProcessor implements Serializable {

    public MetadataMigrationProcessor(@Option("configuration") final Config config) {
        // no-op
    }

    @ElementListener
    public void onElement(@Input final Record in) {
        // no-op
    }

    public static class Config {

        @Option
        private MyDataSet dataset;
    }

    @Data
    @DataSet
    @Version(value = 2, migrationHandler = MyDataSet.InputMapperDataSetHandler.class)
    @GridLayout({ @GridLayout.Row({ "dataStore" }), @GridLayout.Row({ "config" }) })
    @Documentation("")
    public static class MyDataSet implements Serializable {

        @Option
        private MyDataStore dataStore;

        @Option
        private String config;

        public static class InputMapperDataSetHandler implements MigrationHandler {

            @Override
            public Map<String, String> migrate(final int incomingVersion, final Map<String, String> incomingData) {
                final String value = incomingData.remove("option");
                if (value != null) {
                    incomingData.put("config", value);
                }
                return incomingData;
            }
        }

    }

    @Data
    @DataStore
    @Version(value = 2, migrationHandler = MyDataStore.InputMapperDataStoretHandler.class)
    @GridLayout({ @GridLayout.Row({ "url" }) })
    @Documentation("")
    public static class MyDataStore {

        @Option
        private String url;

        public static class InputMapperDataStoretHandler implements MigrationHandler {

            @Override
            public Map<String, String> migrate(final int incomingVersion, final Map<String, String> incomingData) {
                final String value = incomingData.remove("connection");
                if (value != null) {
                    incomingData.put("url", value);
                }
                return incomingData;
            }
        }
    }

}