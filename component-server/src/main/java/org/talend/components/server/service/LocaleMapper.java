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
package org.talend.components.server.service;

import static java.util.Locale.ENGLISH;

import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LocaleMapper {

    // intended to limit and normalize the locales to avoid a tons when used with caching
    public Locale mapLocale(final String requested) {
        return new Locale(getLanguage(requested).toLowerCase(ENGLISH));
    }

    private String getLanguage(final String requested) {
        if (requested == null || requested.startsWith("en_")) {
            return "en";
        }
        final int split = requested.indexOf('_');
        if (split > 0) {
            return requested.substring(0, split);
        }
        return requested;
    }
}
