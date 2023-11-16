/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

import static java.util.Optional.ofNullable;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.runtime.manager.reflect.IconFinder;
import org.talend.sdk.component.tools.ComponentHelper;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

public class FamilyValidator implements Validator {

    private final Validators.ValidatorHelper helper;

    private final IconFinder iconFinder = new IconFinder();

    public FamilyValidator(final ValidatorHelper helper) {
        this.helper = helper;
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        return components.stream().flatMap(this::validateComponentClass);
    }

    private Stream<String> validateComponentClass(final Class<?> component) {
        List<String> errors = new ArrayList<>();
        try {
            final Icon icon = ComponentHelper
                    .findPackageOrFail(component, (Class<?> clazz) -> clazz.isAnnotationPresent(Icon.class),
                            Icon.class.getName())
                    .getAnnotation(Icon.class);

            ofNullable(helper.validateIcon(icon, errors)).ifPresent(errors::add);
        } catch (final IllegalArgumentException iae) {
            try {
                ComponentHelper.findPackageOrFail(component, this::isIndirectIconPresent, Icon.class.getName());
            } catch (final IllegalArgumentException iae2) {
                errors.add(iae.getMessage());
            }
        }
        return errors.stream();
    }

    private boolean isIndirectIconPresent(final AnnotatedElement ae) {
        return iconFinder.findIndirectIcon(ae).isPresent();
    }
}
