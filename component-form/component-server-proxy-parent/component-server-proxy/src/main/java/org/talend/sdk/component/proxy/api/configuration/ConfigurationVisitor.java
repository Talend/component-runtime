/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy.api.configuration;

import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.AllArgsConstructor;
import lombok.Data;

public interface ConfigurationVisitor {

    default void onEntry(ConfigurationEntry entry) {
        if (entry.isCredential()) {
            onCredential(entry);
        }
    }

    default void onCredential(final ConfigurationEntry entry) {
        // no-op
    }

    @Data
    @AllArgsConstructor
    class ConfigurationEntry {

        private final String key;

        private final String value;

        private final SimplePropertyDefinition definition;

        public boolean isCredential() {
            return Boolean.parseBoolean(definition.getMetadata().get("ui::credential"));
        }
    }
}
