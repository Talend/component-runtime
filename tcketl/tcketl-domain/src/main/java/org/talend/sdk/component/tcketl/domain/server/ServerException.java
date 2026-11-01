/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tcketl.domain.server;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * The server answered with an error. {@code code} is the server error code (for example {@code COMPONENT_MISSING}) and
 * {@code description} its human readable text, which is only displayed and never parsed. Subclasses cover the failures
 * where there is no usable answer.
 */
public class ServerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    private final String code;

    private final String description;

    public ServerException(final int status, final String code, final String description) {
        this(status, code, description, null);
    }

    protected ServerException(final int status, final String code, final String description, final Throwable cause) {
        super(message(status, code, description), cause);
        this.status = status;
        this.code = Preconditions.nullToEmpty(code);
        this.description = Preconditions.nullToEmpty(description);
    }

    /** HTTP status, or 0 when there is no HTTP response. */
    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }

    private static String message(final int status, final String code, final String description) {
        final StringBuilder message = new StringBuilder();
        if (status > 0) {
            message.append("HTTP ").append(status);
        }
        if (code != null && !code.isBlank()) {
            message.append(message.length() == 0 ? "" : " ").append(code);
        }
        if (description != null && !description.isBlank()) {
            message.append(message.length() == 0 ? "" : ": ").append(description);
        }
        return message.length() == 0 ? "Component server error" : message.toString();
    }
}
