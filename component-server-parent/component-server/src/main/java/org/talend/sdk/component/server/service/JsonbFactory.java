/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyOrderStrategy;

import org.talend.sdk.component.server.service.qualifier.ComponentServer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class JsonbFactory {

    @Produces
    @ComponentServer
    @ApplicationScoped
    Jsonb jsonb() {
        return JsonbBuilder.create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL));
    }

    void destroyJsonb(@Disposes final Jsonb jsonb) {
        try {
            jsonb.close();
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
