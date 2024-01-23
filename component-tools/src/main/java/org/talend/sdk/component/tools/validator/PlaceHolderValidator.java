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
package org.talend.sdk.component.tools.validator;

import static java.util.Arrays.asList;
import static org.talend.sdk.component.runtime.manager.ParameterMeta.Type.ENUM;
import static org.talend.sdk.component.runtime.manager.ParameterMeta.Type.STRING;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

public class PlaceHolderValidator implements Validator {

    private static final String ERROR_MSG_FORMAT =
            "No %s._placeholder set for %s in Messages.properties of packages: %s";

    private final Validators.ValidatorHelper helper;

    public PlaceHolderValidator(final ValidatorHelper helper) {
        this.helper = helper;
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return components
                .stream()
                .map(helper::buildOrGetParameters)
                .flatMap(Validators::flatten)
                .filter(this::isStringifiable)
                .filter(p -> !hasPlaceholder(loader, p))
                .map(this::buildErrorMessage);
    }

    private String buildErrorMessage(final ParameterMeta p) {
        final String emplacement = p.getSource().declaringClass().getSimpleName() + "." + p.getSource().name();
        return String.format(ERROR_MSG_FORMAT, emplacement, p.getPath(), asList(p.getI18nPackages()));
    }

    private boolean isStringifiable(final ParameterMeta meta) {
        return STRING.equals(meta.getType()) || ENUM.equals(meta.getType());
    }

    private boolean hasPlaceholder(final ClassLoader loader, final ParameterMeta parameterMeta) {
        return parameterMeta.findBundle(loader, Locale.ROOT).placeholder(null).isPresent();
    }
}
