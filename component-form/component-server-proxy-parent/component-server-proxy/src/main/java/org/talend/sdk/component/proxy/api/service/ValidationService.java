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
package org.talend.sdk.component.proxy.api.service;

import java.util.Collection;
import java.util.concurrent.CompletionStage;

import javax.json.JsonObject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public interface ValidationService {

    /**
     * Validates a JSON payload.
     *
     * @param requestContext the request context (lang, placeholders).
     * @param formId the form to validate against the properties.
     * @param properties the properties to validate.
     * @return the list of errors if any.
     */
    CompletionStage<Result> validate(RequestContext requestContext, String formId, JsonObject properties);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class Result {

        private Collection<ValidationError> errors;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ValidationError {

        private String field;

        private String message;
    }
}
