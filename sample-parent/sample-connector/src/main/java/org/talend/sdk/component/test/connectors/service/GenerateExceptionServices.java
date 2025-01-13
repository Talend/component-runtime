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
package org.talend.sdk.component.test.connectors.service;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.runtime.base.lang.exception.InvocationExceptionWrapper;

@Service
public class GenerateExceptionServices implements Serializable {

    public void generateException() throws ComponentException {
        IndexOutOfBoundsException indexOutOfBoundsException =
                new IndexOutOfBoundsException("This is an index out of bound exception");
        IllegalArgumentException illegalArgumentException =
                new IllegalArgumentException("This is an illegal argument exception", indexOutOfBoundsException);
        IllegalStateException illegalStateException =
                new IllegalStateException("This is an illegal state exception", illegalArgumentException);
        throw new ComponentException("This is component exception", illegalStateException);
    }

    public void generateRuntimeException() throws ComponentException {
        throw InvocationExceptionWrapper
                .toRuntimeException(new InvocationTargetException(new CustomException("custom for test")));
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