/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.intellij;

import static java.util.ResourceBundle.getBundle;

import java.util.ResourceBundle;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Configuration {

    private static ResourceBundle bundle;

    public static String getMessage(final String key) {
        if (bundle == null) {
            bundle = getBundle("messages");
        }
        return bundle.getString(key);
    }

    public static String getStarterHost() {
        final String debugHost = System.getProperty("org.talend.component.starter.host", null);
        return debugHost != null ? debugHost : "http://localhost:8080"; // todo use prod url
    }

    public static long getStarterLoadTimeOut() {
        final String timeoutValue = System.getProperty("org.talend.component.starter.timeout");
        long timeout = 40 * 1000;
        if (timeoutValue != null && !timeoutValue.isEmpty()) {
            try {
                timeout = Long.parseLong(timeoutValue);
            } catch (final NumberFormatException e) {
                log.error(e.getLocalizedMessage(), e); // no-op
            }
        }
        return timeout;
    }

    private Configuration() {
        // no-op
    }
}
