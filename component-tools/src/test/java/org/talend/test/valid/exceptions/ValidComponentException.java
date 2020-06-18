package org.talend.test.valid.exceptions;

import org.talend.sdk.component.api.exception.ComponentException;

public class ValidComponentException extends ComponentException {

    public ValidComponentException(ErrorOrigin errorOrigin, String type, String message, StackTraceElement[] stackTrace, Throwable cause) {
        super(errorOrigin, type, message, stackTrace, cause);
    }
}
