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
package org.talend.sdk.component.server.service;

import lombok.Data;
import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.client.WebTarget;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

@MonoMeecrowaveConfig
class PropertiesServiceTest {

    @Inject
    private PropertiesService propertiesService;

    @Inject
    private WebTarget base;

    private ConfigTypeNode connection;

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

    @Test // the class BaseConfig don't contains attribute
    void validateConfigNode() {
        final JsonBuilderFactory factory = Json.createBuilderFactory(emptyMap());
        final List<String> uniques = Arrays.asList("one", "two", "three");
        final List<String> notUniques = Arrays.asList("one", "two", "three", "one");

        String activeIfsError = "- Property 'configuration.connection.activedIfs' is required.";
        String connectionError = "- Property 'configuration.connection' is required.";
        String passwordError = "- Property 'configuration.connection.password' is required.";
        String url1Required = "- Property 'configuration.connection.url1' is required.";
        String url0Error = "- 'configuration.connection.url0' does not match '^https?://.*'.";
        String url1Error = "- 'configuration.connection.url1' does not match '^https?://.*'.";
        String usernameError = "- Property 'configuration.connection.username' is required.";
        String minError = "- Property 'configuration.limit' should be > 100, got 99.";
        String maxError = "- Property 'configuration.limit' should be < 150, got 200.";

        connection = base
                .path("configurationtype/details")
                .queryParam("identifiers", "Y29tcG9uZW50LXdpdGgtdXNlci1qYXJzI2N1c3RvbSNkYXRhc2V0I2RhdGFzZXQ")
                .request(APPLICATION_JSON_TYPE)
                .header("Accept-Encoding", "gzip")
                .get(ConfigTypeNodes.class)
                .getNodes().values().iterator().next();

        JsonObject payload;

        /**
         * min/max
         **/
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("url0", "http://t")
                                .add("username", "abc")
                                .add("password", "aaa")
                                .build())
                        .add("limit", 110))
                .build();
        checkErrors(payload, Arrays.asList(connectionError, url1Required));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("url0", "https://t")
                                .add("url1", "https://t")
                                .add("username", "abc")
                                .add("password", "aaa")
                                .build())
                        .add("limit", 99))
                .build();
        checkErrors(payload, Arrays.asList(connectionError, minError));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "abc")
                                .add("password", "abc")
                                .add("url0", "https://t")
                                .add("url1", "https://t")
                                .build())
                        .add("limit", 200))
                .build();
        checkErrors(payload, Arrays.asList(connectionError, maxError));

        /*
         * url0 pattern
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "abc")
                                .add("password", "abc")
                                .add("url0", "https://www.talend.com")
                                .build()))
                .build();
        checkErrors(payload, Arrays.asList(connectionError, url1Required));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "abc")
                                .add("password", "abc")
                                .add("url0", "mailto://toto@titi.org")
                                .build()))
                .build();
        checkErrors(payload, Arrays.asList(connectionError, url0Error, url1Required));

        /*
         * url1 pattern
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "abc")
                                .add("password", "abc")
                                .add("url1", "mailto://toto@titi.org")
                                .build()))
                .build();
        checkErrors(payload, Arrays.asList(connectionError, url1Error));


        /*
         * uniqVals
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("url0", JsonValue.NULL)
                                .add("url1", JsonValue.NULL)
                                .add("username", "abc")
                                .add("password", JsonValue.NULL)
                                .add("uniqVals", factory.createArrayBuilder(notUniques).build())
                                .add("checkbox1", JsonValue.TRUE)
                                .add("checkbox2", JsonValue.TRUE)
                                .build())
                        .add("limit", 100))
                .build();
        checkErrors(payload, Arrays.asList(connectionError, url1Required));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("url0", JsonValue.NULL)
                                .add("url1", JsonValue.NULL)
                                .add("username", JsonValue.NULL)
                                .add("password", JsonValue.NULL)
                                .add("uniqVals", factory.createArrayBuilder(uniques).build())
                                .add("checkbox1", JsonValue.TRUE)
                                .add("checkbox2", JsonValue.TRUE)
                                .build())
                        .add("limit", 100))
                .build();

        checkErrors(payload, Arrays.asList(connectionError, url1Required, usernameError));

        /*
         * password : @ActiveIf(target = "username", evaluationStrategy = ActiveIf.EvaluationStrategy.CONTAINS, value = "undx")
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "undx")
                                .add("password", JsonValue.NULL).build())
                        .build()).build();
        checkErrors(payload, Arrays.asList(connectionError, passwordError, url1Required));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "abcd")
                                .add("password", JsonValue.NULL).build())
                        .build()).build();
        checkErrors(payload, Arrays.asList(connectionError, url1Required));


        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "undx")
                                .add("password", "abc").build())
                        .build()).build();
        checkErrors(payload, Arrays.asList(connectionError, url1Required));

        /*
         *   valueEval     @ActiveIf(target = "checkbox1", value = "true")
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("checkbox1", JsonValue.TRUE)
                                .add("valueEval", JsonValue.NULL).build())
                        .build()).build();
        checkErrors(payload, Arrays.asList(connectionError, url1Required, usernameError));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("checkbox1", JsonValue.FALSE)
                                .add("valueEval", JsonValue.NULL).build())
                        .build()).build();
        checkErrors(payload, Arrays.asList(connectionError, url1Error));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("checkbox1", JsonValue.TRUE)
                                .add("valueEval", "VALUE_2").build())
                        .build()).build();
        checkErrors(payload, Arrays.asList(connectionError, url1Error));
    }

    private void checkErrors(JsonObject payload, List<String> expected) {
        try {
            propertiesService.validate(connection, payload);
            if (expected != null && expected.size() >0) {
                fail("There should be errors: "+expected);
            }
        } catch (Exception errors) {
            StringBuffer expectedBuffer = new StringBuffer();
            expected.stream().forEach(e -> {
                expectedBuffer.append(e).append("\n");
            });
            expectedBuffer.delete(expectedBuffer.lastIndexOf("\n"), expectedBuffer.length());
            assertEquals(expectedBuffer.toString(), errors.getMessage());
        }
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
