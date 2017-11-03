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

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.spi.ResourceBundleControlProvider;

public abstract class BaseProvider implements ResourceBundleControlProvider {

    @Override
    public ResourceBundle.Control getControl(final String baseName) {
        return supports(baseName) ? new BaseControl() {

            @Override
            public ResourceBundle newBundle(final String baseName, final Locale locale, final String format,
                    final ClassLoader loader, final boolean reload)
                    throws IllegalAccessException, InstantiationException, IOException {
                return createBundle(baseName, locale);
            }
        } : null;
    }

    protected abstract ResourceBundle createBundle(String baseName, Locale locale);

    protected abstract boolean supports(String baseName);
}
