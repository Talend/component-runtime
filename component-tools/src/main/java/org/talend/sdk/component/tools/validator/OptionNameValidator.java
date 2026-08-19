/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.configuration.Option;

public class OptionNameValidator implements Validator {

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        return finder
                .findAnnotatedFields(Option.class)
                .stream() //
                .filter(field -> {
                    final String name = nameOption(field); //
                    return name.contains(".") || name.startsWith("$"); //
                }) //
                .distinct() //
                .map(field -> {
                    final String name = nameOption(field);
                    return "Option name `" + name
                            + "` is invalid, you can't start an option name with a '$' and it can't contain a '.'. "
                            + "Please fix it on field `" + field.getDeclaringClass().getName() + "#" + field.getName()
                            + "`";
                }) //
                .sorted();
    }

    private String nameOption(final Field field) {
        return field.getAnnotation(Option.class).value();
    }
}
