/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tcketl.persistence;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version of a file format, written {@code major.minor}. A reader accepts every file of its own major version (a newer
 * minor only adds fields that are then ignored) and refuses any other major version.
 */
public record FormatVersion(int major, int minor) {

    private static final Pattern SYNTAX = Pattern.compile("(\\d{1,4})\\.(\\d{1,4})");

    public FormatVersion {
        if (major < 0 || minor < 0) {
            throw new IllegalArgumentException("A format version cannot be negative: " + major + "." + minor);
        }
    }

    /**
     * Checks the version declared by a document.
     *
     * @param declared the {@code format-version} value, {@code null} when the document has none.
     * @param supported the version this application writes and reads.
     */
    public static void verify(final String declared, final FormatVersion supported) throws FileFormatException {
        if (declared == null) {
            throw new FileFormatException("The 'format-version' is missing");
        }
        final Matcher matcher = SYNTAX.matcher(declared.strip());
        if (!matcher.matches()) {
            throw new FileFormatException("The 'format-version' is not of the form <major>.<minor>: " + declared);
        }
        final FormatVersion version =
                new FormatVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        if (version.major() != supported.major()) {
            throw new FileFormatException("Unsupported format version " + version + ": this application reads "
                    + supported.major() + ".x files (it writes " + supported + ")");
        }
    }

    @Override
    public String toString() {
        return major + "." + minor;
    }
}
