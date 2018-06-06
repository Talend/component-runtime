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

import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.sdk.component.proxy.api.configuration.ConfigurationReader;
import org.talend.sdk.component.proxy.api.configuration.ConfigurationVisitor;
import org.talend.sdk.component.proxy.api.service.ConfigurationVisitorService;
import org.talend.sdk.component.proxy.api.service.RequestContext;
import org.talend.sdk.component.proxy.service.client.ConfigurationClient;

@ApplicationScoped
public class ConfigurationVisitorServiceImpl implements ConfigurationVisitorService {

    @Inject
    private ConfigurationClient client;

    @Override
    public <T extends ConfigurationVisitor> CompletionStage<T> visit(final RequestContext context, final String formId,
            final Map<String, String> properties, final T visitor) {
        return client.getDetails(context.language(), formId, context::findPlaceholder).thenApply(config -> {
            new ConfigurationReader(properties, visitor, config.getProperties()).visit();
            return visitor;
        });
    }
}
