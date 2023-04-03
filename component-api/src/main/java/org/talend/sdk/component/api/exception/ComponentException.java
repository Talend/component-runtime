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
        this.initCause(transformException(cause));
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
     * Transform all specific cause exceptions to Exception class.
     * 
     * @param t
     * @return An exception of type java.lang.Exception.
     */
    private Exception transformException(final Throwable t) {
        Exception cause = null;
        if (t.getCause() != null) {
            cause = transformException(t.getCause());
        }
        String newMsg = String.format("[%s] : %s", t.getClass(), t.getMessage());

        Exception e = new Exception(newMsg, cause);
        e.setStackTrace(t.getStackTrace());

        return e;
    }

}
