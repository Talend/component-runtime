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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.api.service.schema.DiscoverSchemaExtended;
import org.talend.sdk.component.api.service.schema.FixedSchema;

public class FixedSchemaValidator implements Validator {

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final List<String> errors = new ArrayList<>();
        final Map<Class<?>, String> classes = finder.findAnnotatedClasses(FixedSchema.class)
                .stream()
                .collect(toMap(identity(), d -> d.getAnnotation(FixedSchema.class).value()));
        // search for empty annotations
        errors.addAll(classes.entrySet()
                .stream()
                .filter(e -> e.getValue().isEmpty())
                .map(e -> String.format("Empty @FixedSchema annotation's value in class %s.",
                        e.getKey().getSimpleName()))
                .collect(Collectors.toList()));
        // search for missing methods
        final List<String> methods = Stream
                .concat(finder.findAnnotatedMethods(DiscoverSchema.class)
                        .stream()
                        .map(m -> m.getDeclaredAnnotation(DiscoverSchema.class).value()),
                        finder.findAnnotatedMethods(DiscoverSchemaExtended.class)
                                .stream()
                                .map(m -> m.getDeclaredAnnotation(DiscoverSchemaExtended.class).value()))
                .collect(Collectors.toList());
        errors.addAll(classes.entrySet()
                .stream()
                .filter(e -> !e.getValue().isEmpty())
                .filter(e -> !methods.contains(e.getValue()))
                .map(e -> String.format("@FixedSchema '%s' in class %s is not declared anywhere as DiscoverSchema*.",
                        e.getValue(), e.getKey().getSimpleName()))
                .collect(Collectors.toList()));

        return errors.stream();
    }
}
