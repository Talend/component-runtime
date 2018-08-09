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
package org.talend.sdk.component.proxy.service;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.proxy.front.ConfigurationTypeResource;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.proxy.test.CdiInject;
import org.talend.sdk.component.proxy.test.WithServer;

@CdiInject
@WithServer
class ConfigurationServiceTest {

    @Inject
    private ConfigurationTypeResource resource;

    @Inject
    @UiSpecProxy
    private Jsonb jsonb;

    @Test
    void ensureFormIdEnrichment(final WebTarget proxyClient) {
        final Map<String, Object> wrapper = proxyClient
                .path("actions/execute")
                .queryParam("family", "builtin")
                .queryParam("type", "reloadForm")
                .queryParam("action", "builtin::root::reloadFromId")
                .request(APPLICATION_JSON_TYPE)
                .post(entity(singletonMap("id", "dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseSNkYXRhc2V0I2RhdGFzZXQtMg"),
                        APPLICATION_JSON_TYPE), new GenericType<Map<String, Object>>() {
                        });
        final Ui uiSpec = jsonb.fromJson(jsonb.toJson(wrapper), Ui.class);
        final Iterator<UiSchema> uiSchemas = uiSpec.getUiSchema().iterator();

        // $datasetMetadata
        assertTrue(uiSchemas.hasNext());
        assertNotNull(uiSchemas.next());

        // testConfig
        assertTrue(uiSchemas.hasNext());
        final UiSchema testConfigSchema = uiSchemas.next();
        assertNotNull(testConfigSchema);

        // form, just for the shape validation - config here
        assertTrue(uiSchemas.hasNext());
        assertNotNull(uiSchemas.next());
        assertFalse(uiSchemas.hasNext());

        // now validate the testconfig triggers
        assertEquals(2,
                testConfigSchema
                        .getItems()
                        .stream()
                        .filter(it -> it.getTriggers() != null)
                        .flatMap(it -> it.getTriggers().stream())
                        .peek(trigger -> {
                            final Collection<UiSchema.Parameter> parameters = trigger.getParameters();
                            assertTrue(parameters.stream().anyMatch(
                                    p -> p.getPath().equals("$datasetMetadata.id") && p.getKey().equals("$formId")));
                        })
                        .count());
    }
}
