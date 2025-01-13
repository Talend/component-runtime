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

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.exception.DiscoverSchemaException;

public class InvocationExceptionWrapper {

    /**
     * Wrap the target exception in a Runtime exception
     *
     * @param e the exception to wrap in a way which will remove classloader specific exceptions.
     */
    public static RuntimeException toRuntimeException(final InvocationTargetException e) {
        final Set<Throwable> visited = new HashSet<>();
        visited.add(e.getTargetException());
        return mapException(e.getTargetException(), visited);
    }

    private static RuntimeException mapException(final Throwable targetException, final Collection<Throwable> visited) {
        if (targetException == null) {
            return null;
        }
        if (ComponentException.class.isInstance(targetException)) {
            return ComponentException.class.cast(targetException);
        }
        if (DiscoverSchemaException.class.isInstance(targetException)) {
            return DiscoverSchemaException.class.cast(targetException);
        }
        if (RuntimeException.class.isInstance(targetException)
                && targetException.getClass().getName().startsWith("java.")) {
            final RuntimeException cast = RuntimeException.class.cast(targetException);
            if (cast.getCause() == null
                    || (cast.getCause() != null && cast.getCause().getClass().getName().startsWith("java."))) {
                return cast;
            } // else, let it be wrapped to ensure all the stack is serializable
        }
        final ComponentException exception = new ComponentException(targetException.getClass().getName(),
                targetException.getMessage(), targetException.getStackTrace(), mapCause(targetException, visited));
        if (exception.getSuppressed() != null && exception.getSuppressed().length > 0) {
            Stream
                    .of(exception.getSuppressed())
                    .map(it -> mapCause(it, new HashSet<>()))
                    .filter(Objects::nonNull)
                    .forEach(exception::addSuppressed);
        }
        return exception;
    }

    private static Throwable mapCause(final Throwable targetException, final Collection<Throwable> visited) {
        final Throwable cause = targetException.getCause();
        if (cause == null || !visited.add(cause)) {
            return null;
        }
        return mapException(cause, visited);
    }
}
