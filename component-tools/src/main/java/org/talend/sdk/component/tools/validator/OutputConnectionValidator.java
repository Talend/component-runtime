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

import static java.util.stream.Stream.of;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.MultiOutputIterator;
import org.talend.sdk.component.api.processor.Output;

public class OutputConnectionValidator implements Validator {

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        // outputs must have only one input param
        return Stream
                .concat(components
                        .stream()
                        .flatMap(c -> of(c.getMethods()).filter(m -> m.isAnnotationPresent(ElementListener.class)))
                        .filter(m -> of(m.getParameters()).noneMatch(p -> p.isAnnotationPresent(Output.class)))
                        .filter(m -> of(m.getParameters()).filter(p -> !p.isAnnotationPresent(Output.class))
                                .count() > 1)
                        .map(Method::getDeclaringClass)
                        .distinct()
                        .map(clazz -> "The Output component '" + clazz
                                + "' must have only one single input branch parameter in its ElementListener method."),
                        validateMultiOutputIteratorBranches(components));
    }

    /**
     * A MultiOutputIterator routes records by branch name, so the branches it feeds can't be guessed from the
     * signature: they must be declared through {@code @Output(branches = ...)} to be exposed to the design layer.
     */
    private Stream<String> validateMultiOutputIteratorBranches(final List<Class<?>> components) {
        return components
                .stream()
                .flatMap(c -> of(c.getMethods())
                        .filter(m -> m.isAnnotationPresent(ElementListener.class)
                                || m.isAnnotationPresent(AfterGroup.class)))
                .flatMap(m -> of(m.getParameters()))
                .filter(p -> p.isAnnotationPresent(Output.class))
                .filter(this::isMultiOutputIterator)
                .filter(p -> p.getAnnotation(Output.class).branches().length == 0)
                .map(p -> "The MultiOutputIterator parameter '" + p.getName() + "' of '"
                        + p.getDeclaringExecutable().getDeclaringClass().getName()
                        + "' must declare the branches like : @Output(branches = { \"branch1\", \"branch2\" }).")
                .distinct();
    }

    private boolean isMultiOutputIterator(final Parameter p) {
        return MultiOutputIterator.class == p.getType()
                || (p.getParameterizedType() instanceof ParameterizedType pt
                        && MultiOutputIterator.class == pt.getRawType());
    }
}
