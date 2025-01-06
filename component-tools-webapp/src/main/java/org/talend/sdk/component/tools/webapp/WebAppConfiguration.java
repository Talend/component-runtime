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
package org.talend.sdk.component.tools.webapp;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import org.talend.sdk.component.form.api.ActionService;
import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.internal.converter.PropertyContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class WebAppConfiguration {

    @Produces
    public ActionService actionService() {
        return new ActionService();
    }

    @Produces
    public UiSpecService<Object> uiSpecService(final Client client) {
        final UiSpecService<Object> service = new UiSpecService<>(client);
        service.setConfiguration(new PropertyContext.Configuration(true));
        return service;
    }

    public void release(@Disposes final UiSpecService<Object> uiSpecService) {
        try {
            uiSpecService.close();
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
    }
}
