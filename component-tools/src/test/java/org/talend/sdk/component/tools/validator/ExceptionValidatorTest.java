package org.talend.sdk.component.tools.validator;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.tools.ComponentValidator.Configuration;

class ExceptionValidatorTest {

    private File fileC1;

    @Test
    public void test() {
        final URL resource = Thread.currentThread().getContextClassLoader().getResource(".");
        File c1Class = new File(resource.getPath());
        final FakeHelper helper = new FakeHelper() {
            @Override
            public Stream<File> componentClassFiles() {
                return Stream.of(c1Class);
            }
        };
        final Configuration configuration = new Configuration();
        configuration.setFailOnValidateExceptions(true);
        final ExceptionValidator validator = new ExceptionValidator(helper, configuration);

        final Stream<String> validate = validator.validate(null, null);
        final List<String> errors = validate.collect(Collectors.toList());
        Assertions.assertTrue(errors.isEmpty());

    }

    public static class C1 {

    }

    public static class C2 extends ComponentException {

        public C2(ErrorOrigin errorOrigin, String message, Throwable cause) {
            super(errorOrigin, message, cause);
        }
    }
}