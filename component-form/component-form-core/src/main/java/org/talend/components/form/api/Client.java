// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.form.api;

import java.util.Iterator;
import java.util.Map;

import org.talend.components.server.front.model.ComponentDetail;
import org.talend.components.server.front.model.ComponentDetailList;
import org.talend.components.server.front.model.ComponentIndices;

// note: we can make it async since both impl support it but does it make much sense OOTB?

/**
 * Abstract the HTTP layer. Note that the implementation must support a constructor with a String parameter
 * representing the base of the http requests.
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
