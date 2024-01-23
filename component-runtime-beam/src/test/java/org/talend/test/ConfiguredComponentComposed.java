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
package org.talend.test;

import static java.util.Collections.singletonMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;

import lombok.Data;

@Data
@Version(value = 1, migrationHandler = ConfiguredComponentComposed.Handler.class)
@Processor(family = "chain", name = "configured2")
public class ConfiguredComponentComposed implements Serializable {

    private final Config config;

    private final String value;

    @ElementListener
    public String length(final String data) {
        return config.getName();
    }

    public static class Handler implements MigrationHandler {

        @Override
        public Map<String, String> migrate(final int incomingVersion, final Map<String, String> incomingData) {
            return new HashMap<String, String>() {

                {
                    put("value", "set");
                    putAll(incomingData);
                }
            };
        }
    }

    @Data
    @DataSet
    @Version(value = 1, migrationHandler = Config.ConfigHandler.class)
    public static class Config {

        private String name;

        public static class ConfigHandler implements MigrationHandler {

            @Override
            public Map<String, String> migrate(final int incomingVersion, final Map<String, String> incomingData) {
                return singletonMap("name", "ok");
            }
        }
    }
}
