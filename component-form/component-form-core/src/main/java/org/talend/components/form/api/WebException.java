package org.talend.components.form.api;

import java.util.Map;

public class WebException extends RuntimeException {

    private final int status;

    private final Map<String, Object> data;

    public WebException(final Throwable cause, final int status, final Map<String, Object> data) {
        super(cause);
        this.status = status;
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
