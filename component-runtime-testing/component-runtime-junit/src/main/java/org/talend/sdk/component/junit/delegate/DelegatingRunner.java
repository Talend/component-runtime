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
package org.talend.sdk.component.junit.delegate;

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PROTECTED;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;

import lombok.Getter;

public class DelegatingRunner extends Runner {

    private final Runner delegate;

    @Getter(PROTECTED)
    private final Class<?> testClass;

    public DelegatingRunner(final Class<?> testClass) throws InitializationError {
        this.testClass = testClass;
        try {
            final Optional<? extends Class<? extends Runner>> delegateClass =
                    ofNullable(testClass.getAnnotation(DelegateRunWith.class)).map(DelegateRunWith::value);
            this.delegate =
                    delegateClass.isPresent() ? delegateClass.get().getConstructor(Class.class).newInstance(testClass)
                            : new JUnit4(testClass);
        } catch (final InstantiationException | NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (final InvocationTargetException e) {
            if (InitializationError.class.isInstance(e.getCause())) {
                throw InitializationError.class.cast(e.getCause());
            }
            throw new IllegalStateException(e.getTargetException());
        }
    }

    @Override
    public Description getDescription() {
        return delegate.getDescription();
    }

    @Override
    public void run(final RunNotifier notifier) {
        delegate.run(notifier);
    }
}
