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

import static java.util.Collections.singletonMap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.runtime.manager.component.AbstractMigrationHandler;

import lombok.Data;

@Data
@Processor(family = "chain", name = "configured1")
public class ConfiguredComponentNestedOnly implements Serializable {

    private final Config config;

    @ElementListener
    public String length(final String data) {
        return config.getName();
    }

    @Data
    @DataSet
    @Version(value = 1, migrationHandler = Config.ConfigHandler.class)
    public static class Config {

        private String name;

        public static class ConfigHandler extends AbstractMigrationHandler {

            @Override
            public void migrate(final int incomingVersion) {
                try {
                    addKey("name", "ok");
                } catch (MigrationException e) {

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
