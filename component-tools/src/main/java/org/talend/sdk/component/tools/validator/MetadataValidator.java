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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.runtime.manager.reflect.IconFinder;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

public class MetadataValidator implements Validator {

    private final Validators.ValidatorHelper helper;

    public MetadataValidator(final ValidatorHelper helper) {
        this.helper = helper;
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        return components.stream().flatMap(component -> {
            final Stream<String> iconErrors = this.findIconsError(component);

            final Stream<String> versionErrors = this.findVersionsError(component);

            return Stream.concat(iconErrors, versionErrors);
        }).filter(Objects::nonNull).sorted();
    }

    private Stream<String> findIconsError(final Class<?> component) {
        final IconFinder iconFinder = new IconFinder();
        if (iconFinder.findDirectIcon(component).isPresent()) {
            final Icon icon = component.getAnnotation(Icon.class);
            final List<String> messages = new ArrayList<>();
            messages.add(helper.validateIcon(icon, messages));
            return messages.stream();
        } else if (!iconFinder.findIndirectIcon(component).isPresent()) {
            return Stream.of("No @Icon on " + component);
        }
        return Stream.empty();
    }

    private Stream<String> findVersionsError(final Class<?> component) {
        if (!component.isAnnotationPresent(Version.class)) {
            return Stream.of("Component " + component + " should use @Icon and @Version");
        }
        return Stream.empty();
    }
}
