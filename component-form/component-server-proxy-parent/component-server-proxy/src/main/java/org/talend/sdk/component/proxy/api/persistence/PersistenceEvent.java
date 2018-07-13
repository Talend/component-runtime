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

import java.util.Collection;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.proxy.api.configuration.ConfigurationReader;
import org.talend.sdk.component.proxy.api.configuration.ConfigurationVisitor;
import org.talend.sdk.component.proxy.api.service.RequestContext;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.Getter;

public abstract class PersistenceEvent extends BaseEvent {

    private final Jsonb mapper;

    private final JsonObject enrichment;

    @Getter
    private final Map<String, String> properties;

    private final Collection<SimplePropertyDefinition> definitions;

    protected PersistenceEvent(final RequestContext request, final Jsonb jsonb, final JsonObject enrichment,
            final Collection<SimplePropertyDefinition> definitions, final Map<String, String> properties) {
        super(request);
        this.properties = properties;
        this.mapper = jsonb;
        this.definitions = definitions;
        this.enrichment = enrichment;
    }

    public <T> T getEnrichment(final Class<T> type) {
        return mapper.fromJson(enrichment.toString(), type);
    }

    public <T extends ConfigurationVisitor> T visitProperties(final T visitor) {
        new ConfigurationReader(properties, visitor, definitions).visit();
        return visitor;
    }
}
