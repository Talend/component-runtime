/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import java.util.Iterator;
import java.util.Map;

import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentIndices;

// note: we can make it async since both impl support it but does it make much sense OOTB?

/**
 * Abstract the HTTP layer. Note that the implementation must support a
 * constructor with a String parameter representing the base of the http
 * requests.
 */
public interface Client extends AutoCloseable {

    Map<String, Object> action(String family, String type, String action, final Map<String, Object> params);

    ComponentIndices index(String language);

    ComponentDetailList details(String language, String identifier, String... identifiers);

    default ComponentIndices index() {
        return index("en");
    }

    default ComponentDetailList details(final String identifier, final String... identifiers) {
        return details("en", identifiers);
    }

    default ComponentDetail detail(final String lang, final String identifier) {
        final Iterator<ComponentDetail> iterator = details(lang, identifier, new String[0]).getDetails().iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    default ComponentDetail detail(final String identifier) {
        return detail("en", identifier);
    }

    @Override
    void close();
}
