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
        final File c1Class = new File(resource.getPath());
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
        Assertions.assertFalse(errors.isEmpty());

    }

    public static class C1 {

    }

    public static class C2 extends ComponentException {

        public C2(final ErrorOrigin errorOrigin, final String message, final Throwable cause) {
            super(errorOrigin, message, cause);
        }
    }
}