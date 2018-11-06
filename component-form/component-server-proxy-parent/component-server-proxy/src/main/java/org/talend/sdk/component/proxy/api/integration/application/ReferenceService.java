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
package org.talend.sdk.component.proxy.api.integration.application;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.talend.sdk.component.proxy.service.client.UiSpecContext;

import lombok.Builder;
import lombok.Data;

public interface ReferenceService {

    @Deprecated // this method must disappear since the family is missing from the lookup which leads to inconsistent
                // results
    CompletionStage<Values> findReferencesByTypeAndName(String type, String name, UiSpecContext context);

    default CompletionStage<Values> findReferencesByTypeAndName(String family, String type, String name,
            UiSpecContext context) {
        return findReferencesByTypeAndName(type, name, context);
    }

    CompletionStage<Form> findPropertiesById(String configType, String id, UiSpecContext context);

    @Data
    @Builder
    class Form {

        private String formId;

        private Map<String, String> properties;
    }
}
