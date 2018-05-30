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

import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.client.Client;

import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.proxy.service.client.ConfigurationClient;
import org.talend.sdk.component.proxy.service.client.UiSpecServiceClient;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;

@ApplicationScoped
public class UiSpecServiceProvider {

    @Inject
    private ModelEnricherService modelEnricherService;

    @Inject
    private ConfigurationClient configurationClient;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private ProxyConfiguration configuration;

    @Inject
    @UiSpecProxy
    private Client client;

    @Inject
    @UiSpecProxy
    private Jsonb jsonb;

    public UiSpecService newInstance(final String language, final Function<String, String> placeholderProvider) {
        return new UiSpecService(new UiSpecServiceClient(client, configuration.getTargetServerBase(),
                configurationClient, configurationService, jsonb, language, placeholderProvider), jsonb);
    }
}
