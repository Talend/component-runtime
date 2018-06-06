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

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;

@Getter
public class OnFindById extends BaseEvent {

    private final String id;

    private Map<String, String> properties;

    private String formId;

    public OnFindById(final HttpServletRequest request, final String id) {
        super(request);
        this.id = id;
    }

    public synchronized OnFindById setProperties(final Map<String, String> properties) {
        if (this.properties != null && !this.properties.equals(properties)) {
            throw new IllegalArgumentException("Properties already set");
        }
        this.properties = properties;
        return this;
    }

    public synchronized OnFindById setFormId(final String id) {
        if (this.formId != null && !this.formId.equals(id)) {
            throw new IllegalArgumentException("formId already set");
        }
        this.formId = id;
        return this;
    }
}
