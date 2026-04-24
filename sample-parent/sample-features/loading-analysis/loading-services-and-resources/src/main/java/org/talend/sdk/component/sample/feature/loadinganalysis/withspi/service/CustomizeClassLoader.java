/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.sample.feature.loadinganalysis.withspi.service;

import java.util.stream.Stream;

import org.talend.sdk.component.runtime.manager.ComponentManager;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom class loader customizer for component runtime.
 * <p>
 * This customizer allows specific classes and resources to be loaded from the parent classloader
 * instead of the component's isolated classloader.
 * </p>
 */
@Slf4j
public class CustomizeClassLoader implements ComponentManager.Customizer {

    private static final String DISABLE_CUSTOMIZE_PROPERTY =
            "org.talend.sdk.component.sample.feature.loadinganalysis.withspi.service.CustomizeClassLoader.disabled";

    private static final boolean DISABLE_CUSTOMIZE =
            Boolean.parseBoolean(System.getProperty(DISABLE_CUSTOMIZE_PROPERTY, "false"));

    private static final String EXTERNAL_SPI_CLASS =
            "org.talend.sdk.component.sample.feature.loadinganalysis.classloadertestlibrary.serviceInterfaces.StringProviderFromExternalSPI";

    private static final String MULTIPLE_RESOURCE_PATH = "MULTIPLE_RESOURCE/content.txt";

    private static final String SPI_SERVICE_PATH =
            "META-INF/services/org.talend.sdk.component.sample.feature.loadinganalysis.classloadertestlibrary.serviceInterfaces.StringProviderFromExternalSPI";

    private static final String RESOURCE_LOADED_FROM_EXTERNAL_DEPENDENCY =
            "FROM_EXTERNAL_DEPENDENCY/resource.properties";

    /**
     * Specifies which classes and packages should be loaded from the container classloader.
     * <p>
     * When disabled (default), returns an empty stream.
     * When enabled, returns the list of classes that should be shared across components.
     * </p>
     *
     * @return A stream of class/package names to be loaded from the parent classloader
     */
    @Override
    public Stream<String> containerClassesAndPackages() {
        if (DISABLE_CUSTOMIZE) {
            logDisabledState();
            return Stream.empty();
        }

        logEnabledState();
        return Stream.of(EXTERNAL_SPI_CLASS);
    }

    /**
     * Specifies which resources should be loaded from the parent classloader.
     * <p>
     * This includes resource files and service provider configuration files.
     * </p>
     *
     * @return A stream of resource paths to be loaded from the parent classloader
     */
    @Override
    public Stream<String> parentResources() {
        if (DISABLE_CUSTOMIZE) {
            logDisabledState();
            return Stream.empty();
        }

        logEnabledState();
        return Stream.of(MULTIPLE_RESOURCE_PATH, SPI_SERVICE_PATH, RESOURCE_LOADED_FROM_EXTERNAL_DEPENDENCY);
    }

    /**
     * Logs a message indicating that the customizer is disabled.
     */
    private void logDisabledState() {
        log.info("{} is disabled.\nUse \"{}=false\" property to enable it.",
                CustomizeClassLoader.class.getName(),
                DISABLE_CUSTOMIZE_PROPERTY);
    }

    /**
     * Logs a message indicating that the customizer is enabled.
     */
    private void logEnabledState() {
        log.info("{} is enabled.\nUse \"{}=true\" property to disable it.",
                CustomizeClassLoader.class.getName(),
                DISABLE_CUSTOMIZE_PROPERTY);
    }
}