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
package org.talend.sdk.component.tools.validator;

import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.meta.Documentation;

public class DocumentationWordingValidator implements Validator {

    private final Pattern correctWordingPattern = Pattern.compile("^[A-Z0-9]+.*\\.$");

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {

        final String error = "@Documentation on '%s' is empty or is not capitalized or ends not by a dot.";
        final Stream<String> componentWording = components
                .stream() //
                .filter(c -> c.isAnnotationPresent(Documentation.class)) //
                .filter(c -> isIncorrect(c.getAnnotation(Documentation.class))) //
                .map(c -> String.format(error, c.getName())) //
                .sorted();

        final Stream<String> fieldWording = finder
                .findAnnotatedFields(Option.class)
                .stream() //
                .filter(field -> field.isAnnotationPresent(Documentation.class))
                .filter((Field field) -> this.isIncorrect(field.getAnnotation(Documentation.class)))
                .map(c -> String.format(error, c.getName()))
                .sorted();
        return Stream.concat(componentWording, fieldWording);
    }

    private boolean isIncorrect(final Documentation annotation) {
        final String value = annotation.value();
        return !this.correctWordingPattern.matcher(value).matches();
    }

}
