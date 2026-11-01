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
package org.talend.sdk.component.tcketl.app.session;

import org.talend.sdk.component.tcketl.domain.server.ServerException;
import org.talend.sdk.component.tcketl.domain.server.ServerProtocolException;
import org.talend.sdk.component.tcketl.domain.server.ServerUnavailableException;

/** Texts shown to the user when a call to the component server fails. */
public final class Problems {

    private Problems() {
        // no-op
    }

    public static String describe(final Throwable failure) {
        if (failure instanceof ServerUnavailableException unavailable) {
            return unavailable.description();
        }
        if (failure instanceof ServerProtocolException protocol) {
            return "Unexpected answer from the server: " + protocol.description();
        }
        if (failure instanceof ServerException server) {
            return "The server reported an error: " + server.getMessage();
        }
        return "Unexpected error: " + failure;
    }

    /** Server failures are expected and need no stack trace in the log, anything else is a defect of the client. */
    public static boolean isExpected(final Throwable failure) {
        return failure instanceof ServerException;
    }
}
