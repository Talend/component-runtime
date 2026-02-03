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
package org.talend.sdk.component.junit.http.internal.impl;

import static java.util.Locale.ROOT;

import java.util.function.Predicate;

public class DefaultHeaderFilter implements Predicate<String> {

    @Override
    public boolean test(final String header) {
        return "Authorization".equalsIgnoreCase(header) || header.toLowerCase(ROOT).contains("token")
                || header.toLowerCase(ROOT).contains("secret") || header.toLowerCase(ROOT).contains("password")
                || "User-Agent".equalsIgnoreCase(header) || "Host".equalsIgnoreCase(header)
                || "Date".equalsIgnoreCase(header) || "Content-Encoding".equalsIgnoreCase(header);
    }
}
