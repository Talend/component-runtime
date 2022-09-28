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
package org.talend.sdk.component.junit.http.junit4;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.talend.sdk.component.junit.http.api.ResponseLocator;
import org.talend.sdk.component.junit.http.internal.impl.DefaultResponseLocator;
import org.talend.sdk.component.junit.http.internal.impl.Handlers;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class JUnit4HttpApiPerMethodConfigurator implements TestRule {

    private final JUnit4HttpApi server;

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                final ResponseLocator responseLocator = server.getResponseLocator();
                if (DefaultResponseLocator.class.isInstance(responseLocator)) {
                    final DefaultResponseLocator defaultResponseLocator =
                            DefaultResponseLocator.class.cast(responseLocator);
                    defaultResponseLocator.setTest(description.getClassName() + "_" + description.getMethodName());
                }
                try {
                    base.evaluate();
                } finally {
                    if (DefaultResponseLocator.class.isInstance(responseLocator)) {
                        if (Handlers.isActive("capture")) {
                            final DefaultResponseLocator defaultResponseLocator =
                                    DefaultResponseLocator.class.cast(responseLocator);
                            defaultResponseLocator.flush(Handlers.getBaseCapture());
                        }
                    }
                }
            }
        };
    }
}
