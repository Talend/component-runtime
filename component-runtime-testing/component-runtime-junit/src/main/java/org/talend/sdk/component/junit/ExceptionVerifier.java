package org.talend.sdk.component.junit;

import static org.junit.Assert.fail;

import java.util.function.Consumer;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A rule to access an exception from a test using a Consumer<HttpException>.
 * <code>
 *
 * @Rule public ExceptionVerifier<HttpException> httpExceptionRule = new ExceptionVerifier<>();
 * ...
 * httpExceptionRule.assertWith(e -> {
 * assertEquals(401, e.getResponse().status());
 * assertEquals("expected error message", e.getResponse().error(String.class));
 * });
 *
 * </code>
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
