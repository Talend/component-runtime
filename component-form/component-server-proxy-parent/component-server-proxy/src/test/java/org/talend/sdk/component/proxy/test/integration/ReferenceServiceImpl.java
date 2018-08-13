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
package org.talend.sdk.component.proxy.test.integration;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.sdk.component.proxy.api.integration.application.ReferenceService;
import org.talend.sdk.component.proxy.api.integration.application.Values;
import org.talend.sdk.component.proxy.api.persistence.OnPersist;
import org.talend.sdk.component.proxy.service.client.UiSpecContext;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.proxy.test.InMemoryTestPersistence;

@UiSpecProxy
@ApplicationScoped
public class ReferenceServiceImpl implements ReferenceService {

    @Inject
    private InMemoryTestPersistence persistence;

    @Override
    public CompletionStage<Values> findReferencesByTypeAndName(final String type, final String name,
            final UiSpecContext context) {
        return CompletableFuture.completedFuture(
                new Values(asList(new Values.Item(type + "1", name + "1"), new Values.Item(type + "2", name + "2"))));
    }

    @Override
    public CompletionStage<Form> findPropertiesById(final String id, final UiSpecContext context) {
        if (id.equals("actionServices.reloadFromParentId")) {
            return completedFuture(Form
                    .builder()
                    .formId("dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseTIjZGF0YXN0b3JlI0Nvbm5lY3Rpb24tMQ")
                    .properties(singletonMap("connection.url", "http://foo"))
                    .build());
        }
        final OnPersist byId = persistence.findById(id);
        return CompletableFuture
                .completedFuture(Form.builder().formId(byId.getFormId()).properties(byId.getProperties()).build());
    }
}
