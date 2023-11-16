/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front.base.internal;

import java.util.Locale;
import java.util.Objects;

public class RequestKey {

    private final Locale locale;

    private final boolean includeIconContent;

    private final String query;

    private final int cacheHash;

    public RequestKey(final Locale locale, final boolean includeIconContent, final String query) {
        this.locale = locale;
        this.includeIconContent = includeIconContent;
        this.query = query;
        this.cacheHash = Objects.hash(locale, includeIconContent, query);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final RequestKey that = RequestKey.class.cast(o);
        return Objects.equals(locale, that.locale) && Objects.equals(includeIconContent, that.includeIconContent)
                && Objects.equals(query, that.query);
    }

    @Override
    public int hashCode() {
        return cacheHash;
    }
}
