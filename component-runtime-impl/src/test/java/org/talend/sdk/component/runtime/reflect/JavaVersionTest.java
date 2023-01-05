/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JavaVersionTest {

    @ParameterizedTest
    @CsvSource({ "1.5.0_22,5", "1.8.0_171,8", "8,8", "9.0.4,9", "10.0.2,10", "11.0.13,11", "15.0.2,15", "16.0.2,16",
            "17.0.5,17", "17.0.5+8-2,17", "17-ea,17", "20-ea,20" })
    void getMajor(final String version, final String expected) {
        assertEquals(Integer.parseInt(expected), JavaVersion.getMajorNumber(version));
    }

    @ParameterizedTest
    @CsvSource({ "1.5.0_22,0", "1.8.0_171,0", "8,-1", "9.0.4,0", "10.0.2,0", "11.1.13,1", "15.0.2,0", "16.0.2,0",
            "17.0.5,0", "17.0.5+8-2,0", "17-ea,ea", "20-ea,ea" })
    void minor(final String version, final String expected) {
        assertEquals(expected, JavaVersion.getMinor(version));
    }

    @ParameterizedTest
    @CsvSource({ "1.5.0_22,0", "1.8.0_171,0", "8,-1", "9.0.4,0", "10.0.2,0", "11.1.13,1", "15.0.2,0", "16.0.2,0",
            "17.0.5,0", "17.0.5+8-2,0", "17-ea,-1", "20-ea,-1" })
    void minorNumber(final String version, final String expected) {
        assertEquals(Integer.parseInt(expected), JavaVersion.getMinorNumber(version));
    }

    @ParameterizedTest
    @CsvSource({ "1.5.0_22,22", "1.8.0_171,171", "8,-1", "9.0.4,4", "10.0.2,2", "11.0.13,13", "15.0.2,2", "16.0.2,2",
            "17.0.5,5", "17.0.5+8-2,5+8-2", "17-ea,-1", "20-ea,-1" })
    void revision(final String version, final String expected) {
        assertEquals(expected, JavaVersion.getRevision(version));
    }

    @ParameterizedTest
    @CsvSource({ "1.5.0_22,22", "1.8.0_171,171", "8,-1", "9.0.4,4", "10.0.2,2", "11.0.13,13", "15.0.2,2", "16.0.2,2",
            "17.0.5,5", "17.0.5+8-2,5", "17-ea,-1", "20-ea,-1" })
    void revisionNumber(final String version, final String expected) {
        assertEquals(Integer.parseInt(expected), JavaVersion.getRevisionNumber(version));
    }
}