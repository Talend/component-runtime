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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FormatVersionTest {

    private static final FormatVersion SUPPORTED = new FormatVersion(1, 0);

    @ParameterizedTest
    @ValueSource(strings = { "1.0", "1.7", " 1.2 ", "1.0000" })
    void everyVersionOfTheSupportedMajorIsAccepted(final String declared) {
        assertDoesNotThrow(() -> FormatVersion.verify(declared, SUPPORTED));
    }

    @ParameterizedTest
    @ValueSource(strings = { "2.0", "0.9", "10.0" })
    void anotherMajorIsRefusedAndTheMessageNamesBothVersions(final String declared) {
        final FileFormatException error =
                assertThrows(FileFormatException.class, () -> FormatVersion.verify(declared, SUPPORTED));

        assertEquals("Unsupported format version " + declared + ": this application reads 1.x files (it writes 1.0)",
                error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "1", "v1.0", "1.x", "-1.0", "1.0.0", "1.", ".0", "one.two", "12345.0" })
    void aMalformedVersionIsRefused(final String declared) {
        final FileFormatException error =
                assertThrows(FileFormatException.class, () -> FormatVersion.verify(declared, SUPPORTED));

        assertTrue(error.getMessage().contains("<major>.<minor>"), error.getMessage());
    }

    @Test
    void aMissingVersionIsRefused() {
        assertEquals("The 'format-version' is missing",
                assertThrows(FileFormatException.class, () -> FormatVersion.verify(null, SUPPORTED)).getMessage());
    }

    @Test
    void aVersionPrintsAsMajorDotMinorAndCannotBeNegative() {
        assertEquals("1.12", new FormatVersion(1, 12).toString());
        assertThrows(IllegalArgumentException.class, () -> new FormatVersion(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new FormatVersion(1, -1));
    }
}
