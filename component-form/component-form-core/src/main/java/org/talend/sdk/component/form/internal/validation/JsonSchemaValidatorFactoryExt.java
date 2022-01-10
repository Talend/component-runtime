/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.form.internal.validation;

import java.util.List;
import java.util.stream.Collectors;

import org.talend.sdk.component.form.internal.validation.spi.ext.EnumValidationWithDefaultValue;
import org.talend.sdk.component.form.internal.validation.spi.ext.MaximumValidation;
import org.talend.sdk.component.form.internal.validation.spi.ext.MinimumValidation;
import org.talend.sdk.component.form.internal.validation.spi.ext.RequiredValidation;
import org.talend.sdk.component.form.internal.validation.spi.ext.TypeValidation;

public class JsonSchemaValidatorFactoryExt extends org.apache.johnzon.jsonschema.JsonSchemaValidatorFactory {

    @Override
    public List<org.apache.johnzon.jsonschema.spi.ValidationExtension> createDefaultValidations() {
        List validatons = super.createDefaultValidations()
                .stream()
                .filter(v -> !org.apache.johnzon.jsonschema.spi.builtin.TypeValidation.class.isInstance(v))
                .filter(v -> !org.apache.johnzon.jsonschema.spi.builtin.EnumValidation.class.isInstance(v))
                .filter(v -> !org.apache.johnzon.jsonschema.spi.builtin.MinimumValidation.class.isInstance(v))
                .filter(v -> !org.apache.johnzon.jsonschema.spi.builtin.MaximumValidation.class.isInstance(v))
                .filter(v -> !org.apache.johnzon.jsonschema.spi.builtin.RequiredValidation.class.isInstance(v))
                .collect(Collectors.toList());
        validatons.add(new TypeValidation());
        validatons.add(new EnumValidationWithDefaultValue());
        validatons.add(new MinimumValidation());
        validatons.add(new MaximumValidation());
        validatons.add(new RequiredValidation());

        return validatons;
    }
}
