/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.design.extension.migration;

import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.design.extension.RepositoryModel;
import org.talend.sdk.component.design.extension.repository.Config;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.test.MetadataMigrationProcessor;

@Disabled
class ConfigurationMigrationTest {

    @Test
    void migrateMetadata() {
        try (final ComponentManager manager = new ComponentManager(new File("target/test-dependencies"),
                "META-INF/test/dependencies", "org.talend.test:type=plugin,value=%s")) {
            final String plugin = manager.addPlugin(jarLocation(MetadataMigrationProcessor.class).getAbsolutePath());

            final Config config = manager.findPlugin(plugin).get().get(RepositoryModel.class)
                    .getFamilies().stream().filter(f -> f.getMeta().getName().equals("metadata"))
                    .map(f -> f.getConfigs().iterator().next()).findFirst().get();

            final MigrationHandler handler = findRootMigrationHandler(config);
            final Map<String, String> migrated = handler.migrate(1, new HashMap<String, String>() {

                {
                    put("__version", "1");
                    put("option", "value");
                    put("datastore.connection", "http://talend.com");
                    put("datastore.__version", "1");
                }
            });

            assertNotNull(migrated);
            assertEquals("value", migrated.get("config"));
            assertEquals("http://talend.com", migrated.get("datastore.url"));
        }

    }

    private MigrationHandler findRootMigrationHandler(final Config config) {
        if ("dataset".equals(config.getMeta().getMetadata().get("tcomp::configurationtype::type"))) {
            return config.getMigrationHandler();
        }
        return config.getChildConfigs().stream().filter(c -> "dataset".equals(c.getMeta().getMetadata().get("tcomp::configurationtype::type")))
                .map(Config::getMigrationHandler)
                .findFirst().orElseGet(() -> config.getChildConfigs()
                        .stream().filter(c -> !c.getChildConfigs().isEmpty())
                        .map(this::findRootMigrationHandler)
                        .findFirst().get());
    }

}
