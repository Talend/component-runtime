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

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.runtime.manager.service.http.HttpClientFactoryImpl;

public class HttpValidator implements Validator {

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        // If the class extends HttpClient, it should use @Request.
        List<String> classErrors = components.stream()
                .filter(c -> HttpClient.class.isAssignableFrom(c)
                        && finder.findAnnotatedMethods(Request.class).isEmpty())
                .map(c -> c.getCanonicalName() + " extends HttpClient should use @Request on methods")
                .collect(Collectors.toList());

        List<String> methodError = finder
                .findAnnotatedMethods(Request.class) //
                .stream() //
                .map(Method::getDeclaringClass) //
                .distinct() //
                .flatMap(c -> HttpClientFactoryImpl.createErrors(c).stream())
                .sorted()
                .collect(Collectors.toList());

        return Stream.concat(classErrors.stream(), methodError.stream());

    }
}
