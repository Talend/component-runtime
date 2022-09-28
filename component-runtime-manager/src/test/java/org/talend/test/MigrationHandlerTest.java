/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
import java.util.List;

import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.runtime.manager.component.AbstractMigrationHandler;

import lombok.Data;

@Data
@Version(value = 2, migrationHandler = MigrationHandlerTest.ComponentMigration.class)
@Processor(family = "chain", name = "migrationtest")
public class MigrationHandlerTest implements Serializable {

    private final ConfigDataset config;

    @ElementListener
    public String length(final String data) {
        return config.getName();
    }

    public static class ComponentMigration extends AbstractMigrationHandler {

        @Override
        public void migrate(final int incomingVersion) {
            try {
                changeValue("config.datastore.component", "yes"); // need to override datastore migration
            } catch (MigrationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void doSplitProperty(final String oldKey, final List<String> newKeys) {
            throw new UnsupportedOperationException("#doSplitProperty()");
        }

        @Override
        public void doMergeProperties(final List<String> oldKeys, final String newKey) {
            throw new UnsupportedOperationException("#doMergeProperties()");
        }
    }

    @Data
    @DataSet
    @Version(value = 2, migrationHandler = ConfigDataset.DatasetMigration.class)
    public static class ConfigDataset {

        @Option
        private String name;

        @Option
        private ConfigDatastore datastore;

        public static class DatasetMigration extends AbstractMigrationHandler {

            @Override
            public void migrate(final int incomingVersion) {
                try {
                    addKey("name", "dataset");
                } catch (MigrationException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public void doSplitProperty(final String oldKey, final List<String> newKeys) {
                throw new UnsupportedOperationException("#doSplitProperty()");
            }

            @Override
            public void doMergeProperties(final List<String> oldKeys, final String newKey) {
                throw new UnsupportedOperationException("#doMergeProperties()");
            }
        }
    }

    @Data
    @DataStore
    @Version(value = 2, migrationHandler = ConfigDatastore.DatastoreMigration.class)
    public static class ConfigDatastore {

        @Option
        private String name;

        private String component;

        public static class DatastoreMigration extends AbstractMigrationHandler {

            @Override
            public void migrate(final int incomingVersion) {
                try {
                    addKey("name", "datastore");
                    addKey("component", "no");
                } catch (MigrationException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public void doSplitProperty(final String oldKey, final List<String> newKeys) {
                throw new UnsupportedOperationException("#doSplitProperty()");
            }

            @Override
            public void doMergeProperties(final List<String> oldKeys, final String newKey) {
                throw new UnsupportedOperationException("#doMergeProperties()");
            }
        }
    }
}
