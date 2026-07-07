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
package org.talend.sdk.component.server.service;

/**
 * Holds the HTTP request path for the current thread, set by {@code RequestContextFilter}.
 * Provides context information when a fatal error is recorded during request processing.
 */
public class RequestContextHolder {

    private static final ThreadLocal<String> REQUEST_PATH = new ThreadLocal<>();

    private RequestContextHolder() {
        // utility
    }

    public static void set(final String path) {
        REQUEST_PATH.set(path);
    }

    public static String get() {
        return REQUEST_PATH.get();
    }

    public static void clear() {
        REQUEST_PATH.remove();
    }
}
