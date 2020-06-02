package org.talend.sdk.component.api.exception;

import lombok.Getter;

public class ComponentException extends RuntimeException {

    public static enum ErrorOrigin {
        USER,   // Error caused by user misconfiguration
        BACKEND, // Error caused by backend
        UNKNOWN
    }

    @Getter
    private final ErrorOrigin errorOrigin;

    @Getter
    private final String originalType;

    @Getter
    private final String originalMessage;

    public ComponentException(final ErrorOrigin errorOrigin, final String type, final String message, final StackTraceElement[] stackTrace,
                              final Throwable cause) {
        super((type != null  ? "(" + type + ") " : "") + message, cause);
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
        this(errorOrigin, cause != null ? cause.getClass().getName() :null, message, null, cause);
    }


}
