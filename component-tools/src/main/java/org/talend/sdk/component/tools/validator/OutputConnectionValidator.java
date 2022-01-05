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

import static java.util.stream.Stream.of;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Output;

public class OutputConnectionValidator implements Validator {

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        // outputs must have only one input param
        return components
                .stream()
                .flatMap(c -> of(c.getMethods()).filter(m -> m.isAnnotationPresent(ElementListener.class)))
                .filter(m -> of(m.getParameters()).noneMatch(p -> p.isAnnotationPresent(Output.class)))
                .filter(m -> of(m.getParameters()).filter(p -> !p.isAnnotationPresent(Output.class)).count() > 1)
                .map(Method::getDeclaringClass)
                .distinct()
                .map(clazz -> "The Output component '" + clazz
                        + "' must have only one single input branch parameter in its ElementListener method.");
    }
}
