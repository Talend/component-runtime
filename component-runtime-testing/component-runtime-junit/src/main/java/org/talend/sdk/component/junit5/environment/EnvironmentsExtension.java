/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit5.environment;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.talend.sdk.component.junit.environment.DecoratingEnvironmentProvider;
import org.talend.sdk.component.junit.environment.EnvironmentsConfigurationParser;
import org.talend.sdk.component.junit5.ComponentExtension;
import org.talend.sdk.component.junit5.WithComponents;

public class EnvironmentsExtension implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(final ExtensionContext context) {
        return isAnnotated(context.getTestMethod(), EnvironmentalTest.class);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(final ExtensionContext context) {
        final String format = findAnnotation(context.getRequiredTestMethod(), EnvironmentalTest.class).get().format();
        return new EnvironmentsConfigurationParser(context.getRequiredTestClass())
                .stream()
                .map(e -> new EnvironmentalContext(e,
                        format
                                .replace("${displayName}", context.getDisplayName())
                                .replace("${environment}", DecoratingEnvironmentProvider.class.cast(e).getName()),
                        createComponentExtension(context)));
    }

    private ComponentExtension createComponentExtension(final ExtensionContext context) {
        return context
                .getElement()
                .map(it -> it.isAnnotationPresent(WithComponents.class))
                .filter(it -> it)
                .orElseGet(() -> context
                        .getParent()
                        .flatMap(it -> it.getElement().map(e -> e.isAnnotationPresent(WithComponents.class)))
                        .orElse(false))
                                ? context
                                        .getStore(ExtensionContext.Namespace.create(ComponentExtension.class.getName()))
                                        .get(ComponentExtension.class.getName() + ".instance", ComponentExtension.class)
                                : null;
    }
}
