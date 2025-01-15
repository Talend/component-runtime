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
package org.talend.sdk.component.server.extension.api.action;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import javax.ws.rs.core.Response;

import org.talend.sdk.component.server.front.model.ActionReference;

import lombok.Data;

@Data
public class Action {

    private final ActionReference reference;

    private final BiFunction<Map<String, String>, String, CompletionStage<Response>> handler;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return reference.equals(Action.class.cast(o).reference);
    }

    @Override
    public int hashCode() {
        return reference.hashCode();
    }
}
