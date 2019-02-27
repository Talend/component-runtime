/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.stitch.server;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.apache.meecrowave.testing.ConfigurationInject;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.extension.stitch.model.Model;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

@MonoMeecrowaveConfig
class ModelResourceTest {

    @ConfigurationInject
    private Meecrowave.Builder builder;

    @Test
    void get() {
        final Client client = ClientBuilder.newClient();
        final WebTarget api = client.target("http://localhost:" + builder.getHttpPort() + "/api/v1");
        try {
            final Model model = api.path("model").request(APPLICATION_JSON_TYPE).get(Model.class);

            assertConfigurations(model.getConfigurations().stream().collect(toMap(ConfigTypeNode::getId, identity())));
            assertComponents(model.getComponents());
        } finally {
            client.close();
        }
    }

    private void assertConfigurations(final Map<String, ConfigTypeNode> configurations) {
        assertEquals(6, configurations.size());
        final ConfigTypeNode postGreDataStore = configurations
                .get("ZXh0ZW5zaW9uOjpzdGl0Y2g6OmRhdGFzdG9yZTo6cGxhdGZvcm0ucG9zdGdyZXM6OlpYaDBaVzV6YVc5dU9qcHpkR2wwWTJnNk9tWmhiV2xzZVRvNmNHeGhkR1p2Y20wdWNHOXpkR2R5WlhNPQ==");
        assertNotNull(postGreDataStore);
        assertEquals(1, postGreDataStore.getEdges().size());

        final String pgDataSet =
                "ZXh0ZW5zaW9uOjpzdGl0Y2g6OmRhdGFzZXQ6OnBsYXRmb3JtLnBvc3RncmVzOjpaWGgwWlc1emFXOXVPanB6ZEdsMFkyZzZPbVpoYldsc2VUbzZjR3hoZEdadmNtMHVjRzl6ZEdkeVpYTT0=";
        assertEquals(pgDataSet, postGreDataStore.getEdges().iterator().next());
        assertPostgresProperties(configurations.get(pgDataSet).getProperties().iterator(), "configuration");

        final ConfigTypeNode family = configurations.get(postGreDataStore.getParentId());
        assertNotNull(family);
        assertNull(family.getParentId());
    }

    private void assertComponents(final Collection<ComponentDetail> components) {
        final Iterator<ComponentDetail> componentIt = components.iterator();
        {
            final ComponentDetail value = componentIt.next();
            assertEquals("Stitch", value.getId().getFamily());
            assertEquals("platform.postgres", value.getId().getName());
            assertEquals("Stitch Postgres", value.getDisplayName());
            assertPostgres(value);
        }
        {
            final ComponentDetail value = componentIt.next();
            assertEquals("Stitch", value.getId().getFamily());
            assertEquals("platform.mysql", value.getId().getName());
            assertEquals("Stitch Mysql", value.getDisplayName());
            assertMySql(value);
        }
    }

    private void assertPostgres(final ComponentDetail detail) {
        assertEquals("Stitch", detail.getId().getFamily());
        assertEquals("platform.postgres", detail.getId().getName());
        assertEquals("Stitch Postgres", detail.getDisplayName());
        assertEquals(11, detail.getProperties().size());

        final Iterator<SimplePropertyDefinition> properties = detail.getProperties().iterator();
        assertPostgresProperties(properties, "configuration.dataset");
    }

    private void assertMySql(final ComponentDetail detail) {
        assertEquals("Stitch", detail.getId().getFamily());
        assertEquals("platform.mysql", detail.getId().getName());
        assertEquals("Stitch Mysql", detail.getDisplayName());
        assertEquals(9, detail.getProperties().size());

        final Iterator<SimplePropertyDefinition> properties = detail.getProperties().iterator();
        assertProperty(properties.next(), "configuration", "configuration", "", null, "OBJECT",
                singletonMap("stitch::component", "platform.mysql"), null, null);
        assertProperty(properties.next(), "configuration.dataset", "dataset", "Mysql", null, "OBJECT", emptyMap(), null,
                null);
        assertProperty(properties.next(), "configuration.dataset.configuration", "configuration", "", null, "OBJECT",
                singletonMap("stitch::form", "form"), null, null);
        assertProperty(properties.next(), "configuration.dataset.configuration.datastore", "datastore",
                "Mysql Connection", null, "OBJECT", emptyMap(), null, null);
        // skip stitch connection props, already validated with postgres
        properties.next();
        properties.next();
        properties.next();
        // specific config
        assertProperty(properties.next(), "configuration.dataset.configuration.step_form", "step_form", "Configuration",
                null, "OBJECT", emptyMap(), null, null);
        assertProperty(properties.next(), "configuration.dataset.configuration.step_form.dbname", "dbname", "DB Name",
                null, "STRING", emptyMap(), true, null);
    }

    private void assertPostgresProperties(final Iterator<SimplePropertyDefinition> properties, final String prefix) {
        final String dataPrefix;
        if ("configuration.dataset".equals(prefix)) {
            assertProperty(properties.next(), "configuration", "configuration", "", null, "OBJECT",
                    singletonMap("stitch::component", "platform.postgres"), null, null);
            assertProperty(properties.next(), prefix, prefix.substring(prefix.lastIndexOf('.') + 1), "Postgres", null,
                    "OBJECT", emptyMap(), null, null);
            assertProperty(properties.next(), prefix + ".configuration", "configuration", "", null, "OBJECT",
                    singletonMap("stitch::form", "form"), null, null);
            assertProperty(properties.next(), prefix + ".configuration.datastore", "datastore", "Postgres Connection",
                    null, "OBJECT", emptyMap(), null, null);
            assertProperty(properties.next(), prefix + ".configuration.datastore.stitchConnection", "stitchConnection",
                    "Stitch Connection", null, "OBJECT", emptyMap(), null, null);
            assertProperty(properties.next(), prefix + ".configuration.datastore.stitchConnection.url", "url", "URL",
                    "https://api.stitchdata.com/v4/", "STRING", emptyMap(), true, null);
            assertProperty(properties.next(), prefix + ".configuration.datastore.stitchConnection.token", "token",
                    "OAuth2 Token", null, "STRING", singletonMap("ui::credential", "true"), true, null);
            dataPrefix = prefix + ".configuration";
        } else { // config == datastore
            assertProperty(properties.next(), "configuration", "configuration", "", null, "OBJECT",
                    singletonMap("stitch::form", "form"), null, null);
            assertProperty(properties.next(), prefix + ".datastore", "datastore", "Postgres Connection", null, "OBJECT",
                    emptyMap(), null, null);
            assertProperty(properties.next(), prefix + ".datastore.stitchConnection", "stitchConnection",
                    "Stitch Connection", null, "OBJECT", emptyMap(), null, null);
            assertProperty(properties.next(), prefix + ".datastore.stitchConnection.url", "url", "URL",
                    "https://api.stitchdata.com/v4/", "STRING", emptyMap(), true, null);
            assertProperty(properties.next(), prefix + ".datastore.stitchConnection.token", "token", "OAuth2 Token",
                    null, "STRING", singletonMap("ui::credential", "true"), true, null);
            dataPrefix = prefix;
        }
        assertProperty(properties.next(), dataPrefix + ".step_form", "step_form", "Configuration", null, "OBJECT",
                emptyMap(), null, null);
        assertProperty(properties.next(), dataPrefix + ".step_form.image_version", "image_version", "Image Version",
                null, "STRING", emptyMap(), true, null);
        assertProperty(properties.next(), dataPrefix + ".step_form.frequency_in_minutes", "frequency_in_minutes",
                "Frequency In Minutes", null, "STRING", emptyMap(), true, "^1$|^30$|^60$|^360$|^720$|^1440$");
        assertProperty(properties.next(), dataPrefix + ".step_form.anchor_time", "anchor_time", "Anchor Time", null,
                "STRING", emptyMap(), false,
                "^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2})\\:(\\d{2})\\:(\\d{2})[+-](\\d{2})\\:(\\d{2})");
    }

    private void assertProperty(final SimplePropertyDefinition next, final String path, final String name,
            final String displayName, final String defaultValue, final String type, final Map<String, String> metadata,
            final Boolean required, final String pattern) {
        assertEquals(path, next.getPath());
        assertEquals(name, next.getName());
        assertEquals(displayName, next.getDisplayName());
        assertEquals(defaultValue, next.getDefaultValue());
        assertEquals(type, next.getType());
        assertEquals(metadata, next.getMetadata());
        assertNotNull(next.getValidation());
        assertEquals(required, next.getValidation().getRequired());
        assertEquals(pattern, next.getValidation().getPattern());
    }
}
