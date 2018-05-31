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

import static java.util.Collections.emptyMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.json.JsonBuilderFactory;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class JsonProducer {

    @Produces
    @UiSpecProxy
    @ApplicationScoped
    JsonProvider jsonp() {
        return JsonProvider.provider();
    }

    @Produces
    @UiSpecProxy
    @ApplicationScoped
    JsonBuilderFactory jsonObjectBuilder(@UiSpecProxy final JsonProvider provider) {
        return provider.createBuilderFactory(emptyMap());
    }

    @Produces
    @UiSpecProxy
    @ApplicationScoped
    Jsonb jsonb() {
        return JsonbBuilder.create();
    }

    void disposes(@Disposes @UiSpecProxy final Jsonb jsonb) {
        try {
            jsonb.close();
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
