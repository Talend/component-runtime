/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.test.connectors.migration;

import java.util.HashMap;
import java.util.Map;

import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.test.connectors.config.TheDataset;
import org.talend.sdk.component.test.connectors.config.TheDatastore;
import org.talend.sdk.component.test.connectors.config.InputConfig;
import org.talend.sdk.component.test.connectors.config.OutputConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMigrationHandler implements MigrationHandler {

    protected abstract String getLevel();

    protected abstract int getCurrentVersion();

    // Todo
    @Override
    public Map<String, String> migrate(final int incomingVersion, final Map<String, String> incomingData) {

        Map<String, String> migrated = new HashMap<>(incomingData);
        migrated.put("level", this.getLevel());
        migrated.put("incomingVersion", "" + incomingVersion);
        migrated.put("currentVersion", "" + this.getCurrentVersion());

        return migrated;
    }

    public static class DatastoreMigrationHandler extends AbstractMigrationHandler {

        @Override
        protected String getLevel() {
            return "DATASTORE";
        }

        @Override
        protected int getCurrentVersion() {
            return TheDatastore.DATASTORE_VERSION;
        }
    }

    public static class DatasetMigrationHandler extends AbstractMigrationHandler {

        @Override
        protected String getLevel() {
            return "DATASET";
        }

        @Override
        protected int getCurrentVersion() {
            return TheDataset.DATASET_VERSION;
        }
    }

    public static class InputMigrationHandler extends AbstractMigrationHandler {

        @Override
        protected String getLevel() {
            return "INPUT";
        }

        @Override
        protected int getCurrentVersion() {
            return InputConfig.INPUT_CONFIG_VERSION;
        }
    }

    public static class OutputMigrationHandler extends AbstractMigrationHandler {

        @Override
        protected String getLevel() {
            return "OUTPUT";
        }

        @Override
        protected int getCurrentVersion() {
            return OutputConfig.OUTPUT_CONFIG_VERSION;
        }
    }

}
