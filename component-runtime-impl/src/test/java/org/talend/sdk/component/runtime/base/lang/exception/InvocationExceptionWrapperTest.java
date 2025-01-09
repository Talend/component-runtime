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
package org.talend.sdk.component.runtime.base.lang.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.exception.ComponentException;

class InvocationExceptionWrapperTest {

    @Test
    void ensureOriginalIsReplacedToGuaranteeSerializationAccrossClassLoaders() {
        final RuntimeException mapped = InvocationExceptionWrapper
                .toRuntimeException(new InvocationTargetException(new CustomException("custom for test")));
        assertTrue(ComponentException.class.isInstance(mapped));
        assertEquals("(" + CustomException.class.getName() + ") custom for test", mapped.getMessage());
        assertTrue(ComponentException.class.isInstance(mapped.getCause()));
        assertEquals("(" + AnotherException.class.getName() + ") other", mapped.getCause().getMessage());
    }

    public static class CustomException extends RuntimeException {

        public CustomException(final String message) {
            super(message, new AnotherException("other"));
        }
    }

    public static class AnotherException extends RuntimeException {

        public AnotherException(final String message) {
            super(message);
        }
    }
}
