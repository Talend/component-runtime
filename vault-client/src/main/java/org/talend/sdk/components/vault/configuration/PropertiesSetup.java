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
package org.talend.sdk.components.vault.configuration;

import java.util.Properties;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.configuration.Configuration;

public class PropertiesSetup implements Meecrowave.ConfigurationCustomizer {

    @Override
    public void accept(final Configuration configuration) {
        configuration.loadFromProperties(System.getProperties());
        if (configuration.getProperties() == null) {
            configuration.setProperties(new Properties());
        }
        configuration.getProperties().putAll(System.getProperties());
        configuration
                .getProperties()
                .stringPropertyNames()
                .stream()
                .filter(k -> System.getProperty(k) == null)
                .forEach(k -> System.setProperty(k, configuration.getProperties().getProperty(k)));
    }
}
