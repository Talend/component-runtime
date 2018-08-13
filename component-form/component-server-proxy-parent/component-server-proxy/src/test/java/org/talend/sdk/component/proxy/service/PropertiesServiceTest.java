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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.proxy.api.persistence.OnPersist;
import org.talend.sdk.component.proxy.service.client.UiSpecContext;
import org.talend.sdk.component.proxy.test.CdiInject;
import org.talend.sdk.component.proxy.test.InMemoryTestPersistence;
import org.talend.sdk.component.proxy.test.WithServer;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

@CdiInject
@WithServer
class PropertiesServiceTest {

    @Inject
    private PropertiesService service;

    @Inject
    private Event<OnPersist> persistEvent;

    @Inject
    private InMemoryTestPersistence persistence;

    @AfterEach
    void clear() {
        persistence.clear();
    }

    @Test
    void filterDatasetDatastore() throws ExecutionException, InterruptedException {
        assertDatastoreDataset(
                service
                        .filterProperties(
                                asList(newProperty("configuration", "OBJECT", metadata("thedataset", "dataset")),
                                        newProperty("configuration.param", "NUMBER", emptyMap()),
                                        newProperty("configuration.something", "OBJECT",
                                                metadata("thedatastore", "datastore")),
                                        newProperty("configuration.something.value", "STRING", emptyMap()),
                                        newProperty("configuration.nested", "OBJECT", emptyMap()),
                                        newProperty("configuration.nested.value", "STRING", emptyMap())),
                                new UiSpecContext("en", k -> null))
                        .toCompletableFuture()
                        .get());
    }

    @Test
    void filterNestedRefs() throws ExecutionException, InterruptedException {
        assertDatastoreDataset(service
                .filterProperties(
                        asList(newProperty("configuration", "OBJECT", metadata("thedataset", "dataset")),
                                newProperty("configuration.param", "NUMBER", emptyMap()),
                                newProperty("configuration.something", "OBJECT", metadata("thedatastore", "datastore")),
                                newProperty("configuration.something.value", "STRING", emptyMap()),
                                newProperty("configuration.something.else", "OBJECT",
                                        metadata("theotherdatastore", "something")),
                                newProperty("configuration.something.else.whatever1", "STRING", emptyMap()),
                                newProperty("configuration.something.else.whatever2", "BOOLEAN", emptyMap()),
                                newProperty("configuration.nested", "OBJECT", emptyMap()),
                                newProperty("configuration.nested.value", "STRING", emptyMap())),
                        new UiSpecContext("en", k -> null))
                .toCompletableFuture()
                .get());
    }

    @Test
    void filterMultipleRefs() throws ExecutionException, InterruptedException {
        final List<SimplePropertyDefinition> filtered = service
                .filterProperties(
                        asList(newProperty("configuration", "OBJECT", metadata("thedataset", "dataset")),
                                newProperty("configuration.param", "NUMBER", emptyMap()),
                                newProperty("configuration.something", "OBJECT", metadata("thedatastore", "datastore")),
                                newProperty("configuration.something.value", "STRING", emptyMap()),
                                newProperty("configuration.something.else", "OBJECT",
                                        metadata("theotherdatastore", "something")),
                                newProperty("configuration.something.else.whatever1", "STRING", emptyMap()),
                                newProperty("configuration.something.else.whatever2", "BOOLEAN", emptyMap()),
                                newProperty("configuration.nested", "OBJECT", metadata("something", "whatever")),
                                newProperty("configuration.nested.value", "STRING", emptyMap())),
                        new UiSpecContext("en", k -> null))
                .toCompletableFuture()
                .get();
        assertEquals(4, filtered.size());
        filtered.sort(comparing(SimplePropertyDefinition::getPath));
        assertEquals(
                "configuration/configuration.nested.$selfReference/configuration.param/configuration.something.$selfReference",
                filtered.stream().map(SimplePropertyDefinition::getPath).collect(joining("/")));
        assertEquals("OBJECT/STRING/NUMBER/STRING",
                filtered.stream().map(SimplePropertyDefinition::getType).collect(joining("/")));
    }

    @Test
    void dropDownRefs() throws ExecutionException, InterruptedException {

        final List<SimplePropertyDefinition> srcProps =
                asList(newProperty("configuration", "OBJECT", metadata("thedataset", "dataset")),
                        newProperty("configuration.param", "NUMBER", emptyMap()),
                        newProperty("configuration.something", "OBJECT", metadata("dataset-1", "dataset")),
                        newProperty("configuration.something.value", "STRING", emptyMap()));

        final OnPersist event = new OnPersist(null, null,
                "dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseTIjZGF0YXNldCNkYXRhc2V0LTE", null,
                srcProps
                        .stream()
                        .filter(it -> it.getPath().startsWith("configuration.something"))
                        .map(it -> new SimplePropertyDefinition(it.getPath().substring("configuration.".length()),
                                it.getName(), it.getDisplayName(), it.getType(), it.getDefaultValue(),
                                it.getValidation(), it.getMetadata(), it.getPlaceholder(),
                                it.getProposalDisplayNames()))
                        .collect(toList()),
                new HashMap<String, String>() {

                    {
                        put("something.value", "somevalue");
                    }
                });

        persistEvent
                .fireAsync(event)
                .thenCompose(result -> service.filterProperties(srcProps, new UiSpecContext("en", k -> null)).thenApply(
                        props -> {
                            final Collection<String> values = props.get(2).getProposalDisplayNames().values();
                            assertEquals(2, values.size());
                            assertEquals(Stream
                                    .of("dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseTIjZGF0YXNldCNkYXRhc2V0LTE1",
                                            "dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseTIjZGF0YXNldCNkYXRhc2V0LTE2")
                                    .collect(toSet()), new HashSet<>(values));
                            return null;
                        }))
                .toCompletableFuture()
                .get();
    }

    @Test
    void replaceReferences() throws ExecutionException, InterruptedException {

        final List<SimplePropertyDefinition> srcProps =
                asList(newProperty("configuration", "OBJECT", metadata("thedataset", "dataset")),
                        newProperty("configuration.param", "NUMBER", emptyMap()),
                        newProperty("configuration.something", "OBJECT", metadata("dataset-1", "dataset")),
                        newProperty("configuration.something.value", "STRING", emptyMap()));

        final OnPersist event = new OnPersist(null, null,
                "dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseTIjZGF0YXNldCNkYXRhc2V0LTE", null,
                srcProps
                        .stream()
                        .filter(it -> it.getPath().startsWith("configuration.something"))
                        .map(it -> new SimplePropertyDefinition(it.getPath().substring("configuration.".length()),
                                it.getName(), it.getDisplayName(), it.getType(), it.getDefaultValue(),
                                it.getValidation(), it.getMetadata(), it.getPlaceholder(),
                                it.getProposalDisplayNames()))
                        .collect(toList()),
                new HashMap<String, String>() {

                    {
                        put("config.value", "somevalue");
                        put("config.value2", "other");
                    }
                });

        persistEvent
                .fireAsync(event)
                .thenCompose(OnPersist::getId)
                .thenCompose(id -> service.filterProperties(srcProps, new UiSpecContext("en", k -> null)).thenCompose(
                        props -> service.replaceReferences(new UiSpecContext("en", k -> null), props,
                                new HashMap<String, String>() {

                                    {
                                        put("configuration.param", "test");
                                        put("configuration.something.$selfReference", id);
                                    }
                                })))
                .thenApply(props -> {
                    assertNotNull(props.remove("configuration.something.$selfReference"));
                    assertEquals(new HashMap<String, String>() {

                        {
                            put("configuration.param", "test");
                            put("configuration.something.value", "somevalue");
                            put("configuration.something.value2", "other");
                        }
                    }, props);
                    return null;
                })
                .toCompletableFuture()
                .get();
    }

    private void assertDatastoreDataset(final List<SimplePropertyDefinition> filtered) {
        assertEquals(5, filtered.size());
        filtered.sort(comparing(SimplePropertyDefinition::getPath));
        assertEquals(
                "configuration/configuration.nested/configuration.nested.value/configuration.param/configuration.something.$selfReference",
                filtered.stream().map(SimplePropertyDefinition::getPath).collect(joining("/")));
        assertEquals("OBJECT/OBJECT/STRING/NUMBER/STRING",
                filtered.stream().map(SimplePropertyDefinition::getType).collect(joining("/")));
    }

    private Map<String, String> metadata(final String name, final String type) {
        return new HashMap<String, String>() {

            {
                put("configurationtype::name", name);
                put("configurationtype::type", type);
            }
        };
    }

    private SimplePropertyDefinition newProperty(final String path, final String type, final Map<String, String> meta) {
        return new SimplePropertyDefinition(path, path.substring(path.lastIndexOf('.') + 1), path, type, null,
                new PropertyValidation(false, null, null, null, null, null, null, false, null, null), meta, null, null);
    }
}
