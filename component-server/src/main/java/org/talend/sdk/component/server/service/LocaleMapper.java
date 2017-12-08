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
package org.talend.sdk.component.server.service;

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
