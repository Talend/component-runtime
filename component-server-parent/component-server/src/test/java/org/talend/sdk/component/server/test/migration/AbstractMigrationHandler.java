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
package org.talend.sdk.component.server.test.migration;

import java.util.HashMap;
import java.util.Map;

import org.talend.sdk.component.api.component.MigrationHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMigrationHandler implements MigrationHandler {

    protected abstract String getLevel();

    protected abstract int getCurrentVersion();

    @Override
    public Map<String, String> migrate(int incomingVersion, Map<String, String> incomingData) {
        Map<String, String> migrated = new HashMap<>(incomingData);
        migrated.put("level", getLevel());
        migrated.put("incomingVersion", String.valueOf(incomingVersion));
        migrated.put("currentVersion", String.valueOf(getCurrentVersion()));
        return migrated;
    }

    public static class InputHandler extends AbstractMigrationHandler {

        @Override
        protected String getLevel() {
            return "INPUT";
        }

        @Override
        protected int getCurrentVersion() {
            return MigrationInput.Version;
        }

    }

    public static class DataSetHandler extends AbstractMigrationHandler {

        @Override
        protected String getLevel() {
            return "DATASET";
        }

        @Override
        protected int getCurrentVersion() {
            return MigrationDataSet.Version;
        }

    }

    public static class DataStoreHandler extends AbstractMigrationHandler {

        @Override
        protected String getLevel() {
            return "DATASTORE";
        }

        @Override
        protected int getCurrentVersion() {
            return MigrationDataStore.Version;
        }
    }
}
