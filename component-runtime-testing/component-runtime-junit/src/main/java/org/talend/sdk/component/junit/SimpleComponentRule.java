/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleComponentRule extends BaseComponentsHandler implements TestRule {

    public SimpleComponentRule(final String packageName) {
        this.packageName = packageName;
    }

    @Override
    public SimpleComponentRule withIsolatedPackage(final String pck, final String... packages) {
        super.withIsolatedPackage(pck, packages);
        return this;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                try (final EmbeddedComponentManager manager = start()) {
                    base.evaluate();
                }
            }
        };
    }
}
