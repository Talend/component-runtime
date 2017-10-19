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
package org.talend.components.server.front.base.internal;

import java.util.Locale;
import java.util.Objects;

public class RequestKey {

    private final Locale locale;

    private final int cacheHash;

    public RequestKey(final Locale locale) {
        this.locale = locale;
        this.cacheHash = Objects.hash(locale);
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
        return Objects.equals(locale, that.locale);
    }

    @Override
    public int hashCode() {
        return cacheHash;
    }
}
