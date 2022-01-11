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
package org.talend.sdk.component.tools.validator;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.tools.spi.ValidationExtension;
import org.talend.sdk.component.tools.spi.ValidationExtension.ValidationResult;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

public class ExtensionValidator implements Validator {

    private final ValidationExtension extension;

    private final Validators.ValidatorHelper helper;

    public ExtensionValidator(final ValidationExtension extension, final ValidatorHelper helper) {
        this.extension = extension;
        this.helper = helper;
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final ValidationExtension.ValidationContext context = new ValidationExtension.ValidationContext() {

            @Override
            public AnnotationFinder finder() {
                return finder;
            }

            @Override
            public List<Class<?>> components() {
                return components;
            }

            @Override
            public List<ParameterMeta> parameters(final Class<?> component) {
                return helper.buildOrGetParameters(component);
            }
        };
        final ValidationResult result = this.extension.validate(context);

        final Stream<String> errors;
        if (result == null || result.getErrors() == null) {
            errors = Stream.empty();
        } else {
            errors = result.getErrors().stream();
        }
        return errors.filter(Objects::nonNull);
    }
}
