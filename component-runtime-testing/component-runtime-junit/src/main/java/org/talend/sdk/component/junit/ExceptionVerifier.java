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
package org.talend.sdk.component.junit;

import static org.junit.Assert.fail;

import java.util.function.Consumer;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A rule to access an exception from a test using a {@link Consumer}
 * Usage example :
 * 
 * <pre>
 * {@code
 * Rule public ExceptionVerifier<HttpException> httpExceptionRule = new ExceptionVerifier<>();
 *  &#64;Test
 *  public void test(){
 *      httpExceptionRule.assertWith(e -> {
 *          assertEquals(401, e.getResponse().status());
 *          assertEquals("expected error message", e.getResponse().error(String.class));
 *      });
 *  }
 * }
 * </pre>
 */
public class ExceptionVerifier<T extends RuntimeException> implements TestRule {

    private Consumer<T> consumer;

    public void assertWith(final Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {

        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                    if (consumer != null) {
                        fail("expected exception not thrown");
                    }
                } catch (final RuntimeException e) {
                    if (consumer == null) {
                        throw e;
                    }
                    try {
                        consumer.accept((T) e);
                    } catch (final ClassCastException cce) {
                        throw e;
                    }
                } finally {
                    consumer = null;
                }
            }
        };
    }
}
