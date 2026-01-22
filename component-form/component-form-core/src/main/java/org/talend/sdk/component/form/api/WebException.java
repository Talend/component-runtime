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
package org.talend.sdk.component.form.api;

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
