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

/** The server could not be reached or did not answer in time: connection refused, unknown host, timeout. */
public final class ServerUnavailableException extends ServerException {

    private static final long serialVersionUID = 1L;

    public ServerUnavailableException(final String description, final Throwable cause) {
        super(0, "UNAVAILABLE", description, cause);
    }
}
