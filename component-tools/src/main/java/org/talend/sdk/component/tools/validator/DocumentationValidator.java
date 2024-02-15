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

import java.util.List;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.meta.Documentation;

public class DocumentationValidator implements Validator {

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        return Stream
                .concat(components
                        .stream()
                        .filter(c -> !c.isAnnotationPresent(Documentation.class))
                        .map((Class<?> c) -> "No @Documentation on '" + c.getName() + "'")
                        .sorted(),
                        finder
                                .findAnnotatedFields(Option.class)
                                .stream()
                                .filter(field -> !field.isAnnotationPresent(Documentation.class)
                                        && !field.getType().isAnnotationPresent(Documentation.class))
                                .map(field -> "No @Documentation on '" + field + "'")
                                .sorted());
    }
}
