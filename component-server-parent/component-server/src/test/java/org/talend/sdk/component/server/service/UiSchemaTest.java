/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import static java.util.Collections.emptyList;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import jakarta.ws.rs.client.WebTarget;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.test.ComponentClient;

@MonoMeecrowaveConfig
public class UiSchemaTest {

    @Inject
    private WebTarget base;

    @Inject
    private ComponentClient client;

    private final UiSpecService<Object> uiSpecService = new UiSpecService<>(new Client() {

        @Override // for dynamic_values, just return an empty schema
        public CompletionStage<Map<String, Object>> action(final String family, final String type,
                final String action, final String lang, final Map params, final Object context) {
            final Map<String, Object> result = new HashMap<>();
            result.put("items", emptyList());
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public void close() {
            // no-op
        }
    });

    @Test
    void uiSpecFromDetailsNoDocEn() throws Exception {
        final Collection<UiSchema> uiSchemas = getUiForDetails("en", false).getUiSchema();
        assertUiSchemaDescription(uiSchemas.stream(), (s) -> Objects.isNull(s));
        assertUiSchemaTooltip(uiSchemas.stream(), (ui) -> ui.getTooltip().startsWith("Documentation for"));
    }

    @Test
    void uiSpecFromDetailsDocEn() throws Exception {
        final Collection<UiSchema> uiSchemas = getUiForDetails("en", true).getUiSchema();
        assertUiSchemaDescription(uiSchemas.stream(), (s) -> !Objects.isNull(s));
        assertUiSchemaTooltip(uiSchemas.stream(),
                (ui) -> ui.getTooltip().equals(ui.getDescription()) && ui.getTooltip().startsWith("Documentation for"));
    }

    @Test
    void uiSpecFromDetailsDocFr() throws Exception {
        final Collection<UiSchema> uiSchemas = getUiForDetails("fr", true).getUiSchema();
        assertUiSchemaDescription(uiSchemas.stream(), (s) -> !Objects.isNull(s));
        assertUiSchemaTooltip(uiSchemas.stream(), (ui) -> ui.getTooltip().equals(ui.getDescription())
                && ui.getTooltip().startsWith("Documentation pour"));
    }

    @Test
    void uiSpecFromConfigurationTypeDetailsNoDocEn() throws Exception {
        final Collection<UiSchema> uiSchemas = getUiForDetails("en", false).getUiSchema();
        assertUiSchemaDescription(uiSchemas.stream(), (s) -> Objects.isNull(s));
        assertUiSchemaTooltip(uiSchemas.stream(), (ui) -> ui.getTooltip().startsWith("Documentation for"));
    }

    @Test
    void uiSpecFromConfigurationTypeDetailsDocEn() throws Exception {
        final Collection<UiSchema> uiSchemas = getUiForDetails("en", true).getUiSchema();
        assertUiSchemaDescription(uiSchemas.stream(), (s) -> !Objects.isNull(s));
        assertUiSchemaTooltip(uiSchemas.stream(),
                (ui) -> ui.getTooltip().equals(ui.getDescription()) && ui.getTooltip().startsWith("Documentation for"));
    }

    @Test
    void uiSpecFromConfigurationTypeDetailsDocFr() throws Exception {
        final Collection<UiSchema> uiSchemas = getUiForDetails("fr", true).getUiSchema();
        assertUiSchemaDescription(uiSchemas.stream(), (s) -> !Objects.isNull(s));
        assertUiSchemaTooltip(uiSchemas.stream(), (ui) -> ui.getTooltip().equals(ui.getDescription())
                && ui.getTooltip().startsWith("Documentation pour"));
    }

    private Ui getUiForConfigurationTypeDetails(final String lang, final boolean includeDoc) throws Exception {
        final ConfigTypeNodes details = base
                .path("component/details")
                .queryParam("identifiers", "amRiYy1jb21wb25lbnQjamRiYyNkYXRhc3RvcmUjamRiYw")
                .queryParam("language", lang)
                .request(APPLICATION_JSON_TYPE)
                .get(ConfigTypeNodes.class);
        assertEquals(1, details.getNodes().size());
        final ConfigTypeNode jdbcConnection = details.getNodes().get("amRiYy1jb21wb25lbnQjamRiYyNkYXRhc3RvcmUjamRiYw");
        assertNotNull(jdbcConnection);
        uiSpecService.setConfiguration(new PropertyContext.Configuration(includeDoc));
        return uiSpecService.convert("jdbc", lang, jdbcConnection, null).toCompletableFuture().get();
    }

    private Ui getUiForDetails(final String lang, final boolean includeDoc) throws Exception {
        final ComponentDetailList details = base
                .path("component/details")
                .queryParam("identifiers", client.getJdbcId())
                .queryParam("language", lang)
                .request(APPLICATION_JSON_TYPE)
                .get(ComponentDetailList.class);
        assertEquals(1, details.getDetails().size());
        final ComponentDetail detail = details.getDetails().iterator().next();
        uiSpecService.setConfiguration(new PropertyContext.Configuration(includeDoc));
        return uiSpecService.convert(detail, lang, null).toCompletableFuture().get();
    }

    public void assertUiSchemaDescription(final Stream<UiSchema> uiSchema, final Predicate<String> predicate) {
        flattenUiSchema(uiSchema)
                .forEach(s -> {
                    assertTrue(predicate.test(s.getDescription()), s.toString());
                });
    }

    public void assertUiSchemaTooltip(final Stream<UiSchema> uiSchema, final Predicate<UiSchema> predicate) {
        flattenUiSchema(uiSchema)
                .filter(s -> !Objects.isNull(s.getTooltip()))
                .forEach(s -> {
                    assertTrue(predicate.test(s), s.toString());
                });
    }

    public Stream<UiSchema> flattenUiSchema(final Stream<UiSchema> uiSchema) {
        return uiSchema
                .flatMap(u -> u.getItems() == null ? Stream.of(u)
                        : Stream.concat(Stream.of(u), flattenUiSchema(u.getItems().stream())));
    }

}
