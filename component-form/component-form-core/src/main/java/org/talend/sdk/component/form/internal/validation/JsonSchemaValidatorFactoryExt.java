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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.johnzon.jsonschema.regex.JavaRegex;
import org.apache.johnzon.jsonschema.spi.builtin.PatternValidation;
import org.talend.sdk.component.form.internal.validation.spi.ext.EnumValidationWithDefaultValue;
import org.talend.sdk.component.form.internal.validation.spi.ext.MaximumValidation;
import org.talend.sdk.component.form.internal.validation.spi.ext.MinimumValidation;
import org.talend.sdk.component.form.internal.validation.spi.ext.TypeValidation;

public class JsonSchemaValidatorFactoryExt extends org.apache.johnzon.jsonschema.JsonSchemaValidatorFactory {

    private final AtomicReference<Function<String, Predicate<CharSequence>>> reFactory =
            new AtomicReference<>(this::newRegexFactory);

    public JsonSchemaValidatorFactoryExt() {
        appendExtensions(new PatternValidation(reFactory.get()));
    }

    @Override
    public List<org.apache.johnzon.jsonschema.spi.ValidationExtension> createDefaultValidations() {
        List validations = super.createDefaultValidations()
                .stream()
                .filter(v -> !org.apache.johnzon.jsonschema.spi.builtin.TypeValidation.class.isInstance(v))
                .filter(v -> !org.apache.johnzon.jsonschema.spi.builtin.EnumValidation.class.isInstance(v))
                .filter(v -> !org.apache.johnzon.jsonschema.spi.builtin.MinimumValidation.class.isInstance(v))
                .filter(v -> !org.apache.johnzon.jsonschema.spi.builtin.MaximumValidation.class.isInstance(v))
                .filter(v -> !org.apache.johnzon.jsonschema.spi.builtin.PatternValidation.class.isInstance(v))
                .collect(Collectors.toList());
        validations.add(new TypeValidation());
        validations.add(new EnumValidationWithDefaultValue());
        validations.add(new MinimumValidation());
        validations.add(new MaximumValidation());

        return validations;
    }

    private Predicate<CharSequence> newRegexFactory(final String regex) {
        try {
            return new JavascriptRegex(regex);
        } catch (final RuntimeException re) {
            return new JavaRegex(regex);
        }
    }

}
