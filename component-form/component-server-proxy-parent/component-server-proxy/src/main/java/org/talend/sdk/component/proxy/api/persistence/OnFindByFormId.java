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

import static lombok.AccessLevel.NONE;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.talend.sdk.component.proxy.api.Event;

import lombok.Data;
import lombok.Setter;

@Event
@Data
public class OnFindByFormId {

    private final String formId;

    @Setter(NONE)
    private CompletionStage<Map<String, String>> result;

    /**
     * @param candidates the map (id, display name) of the existing store for that id.
     * @return candidates.
     */
    public synchronized OnFindByFormId composeResult(final CompletionStage<Map<String, String>> candidates) {
        if (this.result != null && !this.result.equals(candidates)) {
            throw new IllegalArgumentException("result already set");
        }
        this.result = candidates;
        return this;
    }
}
