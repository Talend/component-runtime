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
package org.talend.sdk.component.tools.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Output;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class OutputConnectionValidatorTest {

    @ParameterizedTest
    @MethodSource("data")
    void validate(Class<?> clazz, boolean ok) {
        OutputConnectionValidator validator = new OutputConnectionValidator();
        Stream<String> validate = validator.validate(null, Arrays.asList(clazz));
        if (ok) {
            Assertions.assertEquals(0l, validate.count(), "Class " + clazz.getName() + " should be valid");
        } else {
            Assertions.assertEquals(1l, validate.count(), "Class " + clazz.getName() + " should be invalid");
        }

    }

    static class OK {

        @ElementListener
        public void m1(Object input) {
        }
    }

    static class OK1 {

        @ElementListener
        public void m1(Object input, @Output Object output1) {
        }
    }

    static class OK2 {

        @ElementListener
        public void m1(Object input, @Output Object output1, @Output Object output2) {
        }
    }

    static class OK3 {

        @ElementListener
        public void m1(Object input) {
        }

        @ElementListener
        public void m2(Object input, @Output Object output1) {
        }
    }

    static class KO {

        @ElementListener
        public void m1(Object input1, Object input2) {
        }
    }

    static class KO2 {

        @ElementListener
        public void m1(Object input1, Object input2) {
        }

        @ElementListener
        public void m2(Object input, @Output Object output1) {
        }
    }

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(OK.class, true),
                Arguments.of(OK1.class, true),
                Arguments.of(OK2.class, true),
                Arguments.of(OK3.class, true),
                Arguments.of(KO.class, false),
                Arguments.of(KO2.class, false)

        );
    }
}