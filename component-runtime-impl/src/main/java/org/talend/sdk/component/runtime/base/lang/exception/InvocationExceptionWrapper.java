/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import java.lang.reflect.InvocationTargetException;

public class InvocationExceptionWrapper {

    /**
     * Wrap the target exception in a Runtime exception
     *
     * @param e
     */
    public static RuntimeException toRuntimeException(final InvocationTargetException e) {
        final Throwable targetException = e.getTargetException();
        if (RuntimeException.class.isInstance(targetException)) {
            return RuntimeException.class.cast(targetException);
        }
        return new IllegalStateException(targetException);
    }

}
