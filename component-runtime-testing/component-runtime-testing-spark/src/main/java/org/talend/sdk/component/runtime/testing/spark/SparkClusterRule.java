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
package org.talend.sdk.component.runtime.testing.spark;

import java.io.File;

import org.junit.Assert;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.talend.sdk.component.runtime.testing.spark.internal.BaseSpark;

public class SparkClusterRule extends BaseSpark<SparkClusterRule> implements TestRule {

    private final TemporaryFolder temporaryFolder = new TemporaryFolder();

    public SparkClusterRule() {
        // no-op
    }

    public SparkClusterRule(final String scala, final String spark, final int slaves) {
        withScalaVersion(scala).withSparkVersion(spark).withSlaves(slaves);
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return temporaryFolder.apply(new Statement() {

            @Override
            public void evaluate() throws Throwable {
                try (final Instances closeable = start()) {
                    if (closeable.getException() != null) {
                        closeable.close();
                        throw closeable.getException();
                    }
                    base.evaluate();
                }
            }
        }, description);
    }

    @Override
    protected void fail(final String message) {
        Assert.fail(message);
    }

    @Override
    protected void assertTrue(final String message, final boolean value) {
        Assert.assertTrue(message, value);
    }

    @Override
    protected File getRoot() {
        return temporaryFolder.getRoot();
    }
}
