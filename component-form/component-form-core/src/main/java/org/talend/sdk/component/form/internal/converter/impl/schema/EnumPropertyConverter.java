/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.form.internal.converter.impl.schema;

import static java.util.Collections.emptyList;

import java.util.concurrent.CompletionStage;

import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.PropertyConverter;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EnumPropertyConverter implements PropertyConverter {

    private final JsonSchema jsonSchema;

    @Override
    public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> cs) {
        return cs.thenApply(context -> {
            jsonSchema.setType("string");
            if (context.getProperty().getValidation() == null
                    || context.getProperty().getValidation().getEnumValues() == null) {
                jsonSchema.setEnumValues(emptyList());
            } else {
                jsonSchema.setEnumValues(context.getProperty().getValidation().getEnumValues());
            }
            return context;
        });
    }
}
