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

import javax.inject.Inject;

import org.talend.sdk.component.server.test.meecrowave.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.Data;

@MonoMeecrowaveConfig
class PropertiesServiceTest {

    @Inject
    private PropertiesService propertiesService;

    @Test
    void buildProperties() {

        ParameterMeta host = new ParameterMeta(Config.class, ParameterMeta.Type.STRING, "configuration.host", "host",
                null, emptyList(), null, emptyMap());
        ParameterMeta port = new ParameterMeta(Config.class, ParameterMeta.Type.NUMBER, "configuration.port", "port",
                null, emptyList(), null, emptyMap());

        ParameterMeta username = new ParameterMeta(Config.class, ParameterMeta.Type.STRING, "configuration.username",
                "username", null, emptyList(), null, emptyMap());

        ParameterMeta password = new ParameterMeta(Config.class, ParameterMeta.Type.STRING, "configuration.password",
                "password", null, emptyList(), null, emptyMap());

        ParameterMeta config = new ParameterMeta(Config.class, ParameterMeta.Type.OBJECT, "configuration",
                "configuration", null, asList(host, port, username, password), null, emptyMap());

        final List<SimplePropertyDefinition> props = propertiesService
                .buildProperties(singletonList(config), getClass().getClassLoader(), Locale.getDefault(), null)
                .collect(toList());

        assertEquals(5, props.size());
        assertEquals("Configuration", props.get(0).getDisplayName());
        assertEquals("Server Host Name", props.get(1).getDisplayName());
        assertEquals("Password", props.get(2).getDisplayName());
        assertEquals("Server Port", props.get(3).getDisplayName());
        assertEquals("User Name", props.get(4).getDisplayName());
    }

    @Test // the class BaseConfig don't contains attribute
    void validateProp() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParameterMeta attribute = new ParameterMeta(BadConfig.class, ParameterMeta.Type.STRING,
                    "configuration.attribute", "attribute", null, emptyList(), null, emptyMap());

            ParameterMeta config = new ParameterMeta(Config.class, ParameterMeta.Type.OBJECT, "configuration",
                    "configuration", null, singletonList(attribute), null, emptyMap());

            propertiesService
                    .buildProperties(singletonList(config), getClass().getClassLoader(), Locale.getDefault(), null)
                    .collect(toList());
        });
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
