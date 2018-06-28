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
package org.talend.sdk.component.proxy.test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;

import org.talend.sdk.component.proxy.api.persistence.OnEdit;
import org.talend.sdk.component.proxy.api.persistence.OnFindByFormId;
import org.talend.sdk.component.proxy.api.persistence.OnFindById;
import org.talend.sdk.component.proxy.api.persistence.OnPersist;
import org.talend.sdk.component.proxy.service.client.ConfigurationClient;

import lombok.Getter;

@Getter
@ApplicationScoped
public class InMemoryTestPersistence {

    private final Collection<OnPersist> persist = new ArrayList<>();

    private final Collection<OnEdit> edit = new ArrayList<>();

    @Inject
    private ConfigurationClient client;

    void on(@ObservesAsync final OnPersist event) {
        persist.add(event.composeId(completedFuture(UUID.randomUUID().toString())));
    }

    void on(@ObservesAsync final OnEdit event) {
        edit.add(event);
    }

    void on(@ObservesAsync final OnFindById event) {
        final OnPersist persisted = persist.stream().filter(it -> {
            try {
                return it.getId().toCompletableFuture().get().equals(event.getId());
            } catch (final InterruptedException | ExecutionException e) {
                throw new IllegalStateException(e);
            }
        }).findFirst().orElseThrow(
                () -> new IllegalArgumentException("No persisted entries matching id #" + event.getId()));
        event.composeProperties(completedFuture(persisted.getProperties())).composeFormId(
                completedFuture(persisted.getFormId()));
    }

    void on(@ObservesAsync final OnFindByFormId event) {
        client
                .getAllConfigurations("en", k -> null)
                .thenApply(nodes -> nodes.getNodes().get(event.getFormId()))
                .thenApply(node -> event.composeResult(
                        completedFuture(persist.stream().filter(it -> it.getFormId().equals(event.getFormId())).collect(
                                toMap(persist -> {
                                    try {
                                        return persist.getId().toCompletableFuture().get();
                                    } catch (final InterruptedException | ExecutionException e) {
                                        throw new IllegalStateException(e);
                                    }
                                }, it -> node.getDisplayName())))));
    }

    public void clear() {
        persist.clear();
        edit.clear();
    }
}
