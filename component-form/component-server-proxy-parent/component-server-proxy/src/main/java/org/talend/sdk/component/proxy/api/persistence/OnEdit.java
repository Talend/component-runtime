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
package org.talend.sdk.component.proxy.api.persistence;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.proxy.api.Event;
import org.talend.sdk.component.proxy.api.service.RequestContext;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.Getter;

@Event
public class OnEdit extends PersistenceEvent {

    @Getter
    private final String id;

    @Getter
    private CompletionStage<?> completionListener = completedFuture(null);

    public OnEdit(final String id, final RequestContext request, final Jsonb jsonb, final JsonObject enrichment,
            final Collection<SimplePropertyDefinition> definitions, final Map<String, String> properties) {
        super(request, jsonb, enrichment, definitions, properties);
        this.id = id;
    }

    public synchronized OnEdit compose(final CompletionStage<?> composable) {
        this.completionListener =
                completionListener == null ? composable : completionListener.thenCompose(it -> composable);
        return this;
    }
}
