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
package org.talend.sdk.component.tcketl.persistence;

import java.io.IOException;

/**
 * The file exists and is readable but its content is not a valid document: bad JSON, unexpected structure, invalid
 * value or unsupported format version. The message is meant to be shown to the user.
 */
public class FileFormatException extends IOException {

    private static final long serialVersionUID = 1L;

    public FileFormatException(final String message) {
        super(message);
    }

    public FileFormatException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
