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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.proxy.api.persistence.OnPersist;
import org.talend.sdk.component.proxy.service.impl.HttpRequestContext;
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
                        .filterProperties("en", k -> null,
                                asList(newProperty("configuration", "OBJECT", metadata("thedataset", "dataset")),
                                        newProperty("configuration.param", "NUMBER", emptyMap()),
                                        newProperty("configuration.something", "OBJECT",
                                                metadata("thedatastore", "datastore")),
                                        newProperty("configuration.something.value", "STRING", emptyMap()),
                                        newProperty("configuration.nested", "OBJECT", emptyMap()),
                                        newProperty("configuration.nested.value", "STRING", emptyMap())))
                        .toCompletableFuture()
                        .get());
    }

    @Test
    void filterNestedRefs() throws ExecutionException, InterruptedException {
        assertDatastoreDataset(service
                .filterProperties("en", k -> null,
                        asList(newProperty("configuration", "OBJECT", metadata("thedataset", "dataset")),
                                newProperty("configuration.param", "NUMBER", emptyMap()),
                                newProperty("configuration.something", "OBJECT", metadata("thedatastore", "datastore")),
                                newProperty("configuration.something.value", "STRING", emptyMap()),
                                newProperty("configuration.something.else", "OBJECT",
                                        metadata("theotherdatastore", "something")),
                                newProperty("configuration.something.else.whatever1", "STRING", emptyMap()),
                                newProperty("configuration.something.else.whatever2", "BOOLEAN", emptyMap()),
                                newProperty("configuration.nested", "OBJECT", emptyMap()),
                                newProperty("configuration.nested.value", "STRING", emptyMap())))
                .toCompletableFuture()
                .get());
    }

    @Test
    void filterMultipleRefs() throws ExecutionException, InterruptedException {
        final List<SimplePropertyDefinition> filtered = service
                .filterProperties("en", k -> null,
                        asList(newProperty("configuration", "OBJECT", metadata("thedataset", "dataset")),
                                newProperty("configuration.param", "NUMBER", emptyMap()),
                                newProperty("configuration.something", "OBJECT", metadata("thedatastore", "datastore")),
                                newProperty("configuration.something.value", "STRING", emptyMap()),
                                newProperty("configuration.something.else", "OBJECT",
                                        metadata("theotherdatastore", "something")),
                                newProperty("configuration.something.else.whatever1", "STRING", emptyMap()),
                                newProperty("configuration.something.else.whatever2", "BOOLEAN", emptyMap()),
                                newProperty("configuration.nested", "OBJECT", metadata("something", "whatever")),
                                newProperty("configuration.nested.value", "STRING", emptyMap())))
                .toCompletableFuture()
                .get();
        assertEquals(4, filtered.size());
        filtered.sort(comparing(SimplePropertyDefinition::getPath));
        assertEquals("configuration/configuration.nested/configuration.param/configuration.something",
                filtered.stream().map(SimplePropertyDefinition::getPath).collect(joining("/")));
        assertEquals("OBJECT/STRING/NUMBER/STRING",
                filtered.stream().map(SimplePropertyDefinition::getType).collect(joining("/")));
        assertEquals("reference", filtered.get(1).getMetadata().get("proxy::type"));
    }

    @Test
    void dropDownRefs() throws ExecutionException, InterruptedException {

        final List<SimplePropertyDefinition> srcProps =
                asList(newProperty("configuration", "OBJECT", metadata("thedataset", "dataset")),
                        newProperty("configuration.param", "NUMBER", emptyMap()),
                        newProperty("configuration.something", "OBJECT", metadata("dataset-1", "dataset")),
                        newProperty("configuration.something.value", "STRING", emptyMap()));

        final OnPersist event = new OnPersist(null, null, "VGhlVGVzdEZhbWlseTIjZGF0YXNldCNkYXRhc2V0LTE", null, srcProps
                .stream()
                .filter(it -> it.getPath().startsWith("configuration.something"))
                .map(it -> new SimplePropertyDefinition(it.getPath().substring("configuration.".length()), it.getName(),
                        it.getDisplayName(), it.getType(), it.getDefaultValue(), it.getValidation(), it.getMetadata(),
                        it.getPlaceholder(), it.getProposalDisplayNames()))
                .collect(toList()), new HashMap<String, String>() {

                    {
                        put("something.value", "somevalue");
                    }
                });

        persistEvent
                .fireAsync(event)
                .thenCompose(result -> service.filterProperties("en", k -> null, srcProps).thenApply(props -> {
                    final Collection<String> values = props.get(2).getProposalDisplayNames().values();
                    assertEquals(1, values.size());
                    assertEquals("dataset-1", values.iterator().next());
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

        final OnPersist event = new OnPersist(null, null, "VGhlVGVzdEZhbWlseTIjZGF0YXNldCNkYXRhc2V0LTE", null, srcProps
                .stream()
                .filter(it -> it.getPath().startsWith("configuration.something"))
                .map(it -> new SimplePropertyDefinition(it.getPath().substring("configuration.".length()), it.getName(),
                        it.getDisplayName(), it.getType(), it.getDefaultValue(), it.getValidation(), it.getMetadata(),
                        it.getPlaceholder(), it.getProposalDisplayNames()))
                .collect(toList()), new HashMap<String, String>() {

                    {
                        put("config.value", "somevalue");
                        put("config.value2", "other");
                    }
                });

        persistEvent
                .fireAsync(event)
                .thenCompose(OnPersist::getId)
                .thenCompose(id -> service.filterProperties("en", k -> null, srcProps).thenCompose(
                        props -> service.replaceReferences(new HttpRequestContext("en", k -> null, null), props,
                                new HashMap<String, String>() {

                                    {
                                        put("configuration.param", "test");
                                        put("configuration.something", id);
                                    }
                                })))
                .thenApply(props -> {
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
                "configuration/configuration.nested/configuration.nested.value/configuration.param/configuration.something",
                filtered.stream().map(SimplePropertyDefinition::getPath).collect(joining("/")));
        assertEquals("OBJECT/OBJECT/STRING/NUMBER/STRING",
                filtered.stream().map(SimplePropertyDefinition::getType).collect(joining("/")));
        assertEquals("reference", filtered.get(4).getMetadata().get("proxy::type"));
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
