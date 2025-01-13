/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.reflect;

public class JavaVersion {

    /**
     * Retrieve major version
     *
     * @param version
     * @return major version
     */
    public static String getMajor(final String version) {
        final String[] versionElements = version.split("\\.");
        try {
            final String unsureVersion = strip(versionElements[0]);
            return isOldNotation(strip(unsureVersion)) ? strip(versionElements[1]) : unsureVersion;
        } catch (ArrayIndexOutOfBoundsException e) {
            return "-1";
        }
    }

    public static int getMajorNumber(final String version) {
        try {
            return Integer.parseInt(getMajor(version));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static int major() {
        return getMajorNumber(System.getProperty("java.version"));
    }

    /**
     * Retrieve minor version
     * <p>
     * Minor may contain chars like _+-
     *
     * @param version
     * @return minor version
     */
    public static String getMinor(final String version) {
        final String[] versionElements = version.split("\\.");
        try {
            // case for early access previews
            if (versionElements.length == 1) {
                return versionElements[0].indexOf('-') > 0
                        ? versionElements[0].substring(versionElements[0].indexOf('-') + 1)
                        : "-1";
            } else {
                final String unsureVersion = strip(versionElements[0]);
                return isOldNotation(unsureVersion) ? extractFromOldNotation(versionElements[2], true)
                        : versionElements[1];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return "-1";
        }
    }

    public static int getMinorNumber(final String version) {
        try {
            return Integer.parseInt(strip(getMinor(version)));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static int minor() {
        return getMinorNumber(System.getProperty("java.version"));
    }

    /**
     * Retrieve revision
     * <p>
     * Revision may contain chars like _+-
     *
     * @param version
     * @return revision
     */
    public static String getRevision(final String version) {
        final String[] versionElements = version.split("\\.");
        try {
            final String unsureVersion = strip(versionElements[0]);
            return isOldNotation(unsureVersion) ? extractFromOldNotation(versionElements[2], false)
                    : versionElements[2];
        } catch (ArrayIndexOutOfBoundsException e) {
            return "-1";
        }
    }

    public static int getRevisionNumber(final String version) {
        try {
            return Integer.parseInt(strip(getRevision(version)));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static int revision() {
        return getRevisionNumber(System.getProperty("java.version"));
    }

    private static String strip(final String segment) {
        return segment.replaceAll("[-+_].*", "");
    }

    private static boolean isOldNotation(final String segment) {
        return "1".equals(segment);
    }

    private static String extractFromOldNotation(final String segment, final boolean isMinor) {
        return isMinor ? segment.substring(0, segment.indexOf('_')) : segment.substring(segment.indexOf('_') + 1);
    }

}
