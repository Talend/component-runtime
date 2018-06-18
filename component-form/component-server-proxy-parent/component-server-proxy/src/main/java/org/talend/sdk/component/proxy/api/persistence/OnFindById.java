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

import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.talend.sdk.component.proxy.api.Event;
import org.talend.sdk.component.proxy.api.service.RequestContext;

import lombok.Getter;

@Event
@Getter
public class OnFindById extends BaseEvent {

    private final String id;

    private CompletionStage<Map<String, String>> properties;

    private CompletionStage<String> formId;

    public OnFindById(final RequestContext request, final String id) {
        super(request);
        this.id = id;
    }

    public synchronized OnFindById composeProperties(final CompletionStage<Map<String, String>> properties) {
        if (this.properties != null) {
            throw new IllegalArgumentException("Properties already set");
        }
        this.properties = properties;
        return this;
    }

    public synchronized OnFindById composeFormId(final CompletionStage<String> id) {
        if (this.formId != null) {
            throw new IllegalArgumentException("formId already set");
        }
        this.formId = id;
        return this;
    }
}
