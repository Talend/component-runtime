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
package org.talend.sdk.component.form.demo;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import org.talend.sdk.component.form.api.ActionService;
import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.ClientFactory;
import org.talend.sdk.component.form.api.UiSpecService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class DemoConfiguration {

    @Produces
    public Client client() {
        return ClientFactory.createDefault(System.getProperty("demo.components.base", "http://localhost:8080/api/v1"));
    }

    public void releaseClient(@Disposes final Client client) {
        client.close();
    }

    @Produces
    public UiSpecService uiSpecService(final Client client) {
        return new UiSpecService(client);
    }

    public void release(@Disposes final UiSpecService specService) {
        try {
            specService.close();
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    @Produces
    public ActionService actionService() {
        return new ActionService();
    }
}
