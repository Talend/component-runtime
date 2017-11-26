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
package org.talend.sdk.component.i18n;

import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public abstract class BaseControl extends ResourceBundle.Control {

    private static final List<String> FORMAT = singletonList("talend");

    @Override
    public List<String> getFormats(final String baseName) {
        return FORMAT;
    }

    @Override
    public Locale getFallbackLocale(final String baseName, final Locale locale) {
        return Locale.ENGLISH;
    }

    @Override
    public abstract ResourceBundle newBundle(final String baseName, final Locale locale, final String format,
            final ClassLoader loader, final boolean reload)
            throws IllegalAccessException, InstantiationException, IOException;

    @Override // can be overriden to limit the locales (only language handling for instance)
    public String toBundleName(final String baseName, final Locale locale) {
        return super.toBundleName(baseName, locale);
    }

    @Override // don't expire, ensure the impl handles the caching
    public long getTimeToLive(final String baseName, final Locale locale) {
        return TTL_NO_EXPIRATION_CONTROL;
    }

    @Override
    public boolean needsReload(final String baseName, final Locale locale, final String format,
            final ClassLoader loader, final ResourceBundle bundle, final long loadTime) {
        return false;
    }
}
