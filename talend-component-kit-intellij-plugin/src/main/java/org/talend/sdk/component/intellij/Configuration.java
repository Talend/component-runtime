package org.talend.sdk.component.intellij;

import static java.util.ResourceBundle.getBundle;

import java.util.ResourceBundle;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Configuration {

    private static ResourceBundle bundle;

    public static String getMessage(String key) {
        if (bundle == null) {
            bundle = getBundle("messages");
        }
        return bundle.getString(key);
    }

    public static String getStarterHost() {
        final String debugHost = System.getProperty("org.talend.component.starter.host", null);
        return debugHost != null ? debugHost : "http://localhost:8080"; //todo use prod url
    }

    public static long getStarterLoadTimeOut() {
        final String timeoutValue = System.getProperty("org.talend.component.starter.timeout");
        long timeout = 40 * 1000;
        if (timeoutValue != null && !timeoutValue.isEmpty()) {
            try {
                timeout = Long.parseLong(timeoutValue);
            } catch (final NumberFormatException e) {
                log.error(e.getLocalizedMessage(), e);  //no-op
            }
        }
        return timeout;
    }

    private Configuration() {
    }
}
