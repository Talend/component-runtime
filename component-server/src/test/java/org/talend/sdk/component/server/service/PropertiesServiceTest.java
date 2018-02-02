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
package org.talend.sdk.component.server.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.server.test.meecrowave.MonoMeecrowaveConfig;

import lombok.Data;

@MonoMeecrowaveConfig class PropertiesServiceTest {

    @Inject
    private PropertiesService propertiesService;

    private static void boolWrapper(final BoolBool wrapper) {
        // no-op
    }

    @Test
    void booleanDefault() throws NoSuchMethodException {
        final List<SimplePropertyDefinition> props =
                propertiesService
                        .buildProperties(
                                new ParameterModelService().buildParameterMetas(
                                        getClass().getDeclaredMethod("boolWrapper", BoolBool.class), null),
                                Thread.currentThread().getContextClassLoader(), Locale.ROOT, null)
                        .collect(toList());
        assertEquals("true", props.stream().filter(p -> p.getName().equals("val")).findFirst().get().getDefaultValue());
    }

    @Test
    void buildProperties() {
        final String[] i18nPackages = { Config.class.getPackage().getName() };

        final ParameterMeta host = new ParameterMeta(null, Config.class, ParameterMeta.Type.STRING,
                "configuration.host", "host", i18nPackages, emptyList(), null, emptyMap());
        final ParameterMeta port = new ParameterMeta(null, Config.class, ParameterMeta.Type.NUMBER,
                "configuration.port", "port", i18nPackages, emptyList(), null, emptyMap());
        final ParameterMeta username = new ParameterMeta(null, Config.class, ParameterMeta.Type.STRING,
                "configuration.username", "username", i18nPackages, emptyList(), null, emptyMap());
        final ParameterMeta password = new ParameterMeta(null, Config.class, ParameterMeta.Type.STRING,
                "configuration.password", "password", i18nPackages, emptyList(), null, emptyMap());
        final ParameterMeta config = new ParameterMeta(null, Config.class, ParameterMeta.Type.OBJECT, "configuration",
                "configuration", i18nPackages, asList(host, port, username, password), null, emptyMap());

        final List<SimplePropertyDefinition> props = propertiesService
                .buildProperties(singletonList(config), getClass().getClassLoader(), Locale.getDefault(), null)
                .collect(toList());

        assertEquals(5, props.size());
        assertEquals("Configuration", props.get(0).getDisplayName());
        assertEquals("Server Host Name", props.get(1).getDisplayName());
        assertEquals("Enter the server host name...", props.get(1).getPlaceholder());
        assertEquals("Password", props.get(2).getDisplayName());
        assertEquals("Server Port", props.get(3).getDisplayName());
        assertEquals("Enter the server port...", props.get(3).getPlaceholder());
        assertEquals("User Name", props.get(4).getDisplayName());
    }

    @Test // the class BaseConfig don't contains attribute
    void validateProp() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParameterMeta attribute = new ParameterMeta(null, BadConfig.class, ParameterMeta.Type.STRING,
                    "configuration.attribute", "attribute", null, emptyList(), null, emptyMap());

            ParameterMeta config = new ParameterMeta(null, Config.class, ParameterMeta.Type.OBJECT, "configuration",
                    "configuration", null, singletonList(attribute), null, emptyMap());

            propertiesService
                    .buildProperties(singletonList(config), getClass().getClassLoader(), Locale.ROOT, null)
                    .forEach(Objects::nonNull);
        });
    }

    @Data
    public static class BoolBool {

        @Option
        private BoolWrapper wrapper;
    }

    @Data
    public static class BoolWrapper {

        @Option
        private boolean val = true;
    }

    @Data
    public static class Config {

        private String host;

        private int port;

        private String username;

        private String password;

    }

    @Data
    public static class BadConfig {
        // no attribute
    }

}
