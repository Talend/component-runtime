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
package org.talend.sdk.component.server.configuration;

import java.io.File;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.configuration.Configuration;

public class PropertiesSetup implements Meecrowave.ConfigurationCustomizer {

    @Override
    public void accept(final Configuration configuration) {
        // By default we want to skip vault calls zipkin logs, we can still override it...
        System.setProperty("geronimo.opentracing.client.filter.request.skip", "true");
        System.setProperty("geronimo.opentracing.filter.skippedTracing.urls", ".*/login$,.*/decrypt/.*");
        System.setProperty("geronimo.opentracing.filter.skippedTracing.matcherType", "regex");
        // environment dft ordinal 400
        final String httpPort = System.getenv("TALEND_COMPONENT_SERVER_PORT");
        if (httpPort != null) {
            System.setProperty("http", httpPort);
            configuration.setHttpPort(Integer.parseInt(httpPort));
        }
        final String log4jLayout = System.getenv("LOGGING_LAYOUT");
        final String appHome = System.getenv("TALEND_APP_HOME");
        if (log4jLayout != null && appHome != null) {
            final String initialConfig = System.getProperty("log4j.configurationFile", "default.properties");
            final String newConfig = String.format("%s/conf/log4j2-component-server-%s.xml", appHome, log4jLayout);
            if (!newConfig.equals(initialConfig)) {
                System.setProperty("log4j.configurationFile", newConfig);
                LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
                ctx.setConfigLocation(new File(newConfig).toURI());
                ctx.reconfigure();
                ctx.updateLoggers();
            }
        }
        //
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
