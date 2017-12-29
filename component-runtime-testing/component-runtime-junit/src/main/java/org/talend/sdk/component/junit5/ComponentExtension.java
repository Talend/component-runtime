/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit5;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.talend.sdk.component.junit.BaseComponentsHandler;
import org.talend.sdk.component.junit.ComponentsHandler;

/**
 * Extension allowing the test to use a {@link org.talend.sdk.component.junit.ComponentsHandler}
 * and auto register components from current project.
 */
public class ComponentExtension extends BaseComponentsHandler
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(ComponentExtension.class.getName());

    @Override
    public void beforeAll(final ExtensionContext extensionContext) {
        final WithComponents element =
                extensionContext.getElement().map(e -> e.getAnnotation(WithComponents.class)).orElseThrow(
                        () -> new IllegalArgumentException(
                                "No annotation @WithComponents on " + extensionContext.getRequiredTestClass()));
        this.packageName = element.value();
        final ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
        store.put(EmbeddedComponentManager.class.getName(), start());
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext) {
        EmbeddedComponentManager.class
                .cast(extensionContext.getStore(NAMESPACE).get(EmbeddedComponentManager.class.getName()))
                .close();
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) {
        Class<?> testClass = extensionContext.getRequiredTestClass();
        while (testClass != Object.class) {
            Stream.of(testClass.getDeclaredFields()).filter(c -> c.isAnnotationPresent(Injected.class)).forEach(f -> {
                f.setAccessible(true);
                if (f.getType() != ComponentsHandler.class) {
                    throw new IllegalArgumentException(
                            "@Injected not supported on " + f + ", type should be ComponentsHandler");
                }
                try {
                    f.set(extensionContext.getRequiredTestInstance(), ComponentExtension.this);
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            });
            testClass = testClass.getSuperclass();
        }
    }
}
