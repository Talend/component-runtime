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
package org.talend.sdk.component.api.exception;

import lombok.Getter;

public class ComponentException extends RuntimeException {

    public static enum ErrorOrigin {
        USER, // Error caused by user misconfiguration
        BACKEND, // Error caused by backend
        UNKNOWN // Any other error
    }

    @Getter
    private final ErrorOrigin errorOrigin;

    @Getter
    private final String originalType;

    @Getter
    private final String originalMessage;

    public ComponentException(final ErrorOrigin errorOrigin, final String type, final String message,
            final StackTraceElement[] stackTrace, final Throwable cause) {
        super((type != null ? "(" + type + ") " : "") + message);
        this.initCause(toGenericThrowable(cause));
        this.errorOrigin = errorOrigin;
        originalType = type;
        originalMessage = message;
        if (stackTrace != null) {
            setStackTrace(stackTrace);
        }
    }

    public ComponentException(final String type, final String message, final StackTraceElement[] stackTrace,
            final Throwable cause) {
        this(ErrorOrigin.UNKNOWN, type, message, stackTrace, cause);
    }

    public ComponentException(final ErrorOrigin errorOrigin, final String message) {
        this(errorOrigin, message, null);
    }

    public ComponentException(final ErrorOrigin errorOrigin, final String message, final Throwable cause) {
        this(errorOrigin, cause != null ? cause.getClass().getName() : null, message, null, cause);
    }

    public ComponentException(final String message) {
        this(ErrorOrigin.UNKNOWN, message);
    }

    public ComponentException(final String message, final Throwable cause) {
        this(ErrorOrigin.UNKNOWN, message, cause);
    }

    public ComponentException(final Throwable cause) {
        this(ErrorOrigin.UNKNOWN, cause.getMessage(), cause);
    }

    /**
     * Convert all exception stack to generic throwable stack to avoid unknown exception at deserialization time..
     * 
     * @param t
     * @return An Throwable.
     */
    protected Throwable toGenericThrowable(final Throwable t) {
        if (t == null) {
            return null;
        }
        if (t instanceof ComponentException) {
            return t;
        }
        Throwable generic = new Throwable(String.format("(%s) : %s", t.getClass().getName(), t.getMessage()));
        generic.setStackTrace(t.getStackTrace());

        Throwable cause = t.getCause();
        Throwable genericCause = null;
        if (cause != null) {
            genericCause = toGenericThrowable(cause);
        } else {
            return generic;
        }

        if (genericCause != null) {
            generic = new Throwable(generic.getMessage(), genericCause);
        }

        return generic;
    }

}
