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

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

import java.util.List;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.talend.sdk.component.junit.environment.DecoratingEnvironmentProvider;
import org.talend.sdk.component.junit.environment.EnvironmentProvider;
import org.talend.sdk.component.junit5.ComponentExtension;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EnvironmentalContext implements TestTemplateInvocationContext {

    private final EnvironmentProvider provider;

    private final String displayName;

    private final ComponentExtension componentExtension;

    @Override
    public String getDisplayName(final int invocationIndex) {
        return displayName;
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
        return singletonList(new EnvironmentalLifecycle(provider, componentExtension, null));
    }

    @AllArgsConstructor
    public static class EnvironmentalLifecycle implements BeforeEachCallback, AfterEachCallback, ExecutionCondition,
            ParameterResolver, TestInstancePostProcessor {

        private final EnvironmentProvider provider;

        private final ComponentExtension componentExtension;

        private AutoCloseable closeable;

        @Override
        public void beforeEach(final ExtensionContext context) {
            closeable = provider.start(context.getRequiredTestClass(), context.getRequiredTestClass().getAnnotations());
            ofNullable(componentExtension).ifPresent(c -> {
                c.doStart(context);
                c.doInject(context);
            });
        }

        @Override
        public void afterEach(final ExtensionContext context) {
            ofNullable(componentExtension).ifPresent(c -> {
                c.resetState();
                c.doStop(context);
            });
            ofNullable(closeable).ifPresent(c -> {
                try {
                    c.close();
                } catch (final Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        }

        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(final ExtensionContext context) {
            return isActive() ? ConditionEvaluationResult.enabled("provider is active")
                    : ConditionEvaluationResult.disabled("provider is disabled");
        }

        private boolean isActive() {
            return DecoratingEnvironmentProvider.class.isInstance(provider)
                    && DecoratingEnvironmentProvider.class.cast(provider).isActive();
        }

        @Override
        public boolean supportsParameter(final ParameterContext parameterContext,
                final ExtensionContext extensionContext) throws ParameterResolutionException {
            return componentExtension != null
                    && componentExtension.supportsParameter(parameterContext, extensionContext);
        }

        @Override
        public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return componentExtension == null ? null
                    : componentExtension.resolveParameter(parameterContext, extensionContext);
        }

        @Override
        public void postProcessTestInstance(final Object o, final ExtensionContext extensionContext) {
            if (componentExtension != null) {
                componentExtension.postProcessTestInstance(o, extensionContext);
            }
        }
    }
}
