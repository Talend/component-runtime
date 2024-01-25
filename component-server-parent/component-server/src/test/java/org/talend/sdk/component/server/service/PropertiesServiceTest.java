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
package org.talend.sdk.component.server.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.Data;

@MonoMeecrowaveConfig
class PropertiesServiceTest {

    @Inject
    private PropertiesService propertiesService;

    private static void gridLayout(@Option final WithLayout layout) {
        // no-op
    }

    private static void boolWrapper(@Option final BoolBool wrapper) {
        // no-op
    }

    private static void multipleParams(@Option("aa") final String first, @Option("b") final BoolWrapper config,
            @Option("a") final String last) {
        // no-op
    }

    @Test
    void gridLayoutTranslation() throws NoSuchMethodException {
        final List<ParameterMeta> params = new ParameterModelService(new PropertyEditorRegistry())
                .buildParameterMetas(getClass().getDeclaredMethod("gridLayout", WithLayout.class), null,
                        new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test")));
        final List<SimplePropertyDefinition> props = propertiesService
                .buildProperties(params, Thread.currentThread().getContextClassLoader(), Locale.ROOT, null)
                .collect(toList());
        assertEquals(3, props.size());

        final Map<String, String> metadata = props.iterator().next().getMetadata();
        assertEquals(3, metadata.size());
        assertEquals("first", metadata.get("ui::gridlayout::NumeroUn::value"));
        assertEquals("second", metadata.get("ui::gridlayout::NumeroDeux::value"));
    }

    @Test
    void parameterIndexMeta() throws NoSuchMethodException {
        final List<ParameterMeta> params = new ParameterModelService(new PropertyEditorRegistry())
                .buildParameterMetas(
                        getClass().getDeclaredMethod("multipleParams", String.class, BoolWrapper.class, String.class),
                        null, new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test")));
        final List<SimplePropertyDefinition> props = propertiesService
                .buildProperties(params, Thread.currentThread().getContextClassLoader(), Locale.ROOT, null)
                .collect(toList());
        assertEquals(4, props.size());
        props.forEach(p -> {
            final boolean hasIndex = p.getMetadata().containsKey("definition::parameter::index");
            final boolean isNested = p.getPath().contains(".");
            assertTrue((isNested && !hasIndex) || (!isNested && hasIndex));
        });
        props.sort(comparing(p -> Integer.parseInt(p.getMetadata().getOrDefault("definition::parameter::index", "4"))));
        assertEquals("aa/b/a/b.val", props.stream().map(SimplePropertyDefinition::getPath).collect(joining("/")));
    }

    @Test
    void booleanDefault() throws NoSuchMethodException {
        final List<SimplePropertyDefinition> props = propertiesService
                .buildProperties(
                        new ParameterModelService(new PropertyEditorRegistry())
                                .buildParameterMetas(getClass().getDeclaredMethod("boolWrapper", BoolBool.class), null,
                                        new BaseParameterEnricher.Context(
                                                new LocalConfigurationService(emptyList(), "tools"))),
                        Thread.currentThread().getContextClassLoader(), Locale.ROOT, null)
                .collect(toList());
        assertEquals("true", props.stream().filter(p -> p.getName().equals("val")).findFirst().get().getDefaultValue());
    }

    private List<SimplePropertyDefinition> getProperties(final String locale) {
        final String[] i18nPackages = { Config.class.getPackage().getName() };

        final ParameterMeta host = new ParameterMeta(null, Config.class, ParameterMeta.Type.STRING,
                "configuration.host", "host", i18nPackages, emptyList(), null, emptyMap(), false);
        final ParameterMeta port = new ParameterMeta(null, Config.class, ParameterMeta.Type.NUMBER,
                "configuration.port", "port", i18nPackages, emptyList(), null, emptyMap(), false);
        final ParameterMeta username = new ParameterMeta(null, Config.class, ParameterMeta.Type.STRING,
                "configuration.username", "username", i18nPackages, emptyList(), null, emptyMap(), false);
        final ParameterMeta password = new ParameterMeta(null, Config.class, ParameterMeta.Type.STRING,
                "configuration.password", "password", i18nPackages, emptyList(), null, emptyMap(), false);
        final ParameterMeta config = new ParameterMeta(null, Config.class, ParameterMeta.Type.OBJECT, "configuration",
                "configuration", i18nPackages, asList(host, port, username, password), null, emptyMap(), false);
        return propertiesService
                .buildProperties(singletonList(config), getClass().getClassLoader(), Locale.forLanguageTag(locale),
                        null)
                .collect(toList());
    }

    @Test
    void buildPropertiesEn() {
        final List<SimplePropertyDefinition> props = getProperties("en");
        assertEquals(5, props.size());
        assertEquals("Configuration", props.get(0).getDisplayName());
        assertEquals("Server Host Name", props.get(1).getDisplayName());
        assertEquals("Enter the server host name...", props.get(1).getPlaceholder());
        assertEquals("Password", props.get(2).getDisplayName());
        assertEquals("Server Port", props.get(3).getDisplayName());
        assertEquals("Enter the server port...", props.get(3).getPlaceholder());
        assertEquals("User Name", props.get(4).getDisplayName());
    }

    @Test
    void buildPropertiesFr() {
        final List<SimplePropertyDefinition> props = getProperties("fr");
        assertEquals(5, props.size());
        assertEquals("Server Host Name FR", props.get(1).getDisplayName());
        assertEquals("Enter the server host name FR...", props.get(1).getPlaceholder());
        assertEquals("Password FR", props.get(2).getDisplayName());
        assertEquals("Server Port FR", props.get(3).getDisplayName());
        assertEquals("Enter the server port FR...", props.get(3).getPlaceholder());
        assertEquals("User Name FR", props.get(4).getDisplayName());
    }

    @Test // the class BaseConfig don't contains attribute
    void validateProp() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParameterMeta attribute = new ParameterMeta(null, BadConfig.class, ParameterMeta.Type.STRING,
                    "configuration.attribute", "attribute", null, emptyList(), null, emptyMap(), false);

            ParameterMeta config = new ParameterMeta(null, Config.class, ParameterMeta.Type.OBJECT, "configuration",
                    "configuration", null, singletonList(attribute), null, emptyMap(), false);

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

        @Option
        private String host;

        private int port;

        private String username;

        private String password;

    }

    @GridLayout(names = "NumberOne", value = @GridLayout.Row("first"))
    @GridLayout(names = "NumberTwo", value = @GridLayout.Row("second"))
    public static class WithLayout {

        @Option
        private String first;

        @Option
        private String second;
    }

    @Data
    public static class BadConfig {
        // no attribute
    }

}
