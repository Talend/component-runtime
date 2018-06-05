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

import static lombok.AccessLevel.PROTECTED;

import java.util.Map;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.servlet.http.HttpServletRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = PROTECTED)
public abstract class PersistenceEvent {

    private final HttpServletRequest request;

    private final Jsonb mapper;

    private final JsonObject enrichment;

    @Getter
    private final Map<String, String> properties;

    public <T> T getAttribute(final String key, final Class<T> type) {
        return type.cast(request.getAttribute(key));
    }

    public <T> T getEnrichment(final Class<T> type) {
        return mapper.fromJson(enrichment.toString(), type);
    }
}
