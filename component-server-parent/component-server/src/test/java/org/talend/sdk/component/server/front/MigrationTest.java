/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.test.ComponentClient;
import org.talend.sdk.component.server.test.migration.MigrationDataSet;
import org.talend.sdk.component.server.test.migration.MigrationDataStore;
import org.talend.sdk.component.server.test.migration.MigrationInput;

@MonoMeecrowaveConfig
public class MigrationTest {

    @Inject
    private ComponentClient client;

    @Inject
    private WebTarget base;

    private final String dataStoreId = "bWlncmF0aW9uLWNvbXBvbmVudCNtaWdyYXRpb24jZGF0YXN0b3JlI2RhdGFzdG9yZQ";

    private final String dataSetId = "bWlncmF0aW9uLWNvbXBvbmVudCNtaWdyYXRpb24jZGF0YXNldCNkYXRhc2V0";

    /**
     * component tests
     * registry version: 5
     */
    @Test
    void migrateComponentLower() {
        final int incomingVersion = 3;
        Map<String, String> conf = new HashMap<>();
        conf.put("configuration.inputKey", "keylevel0");
        final Map<String, String> migrated = migrateComponent(getInputComponentId(), incomingVersion, conf);
        assertEquals("INPUT", migrated.get("level"));
        assertEquals(String.valueOf(MigrationInput.Version), migrated.get("currentVersion"));
        assertEquals(String.valueOf(incomingVersion), migrated.get("incomingVersion"));
    }

    @Test
    void migrateComponentEqual() {
        Map<String, String> conf = new HashMap<>();
        conf.put("configuration.inputKey", "keylevel0");
        final Map<String, String> migrated = migrateComponent(getInputComponentId(), 5, conf);
        assertEquals(conf, migrated);
    }

    @Test
    void migrateComponentGreater() {
        Map<String, String> conf = new HashMap<>();
        conf.put("configuration.inputKey", "keylevel0");
        final Map<String, String> migrated = migrateComponent(getInputComponentId(), 6, conf);
        assertEquals(conf, migrated);
    }

    /**
     * dataset tests
     * registry version: undefined.
     */
    @Test
    void migrateDataSetLower() {
        final int incomingVersion = -1;
        Map<String, String> conf = new HashMap<>();
        conf.put("configuration.dataSetKey", "keylevel0");
        final Map<String, String> migrated = migrateConfigurationType(getDataSetID(), incomingVersion, conf);
        assertEquals("DATASET", migrated.get("configuration.level"));
        assertEquals(String.valueOf(MigrationDataSet.Version), migrated.get("configuration.currentVersion"));
        assertEquals(String.valueOf(incomingVersion), migrated.get("configuration.incomingVersion"));
    }

    @Test
    void migrateDataSetEqual() {
        Map<String, String> conf = new HashMap<>();
        conf.put("configuration.dataSetKey", "keylevel0");
        // Version value not set defaults to 1
        final Map<String, String> migrated = migrateConfigurationType(getDataSetID(), 1, conf);
        assertEquals(conf, migrated);
    }

    @Test
    void migrateDataSetGreater() {
        Map<String, String> conf = new HashMap<>();
        conf.put("configuration.dataSetKey", "keylevel0");
        final Map<String, String> migrated = migrateConfigurationType(getDataSetID(), 3, conf);
        assertEquals(conf, migrated);
    }

    /**
     * datastore tests
     * registry version: 2
     */
    @Test
    void migrateDataStoreLower() {
        final int incomingVersion = 1;
        Map<String, String> conf = new HashMap<>();
        conf.put("configuration.dataStoreKey", "keylevel0");
        final Map<String, String> migrated = migrateConfigurationType(getDataStoreID(), incomingVersion, conf);
        assertEquals("DATASTORE", migrated.get("configuration.level"));
        assertEquals(String.valueOf(MigrationDataStore.Version), migrated.get("configuration.currentVersion"));
        assertEquals(String.valueOf(incomingVersion), migrated.get("configuration.incomingVersion"));
    }

    @Test
    void migrateDataStoreEqual() {
        Map<String, String> conf = new HashMap<>();
        conf.put("configuration.dataStoreKey", "keylevel0");
        final Map<String, String> migrated = migrateConfigurationType(getDataStoreID(), 2, conf);
        assertEquals(conf, migrated);
    }

    @Test
    void migrateDataStoreGreater() {
        Map<String, String> conf = new HashMap<>();
        conf.put("configuration.dataStoreKey", "keylevel0");
        final Map<String, String> migrated = migrateConfigurationType(getDataStoreID(), 3, conf);
        assertEquals(conf, migrated);
    }

    /**
     * end tests
     */

    private Map<String, String> migrateComponent(final String component, final int version,
            final Map<String, String> config) {
        return callMigrate("component", component, version, config);
    }

    private Map<String, String> migrateConfigurationType(final String configuration, final int version,
            final Map<String, String> config) {
        return callMigrate("configurationtype", configuration, version, config);
    }

    private Map<String, String> callMigrate(final String endpoint, final String id, final int version,
            final Map<String, String> config) {
        return base
                .path(String.format("/%s/migrate/%s/%d", endpoint, id, version))
                .request(APPLICATION_JSON_TYPE)
                .post(Entity.entity(config, APPLICATION_JSON_TYPE))
                .readEntity(Map.class);
    }

    private String getInputComponentId() {
        return client.getComponentId("migration", "Input");
    }

    private String getDataSetID() {
        ConfigTypeNodes index = client.fetchConfigTypeNodes();
        return index.getNodes().get(dataSetId).getId();
    }

    private String getDataStoreID() {
        ConfigTypeNodes index = client.fetchConfigTypeNodes();
        return index.getNodes().get(dataStoreId).getId();
    }

}
