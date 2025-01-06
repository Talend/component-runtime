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
package org.talend.sdk.component.junit.environment;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.talend.sdk.component.junit.delegate.DelegatingRunner;

public class MultiEnvironmentsRunner extends DelegatingRunner {

    private final EnvironmentsConfigurationParser configuration;

    public MultiEnvironmentsRunner(final Class<?> testClass) throws InitializationError {
        super(testClass);
        configuration = new EnvironmentsConfigurationParser(testClass);
    }

    @Override
    public void run(final RunNotifier notifier) {
        configuration.stream().forEach(e -> {
            if (DecoratingEnvironmentProvider.class.isInstance(e)) {
                final DecoratingEnvironmentProvider dep = DecoratingEnvironmentProvider.class.cast(e);
                if (!dep.isActive()) {
                    notifier.fireTestFinished(Description.createTestDescription(getTestClass(), dep.getName()));
                    return;
                }
            }
            try (final AutoCloseable ignored = e.start(getTestClass(), getTestClass().getAnnotations())) {
                MultiEnvironmentsRunner.super.run(notifier);
            } catch (final Exception e1) {
                throw new IllegalStateException(e1);
            }
        });
    }
}
